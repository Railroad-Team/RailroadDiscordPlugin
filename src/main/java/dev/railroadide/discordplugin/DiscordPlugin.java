package dev.railroadide.discordplugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.railroadide.discordplugin.activity.DiscordActivity;
import dev.railroadide.discordplugin.core.DiscordCore;
import dev.railroadide.logger.Logger;
import dev.railroadide.railroad.Railroad;
import dev.railroadide.railroad.localization.L18n;
import dev.railroadide.railroad.plugin.spi.Plugin;
import dev.railroadide.railroad.plugin.spi.PluginContext;
import dev.railroadide.railroad.plugin.spi.dto.Document;
import dev.railroadide.railroad.plugin.spi.dto.Project;
import dev.railroadide.railroad.plugin.spi.event.EventBus;
import dev.railroadide.railroad.plugin.spi.event.EventListener;
import dev.railroadide.railroad.plugin.spi.events.EnterDefaultStateEvent;
import dev.railroadide.railroad.plugin.spi.events.FileEvent;
import dev.railroadide.railroad.plugin.spi.events.FileModifiedEvent;
import dev.railroadide.railroad.plugin.spi.events.ProjectEvent;
import dev.railroadide.railroad.plugin.spi.events.input.GenericKeyEvent;
import dev.railroadide.railroad.plugin.spi.events.input.GenericMouseEvent;
import dev.railroadide.railroad.plugin.spi.services.ApplicationInfoService;
import dev.railroadide.railroad.plugin.spi.services.IDEStateService;
import dev.railroadide.railroad.settings.DefaultSettingCodecs;
import dev.railroadide.railroad.settings.Setting;
import dev.railroadide.railroad.settings.SettingCategory;
import dev.railroadide.railroad.settings.handler.SettingsHandler;
import dev.railroadide.railroad.utility.javafx.ComboBoxConverter;
import javafx.application.Platform;
import lombok.Getter;

import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntSupplier;

public class DiscordPlugin implements Plugin {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private static final long DEFAULT_CLIENT_ID = 853387211897700394L;
    @Getter
    public static Logger logger;

    private DiscordCore discordCore;
    private ScheduledExecutorService inactivityScheduler;
    private ScheduledFuture<?> hideActivityTask;
    private final AtomicLong inactivityToken = new AtomicLong();
    private EventListener<GenericKeyEvent> keyEventHandler;
    private EventListener<GenericMouseEvent> mouseEventHandler;
    private IntSupplier hideAfterMinutesSupplier = () -> 0;
    private volatile DiscordActivity lastKnownActivity;
    private volatile boolean activityHiddenByInactivity;

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
        this.hideAfterMinutesSupplier = () -> {
            Integer configuredValue = hideAfterMinutes.getValue();
            if (configuredValue == null)
                return 0;

            return Math.max(0, configuredValue);
        };
        initializeInactivityTracking();

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

                DiscordActivity activity = DiscordActivity.builder()
                        .playing()
                        .state(applicationInfo.getVersion())
                        .details(L18n.localize("discord.activity.details.working_on", project.getAlias()))
                        .startNow()
                        .largeImage("logo")
                        .build();
                publishActivity(activity);
            } else if (event.isClosed()) {
                clearAndForgetActivity();
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

                DiscordActivity activity = DiscordActivity.builder()
                        .playing()
                        .state(applicationInfo.getVersion())
                        .details(L18n.localize("discord.activity.details.in_project", projectAlias) + "\n" +
                                L18n.localize("discord.activity.details.editing_file", name))
                        .startNow()
                        .largeImage("logo") // TODO: Set the image to the file type
                        .build();
                publishActivity(activity);
            }
        });

        eventBus.subscribe(FileModifiedEvent.class, event -> markUserInteraction());

        eventBus.subscribe(EnterDefaultStateEvent.class, event -> {
            if (discordCore != null) {
                ApplicationInfoService applicationInfo = context.getService(ApplicationInfoService.class);
                if (applicationInfo == null) {
                    logger.warn("ApplicationInfoService is not available, cannot update Discord activity.");
                    return;
                }

                DiscordActivity activity = DiscordActivity.builder()
                        .playing()
                        .state(applicationInfo.getVersion())
                        .details(L18n.localize("discord.activity.details.modding_minecraft"))
                        .startNow()
                        .largeImage("logo")
                        .build();
                publishActivity(activity);
            }
        });

        try {
            discordCore = new DiscordCore(String.valueOf(discordId.getValue()), shouldReconnectOnActivityUpdate::getValue);
            discordCore.connect();

            logger.info("Discord integration started successfully with client ID: " + discordId.getValue());

            DiscordActivity activity = DiscordActivity.builder()
                    .playing()
                    .state(context.getService(ApplicationInfoService.class).getVersion())
                    .details(L18n.localize("discord.activity.details.modding_minecraft"))
                    .startNow()
                    .largeImage("logo")
                    .build();
            publishActivity(activity);

            discordId.addListener((oldValue, newValue) -> {
                if (discordCore != null) {
                    discordCore.setClientId(String.valueOf(newValue));
                }
            });

            hideAfterMinutes.addListener((oldValue, newValue) -> {
                if (newValue == null || newValue <= 0) {
                    cancelHideActivityTask();
                    restoreActivityIfHidden();
                    return;
                }

                markUserInteraction();
            });
        } catch (Exception exception) {
            logger.error("Failed to start Discord integration", exception);
            discordCore = null;
            shutdownInactivityTracking();
        }
    }

    private void initializeInactivityTracking() {
        this.inactivityScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "discord-inactivity-tracker");
            thread.setDaemon(true);
            return thread;
        });

        registerInputListeners();
    }

    private void registerInputListeners() {
        Runnable register = () -> {
            this.keyEventHandler = event -> markUserInteraction();
            this.mouseEventHandler = event -> markUserInteraction();
            Railroad.EVENT_BUS.subscribe(GenericKeyEvent.class, this.keyEventHandler);
            Railroad.EVENT_BUS.subscribe(GenericMouseEvent.class, this.mouseEventHandler);
        };

        try {
            if (Platform.isFxApplicationThread()) {
                register.run();
            } else {
                Platform.runLater(register);
            }
        } catch (IllegalStateException exception) {
            logger.warn("JavaFX is not initialized, keyboard and mouse inactivity tracking is disabled.", exception);
        }
    }

    private void unregisterInputListeners() {
        Runnable unregister = () -> {
            if (this.keyEventHandler != null) {
                Railroad.EVENT_BUS.unsubscribe(GenericKeyEvent.class, this.keyEventHandler);
                this.keyEventHandler = null;
            }

            if (this.mouseEventHandler != null) {
                Railroad.EVENT_BUS.unsubscribe(GenericMouseEvent.class, this.mouseEventHandler);
                this.mouseEventHandler = null;
            }
        };

        try {
            if (Platform.isFxApplicationThread()) {
                unregister.run();
            } else {
                Platform.runLater(unregister);
            }
        } catch (IllegalStateException ignored) {
            this.keyEventHandler = null;
            this.mouseEventHandler = null;
        }
    }

    private void publishActivity(DiscordActivity activity) {
        DiscordCore core = this.discordCore;
        if (core == null)
            return;

        synchronized (this) {
            this.lastKnownActivity = activity;
            this.activityHiddenByInactivity = false;
        }

        // Reset/invalidate any existing hide task before publishing, so a stale timer
        // cannot clear the freshly published activity.
        scheduleHideActivityTask();
        core.getActivityManager().updateActivity(activity);
    }

    private void markUserInteraction() {
        DiscordCore core = this.discordCore;
        if (core == null)
            return;

        DiscordActivity activityToRestore = null;
        boolean hasVisibleActivity;
        synchronized (this) {
            hasVisibleActivity = this.lastKnownActivity != null;
            if (this.activityHiddenByInactivity && this.lastKnownActivity != null) {
                this.activityHiddenByInactivity = false;
                activityToRestore = this.lastKnownActivity;
            }
        }

        if (activityToRestore != null) {
            // Reset/invalidate any existing hide task before restoring, so a stale timer
            // cannot clear the restored activity.
            scheduleHideActivityTask();
            core.getActivityManager().updateActivity(activityToRestore);
            return;
        }

        if (!hasVisibleActivity)
            return;

        scheduleHideActivityTask();
    }

    private synchronized void scheduleHideActivityTask() {
        cancelHideActivityTaskLocked();

        int hideAfterMinutes = this.hideAfterMinutesSupplier.getAsInt();
        if (hideAfterMinutes <= 0 || this.inactivityScheduler == null)
            return;

        long token = this.inactivityToken.incrementAndGet();
        this.hideActivityTask = this.inactivityScheduler.schedule(() -> hideActivityForInactivity(token), hideAfterMinutes, TimeUnit.MINUTES);
    }

    private void hideActivityForInactivity(long token) {
        DiscordCore core = this.discordCore;
        if (core == null)
            return;

        synchronized (this) {
            if (this.inactivityToken.get() != token)
                return;

            this.activityHiddenByInactivity = true;
            this.hideActivityTask = null;
        }

        core.getActivityManager().clearActivity();
    }

    private void restoreActivityIfHidden() {
        DiscordCore core = this.discordCore;
        if (core == null)
            return;

        DiscordActivity activityToRestore = null;
        synchronized (this) {
            if (this.activityHiddenByInactivity && this.lastKnownActivity != null) {
                this.activityHiddenByInactivity = false;
                activityToRestore = this.lastKnownActivity;
            }
        }

        if (activityToRestore != null) {
            core.getActivityManager().updateActivity(activityToRestore);
        }
    }

    private synchronized void clearAndForgetActivity() {
        cancelHideActivityTaskLocked();
        this.lastKnownActivity = null;
        this.activityHiddenByInactivity = false;

        if (this.discordCore != null) {
            this.discordCore.getActivityManager().clearActivity();
        }
    }

    private synchronized void cancelHideActivityTask() {
        cancelHideActivityTaskLocked();
    }

    private void cancelHideActivityTaskLocked() {
        this.inactivityToken.incrementAndGet();

        if (this.hideActivityTask == null)
            return;

        this.hideActivityTask.cancel(false);
        this.hideActivityTask = null;
    }

    private void shutdownInactivityTracking() {
        cancelHideActivityTask();

        if (this.inactivityScheduler != null) {
            this.inactivityScheduler.shutdownNow();
            this.inactivityScheduler = null;
        }

        unregisterInputListeners();
        this.lastKnownActivity = null;
        this.activityHiddenByInactivity = false;
    }

    @Override
    public void onDisable(PluginContext context) {
        shutdownInactivityTracking();

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
