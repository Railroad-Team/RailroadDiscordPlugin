package dev.railroadide.discordplugin.settings.ui;

import dev.railroadide.discordplugin.DisplayMode;
import dev.railroadide.discordplugin.settings.DisplayContent;
import dev.railroadide.railroad.ui.RRHBox;
import dev.railroadide.railroad.ui.RRTextField;
import dev.railroadide.railroad.ui.RRVBox;
import dev.railroadide.railroad.ui.localized.LocalizedComboBox;
import dev.railroadide.railroad.ui.localized.LocalizedLabel;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.util.function.Consumer;

public class DisplayContentEditorPane extends RRVBox {
    private final StringProperty firstLineProperty = new SimpleStringProperty();
    private final StringProperty secondLineProperty = new SimpleStringProperty();
    private final StringProperty largeIconKeyProperty = new SimpleStringProperty();
    private final StringProperty largeIconTextProperty = new SimpleStringProperty();
    private final StringProperty smallIconKeyProperty = new SimpleStringProperty();
    private final StringProperty smallIconTextProperty = new SimpleStringProperty();
    private final ObjectProperty<DisplayContent.ElapsedTimeType> elapsedTimeTypeProperty = new SimpleObjectProperty<>();
    private final StringProperty button1LabelProperty = new SimpleStringProperty();
    private final StringProperty button1UrlProperty = new SimpleStringProperty();
    private final StringProperty button2LabelProperty = new SimpleStringProperty();
    private final StringProperty button2UrlProperty = new SimpleStringProperty();
    private Consumer<DisplayContent> onContentChanged = $ -> {
    };

    public DisplayContentEditorPane(DisplayMode mode, DisplayContent content) {
        super(10);
        setPadding(new Insets(15));

        DisplayContent initialContent = content != null ? content : new DisplayContent();
        this.firstLineProperty.set(initialContent.getFirstLine());
        this.secondLineProperty.set(initialContent.getSecondLine());
        this.largeIconKeyProperty.set(initialContent.getLargeIconKey());
        this.largeIconTextProperty.set(initialContent.getLargeIconText());
        this.smallIconKeyProperty.set(initialContent.getSmallIconKey());
        this.smallIconTextProperty.set(initialContent.getSmallIconText());
        this.elapsedTimeTypeProperty.set(initialContent.getElapsedTimeType());
        this.button1LabelProperty.set(initialContent.getButton1Label());
        this.button1UrlProperty.set(initialContent.getButton1Url());
        this.button2LabelProperty.set(initialContent.getButton2Label());
        this.button2UrlProperty.set(initialContent.getButton2Url());

        ObservableList<String> lineOptions = switch (mode) {
            case APPLICATION ->
                    FXCollections.observableArrayList("${application_name}", "${application_name} - ${application_version}", "custom");
            case PROJECT ->
                    FXCollections.observableArrayList("${project_name}", "${project_name} - ${project_description}", "${project_name} (${project_path})", "custom");
            case DOCUMENT ->
                    FXCollections.observableArrayList("${document_name}", "${document_name} (${document_path})", "${document_name} - ${document_type}", "custom");
        };

        var firstLineRow = new RRHBox(5);
        var firstLineLabel = new LocalizedLabel("discord.settings.display_content.first_line");
        LocalizedComboBox<String> firstLineComboBox = new LocalizedComboBox<>();
        firstLineComboBox.setItems(lineOptions);
        if (firstLineComboBox.getItems().contains(this.firstLineProperty.get())) {
            firstLineComboBox.getSelectionModel().select(this.firstLineProperty.get());
        } else {
            firstLineComboBox.getSelectionModel().select("custom");
        }
        firstLineComboBox.setOnAction($ -> {
            String selected = firstLineComboBox.getSelectionModel().getSelectedItem();
            if (selected != null && !selected.equals("custom")) {
                this.firstLineProperty.set(selected);
            }
        });
        var firstCustomField = new RRHBox(5);
        var customLabel = new LocalizedLabel("discord.settings.display_content.custom");
        customLabel.visibleProperty().bind(firstLineComboBox.getSelectionModel().selectedItemProperty().isEqualTo("custom"));
        var customFieldInputField = new RRTextField("discord.settings.display_content.first_line.placeholder");
        customFieldInputField.textProperty().bindBidirectional(this.firstLineProperty);
        firstCustomField.getChildren().addAll(customLabel, customFieldInputField);
        firstCustomField.visibleProperty().bind(firstLineComboBox.getSelectionModel().selectedItemProperty().isEqualTo("custom"));
        firstCustomField.setAlignment(Pos.CENTER_LEFT);

        firstLineRow.getChildren().addAll(firstLineLabel, firstLineComboBox, firstCustomField);
        firstLineRow.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(firstLineRow);

        var secondLineRow = new RRHBox(5);
        var secondLineLabel = new LocalizedLabel("discord.settings.display_content.second_line");
        LocalizedComboBox<String> secondLineComboBox = new LocalizedComboBox<>();
        secondLineComboBox.setItems(lineOptions);
        if (secondLineComboBox.getItems().contains(this.secondLineProperty.get())) {
            secondLineComboBox.getSelectionModel().select(this.secondLineProperty.get());
        } else {
            secondLineComboBox.getSelectionModel().select("custom");
        }
        secondLineComboBox.setOnAction($ -> {
            String selected = secondLineComboBox.getSelectionModel().getSelectedItem();
            if (selected != null && !selected.equals("custom")) {
                this.secondLineProperty.set(selected);
            }
        });
        var secondCustomField = new RRHBox(5);
        var secondCustomLabel = new LocalizedLabel("discord.settings.display_content.custom");
        secondCustomLabel.visibleProperty().bind(secondLineComboBox.getSelectionModel().selectedItemProperty().isEqualTo("custom"));
        var secondCustomFieldInputField = new RRTextField("discord.settings.display_content.second_line.placeholder");
        secondCustomFieldInputField.textProperty().bindBidirectional(this.secondLineProperty);
        secondCustomField.getChildren().addAll(secondCustomLabel, secondCustomFieldInputField);
        secondCustomField.visibleProperty().bind(secondLineComboBox.getSelectionModel().selectedItemProperty().isEqualTo("custom"));
        secondCustomField.setAlignment(Pos.CENTER_LEFT);

        secondLineRow.getChildren().addAll(secondLineLabel, secondLineComboBox, secondCustomField);
        secondLineRow.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(secondLineRow);

        ObservableList<String> iconOptions = switch (mode) {
            case APPLICATION -> FXCollections.observableArrayList("${application_icon}", "custom");
            case PROJECT -> FXCollections.observableArrayList("${application_icon}", "${project_icon}", "custom");
            case DOCUMENT ->
                    FXCollections.observableArrayList("${application_icon}", "${project_icon}", "${document_icon}", "custom");
        };

        var largeIconRow = new RRHBox(5);
        var largeIconLabel = new LocalizedLabel("discord.settings.display_content.large_icon");
        LocalizedComboBox<String> largeIconComboBox = new LocalizedComboBox<>();
        largeIconComboBox.setItems(iconOptions);
        if (largeIconComboBox.getItems().contains(this.largeIconKeyProperty.get())) {
            largeIconComboBox.getSelectionModel().select(this.largeIconKeyProperty.get());
        } else {
            largeIconComboBox.getSelectionModel().select("custom");
        }
        largeIconComboBox.setOnAction($ -> {
            String selected = largeIconComboBox.getSelectionModel().getSelectedItem();
            if (selected != null && !selected.equals("custom")) {
                this.largeIconKeyProperty.set(selected);
            }
        });
        var largeIconCustomField = new RRHBox(5);
        var largeIconCustomLabel = new LocalizedLabel("discord.settings.display_content.custom");
        largeIconCustomLabel.visibleProperty().bind(largeIconComboBox.getSelectionModel().selectedItemProperty().isEqualTo("custom"));
        var largeIconCustomFieldInputField = new RRTextField("discord.settings.display_content.large_icon.placeholder");
        largeIconCustomFieldInputField.textProperty().bindBidirectional(this.largeIconKeyProperty);
        largeIconCustomField.getChildren().addAll(largeIconCustomLabel, largeIconCustomFieldInputField);
        largeIconCustomField.visibleProperty().bind(largeIconComboBox.getSelectionModel().selectedItemProperty().isEqualTo("custom"));
        largeIconCustomField.setAlignment(Pos.CENTER_LEFT);

        largeIconRow.getChildren().addAll(largeIconLabel, largeIconComboBox, largeIconCustomField);
        largeIconRow.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(largeIconRow);

        var textIconOptions = switch (mode) {
            case APPLICATION -> FXCollections.observableArrayList("${application_name}", "custom");
            case PROJECT -> FXCollections.observableArrayList("${application_name}", "${project_name}", "custom");
            case DOCUMENT ->
                    FXCollections.observableArrayList("${application_name}", "${project_name}", "${document_name}", "custom");
        };

        var largeIconTextRow = new RRHBox(5);
        var largeIconTextLabel = new LocalizedLabel("discord.settings.display_content.large_icon_text");
        LocalizedComboBox<String> largeIconTextComboBox = new LocalizedComboBox<>();
        largeIconTextComboBox.setItems(textIconOptions);
        if (largeIconTextComboBox.getItems().contains(this.largeIconTextProperty.get())) {
            largeIconTextComboBox.getSelectionModel().select(this.largeIconTextProperty.get());
        } else {
            largeIconTextComboBox.getSelectionModel().select("custom");
        }
        largeIconTextComboBox.setOnAction($ -> {
            String selected = largeIconTextComboBox.getSelectionModel().getSelectedItem();
            if (selected != null && !selected.equals("custom")) {
                this.largeIconTextProperty.set(selected);
            }
        });
        var largeIconTextCustomField = new RRHBox(5);
        var largeIconTextCustomLabel = new LocalizedLabel("discord.settings.display_content.custom");
        largeIconTextCustomLabel.visibleProperty().bind(largeIconTextComboBox.getSelectionModel().selectedItemProperty().isEqualTo("custom"));
        var largeIconTextCustomFieldInputField = new RRTextField("discord.settings.display_content.large_icon_text.placeholder");
        largeIconTextCustomFieldInputField.textProperty().bindBidirectional(this.largeIconTextProperty);
        largeIconTextCustomField.getChildren().addAll(largeIconTextCustomLabel, largeIconTextCustomFieldInputField);
        largeIconTextCustomField.visibleProperty().bind(largeIconTextComboBox.getSelectionModel().selectedItemProperty().isEqualTo("custom"));
        largeIconTextCustomField.setAlignment(Pos.CENTER_LEFT);

        largeIconTextRow.getChildren().addAll(largeIconTextLabel, largeIconTextComboBox, largeIconTextCustomField);
        largeIconTextRow.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(largeIconTextRow);

        var smallIconRow = new RRHBox(5);
        var smallIconLabel = new LocalizedLabel("discord.settings.display_content.small_icon");
        LocalizedComboBox<String> smallIconComboBox = new LocalizedComboBox<>();
        smallIconComboBox.setItems(iconOptions);
        if (smallIconComboBox.getItems().contains(this.smallIconKeyProperty.get())) {
            smallIconComboBox.getSelectionModel().select(this.smallIconKeyProperty.get());
        } else {
            smallIconComboBox.getSelectionModel().select("custom");
        }
        smallIconComboBox.setOnAction($ -> {
            String selected = smallIconComboBox.getSelectionModel().getSelectedItem();
            if (selected != null && !selected.equals("custom")) {
                this.smallIconKeyProperty.set(selected);
            }
        });
        var smallIconCustomField = new RRHBox(5);
        var smallIconCustomLabel = new LocalizedLabel("discord.settings.display_content.custom");
        smallIconCustomLabel.visibleProperty().bind(smallIconComboBox.getSelectionModel().selectedItemProperty().isEqualTo("custom"));
        var smallIconCustomFieldInputField = new RRTextField("discord.settings.display_content.small_icon.placeholder");
        smallIconCustomFieldInputField.textProperty().bindBidirectional(this.smallIconKeyProperty);
        smallIconCustomField.getChildren().addAll(smallIconCustomLabel, smallIconCustomFieldInputField);
        smallIconCustomField.visibleProperty().bind(smallIconComboBox.getSelectionModel().selectedItemProperty().isEqualTo("custom"));
        smallIconCustomField.setAlignment(Pos.CENTER_LEFT);

        smallIconRow.getChildren().addAll(smallIconLabel, smallIconComboBox, smallIconCustomField);
        smallIconRow.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(smallIconRow);

        var smallIconTextRow = new RRHBox(5);
        var smallIconTextLabel = new LocalizedLabel("discord.settings.display_content.small_icon_text");
        LocalizedComboBox<String> smallIconTextComboBox = new LocalizedComboBox<>();
        smallIconTextComboBox.setItems(textIconOptions);
        if (smallIconTextComboBox.getItems().contains(this.smallIconTextProperty.get())) {
            smallIconTextComboBox.getSelectionModel().select(this.smallIconTextProperty.get());
        } else {
            smallIconTextComboBox.getSelectionModel().select("custom");
        }
        smallIconTextComboBox.setOnAction($ -> {
            String selected = smallIconTextComboBox.getSelectionModel().getSelectedItem();
            if (selected != null && !selected.equals("custom")) {
                this.smallIconTextProperty.set(selected);
            }
        });
        var smallIconTextCustomField = new RRHBox(5);
        var smallIconTextCustomLabel = new LocalizedLabel("discord.settings.display_content.custom");
        smallIconTextCustomLabel.visibleProperty().bind(smallIconTextComboBox.getSelectionModel().selectedItemProperty().isEqualTo("custom"));
        var smallIconTextCustomFieldInputField = new RRTextField("discord.settings.display_content.small_icon_text.placeholder");
        smallIconTextCustomFieldInputField.textProperty().bindBidirectional(this.smallIconTextProperty);
        smallIconTextCustomField.getChildren().addAll(smallIconTextCustomLabel, smallIconTextCustomFieldInputField);
        smallIconTextCustomField.visibleProperty().bind(smallIconTextComboBox.getSelectionModel().selectedItemProperty().isEqualTo("custom"));
        smallIconTextCustomField.setAlignment(Pos.CENTER_LEFT);

        smallIconTextRow.getChildren().addAll(smallIconTextLabel, smallIconTextComboBox, smallIconTextCustomField);
        smallIconTextRow.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(smallIconTextRow);

        var elapsedTimeTypeRow = new RRHBox(5);
        var elapsedTimeTypeLabel = new LocalizedLabel("discord.settings.display_content.elapsed_time_type");
        LocalizedComboBox<DisplayContent.ElapsedTimeType> elapsedTimeTypeComboBox = new LocalizedComboBox<>(DisplayContent.ElapsedTimeType::getLocalizationKey);
        ObservableList<DisplayContent.ElapsedTimeType> elapsedTimeOptions = getElapsedTimeOptions(mode);
        elapsedTimeTypeComboBox.setItems(elapsedTimeOptions);
        DisplayContent.ElapsedTimeType elapsedTimeSelection = this.elapsedTimeTypeProperty.get();
        if (elapsedTimeSelection == null || !elapsedTimeOptions.contains(elapsedTimeSelection)) {
            elapsedTimeSelection = getDefaultElapsedTimeType(mode);
            this.elapsedTimeTypeProperty.set(elapsedTimeSelection);
        }
        elapsedTimeTypeComboBox.getSelectionModel().select(elapsedTimeSelection);
        elapsedTimeTypeComboBox.setOnAction($ -> {
            DisplayContent.ElapsedTimeType selected = elapsedTimeTypeComboBox.getSelectionModel().getSelectedItem();
            this.elapsedTimeTypeProperty.set(selected);
        });
        elapsedTimeTypeRow.getChildren().addAll(elapsedTimeTypeLabel, elapsedTimeTypeComboBox);
        elapsedTimeTypeRow.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(elapsedTimeTypeRow);

        var buttonsVBox = new RRVBox(5);

        var button1Row = new RRHBox(5);
        var button1Label = new LocalizedLabel("discord.settings.display_content.button1");
        var button1LabelField = new RRTextField("discord.settings.display_content.button_label.placeholder");
        button1LabelField.textProperty().bindBidirectional(this.button1LabelProperty);
        var button1UrlField = new RRTextField("discord.settings.display_content.button_url.placeholder");
        button1UrlField.textProperty().bindBidirectional(this.button1UrlProperty);
        button1Row.getChildren().addAll(button1Label, button1LabelField, button1UrlField);
        button1Row.setAlignment(Pos.CENTER_LEFT);
        buttonsVBox.getChildren().add(button1Row);

        var button2Row = new RRHBox(5);
        var button2Label = new LocalizedLabel("discord.settings.display_content.button2");
        var button2LabelField = new RRTextField("discord.settings.display_content.button_label.placeholder");
        button2LabelField.textProperty().bindBidirectional(this.button2LabelProperty);
        var button2UrlField = new RRTextField("discord.settings.display_content.button_url.placeholder");
        button2UrlField.textProperty().bindBidirectional(this.button2UrlProperty);
        button2Row.getChildren().addAll(button2Label, button2LabelField, button2UrlField);
        button2Row.setAlignment(Pos.CENTER_LEFT);
        buttonsVBox.getChildren().add(button2Row);

        getChildren().add(buttonsVBox);

        registerContentListeners();
    }

    public DisplayContent getContent() {
        return new DisplayContent(
                this.firstLineProperty.get(),
                this.secondLineProperty.get(),
                this.largeIconKeyProperty.get(),
                this.largeIconTextProperty.get(),
                this.smallIconKeyProperty.get(),
                this.smallIconTextProperty.get(),
                this.elapsedTimeTypeProperty.get(),
                this.button1LabelProperty.get(),
                this.button1UrlProperty.get(),
                this.button2LabelProperty.get(),
                this.button2UrlProperty.get()
        );
    }

    public void setOnContentChanged(Consumer<DisplayContent> onContentChanged) {
        this.onContentChanged = onContentChanged != null ? onContentChanged : $ -> {
        };
        notifyContentChanged();
    }

    private void registerContentListeners() {
        this.firstLineProperty.addListener(($, oldValue, newValue) -> notifyContentChanged());
        this.secondLineProperty.addListener(($, oldValue, newValue) -> notifyContentChanged());
        this.largeIconKeyProperty.addListener(($, oldValue, newValue) -> notifyContentChanged());
        this.largeIconTextProperty.addListener(($, oldValue, newValue) -> notifyContentChanged());
        this.smallIconKeyProperty.addListener(($, oldValue, newValue) -> notifyContentChanged());
        this.smallIconTextProperty.addListener(($, oldValue, newValue) -> notifyContentChanged());
        this.elapsedTimeTypeProperty.addListener(($, oldValue, newValue) -> notifyContentChanged());
        this.button1LabelProperty.addListener(($, oldValue, newValue) -> notifyContentChanged());
        this.button1UrlProperty.addListener(($, oldValue, newValue) -> notifyContentChanged());
        this.button2LabelProperty.addListener(($, oldValue, newValue) -> notifyContentChanged());
        this.button2UrlProperty.addListener(($, oldValue, newValue) -> notifyContentChanged());
    }

    private void notifyContentChanged() {
        this.onContentChanged.accept(getContent());
    }

    private static ObservableList<DisplayContent.ElapsedTimeType> getElapsedTimeOptions(DisplayMode mode) {
        return switch (mode) {
            case APPLICATION -> FXCollections.observableArrayList(
                    DisplayContent.ElapsedTimeType.APPLICATION_START,
                    DisplayContent.ElapsedTimeType.HIDE
            );
            case PROJECT -> FXCollections.observableArrayList(
                    DisplayContent.ElapsedTimeType.APPLICATION_START,
                    DisplayContent.ElapsedTimeType.PROJECT_OPEN,
                    DisplayContent.ElapsedTimeType.HIDE
            );
            case DOCUMENT -> FXCollections.observableArrayList(
                    DisplayContent.ElapsedTimeType.APPLICATION_START,
                    DisplayContent.ElapsedTimeType.PROJECT_OPEN,
                    DisplayContent.ElapsedTimeType.FILE_OPEN,
                    DisplayContent.ElapsedTimeType.HIDE
            );
        };
    }

    private static DisplayContent.ElapsedTimeType getDefaultElapsedTimeType(DisplayMode mode) {
        return switch (mode) {
            case APPLICATION -> DisplayContent.ElapsedTimeType.APPLICATION_START;
            case PROJECT -> DisplayContent.ElapsedTimeType.PROJECT_OPEN;
            case DOCUMENT -> DisplayContent.ElapsedTimeType.FILE_OPEN;
        };
    }
}
