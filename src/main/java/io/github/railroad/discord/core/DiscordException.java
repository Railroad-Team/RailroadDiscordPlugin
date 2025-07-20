package io.github.railroad.discord.core;

import io.github.railroad.discord.data.DiscordResult;
import lombok.Getter;

/**
 * Exception which is thrown when a {@link DiscordResult} that is not {@link DiscordResult#OK} occurs.
 */
@Getter
public class DiscordException extends RuntimeException {
    private final DiscordResult result;

    public DiscordException(DiscordResult result) {
        super("Discord error: " + result);
        this.result = result;
    }

    public DiscordException(DiscordResult result, String message) {
        super("Discord error: " + result + " - " + message);
        this.result = result;
    }

    public DiscordException(DiscordResult result, String message, Throwable cause) {
        super("Discord error: " + result + " - " + message, cause);
        this.result = result;
    }
}
