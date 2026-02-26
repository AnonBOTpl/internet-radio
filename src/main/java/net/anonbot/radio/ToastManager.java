package net.anonbot.radio;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class ToastManager {

    private final Stage ownerStage;
    private Stage currentToast;

    public ToastManager(Stage ownerStage) {
        this.ownerStage = ownerStage;
    }

    public void showToast(String stationName, String songName, String imgUrl, int durationSeconds, Runnable onBlacklist) {
        Platform.runLater(() -> {
            if (currentToast != null) { currentToast.close(); currentToast = null; }

            Stage toast = new Stage();
            toast.initStyle(StageStyle.TRANSPARENT);
            toast.setAlwaysOnTop(true);
            currentToast = toast;

            HBox root = new HBox(12);
            root.setPadding(new Insets(10, 14, 10, 14));
            root.setAlignment(Pos.CENTER_LEFT);
            root.setStyle("-fx-background-color: rgba(20,20,20,0.93); " +
                    "-fx-background-radius: 10; " +
                    "-fx-border-radius: 10; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 14, 0, 0, 4);");

            // Okładka / Logo / Nutka
            StackPane imgPane = new StackPane();
            imgPane.setPrefSize(40, 40);
            Label fallback = new Label("🎵");
            fallback.setStyle("-fx-font-size: 20px; -fx-text-fill: #888;");
            ImageView imgView = new ImageView();
            imgView.setFitWidth(40);
            imgView.setFitHeight(40);
            imgView.setPreserveRatio(true);
            
            if (imgUrl != null && !imgUrl.isEmpty()) {
                try { imgView.setImage(new Image(imgUrl, 40, 40, true, true, true)); } 
                catch (Exception ignored) {}
            }
            imgPane.getChildren().addAll(fallback, imgView);

            VBox textBox = new VBox(3);
            Label stLbl = new Label(stationName);
            stLbl.setStyle("-fx-text-fill: #55FF55; -fx-font-size: 12px; -fx-font-weight: bold;");
            stLbl.setMaxWidth(220);
            Label songLbl = new Label(songName);
            songLbl.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
            songLbl.setMaxWidth(220);
            textBox.getChildren().addAll(stLbl, songLbl);
            HBox.setHgrow(textBox, Priority.ALWAYS);

            Button blockBtn = new Button("🚫");
            blockBtn.setStyle("-fx-background-color: #880000; -fx-text-fill: white; " +
                    "-fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 14px; -fx-padding: 2 6 2 6;");
            blockBtn.setTooltip(new javafx.scene.control.Tooltip("Dodaj do czarnej listy"));
            blockBtn.setOnAction(e -> {
                if (onBlacklist != null) onBlacklist.run();
                toast.close();
            });

            root.getChildren().addAll(imgPane, textBox, blockBtn);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            toast.setScene(scene);
            toast.setWidth(340);
            toast.setHeight(70);

            positionToastOnScreen(toast);
            toast.show();

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
            fadeIn.setFromValue(0); fadeIn.setToValue(1); fadeIn.play();

            Thread timer = new Thread(() -> {
                try { Thread.sleep(durationSeconds * 1000L); }
                catch (InterruptedException ignored) {}
                Platform.runLater(() -> {
                    if (toast.isShowing()) {
                        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), root);
                        fadeOut.setFromValue(1); fadeOut.setToValue(0);
                        fadeOut.setOnFinished(e -> toast.close());
                        fadeOut.play();
                    }
                });
            }, "Toast-Timer");
            timer.setDaemon(true);
            timer.start();
        });
    }

    private void positionToastOnScreen(Stage toast) {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double x = screenBounds.getMaxX() - toast.getWidth() - 20;
        double y = screenBounds.getMaxY() - toast.getHeight() - 20;
        toast.setX(x);
        toast.setY(y);
    }
}