package dev.railroadide.discordplugin;

import lombok.Getter;

import java.util.Locale;

@Getter
public enum DisplayMode {
    APPLICATION,
    PROJECT,
    DOCUMENT;

    private final String translationKey;

    DisplayMode() {
        this.translationKey = "discord.display_mode." + name().toLowerCase(Locale.ROOT);
    }

    public static DisplayMode fromTranslationKey(String key) {
        for (DisplayMode mode : values()) {
            if (mode.getTranslationKey().equals(key)) {
                return mode;
            }
        }

        return null;
    }
}
