package dev.railroadide.discordplugin.settings.ui;

import dev.railroadide.discordplugin.DiscordPlugin;

import java.util.function.Function;

public record ActivityVariable(String key, String translationKey,
                               String descriptionTranslationKey,
                               VariableContext context,
                               Function<VariableFetchContext, String> valueSupplier) {
    public String fetch(VariableFetchContext variableContext) {
        try {
            return this.valueSupplier.apply(variableContext);
        } catch (Exception exception) {
            DiscordPlugin.getLogger().warn("Failed to fetch variable value for key '{}'", this.key, exception);
            return "";
        }
    }
}
