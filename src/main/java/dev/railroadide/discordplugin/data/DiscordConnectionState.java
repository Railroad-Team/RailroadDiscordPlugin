package dev.railroadide.discordplugin.data;

public enum DiscordConnectionState {
    HANDSHAKE,
    CONNECTED,
    ERROR;

    public static final DiscordConnectionState[] VALUES = values();
}
