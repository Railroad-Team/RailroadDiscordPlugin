package dev.railroadide.discordplugin.settings.ui;

import dev.railroadide.discordplugin.DisplayMode;
import dev.railroadide.discordplugin.data.DiscordUser;
import dev.railroadide.discordplugin.settings.DisplayContent;
import dev.railroadide.railroad.AppResources;
import dev.railroadide.railroad.Services;
import dev.railroadide.railroad.plugin.spi.dto.Document;
import dev.railroadide.railroad.plugin.spi.dto.Project;
import dev.railroadide.railroad.ui.RRHBox;
import dev.railroadide.railroad.ui.RRStackPane;
import dev.railroadide.railroad.ui.RRVBox;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DisplayPreviewPane extends RRStackPane {
    private static final double PREVIEW_WIDTH = 380;
    private static final double PREVIEW_BUTTON_MAX_WIDTH = 164;
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([a-zA-Z0-9_]+)}");

    private final ObjectProperty<DisplayMode> displayMode = new SimpleObjectProperty<>();
    private final ObservableMap<DisplayMode, DisplayContent> contentMap;
    private final ObjectProperty<DiscordUser> currentUser = new SimpleObjectProperty<>();
    private final long previewOpenedAtMillis = System.currentTimeMillis();
    private long projectOpenedAtMillis = this.previewOpenedAtMillis;
    private long fileOpenedAtMillis = this.previewOpenedAtMillis;
    private String projectIdentity;
    private String documentIdentity;
    private Timeline elapsedTimeline;

    public DisplayPreviewPane(DisplayMode displayMode, ObservableMap<DisplayMode, DisplayContent> contentMap) {
        this.displayMode.set(displayMode);
        this.contentMap = contentMap;

        setMinWidth(PREVIEW_WIDTH);
        setPrefWidth(PREVIEW_WIDTH);
        setMaxWidth(PREVIEW_WIDTH);

        getStylesheets().add(getClass().getResource("/assets/discord/styles/preview.css").toExternalForm());
        getStyleClass().add("presence-card");

        this.currentUser.addListener((obs, oldUser, newUser) -> updateContent());
        updateContent();
        this.contentMap.addListener((MapChangeListener<? super DisplayMode, ? super DisplayContent>) change -> updateContent());
        this.displayMode.addListener((obs, oldMode, newMode) -> updateContent());
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                stopElapsedTimer();
            } else {
                updateContent();
            }
        });
    }

    private void updateContent() {
        getChildren().clear();

        DisplayMode currentMode = this.displayMode.get();
        DisplayContent currentContent = currentMode != null ? this.contentMap.get(currentMode) : null;
        Project project = Services.IDE_STATE.getCurrentProject();
        Document document = Services.IDE_STATE.getActiveDocument();
        updateElapsedAnchors(project, document);
        VariableFetchContext variableContext = new VariableFetchContext(
                Services.APPLICATION_INFO,
                project,
                document,
                Services.DOCUMENT_EDITOR_STATE.getCursors());

        var content = new RRVBox(12);
        content.getStyleClass().add("presence-content");
        content.setFillWidth(true);

        var top = new RRVBox(10);
        top.setFillWidth(true);
        top.setAlignment(Pos.CENTER_LEFT);

        var avatarStack = new StackPane();
        avatarStack.getStyleClass().add("avatar-stack");
        avatarStack.setMinSize(64, 64);
        avatarStack.setPrefSize(64, 64);
        avatarStack.setMaxSize(64, 64);

        var avatar = new ImageView(resolveAvatarImage());
        avatar.setFitWidth(64);
        avatar.setFitHeight(64);
        avatar.setPreserveRatio(true);

        // clip to circle
        var clip = new Circle(32, 32, 32);
        avatar.setClip(clip);

        var statusRing = new Circle(10);
        statusRing.getStyleClass().add("status-ring");
        StackPane.setAlignment(statusRing, Pos.BOTTOM_RIGHT);
        statusRing.setTranslateX(2);
        statusRing.setTranslateY(2);

        var status = new Circle(7);
        status.getStyleClass().add("status-online");
        StackPane.setAlignment(status, Pos.BOTTOM_RIGHT);
        status.setTranslateX(2);
        status.setTranslateY(2);

        avatarStack.getChildren().addAll(avatar, statusRing, status);

        String username = "Discord User";
        DiscordUser user = this.currentUser.get();
        if (user != null && user.getUsername() != null && !user.getUsername().isBlank()) {
            username = user.getUsername();
            if (user.getDiscriminator() != null && !user.getDiscriminator().isBlank()) {
                username += "#" + normalizeDiscriminator(user.getDiscriminator());
            }
        }

        var name = new Label(username);
        name.getStyleClass().add("username");
        name.setTextAlignment(TextAlignment.CENTER);
        name.setAlignment(Pos.CENTER);
        name.setMaxWidth(Double.MAX_VALUE);
        configureOverflowBehavior(name);

        top.getChildren().addAll(avatarStack, name);
        top.setAlignment(Pos.CENTER);

        var playing = new Label("PLAYING A GAME");
        playing.getStyleClass().add("section-label");
        playing.setMaxWidth(Double.MAX_VALUE);
        configureOverflowBehavior(playing);

        var activity = new RRHBox(10);
        activity.setAlignment(Pos.TOP_LEFT);
        activity.getStyleClass().add("activity-row");
        activity.setMaxWidth(Double.MAX_VALUE);

        var activityIconStack = new RRStackPane();
        activityIconStack.getStyleClass().add("activity-icon-stack");

        var largeIcon = new ImageView(AppResources.icon());
        largeIcon.setFitWidth(42);
        largeIcon.setFitHeight(42);
        Tooltip.install(largeIcon, new Tooltip(convertVariableString(variableContext, currentContent != null && currentContent.getLargeIconText() != null ? currentContent.getLargeIconText() : "${application_name}")));
        activityIconStack.getChildren().add(largeIcon);

        String resolvedSmallIconKey = convertVariableString(variableContext, currentContent != null ? currentContent.getSmallIconKey() : null);
        boolean hasSmallIcon = resolvedSmallIconKey != null && !resolvedSmallIconKey.isBlank();
        if (hasSmallIcon) {
            var smallIconRing = new Circle(8);
            smallIconRing.getStyleClass().add("activity-small-icon-ring");
            StackPane.setAlignment(smallIconRing, Pos.BOTTOM_RIGHT);
            smallIconRing.setTranslateX(2);
            smallIconRing.setTranslateY(2);

            var smallIcon = new ImageView(AppResources.icon());
            smallIcon.setFitWidth(14);
            smallIcon.setFitHeight(14);
            smallIcon.setPreserveRatio(true);
            smallIcon.setClip(new Circle(7, 7, 7));
            StackPane.setAlignment(smallIcon, Pos.BOTTOM_RIGHT);
            smallIcon.setTranslateX(2);
            smallIcon.setTranslateY(2);

            String smallIconTooltipText = convertVariableString(variableContext, currentContent != null ? currentContent.getSmallIconText() : null);
            if (smallIconTooltipText != null && !smallIconTooltipText.isBlank()) {
                Tooltip.install(smallIcon, new Tooltip(smallIconTooltipText));
            }

            activityIconStack.getChildren().addAll(smallIconRing, smallIcon);
        }

        var activityText = new RRVBox(2);
        HBox.setHgrow(activityText, Priority.ALWAYS);
        activityText.setMaxWidth(Double.MAX_VALUE);
        var firstLine = new Label(convertVariableString(variableContext, currentContent != null && currentContent.getFirstLine() != null ? currentContent.getFirstLine() : "${application_name}"));
        var secondLine = new Label(convertVariableString(variableContext, currentContent != null && currentContent.getSecondLine() != null ? currentContent.getSecondLine() : "${project_name}"));
        var elapsed = new Label("");

        firstLine.getStyleClass().add("activity-title");
        secondLine.getStyleClass().add("activity-details");
        elapsed.getStyleClass().add("activity-elapsed");
        firstLine.maxWidthProperty().bind(activityText.widthProperty());
        secondLine.maxWidthProperty().bind(activityText.widthProperty());
        elapsed.maxWidthProperty().bind(activityText.widthProperty());
        configureOverflowBehavior(firstLine);
        configureOverflowBehavior(secondLine);
        configureOverflowBehavior(elapsed);
        configureElapsedLabel(elapsed, currentContent);

        activityText.getChildren().addAll(firstLine, secondLine, elapsed);
        activity.getChildren().addAll(activityIconStack, activityText);

        content.getChildren().addAll(top, playing, activity);

        var buttonRow = buildButtonRow(variableContext, currentContent);
        if (buttonRow != null) {
            content.getChildren().add(buttonRow);
        }

        content.setAlignment(Pos.CENTER);
        getChildren().add(content);
        setAlignment(content, Pos.CENTER);
    }

    public DisplayMode getDisplayMode() {
        return this.displayMode.get();
    }

    public ObjectProperty<DisplayMode> displayModeProperty() {
        return this.displayMode;
    }

    public void setDisplayMode(DisplayMode displayMode) {
        this.displayMode.set(displayMode);
    }

    public DiscordUser getCurrentUser() {
        return this.currentUser.get();
    }

    public ObjectProperty<DiscordUser> currentUserProperty() {
        return this.currentUser;
    }

    public void setCurrentUser(DiscordUser user) {
        if (Platform.isFxApplicationThread()) {
            this.currentUser.set(user);
        } else {
            Platform.runLater(() -> this.currentUser.set(user));
        }
    }

    public static String convertVariableString(VariableFetchContext variableContext, String input) {
        if (input == null || input.isEmpty())
            return "";

        Matcher matcher = VARIABLE_PATTERN.matcher(input);
        var output = new StringBuilder();
        while (matcher.find()) {
            String variableKey = matcher.group(1);
            ActivityVariable variable = ActivityVariables.getByKey(variableKey);

            String replacement;
            if (variable != null) {
                replacement = variable.fetch(variableContext);
            } else {
                replacement = matcher.group(0);
            }

            if (replacement == null)
                replacement = "";
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(output);
        return output.toString();
    }

    private Image resolveAvatarImage() {
        DiscordUser discordUser = this.currentUser.get();
        if (discordUser != null && discordUser.getAvatar() != null && !discordUser.getAvatar().isBlank()) {
            String avatarUrl = discordUser.getAvatarUrl();
            if (avatarUrl != null && !avatarUrl.isBlank())
                return new Image(avatarUrl, true);
        }

        return AppResources.icon();
    }

    private RRHBox buildButtonRow(VariableFetchContext variableContext, DisplayContent displayContent) {
        if (displayContent == null)
            return null;

        var buttonRow = new RRHBox(8);
        buttonRow.getStyleClass().add("activity-buttons-row");
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        addButtonIfPresent(buttonRow, variableContext, displayContent.getButton1Label(), displayContent.getButton1Url());
        addButtonIfPresent(buttonRow, variableContext, displayContent.getButton2Label(), displayContent.getButton2Url());

        if (buttonRow.getChildren().isEmpty())
            return null;

        return buttonRow;
    }

    private static void addButtonIfPresent(RRHBox row, VariableFetchContext variableContext, String rawLabel, String rawUrl) {
        String label = convertVariableString(variableContext, rawLabel);
        String url = convertVariableString(variableContext, rawUrl);
        if (label == null || label.isBlank() || url == null || url.isBlank())
            return;

        var button = new Button(label);
        button.getStyleClass().add("activity-button");
        button.setFocusTraversable(false);
        button.setMnemonicParsing(false);
        button.setTextOverrun(OverrunStyle.ELLIPSIS);
        button.setWrapText(false);
        button.setMaxWidth(PREVIEW_BUTTON_MAX_WIDTH);
        Tooltip.install(button, new Tooltip(label + "\n" + url));
        row.getChildren().add(button);
    }

    private void configureElapsedLabel(Label elapsedLabel, DisplayContent displayContent) {
        stopElapsedTimer();

        DisplayContent.ElapsedTimeType elapsedTimeType = displayContent != null ? displayContent.getElapsedTimeType() : null;
        if (elapsedTimeType == null)
            elapsedTimeType = DisplayContent.ElapsedTimeType.APPLICATION_START;

        if (elapsedTimeType == DisplayContent.ElapsedTimeType.HIDE) {
            elapsedLabel.setText("");
            elapsedLabel.setManaged(false);
            elapsedLabel.setVisible(false);
            return;
        }

        elapsedLabel.setManaged(true);
        elapsedLabel.setVisible(true);

        DisplayContent.ElapsedTimeType effectiveElapsedTimeType = elapsedTimeType;
        Runnable updater = () -> {
            updateElapsedAnchors(Services.IDE_STATE.getCurrentProject(), Services.IDE_STATE.getActiveDocument());
            elapsedLabel.setText(formatElapsedText(effectiveElapsedTimeType));
        };
        updater.run();

        this.elapsedTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> updater.run()));
        this.elapsedTimeline.setCycleCount(Timeline.INDEFINITE);
        this.elapsedTimeline.play();
    }

    private void stopElapsedTimer() {
        if (this.elapsedTimeline != null) {
            this.elapsedTimeline.stop();
            this.elapsedTimeline = null;
        }
    }

    private void updateElapsedAnchors(Project project, Document document) {
        String currentProjectIdentity = projectIdentity(project);
        if (currentProjectIdentity == null) {
            this.projectIdentity = null;
            this.projectOpenedAtMillis = this.previewOpenedAtMillis;
        } else if (!currentProjectIdentity.equals(this.projectIdentity)) {
            this.projectIdentity = currentProjectIdentity;
            this.projectOpenedAtMillis = System.currentTimeMillis();
        }

        String currentDocumentIdentity = documentIdentity(document);
        if (currentDocumentIdentity == null) {
            this.documentIdentity = null;
            this.fileOpenedAtMillis = this.previewOpenedAtMillis;
        } else if (!currentDocumentIdentity.equals(this.documentIdentity)) {
            this.documentIdentity = currentDocumentIdentity;
            this.fileOpenedAtMillis = System.currentTimeMillis();
        }
    }

    private long getElapsedStartMillis(DisplayContent.ElapsedTimeType elapsedTimeType) {
        return switch (elapsedTimeType) {
            case APPLICATION_START -> this.previewOpenedAtMillis;
            case PROJECT_OPEN -> this.projectOpenedAtMillis;
            case FILE_OPEN -> this.fileOpenedAtMillis;
            case HIDE -> System.currentTimeMillis();
        };
    }

    private String formatElapsedText(DisplayContent.ElapsedTimeType elapsedTimeType) {
        long elapsedMillis = Math.max(0L, System.currentTimeMillis() - getElapsedStartMillis(elapsedTimeType));
        long totalSeconds = elapsedMillis / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        String elapsedValue = hours > 0
                ? String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
                : String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
        return "elapsed " + elapsedValue;
    }

    private static String projectIdentity(Project project) {
        if (project == null || project.getPath() == null)
            return null;

        return project.getPath().toString();
    }

    private static String documentIdentity(Document document) {
        if (document == null)
            return null;

        if (document.getPath() != null)
            return document.getPath().toString();

        return document.getName();
    }

    private static void configureOverflowBehavior(Labeled labeled) {
        labeled.setTextOverrun(OverrunStyle.ELLIPSIS);
        labeled.setWrapText(false);
        installOverflowTooltip(labeled);
    }

    private static void installOverflowTooltip(Labeled labeled) {
        Runnable updater = () -> {
            String value = labeled.getText();
            if (value == null || value.isBlank()) {
                labeled.setTooltip(null);
                return;
            }

            double availableWidth = labeled.getWidth() - labeled.getInsets().getLeft() - labeled.getInsets().getRight();
            if (availableWidth <= 0)
                return;

            Text measuredText = new Text(value);
            measuredText.setFont(labeled.getFont());
            boolean overflow = measuredText.getLayoutBounds().getWidth() > availableWidth;

            if (!overflow) {
                labeled.setTooltip(null);
                return;
            }

            Tooltip tooltip = labeled.getTooltip();
            if (tooltip == null) {
                labeled.setTooltip(new Tooltip(value));
            } else {
                tooltip.setText(value);
            }
        };

        labeled.textProperty().addListener(($, oldValue, newValue) -> updater.run());
        labeled.widthProperty().addListener(($, oldValue, newValue) -> updater.run());
        labeled.fontProperty().addListener(($, oldValue, newValue) -> updater.run());
        Platform.runLater(updater);
    }

    private static String normalizeDiscriminator(String discriminator) {
        if (discriminator == null || discriminator.isBlank())
            return "";

        if (!discriminator.chars().allMatch(Character::isDigit))
            return discriminator;

        if (discriminator.length() >= 4)
            return discriminator;

        try {
            return String.format(Locale.ROOT, "%04d", Integer.parseInt(discriminator));
        } catch (NumberFormatException exception) {
            return discriminator;
        }
    }
}
