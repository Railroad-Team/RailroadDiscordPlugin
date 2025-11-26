package dev.railroadide.discordplugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.railroadide.core.localization.LocalizationService;
import dev.railroadide.core.localization.LocalizationServiceLocator;
import dev.railroadide.core.registry.Registry;
import dev.railroadide.core.settings.DefaultSettingCodecs;
import dev.railroadide.core.settings.Setting;
import dev.railroadide.core.settings.SettingCategory;
import dev.railroadide.core.utility.ComboBoxConverter;
import dev.railroadide.discordplugin.activity.DiscordActivity;
import dev.railroadide.discordplugin.core.DiscordCore;
import dev.railroadide.logger.Logger;
import dev.railroadide.railroadpluginapi.Plugin;
import dev.railroadide.railroadpluginapi.PluginContext;
import dev.railroadide.railroadpluginapi.Registries;
import dev.railroadide.railroadpluginapi.dto.Document;
import dev.railroadide.railroadpluginapi.dto.Project;
import dev.railroadide.railroadpluginapi.event.EventBus;
import dev.railroadide.railroadpluginapi.events.EnterDefaultStateEvent;
import dev.railroadide.railroadpluginapi.events.FileEvent;
import dev.railroadide.railroadpluginapi.events.ProjectEvent;
import dev.railroadide.railroadpluginapi.services.ApplicationInfoService;
import dev.railroadide.railroadpluginapi.services.IDEStateService;
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

        Registry<Setting<?>> settingRegistry = Registries.getSettingsRegistry(context);
        Setting<Long> discordId = (Setting<Long>) settingRegistry.register("discord:client_id", Setting.builder(Long.class, "discord:client_id")
                .treePath("plugins.discord")
                .category(SettingCategory.simple("railroad:plugins.discord"))
                .description("discord.setting.client_id.description")
                .codec(DefaultSettingCodecs.LONG)
                .defaultValue(DEFAULT_CLIENT_ID)
                .build());

        Setting<Boolean> shouldReconnectOnActivityUpdate = (Setting<Boolean>) settingRegistry.register("discord:reconnect_on_activity_update", Setting.builder(Boolean.class, "discord:reconnect_on_activity_update")
                .treePath("plugins.discord")
                .category(SettingCategory.simple("railroad:plugins.discord"))
                .description("discord.setting.reconnect_on_activity_update.description")
                .codec(DefaultSettingCodecs.BOOLEAN)
                .defaultValue(true)
                .build());

        Setting<Integer> hideAfterMinutes = (Setting<Integer>) settingRegistry.register("discord:hide_after_minutes", Setting.builder(Integer.class, "discord:hide_after_minutes")
                .treePath("plugins.discord")
                .category(SettingCategory.simple("railroad:plugins.discord"))
                .description("discord.setting.hide_after_minutes.description")
                .codec(DefaultSettingCodecs.INTEGER)
                .defaultValue(20)
                .build());

        Setting<DisplayMode> displayMode = (Setting<DisplayMode>) settingRegistry.register("discord:display_mode", Setting.builder(DisplayMode.class, "discord:display_mode")
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
            discordCore = new DiscordCore(String.valueOf(discordId.getValue()), shouldReconnectOnActivityUpdate::getValue);
            discordCore.connect();

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
            settingRegistry.unregister("discord:reconnect_on_activity_update");
            settingRegistry.unregister("discord:hide_after_minutes");
            settingRegistry.unregister("discord:display_mode");
        } catch (Exception ignored) {
        }
    }
}