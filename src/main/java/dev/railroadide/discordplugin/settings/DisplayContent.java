package dev.railroadide.discordplugin.settings;

import com.google.gson.JsonObject;
import dev.railroadide.discordplugin.DisplayMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class DisplayContent {
    public static final Map<DisplayMode, DisplayContent> DEFAULT_VALUES = Map.of();

    private String firstLine;
    private String secondLine;
    private String largeIconKey;
    private String largeIconText;
    private String smallIconKey;
    private String smallIconText;
    private ElapsedTimeType elapsedTimeType;
    private String button1Label;
    private String button1Url;
    private String button2Label;
    private String button2Url;

    public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("firstLine", firstLine);
        json.addProperty("secondLine", secondLine);
        json.addProperty("largeIconKey", largeIconKey);
        json.addProperty("largeIconText", largeIconText);
        json.addProperty("smallIconKey", smallIconKey);
        json.addProperty("smallIconText", smallIconText);
        json.addProperty("elapsedTimeType", elapsedTimeType != null ? elapsedTimeType.name() : null);
        json.addProperty("button1Label", button1Label);
        json.addProperty("button1Url", button1Url);
        json.addProperty("button2Label", button2Label);
        json.addProperty("button2Url", button2Url);
        return json;
    }

    public static DisplayContent fromJson(JsonObject json) {
        if (json == null)
            return new DisplayContent();

        String firstLine = json.has("firstLine") ? json.get("firstLine").getAsString() : null;
        String secondLine = json.has("secondLine") ? json.get("secondLine").getAsString() : null;
        String largeIcon = json.has("largeIconKey") ? json.get("largeIconKey").getAsString() : null;
        String largeIconText = json.has("largeIconText") ? json.get("largeIconText").getAsString() : null;
        String smallIcon = json.has("smallIconKey") ? json.get("smallIconKey").getAsString() : null;
        String smallIconText = json.has("smallIconText") ? json.get("smallIconText").getAsString() : null;
        ElapsedTimeType elapsedTimeType = null;
        if (json.has("elapsedTimeType") && !json.get("elapsedTimeType").isJsonNull()) {
            try {
                elapsedTimeType = ElapsedTimeType.valueOf(json.get("elapsedTimeType").getAsString());
            } catch (IllegalArgumentException ignored) {
            }
        }
        String button1Label = json.has("button1Label") ? json.get("button1Label").getAsString() : null;
        String button1Url = json.has("button1Url") ? json.get("button1Url").getAsString() : null;
        String button2Label = json.has("button2Label") ? json.get("button2Label").getAsString() : null;
        String button2Url = json.has("button2Url") ? json.get("button2Url").getAsString() : null;

        return new DisplayContent(firstLine, secondLine, largeIcon, largeIconText, smallIcon, smallIconText, elapsedTimeType, button1Label, button1Url, button2Label, button2Url);
    }

    @Getter
    public enum ElapsedTimeType {
        APPLICATION_START,
        PROJECT_OPEN,
        FILE_OPEN,
        HIDE;

        private final String localizationKey = "discord.elapsed_time." + name().toLowerCase();
    }
}
