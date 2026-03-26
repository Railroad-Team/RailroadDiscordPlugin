package dev.railroadide.discordplugin.settings.ui;

import dev.railroadide.discordplugin.DiscordPlugin;
import dev.railroadide.discordplugin.DisplayMode;
import dev.railroadide.discordplugin.data.DiscordUser;
import dev.railroadide.discordplugin.settings.DisplayContent;
import dev.railroadide.railroad.ui.RRHBox;
import dev.railroadide.railroad.ui.RRVBox;
import dev.railroadide.railroad.ui.localized.LocalizedComboBox;
import io.github.palexdev.materialfx.factories.InsetsFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.geometry.Insets;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DisplayLayoutPane extends RRHBox {
    private final ObservableMap<DisplayMode, DisplayContent> contentMap = FXCollections.observableHashMap();

    private final Supplier<DiscordUser> userSupplier;
    private final Consumer<Consumer<DiscordUser>> userListenerRegistrar;
    private final Consumer<Consumer<DiscordUser>> userListenerRemover;
    private Consumer<DiscordUser> registeredUserListener;

    public DisplayLayoutPane() {
        this(DiscordPlugin::getCurrentDiscordUser, DiscordPlugin::addCurrentUserListener, DiscordPlugin::removeCurrentUserListener);
    }

    public DisplayLayoutPane(Supplier<DiscordUser> userSupplier) {
        this(userSupplier, listener -> {}, listener -> {});
    }

    public DisplayLayoutPane(Supplier<DiscordUser> userSupplier,
                             Consumer<Consumer<DiscordUser>> userListenerRegistrar,
                             Consumer<Consumer<DiscordUser>> userListenerRemover) {
        this.userSupplier = userSupplier;
        this.userListenerRegistrar = userListenerRegistrar;
        this.userListenerRemover = userListenerRemover;
        setPadding(new Insets(10));
        sceneProperty().addListener(($, oldScene, newScene) -> {
            if (newScene == null) {
                unregisterCurrentUserListener();
            }
        });
    }

    public Map<DisplayMode, DisplayContent> toMap() {
        return new HashMap<>(this.contentMap);
    }

    public void load(Map<DisplayMode, DisplayContent> map) {
        this.contentMap.clear();
        this.contentMap.putAll(map);
        getChildren().clear();
        buildChildren();
    }

    private void buildChildren() {
        unregisterCurrentUserListener();

        var editorContainer = new RRVBox(4);
        LocalizedComboBox<DisplayMode> modeSelector = new LocalizedComboBox<>(DisplayMode::getTranslationKey);
        modeSelector.setItems(FXCollections.observableArrayList(DisplayMode.values()));
        modeSelector.getSelectionModel().selectFirst();
        var preview = new DisplayPreviewPane(modeSelector.getSelectionModel().getSelectedItem(), this.contentMap);
        preview.setCurrentUser(this.userSupplier != null ? this.userSupplier.get() : null);

        this.registeredUserListener = preview::setCurrentUser;
        this.userListenerRegistrar.accept(this.registeredUserListener);

        getChildren().add(preview);
        editorContainer.getChildren().add(modeSelector);
        editorContainer.setPadding(InsetsFactory.left(10));

        Map<DisplayMode, DisplayContentEditorPane> editors = new HashMap<>();
        for (DisplayMode mode : DisplayMode.values()) {
            DisplayContent content = this.contentMap.getOrDefault(mode, new DisplayContent());
            var editor = new DisplayContentEditorPane(mode, content);
            editor.setOnContentChanged(newContent -> this.contentMap.put(mode, newContent));
            editors.put(mode, editor);
        }

        modeSelector.getSelectionModel().selectedItemProperty().addListener(($, oldMode, selectedMode) -> {
            if (selectedMode == null)
                return;

            preview.setDisplayMode(selectedMode);
            editorContainer.getChildren().setAll(modeSelector, editors.get(selectedMode));
        });

        editorContainer.getChildren().add(editors.get(modeSelector.getSelectionModel().getSelectedItem()));
        getChildren().add(editorContainer);
    }

    private void unregisterCurrentUserListener() {
        if (this.registeredUserListener != null) {
            this.userListenerRemover.accept(this.registeredUserListener);
            this.registeredUserListener = null;
        }
    }
}
