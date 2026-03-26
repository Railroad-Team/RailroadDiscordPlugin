package dev.railroadide.discordplugin.activity;

import dev.railroadide.discordplugin.settings.DiscordPluginSettings;
import dev.railroadide.discordplugin.activity.discord.DiscordActivity;

public class ActivityBuilder {
    private final ActivityState activityState;
    private final DiscordPluginSettings settings;

    private final DiscordActivity.Builder builder = DiscordActivity.builder();

    public ActivityBuilder(ActivityState activityState, DiscordPluginSettings settings) {
        this.activityState = activityState;
        this.settings = settings;
    }

    public DiscordActivity build(ActivityManager manager) {
        DiscordActivity activity = this.builder.build();
        manager.publishActivity(activity);
        return activity;
    }
}
