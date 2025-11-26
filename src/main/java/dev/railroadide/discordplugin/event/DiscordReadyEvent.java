package dev.railroadide.discordplugin.event;

import com.google.gson.annotations.SerializedName;
import dev.railroadide.discordplugin.core.DiscordCore;
import dev.railroadide.discordplugin.data.DiscordUser;

public class DiscordReadyEvent {
    public static class Data {
        private int v;
        private Config config;
        private DiscordUser user;

        @Override
        public String toString() {
            return "ReadyData{" +
                    "v=" + v +
                    ", config=" + config +
                    ", user=" + user +
                    '}';
        }

        record Config(@SerializedName("cdn_host") String cdnHost,
                      @SerializedName("api_endpoint") String apiEndpoint,
                      String environment) {
            @Override
            public String toString() {
                return "ReadyConfig{" +
                        "cdn_host='" + cdnHost + '\'' +
                        ", api_endpoint='" + apiEndpoint + '\'' +
                        ", environment='" + environment + '\'' +
                        '}';
            }
        }
    }

    public static class Handler extends DiscordEventHandler<Data> {
        public Handler(DiscordCore core) {
            super(core);
        }

        @Override
        public void handle(DiscordCommand command, Data data) {
            this.core.onReady();
            this.core.setCurrentUser(data.user);
        }

        @Override
        public Class<Data> getDataClass() {
            return Data.class;
        }

        @Override
        public boolean shouldRegister() {
            return false;
        }
    }
}
