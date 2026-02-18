package dev.railroadide.discordplugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.railroadide.discordplugin.activity.DiscordActivity;
import dev.railroadide.discordplugin.activity.InactivityManager;
import dev.railroadide.discordplugin.core.DiscordCore;
import dev.railroadide.logger.Logger;
import dev.railroadide.railroad.localization.L18n;
import dev.railroadide.railroad.plugin.spi.Plugin;
import dev.railroadide.railroad.plugin.spi.PluginContext;
import dev.railroadide.railroad.plugin.spi.dto.Document;
import dev.railroadide.railroad.plugin.spi.dto.Project;
import dev.railroadide.railroad.plugin.spi.event.EventBus;
import dev.railroadide.railroad.plugin.spi.events.EnterDefaultStateEvent;
import dev.railroadide.railroad.plugin.spi.events.FileEvent;
import dev.railroadide.railroad.plugin.spi.events.FileModifiedEvent;
import dev.railroadide.railroad.plugin.spi.events.ProjectEvent;
import dev.railroadide.railroad.plugin.spi.services.ApplicationInfoService;
import dev.railroadide.railroad.plugin.spi.services.IDEStateService;
import lombok.Getter;

import java.nio.file.Files;

public class DiscordPlugin implements Plugin {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    @Getter
    public static Logger logger;

    private DiscordCore discordCore;
    private InactivityManager inactivityManager;
    private DiscordPluginSettings settings;

    @Override
    public void onEnable(PluginContext context) {
        if (context == null)
            throw new IllegalArgumentException("PluginContext cannot be null");

        this.settings = new DiscordPluginSettings();

        logger = context.getLogger();

        this.inactivityManager = new InactivityManager(discordCore, logger);

        this.inactivityManager.setHideAfterMinutesSupplier(() -> {
            Integer configuredValue = this.settings.hideAfterMinutes.getValue();
            if (configuredValue == null)
                return 0;

            return Math.max(0, configuredValue);
        });
        this.inactivityManager.initializeInactivityTracking();

        ApplicationInfoService applicationInfo = context.getService(ApplicationInfoService.class);
        if (applicationInfo == null)
            throw new IllegalStateException("ApplicationInfoService is required for DiscordPlugin to function");

        EventBus eventBus = context.getEventBus();
        eventBus.subscribe(ProjectEvent.class, event -> {
            if (discordCore == null)
                return;

            if (event.isOpened()) {
                Project project = event.project();
                if (project == null) {
                    logger.warn("Project is null, cannot update Discord activity.");
                    return;
                }

                DiscordActivity activity = DiscordActivity.builder()
                        .playing()
                        .state(applicationInfo.getVersion())
                        .details(L18n.localize("discord.activity.details.working_on", project.getAlias()))
                        .startNow()
                        .largeImage("logo")
                        .build();
                this.inactivityManager.publishActivity(activity);
            } else if (event.isClosed()) {
                this.inactivityManager.clearAndForgetActivity();
            }
        });

        eventBus.subscribe(FileEvent.class, event -> {
            if (discordCore == null)
                return;

            if (event.isActivatedEvent()) {
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

                DiscordActivity activity = DiscordActivity.builder()
                        .playing()
                        .state(applicationInfo.getVersion())
                        .details(L18n.localize("discord.activity.details.in_project", projectAlias) + "\n" +
                                L18n.localize("discord.activity.details.editing_file", name))
                        .startNow()
                        .largeImage("logo") // TODO: Set the image to the file type
                        .build();
                this.inactivityManager.publishActivity(activity);
            }
        });

        eventBus.subscribe(FileModifiedEvent.class, event -> this.inactivityManager.markUserInteraction());

        eventBus.subscribe(EnterDefaultStateEvent.class, event -> {
            if (discordCore != null) {
                DiscordActivity activity = DiscordActivity.builder()
                        .playing()
                        .state(applicationInfo.getVersion())
                        .details(L18n.localize("discord.activity.details.modding_minecraft"))
                        .startNow()
                        .largeImage("logo")
                        .build();
                this.inactivityManager.publishActivity(activity);
            }
        });

        try {
            discordCore = new DiscordCore(String.valueOf(this.settings.discordId.getValue()), this.settings.shouldReconnectOnActivityUpdate::getValue);
            discordCore.connect();

            logger.info("Discord integration started successfully with client ID: " + this.settings.discordId.getValue());

            DiscordActivity activity = DiscordActivity.builder()
                    .playing()
                    .state(applicationInfo.getVersion())
                    .details(L18n.localize("discord.activity.details.modding_minecraft"))
                    .startNow()
                    .largeImage("logo")
                    .build();
            this.inactivityManager.publishActivity(activity);

            this.settings.discordId.addListener((oldValue, newValue) -> {
                if (discordCore != null) {
                    discordCore.setClientId(String.valueOf(newValue));
                }
            });

            this.settings.hideAfterMinutes.addListener((oldValue, newValue) -> {
                if (newValue == null || newValue <= 0) {
                    this.inactivityManager.cancelHideActivityTask();
                    this.inactivityManager.restoreActivityIfHidden();
                    return;
                }

                this.inactivityManager.markUserInteraction();
            });
        } catch (Exception exception) {
            logger.error("Failed to start Discord integration", exception);
            onDisable(context);
        }
    }

    @Override
    public void onDisable(PluginContext context) {
        if (this.inactivityManager != null) {
            this.inactivityManager.shutdownInactivityTracking();
            this.inactivityManager = null;
        }

        if (discordCore != null) {
            discordCore.close();
            discordCore = null;
        }

        try {
            this.settings.unregisterSettings();
            this.settings = null;
        } catch (Exception ignored) {
        }
    }
}
