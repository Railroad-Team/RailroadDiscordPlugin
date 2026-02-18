package dev.railroadide.discordplugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.railroadide.discordplugin.activity.DiscordActivity;
import dev.railroadide.discordplugin.core.DiscordCore;
import dev.railroadide.logger.Logger;
import dev.railroadide.railroad.Services;
import dev.railroadide.railroad.localization.L18n;
import dev.railroadide.railroad.plugin.spi.Plugin;
import dev.railroadide.railroad.plugin.spi.PluginContext;
import dev.railroadide.railroad.plugin.spi.dto.Document;
import dev.railroadide.railroad.plugin.spi.dto.Project;
import dev.railroadide.railroad.plugin.spi.event.EventBus;
import dev.railroadide.railroad.plugin.spi.events.EnterDefaultStateEvent;
import dev.railroadide.railroad.plugin.spi.events.FileEvent;
import dev.railroadide.railroad.plugin.spi.events.ProjectEvent;
import dev.railroadide.railroad.plugin.spi.services.ApplicationInfoService;
import dev.railroadide.railroad.plugin.spi.services.DocumentEditorStateService;
import dev.railroadide.railroad.plugin.spi.services.IDEStateService;
import dev.railroadide.railroad.settings.DefaultSettingCodecs;
import dev.railroadide.railroad.settings.Setting;
import dev.railroadide.railroad.settings.SettingCategory;
import dev.railroadide.railroad.settings.handler.SettingsHandler;
import dev.railroadide.railroad.utility.javafx.ComboBoxConverter;
import lombok.Getter;

import java.nio.file.Files;

public class DiscordPlugin implements Plugin {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private static final long DEFAULT_CLIENT_ID = 853387211897700394L;
    @Getter
    public static Logger logger;

    private DiscordCore discordCore;

    @SuppressWarnings("unchecked")
    @Override
    public void onEnable(PluginContext context) {
        if (context == null)
            throw new IllegalArgumentException("PluginContext cannot be null");

        Setting<Long> discordId = (Setting<Long>) SettingsHandler.SETTINGS_REGISTRY.register("discord:client_id", Setting.builder(Long.class, "discord:client_id")
                .treePath("plugins.discord")
                .category(SettingCategory.simple("railroad:plugins.discord"))
                .description("discord.setting.client_id.description")
                .codec(DefaultSettingCodecs.LONG)
                .defaultValue(DEFAULT_CLIENT_ID)
                .build());

        Setting<Boolean> shouldReconnectOnActivityUpdate = (Setting<Boolean>) SettingsHandler.SETTINGS_REGISTRY.register("discord:reconnect_on_activity_update", Setting.builder(Boolean.class, "discord:reconnect_on_activity_update")
                .treePath("plugins.discord")
                .category(SettingCategory.simple("railroad:plugins.discord"))
                .description("discord.setting.reconnect_on_activity_update.description")
                .codec(DefaultSettingCodecs.BOOLEAN)
                .defaultValue(true)
                .build());

        Setting<Integer> hideAfterMinutes = (Setting<Integer>) SettingsHandler.SETTINGS_REGISTRY.register("discord:hide_after_minutes", Setting.builder(Integer.class, "discord:hide_after_minutes")
                .treePath("plugins.discord")
                .category(SettingCategory.simple("railroad:plugins.discord"))
                .description("discord.setting.hide_after_minutes.description")
                .codec(DefaultSettingCodecs.INTEGER)
                .defaultValue(20)
                .build());

        Setting<DisplayMode> displayMode = (Setting<DisplayMode>) SettingsHandler.SETTINGS_REGISTRY.register("discord:display_mode", Setting.builder(DisplayMode.class, "discord:display_mode")
                .treePath("plugins.discord")
                .category(SettingCategory.simple("railroad:plugins.discord"))
                .description("discord.setting.display_mode.description")
                .codec(DefaultSettingCodecs.ofEnum("discord:display_mode", DisplayMode.class, DisplayMode::name, name -> {
                    try {
                        return DisplayMode.valueOf(name);
                    } catch (IllegalArgumentException ignored) {
                        return DisplayMode.DOCUMENT;
                    }
                }, new ComboBoxConverter<>(DisplayMode::getTranslationKey, name -> {
                    DisplayMode mode = DisplayMode.fromTranslationKey(name);
                    return mode != null ? mode : DisplayMode.DOCUMENT;
                })))
                .defaultValue(DisplayMode.DOCUMENT)
                .build());

        logger = context.getLogger();

        EventBus eventBus = context.getEventBus();
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
                        .details(L18n.localize("discord.activity.details.working_on", project.getAlias()))
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

            if (event.isActivatedEvent()) {
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
                    name = Files.exists(file.getPath()) ? file.getPath().getFileName().toString() : L18n.localize("discord.activity.details.unknown_file");
                }

                String projectAlias = ideState.getCurrentProject() != null ? ideState.getCurrentProject().getAlias() : L18n.localize("discord.activity.details.invalid_project");

                DiscordActivity.builder()
                        .playing()
                        .state(applicationInfo.getVersion())
                        .details(L18n.localize("discord.activity.details.in_project", projectAlias) + "\n" +
                                L18n.localize("discord.activity.details.editing_file", name))
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
                        .details(L18n.localize("discord.activity.details.modding_minecraft"))
                        .startNow()
                        .largeImage("logo")
                        .buildAndSet(discordCore.getActivityManager());
            }
        });

        try {
            discordCore = new DiscordCore(String.valueOf(discordId.getValue()), shouldReconnectOnActivityUpdate::getValue);
            discordCore.connect();

            logger.info("Discord integration started successfully with client ID: " + discordId.getValue());

            DiscordActivity.builder()
                    .playing()
                    .state(context.getService(ApplicationInfoService.class).getVersion())
                    .details(L18n.localize("discord.activity.details.modding_minecraft"))
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

        try {
            SettingsHandler.SETTINGS_REGISTRY.unregister("discord:client_id");
            SettingsHandler.SETTINGS_REGISTRY.unregister("discord:reconnect_on_activity_update");
            SettingsHandler.SETTINGS_REGISTRY.unregister("discord:hide_after_minutes");
            SettingsHandler.SETTINGS_REGISTRY.unregister("discord:display_mode");
        } catch (Exception ignored) {
        }
    }
}