package net.anonbot.radio;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;

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
        dialog.setTitle(modClient.t("🚫 Czarna lista piosenek", "🚫 Song Blacklist"));
        dialog.setResizable(true);
        dialog.setMinWidth(400);
        dialog.setMinHeight(350);

        VBox root = new VBox(10);
        root.setPadding(new Insets(16));
        root.getStyleClass().add("dialog-root");

        Label titleLbl = new Label(modClient.t("🚫 Czarna lista powiadomień", "🚫 Notification Blacklist"));
        titleLbl.getStyleClass().add("dialog-title");

        Label hint = new Label(modClient.t(
                "Frazy na liście nie będą pokazywane w powiadomieniach.",
                "Phrases on the list won't appear in notifications."));
        hint.getStyleClass().add("dialog-hint");

        // Fix: pracujemy na ObservableList jako KOPII — zmiany przez dedykowane metody modClient
        ObservableList<String> items = FXCollections.observableArrayList(modClient.getBlacklist());

        ListView<String> list = new ListView<>(items);
        list.getStyleClass().add("blacklist-view");
        list.setPrefHeight(200);
        VBox.setVgrow(list, Priority.ALWAYS);

        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }

                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);

                Label lbl = new Label("🚫 " + item);
                lbl.setMaxWidth(Double.MAX_VALUE);
                lbl.setStyle("-fx-text-fill: white;");
                HBox.setHgrow(lbl, Priority.ALWAYS);

                Button removeBtn = new Button("✕");
                removeBtn.setStyle("-fx-background-color: #880000; -fx-text-fill: white; " +
                        "-fx-background-radius: 4; -fx-cursor: hand;");
                removeBtn.setOnAction(e -> {
                    // Fix: usuwamy przez metodę (synchronizowana) zamiast bezpośrednio na liście
                    modClient.removeFromBlacklist(item);
                    items.setAll(modClient.getBlacklist());
                });

                row.getChildren().addAll(lbl, removeBtn);
                setGraphic(row);
            }
        });

        HBox addRow = new HBox(8);
        addRow.setAlignment(Pos.CENTER_LEFT);
        TextField phraseField = new TextField();
        phraseField.setPromptText(modClient.t("Wpisz frazę do zablokowania...", "Enter phrase to block..."));
        HBox.setHgrow(phraseField, Priority.ALWAYS);

        Button addBtn = new Button(modClient.t("+ Dodaj", "+ Add"));
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> {
            String phrase = phraseField.getText().trim();
            if (!phrase.isEmpty()) {
                modClient.addToBlacklist(phrase);
                items.setAll(modClient.getBlacklist());
                phraseField.clear();
            }
        });
        phraseField.setOnAction(e -> addBtn.fire());
        addRow.getChildren().addAll(phraseField, addBtn);

        Button closeBtn = new Button(modClient.t("✓ Zamknij", "✓ Close"));
        closeBtn.getStyleClass().add("btn-primary");
        closeBtn.setOnAction(e -> dialog.close());

        root.getChildren().addAll(titleLbl, hint, new Separator(), list, addRow, new Separator(), closeBtn);

        Scene scene = new Scene(root, 440, 420);
        try { scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm()); }
        catch (Exception ignored) {}
        dialog.setScene(scene);
        dialog.show();
    }
}
