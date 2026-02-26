package net.anonbot.radio;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

public class HistoryDialog {

    private final RadioModClient modClient;
    private final Stage owner;

    public HistoryDialog(RadioModClient modClient, Stage owner) {
        this.modClient = modClient;
        this.owner = owner;
    }

    public void show() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle(modClient.t("📜 Historia piosenek", "📜 Song History"));
        dialog.setResizable(true);
        dialog.setMinWidth(500);
        dialog.setMinHeight(400);

        VBox root = new VBox(10);
        root.setPadding(new Insets(16));
        root.getStyleClass().add("dialog-root");

        Label title = new Label(modClient.t("📜 Historia piosenek", "📜 Song History"));
        title.getStyleClass().add("dialog-title");

        Label hint = new Label(modClient.t(
                "Ostatnie 200 piosenek. Dwuklik = skopiuj tytuł.",
                "Last 200 songs. Double-click = copy title."));
        hint.getStyleClass().add("dialog-hint");

        // Lista historii
        List<RadioModClient.HistoryEntry> entries = modClient.getHistory();
        ListView<RadioModClient.HistoryEntry> list = new ListView<>(FXCollections.observableList(entries));
        list.getStyleClass().add("blacklist-view");
        VBox.setVgrow(list, Priority.ALWAYS);

        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(RadioModClient.HistoryEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }

                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);

                Label timeLbl = new Label(item.time);
                timeLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");
                timeLbl.setMinWidth(90);

                Label stationLbl = new Label("📻 " + item.station);
                stationLbl.setStyle("-fx-text-fill: #55FF55; -fx-font-size: 11px;");
                stationLbl.setMinWidth(120);
                stationLbl.setMaxWidth(140);

                Label songLbl = new Label("♪ " + item.song);
                songLbl.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
                songLbl.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(songLbl, Priority.ALWAYS);

                // Przycisk blokady
                Button blockBtn = new Button("🚫");
                blockBtn.setStyle("-fx-background-color: #550000; -fx-text-fill: white; " +
                        "-fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 1 4 1 4;");
                blockBtn.setTooltip(new Tooltip(modClient.t("Dodaj do czarnej listy", "Add to blacklist")));
                blockBtn.setOnAction(e -> {
                    modClient.addToBlacklist(item.song);
                    list.setItems(FXCollections.observableList(modClient.getHistory()));
                });

                row.getChildren().addAll(timeLbl, stationLbl, songLbl, blockBtn);
                setGraphic(row);
            }
        });

        // Dwuklik = kopiuj do schowka
        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                RadioModClient.HistoryEntry sel = list.getSelectionModel().getSelectedItem();
                if (sel != null) {
                    javafx.scene.input.Clipboard cb = javafx.scene.input.Clipboard.getSystemClipboard();
                    javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
                    cc.putString(sel.song);
                    cb.setContent(cc);
                    hint.setText(modClient.t("✓ Skopiowano: ", "✓ Copied: ") + sel.song);
                }
            }
        });

        // Przyciski dolne
        HBox btnRow = new HBox(8);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        Button clearBtn = new Button(modClient.t("🗑 Wyczyść historię", "🗑 Clear history"));
        clearBtn.getStyleClass().add("btn-stop");
        clearBtn.setOnAction(e -> {
            modClient.getHistory().clear();
            list.setItems(FXCollections.observableList(modClient.getHistory()));
        });

        Button closeBtn = new Button(modClient.t("Zamknij", "Close"));
        closeBtn.getStyleClass().add("btn-primary");
        closeBtn.setOnAction(e -> dialog.close());

        btnRow.getChildren().addAll(clearBtn, closeBtn);

        root.getChildren().addAll(title, hint, new Separator(), list, btnRow);

        Scene scene = new Scene(root, 580, 450);
        try { scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm()); }
        catch (Exception ignored) {}
        dialog.setScene(scene);
        dialog.show();
    }
}
