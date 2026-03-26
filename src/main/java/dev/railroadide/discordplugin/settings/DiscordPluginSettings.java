package dev.railroadide.discordplugin.settings;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.railroadide.discordplugin.DisplayMode;
import dev.railroadide.discordplugin.settings.ui.DisplayLayoutPane;
import dev.railroadide.railroad.settings.DefaultSettingCodecs;
import dev.railroadide.railroad.settings.Setting;
import dev.railroadide.railroad.settings.SettingCategory;
import dev.railroadide.railroad.settings.SettingCodec;
import dev.railroadide.railroad.settings.handler.SettingsHandler;
import dev.railroadide.railroad.utility.javafx.ComboBoxConverter;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class DiscordPluginSettings {
    private static final long DEFAULT_CLIENT_ID = 853387211897700394L;
    private static final SettingCodec<Map<DisplayMode, DisplayContent>, DisplayLayoutPane> DISPLAY_CONTENT_CODEC =
            SettingCodec.<Map<DisplayMode, DisplayContent>, DisplayLayoutPane>builder("discord:display_content")
                    .nodeToValue(DisplayLayoutPane::toMap)
                    .valueToNode((map, pane) -> pane.load(map))
                    .jsonEncoder(map -> {
                        var array = new JsonArray();
                        for (Map.Entry<DisplayMode, DisplayContent> entry : map.entrySet()) {
                            var obj = new JsonObject();
                            obj.add(entry.getKey().name(), entry.getValue().toJson());
                            array.add(obj);
                        }

                        return array;
                    })
                    .jsonDecoder(json -> {
                        Map<DisplayMode, DisplayContent> map = new HashMap<>();
                        if (json.isJsonArray()) {
                            for (JsonElement element : json.getAsJsonArray()) {
                                if (element.isJsonObject()) {
                                    JsonObject obj = element.getAsJsonObject();
                                    for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                                        DisplayMode mode = DisplayMode.fromName(entry.getKey());
                                        if (mode != null) {
                                            DisplayContent content = DisplayContent.fromJson(entry.getValue().getAsJsonObject());
                                            map.put(mode, content);
                                        }
                                    }
                                }
                            }
                        }

                        return map;
                    })
                    .createNode(map -> {
                        var pane = new DisplayLayoutPane();
                        pane.load(map);
                        return pane;
                    })
                    .build();


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

    public final Setting<Map<DisplayMode, DisplayContent>> displayContent = (Setting<Map<DisplayMode, DisplayContent>>) SettingsHandler.SETTINGS_REGISTRY.register("discord:display_content", Setting.builder((Class<Map<DisplayMode, DisplayContent>>) (Class<?>) Map.class, "discord:display_content")
            .treePath("plugins.discord")
            .category(SettingCategory.simple("railroad:plugins.discord"))
            .description("discord.setting.display_content.description")
            .codec(DISPLAY_CONTENT_CODEC)
            .defaultValue(DisplayContent.DEFAULT_VALUES)
            .build());

    public void unregisterSettings() {
        SettingsHandler.SETTINGS_REGISTRY.unregister("discord:client_id");
        SettingsHandler.SETTINGS_REGISTRY.unregister("discord:reconnect_on_activity_update");
        SettingsHandler.SETTINGS_REGISTRY.unregister("discord:hide_after_minutes");
        SettingsHandler.SETTINGS_REGISTRY.unregister("discord:display_mode");
    }
}
