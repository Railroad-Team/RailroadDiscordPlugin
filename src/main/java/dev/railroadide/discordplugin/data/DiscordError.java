package dev.railroadide.discordplugin.data;

import lombok.Getter;

@Getter
public class DiscordError {
    private int code;
    private String message;

    @Override
    public String toString() {
        return "Error " + getCode() + ": " + getMessage();
    }
}
