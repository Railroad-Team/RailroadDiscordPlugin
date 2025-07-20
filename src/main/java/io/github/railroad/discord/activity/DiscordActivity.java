package io.github.railroad.discord.activity;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Getter
public class DiscordActivity {
    private final DiscordActivityTimestamps timestamps;
    private final DiscordActivityAssets assets;
    private final DiscordActivityParty party;
    private transient final DiscordActivitySecrets secretsBak;
    private transient final List<DiscordActivityButton> buttonsBak;
    private Long applicationId;
    @Setter
    private String name;
    private int type;
    @Setter
    private String state;
    @Setter
    private String details;
    @Setter
    private boolean instance;
    private List<DiscordActivityButton> buttons;
    private DiscordActivitySecrets secrets;

    public DiscordActivity() {
        this.timestamps = new DiscordActivityTimestamps();
        this.assets = new DiscordActivityAssets();
        this.party = new DiscordActivityParty();

        this.secretsBak = new DiscordActivitySecrets();
        this.buttonsBak = new ArrayList<>();
        setActivityButtonsMode(ActivityButtonsMode.SECRETS);
    }

    public static Builder builder() {
        return new Builder();
    }

    public void setApplicationId(long applicationId) {
        this.applicationId = applicationId;
    }

    public ActivityType getType() {
        return ActivityType.values()[type];
    }

    public void setType(ActivityType type) {
        this.type = type.ordinal();
    }

    public List<DiscordActivityButton> getButtons() {
        return Collections.unmodifiableList(buttonsBak);
    }

    public void addButton(DiscordActivityButton button) {
        if (buttonsBak.size() == 2)
            throw new IllegalStateException("Cannot add more than 2 buttons");

        buttonsBak.add(button);
    }

    public boolean removeButton(DiscordActivityButton button) {
        return buttons.remove(button);
    }

    public ActivityButtonsMode getActivityButtonsMode() {
        return buttons != null ? ActivityButtonsMode.BUTTONS : ActivityButtonsMode.SECRETS;
    }

    /**
     * <p>Changes the button display mode</p>
     * <p>Only custom buttons (ActivityButtonsMode.BUTTONS) or "Ask to join"/"Spectate" (ActivityButtonsMode.SECRETS) buttons can be displayed at the same time</p>
     *
     * @param mode button mode
     */
    public void setActivityButtonsMode(ActivityButtonsMode mode) {
        if (mode == ActivityButtonsMode.SECRETS) {
            this.buttons = null;
            this.secrets = secretsBak;
        } else {
            this.secrets = null;
            this.buttons = buttonsBak;
        }
    }

    @Override
    public String toString() {
        return "DiscordActivity{" +
                "timestamps=" + timestamps +
                ", assets=" + assets +
                ", party=" + party +
                ", secretsBak=" + secretsBak +
                ", buttonsBak=" + buttonsBak +
                ", applicationId=" + applicationId +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", state='" + state + '\'' +
                ", details='" + details + '\'' +
                ", instance=" + instance +
                ", buttons=" + buttons +
                ", secrets=" + secrets +
                '}';
    }

    public static class Builder {
        private final DiscordActivity activity;

        public Builder() {
            activity = new DiscordActivity();
        }

        public Builder applicationId(long applicationId) {
            activity.setApplicationId(applicationId);
            return this;
        }

        public Builder name(String name) {
            activity.setName(name);
            return this;
        }

        public Builder type(ActivityType type) {
            activity.setType(type);
            return this;
        }

        public Builder playing() {
            activity.setType(ActivityType.PLAYING);
            return this;
        }

        public Builder streaming() {
            activity.setType(ActivityType.STREAMING);
            return this;
        }

        public Builder listening() {
            activity.setType(ActivityType.LISTENING);
            return this;
        }

        public Builder watching() {
            activity.setType(ActivityType.WATCHING);
            return this;
        }

        public Builder custom() {
            activity.setType(ActivityType.CUSTOM);
            return this;
        }

        public Builder competing() {
            activity.setType(ActivityType.COMPETING);
            return this;
        }

        public Builder state(String state) {
            activity.setState(state);
            return this;
        }

        public Builder details(String details) {
            activity.setDetails(details);
            return this;
        }

        public Builder instance(boolean instance) {
            activity.setInstance(instance);
            return this;
        }

        /**
         * Configure timestamps via a consumer.
         */
        public Builder timestamps(Consumer<DiscordActivityTimestamps> config) {
            config.accept(activity.timestamps);
            return this;
        }

        public Builder startAt(Instant start) {
            activity.timestamps.setStart(start);
            return this;
        }

        public Builder startNow() {
            activity.timestamps.setStart(Instant.now());
            return this;
        }

        public Builder endAt(Instant end) {
            activity.timestamps.setEnd(end);
            return this;
        }

        public Builder endNow() {
            activity.timestamps.setEnd(Instant.now());
            return this;
        }

        /**
         * Configure assets via a consumer.
         */
        public Builder assets(Consumer<DiscordActivityAssets> config) {
            config.accept(activity.assets);
            return this;
        }

        public Builder largeImage(String largeImage) {
            activity.assets.setLargeImage(largeImage);
            return this;
        }

        public Builder smallImage(String smallImage) {
            activity.assets.setSmallImage(smallImage);
            return this;
        }

        public Builder largeText(String largeText) {
            activity.assets.setLargeText(largeText);
            return this;
        }

        public Builder smallText(String smallText) {
            activity.assets.setSmallText(smallText);
            return this;
        }

        /**
         * Configure party via a consumer.
         */
        public Builder party(Consumer<DiscordActivityParty> config) {
            config.accept(activity.party);
            return this;
        }

        /**
         * Add a custom button (up to 2) and switch to BUTTONS mode.
         */
        public Builder addButton(DiscordActivityButton button) {
            activity.addButton(button);
            activity.setActivityButtonsMode(ActivityButtonsMode.BUTTONS);
            return this;
        }

        /**
         * Use secrets-based buttons mode.
         */
        public Builder secrets(DiscordActivitySecrets secrets) {
            activity.secrets = secrets;
            activity.setActivityButtonsMode(ActivityButtonsMode.SECRETS);
            return this;
        }

        /**
         * Build the configured DiscordActivity.
         */
        public DiscordActivity build() {
            return activity;
        }

        public DiscordActivity buildAndSet(DiscordActivityManager manager) {
            DiscordActivity activity = build();
            manager.updateActivity(activity);
            return activity;
        }
    }

    public enum ActivityButtonsMode {
        BUTTONS,
        SECRETS
    }

    public enum ActivityType {
        PLAYING,
        STREAMING,
        LISTENING,
        WATCHING,
        CUSTOM,
        COMPETING
    }
}
