package dev.railroadide.discordplugin.activity;

import dev.railroadide.discordplugin.core.DiscordCore;
import dev.railroadide.logger.Logger;
import dev.railroadide.railroad.Railroad;
import dev.railroadide.railroad.plugin.spi.event.EventListener;
import dev.railroadide.railroad.plugin.spi.events.input.GenericKeyEvent;
import dev.railroadide.railroad.plugin.spi.events.input.GenericMouseEvent;
import javafx.application.Platform;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntSupplier;

public class InactivityManager {
    private final DiscordCore discordCore;
    private final Logger logger;

    private ScheduledExecutorService inactivityScheduler;
    private ScheduledFuture<?> hideActivityTask;
    private final AtomicLong inactivityToken = new AtomicLong();
    private EventListener<GenericKeyEvent> keyEventHandler;
    private EventListener<GenericMouseEvent> mouseEventHandler;
    private IntSupplier hideAfterMinutesSupplier = () -> 0;
    private volatile DiscordActivity lastKnownActivity;
    private volatile boolean activityHiddenByInactivity;

    public InactivityManager(DiscordCore discordCore, Logger logger) {
        this.discordCore = discordCore;
        this.logger = logger;
    }

    public void setHideAfterMinutesSupplier(IntSupplier supplier) {
        if (supplier == null) {
            supplier = () -> 0;
        }

        this.hideAfterMinutesSupplier = supplier;
    }

    public void initializeInactivityTracking() {
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

    public void publishActivity(DiscordActivity activity) {
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

    public void markUserInteraction() {
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

    public void restoreActivityIfHidden() {
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

    public synchronized void clearAndForgetActivity() {
        cancelHideActivityTaskLocked();
        this.lastKnownActivity = null;
        this.activityHiddenByInactivity = false;

        if (this.discordCore != null) {
            this.discordCore.getActivityManager().clearActivity();
        }
    }

    public synchronized void cancelHideActivityTask() {
        cancelHideActivityTaskLocked();
    }

    private void cancelHideActivityTaskLocked() {
        this.inactivityToken.incrementAndGet();

        if (this.hideActivityTask == null)
            return;

        this.hideActivityTask.cancel(false);
        this.hideActivityTask = null;
    }

    public void shutdownInactivityTracking() {
        cancelHideActivityTask();

        if (this.inactivityScheduler != null) {
            this.inactivityScheduler.shutdownNow();
            this.inactivityScheduler = null;
        }

        unregisterInputListeners();
        this.lastKnownActivity = null;
        this.activityHiddenByInactivity = false;
    }
}
