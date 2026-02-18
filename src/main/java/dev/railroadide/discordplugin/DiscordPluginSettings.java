package dev.railroadide.discordplugin;

import dev.railroadide.railroad.settings.DefaultSettingCodecs;
import dev.railroadide.railroad.settings.Setting;
import dev.railroadide.railroad.settings.SettingCategory;
import dev.railroadide.railroad.settings.handler.SettingsHandler;
import dev.railroadide.railroad.utility.javafx.ComboBoxConverter;

@SuppressWarnings("unchecked")
public final class DiscordPluginSettings {
    private static final long DEFAULT_CLIENT_ID = 853387211897700394L;

    public final Setting<Long> discordId = (Setting<Long>) SettingsHandler.SETTINGS_REGISTRY.register("discord:client_id", Setting.builder(Long.class, "discord:client_id")
            .treePath("plugins.discord")
            .category(SettingCategory.simple("railroad:plugins.discord"))
            .description("discord.setting.client_id.description")
            .codec(DefaultSettingCodecs.LONG)
            .defaultValue(DEFAULT_CLIENT_ID)
            .build());

    public final Setting<Boolean> shouldReconnectOnActivityUpdate = (Setting<Boolean>) SettingsHandler.SETTINGS_REGISTRY.register("discord:reconnect_on_activity_update", Setting.builder(Boolean.class, "discord:reconnect_on_activity_update")
            .treePath("plugins.discord")
            .category(SettingCategory.simple("railroad:plugins.discord"))
            .description("discord.setting.reconnect_on_activity_update.description")
            .codec(DefaultSettingCodecs.BOOLEAN)
            .defaultValue(true)
            .build());

    public final Setting<Integer> hideAfterMinutes = (Setting<Integer>) SettingsHandler.SETTINGS_REGISTRY.register("discord:hide_after_minutes", Setting.builder(Integer.class, "discord:hide_after_minutes")
            .treePath("plugins.discord")
            .category(SettingCategory.simple("railroad:plugins.discord"))
            .description("discord.setting.hide_after_minutes.description")
            .codec(DefaultSettingCodecs.INTEGER)
            .defaultValue(20)
            .build());

    public final Setting<DisplayMode> displayMode = (Setting<DisplayMode>) SettingsHandler.SETTINGS_REGISTRY.register("discord:display_mode", Setting.builder(DisplayMode.class, "discord:display_mode")
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

    public void unregisterSettings() {
        SettingsHandler.SETTINGS_REGISTRY.unregister("discord:client_id");
        SettingsHandler.SETTINGS_REGISTRY.unregister("discord:reconnect_on_activity_update");
        SettingsHandler.SETTINGS_REGISTRY.unregister("discord:hide_after_minutes");
        SettingsHandler.SETTINGS_REGISTRY.unregister("discord:display_mode");
    }
}
