package dev.railroadide.discordplugin.event;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DiscordCommand {
    private Type cmd;
    private JsonElement data;

    private JsonElement args;
    @SerializedName("evt")
    private Event event;
    private String nonce;

    public boolean isError() {
        return this.event == Event.ERROR;
    }

    @Override
    public String toString() {
        return "DiscordCommand{" +
                "type=" + cmd +
                ", data=" + data +
                ", args=" + args +
                ", event=" + event +
                ", nonce='" + nonce + '\'' +
                '}';
    }

    public enum Type {
        ACTIVITY_INVITE_USER,
        CLOSE_ACTIVITY_JOIN_REQUEST,
        DISPATCH,
        GET_IMAGE,
        GET_NETWORKING_CONFIG,
        GET_RELATIONSHIPS,
        GET_USER,
        OPEN_OVERLAY_ACTIVITY_INVITE,
        OPEN_OVERLAY_GUILD_INVITE,
        OPEN_OVERLAY_VOICE_SETTINGS,
        SEND_ACTIVITY_JOIN_INVITE,
        SET_ACTIVITY,
        SET_OVERLAY_LOCKED,
        SUBSCRIBE
    }

    public enum Event {
        ACTIVITY_INVITE,
        ACTIVITY_JOIN,
        ACTIVITY_JOIN_REQUEST,
        ACTIVITY_SPECTATE,
        CURRENT_USER_UPDATE,
        ERROR,
        LOBBY_DELETE,
        LOBBY_MEMBER_CONNECT,
        LOBBY_MEMBER_DISCONNECT,
        LOBBY_MEMBER_UPDATE,
        LOBBY_MESSAGE,
        LOBBY_UPDATE,
        OVERLAY_UPDATE,
        READY,
        RELATIONSHIP_UPDATE,
        SPEAKING_START,
        SPEAKING_STOP,
        VOICE_SETTINGS_UPDATE_2
    }
}
