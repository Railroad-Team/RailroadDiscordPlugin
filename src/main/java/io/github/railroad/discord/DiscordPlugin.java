package io.github.railroad.discord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.railroad.core.localization.LocalizationService;
import io.github.railroad.core.localization.LocalizationServiceLocator;
import io.github.railroad.core.registry.Registry;
import io.github.railroad.core.settings.DefaultSettingCodecs;
import io.github.railroad.core.settings.Setting;
import io.github.railroad.core.settings.SettingCategory;
import io.github.railroad.discord.activity.DiscordActivity;
import io.github.railroad.discord.core.DiscordCore;
import io.github.railroad.logger.Logger;
import io.github.railroad.railroadpluginapi.Plugin;
import io.github.railroad.railroadpluginapi.PluginContext;
import io.github.railroad.railroadpluginapi.Registries;
import io.github.railroad.railroadpluginapi.dto.Document;
import io.github.railroad.railroadpluginapi.dto.Project;
import io.github.railroad.railroadpluginapi.event.EventBus;
import io.github.railroad.railroadpluginapi.events.EnterDefaultStateEvent;
import io.github.railroad.railroadpluginapi.events.FileEvent;
import io.github.railroad.railroadpluginapi.events.ProjectEvent;
import io.github.railroad.railroadpluginapi.services.ApplicationInfoService;
import io.github.railroad.railroadpluginapi.services.IDEStateService;

import java.nio.file.Files;

public class DiscordPlugin implements Plugin {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private DiscordCore discordCore;

    @Override
    public void onEnable(PluginContext context) {
        if (context == null)
            throw new IllegalArgumentException("PluginContext cannot be null");

        Registry<Setting<?>> settingRegistry = Registries.getSettingsRegistry(context);
        // noinspection unchecked
        Setting<Long> discordId = (Setting<Long>) settingRegistry.register("discord:client_id", Setting.builder(Long.class, "discord:client_id")
                .treePath("plugins.discord")
                .category(SettingCategory.simple("railroad:plugins.discord"))
                .codec(DefaultSettingCodecs.LONG)
                .defaultValue(853387211897700394L) // Default client ID for Railroad Discord
                .build());

        Logger logger = context.getLogger(); // TODO: Use setLogger method, meaning we can have a constant logger

        EventBus eventBus = context.getEventBus();
        LocalizationService localizationService = LocalizationServiceLocator.getInstance();
        eventBus.subscribe(ProjectEvent.class, event -> {
            if (discordCore == null)
                return;

            if (event.isOpened()) {
                ApplicationInfoService applicationInfo = context.getService(ApplicationInfoService.class);
                if (applicationInfo == null) {
                    logger.warn("ApplicationInfoService is not available, cannot update Discord activity.");
                    return;
                }

                Project project = event.project();
                if (project == null) {
                    logger.warn("Project is null, cannot update Discord activity.");
                    return;
                }

                DiscordActivity.builder()
                        .playing()
                        .state(applicationInfo.getVersion())
                        .details(localizationService.get("discord.activity.details.working_on", project.getAlias()))
                        .startNow()
                        .largeImage("logo")
                        .buildAndSet(discordCore.getActivityManager());
            } else if (event.isClosed()) {
                discordCore.getActivityManager().clearActivity();
            }
        });

        eventBus.subscribe(FileEvent.class, event -> {
            if (discordCore == null)
                return;

            if (event.isActivated()) {
                ApplicationInfoService applicationInfo = context.getService(ApplicationInfoService.class);
                if (applicationInfo == null) {
                    logger.warn("ApplicationInfoService is not available, cannot update Discord activity.");
                    return;
                }

                IDEStateService ideState = context.getService(IDEStateService.class);
                if (ideState == null) {
                    logger.warn("IDEStateService is not available, cannot update Discord activity.");
                    return;
                }

                Document file = event.file();

                String name = file.getName();
                if (file.getName() == null) {
                    name = Files.exists(file.getPath()) ? file.getPath().getFileName().toString() : localizationService.get("discord.activity.details.unknown_file");
                }

                String projectAlias = ideState.getCurrentProject() != null ? ideState.getCurrentProject().getAlias() : localizationService.get("discord.activity.details.invalid_project");

                DiscordActivity.builder()
                        .playing()
                        .state(applicationInfo.getVersion())
                        .details(localizationService.get("discord.activity.details.in_project", projectAlias) + "\n" +
                                localizationService.get("discord.activity.details.editing_file", name))
                        .startNow()
                        .largeImage("logo") // TODO: Set the image to the file type
                        .buildAndSet(discordCore.getActivityManager());
            }
        });

        eventBus.subscribe(EnterDefaultStateEvent.class, event -> {
            if (discordCore != null) {
                ApplicationInfoService applicationInfo = context.getService(ApplicationInfoService.class);
                if (applicationInfo == null) {
                    logger.warn("ApplicationInfoService is not available, cannot update Discord activity.");
                    return;
                }

                DiscordActivity.builder()
                        .playing()
                        .state(applicationInfo.getVersion())
                        .details(localizationService.get("discord.activity.details.modding_minecraft"))
                        .startNow()
                        .largeImage("logo")
                        .buildAndSet(discordCore.getActivityManager());
            }
        });

        try {
            discordCore = new DiscordCore(String.valueOf(discordId.getValue()));
            discordCore.setLogger(logger);
            discordCore.start();

            logger.info("Discord integration started successfully with client ID: " + discordId.getValue());

            DiscordActivity.builder()
                    .playing()
                    .state(context.getService(ApplicationInfoService.class).getVersion())
                    .details(localizationService.get("discord.activity.details.modding_minecraft"))
                    .startNow()
                    .largeImage("logo")
                    .buildAndSet(discordCore.getActivityManager());

            discordId.addListener((oldValue, newValue) -> {
                if (discordCore != null) {
                    discordCore.setClientId(String.valueOf(newValue));
                }
            });
        } catch (Exception exception) {
            logger.error("Failed to start Discord integration", exception);
            discordCore = null;
        }
    }

    @Override
    public void onDisable(PluginContext context) {
        if (discordCore != null) {
            discordCore.close();
            discordCore = null;
        }

        Registry<Setting<?>> settingRegistry = Registries.getSettingsRegistry(context);
        try {
            settingRegistry.unregister("discord:client_id");
        } catch (Exception ignored) {}
    }
}