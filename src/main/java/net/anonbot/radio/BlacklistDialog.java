package net.anonbot.radio;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class BlacklistDialog {

    private final RadioModClient modClient;
    private final Stage owner;

    public BlacklistDialog(RadioModClient modClient, Stage owner) {
        this.modClient = modClient;
        this.owner = owner;
    }

    public void show() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("🚫 Czarna Lista Piosenek");
        dialog.setResizable(true);
        dialog.setMinWidth(400);
        dialog.setMinHeight(350);

        VBox root = new VBox(10);
        root.setPadding(new Insets(16));
        root.getStyleClass().add("dialog-root");

        Label title = new Label("🚫 Czarna Lista Powiadomień");
        title.getStyleClass().add("dialog-title");

        Label hint = new Label("Frazy na liście nie będą pokazywane w powiadomieniach.");
        hint.getStyleClass().add("dialog-hint");

        // Lista
        ListView<String> list = new ListView<>(FXCollections.observableList(modClient.getBlacklist()));
        list.getStyleClass().add("blacklist-view");
        list.setPrefHeight(200);
        list.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);
                Label lbl = new Label("🚫 " + item);
                lbl.setMaxWidth(Double.MAX_VALUE);
                lbl.setStyle("-fx-text-fill: white;");
                HBox.setHgrow(lbl, Priority.ALWAYS);
                Button removeBtn = new Button("✕");
                removeBtn.setStyle("-fx-background-color: #880000; -fx-text-fill: white; -fx-background-radius: 4; -fx-cursor: hand;");
                removeBtn.setOnAction(e -> {
                    modClient.getBlacklist().remove(item);
                    modClient.saveConfig();
                    list.setItems(FXCollections.observableList(modClient.getBlacklist()));
                });
                row.getChildren().addAll(lbl, removeBtn);
                setGraphic(row);
            }
        });

        // Dodawanie
        HBox addRow = new HBox(8);
        addRow.setAlignment(Pos.CENTER_LEFT);
        TextField phraseField = new TextField();
        phraseField.setPromptText("Wpisz frazę do zablokowania...");
        HBox.setHgrow(phraseField, Priority.ALWAYS);
        Button addBtn = new Button("+ Dodaj");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> {
            String phrase = phraseField.getText().trim();
            if (!phrase.isEmpty() && !modClient.getBlacklist().contains(phrase)) {
                modClient.getBlacklist().add(phrase);
                modClient.saveConfig();
                list.setItems(FXCollections.observableList(modClient.getBlacklist()));
                phraseField.clear();
            }
        });
        phraseField.setOnAction(e -> addBtn.fire());
        addRow.getChildren().addAll(phraseField, addBtn);

        Button closeBtn = new Button("✓ Zamknij");
        closeBtn.getStyleClass().add("btn-primary");
        closeBtn.setOnAction(e -> dialog.close());

        root.getChildren().addAll(title, hint, new Separator(), list, addRow, new Separator(), closeBtn);

        Scene scene = new Scene(root, 440, 420);
        try { scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm()); }
        catch (Exception ignored) {}
        dialog.setScene(scene);
        dialog.show();
    }
}
