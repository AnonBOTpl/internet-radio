package net.anonbot.radio;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class MiniPlayer {

    private static final double WIDTH  = 490;
    private static final double HEIGHT = 60;

    private final Stage miniStage;
    private final Stage mainStage;
    private final RadioModClient modClient;

    private Label stationLbl;
    private Label songLbl;
    private Button playBtn;
    private Button pinBtn;
    private HBox root;
    private boolean visible = false;
    private double dragOffsetX;
    
    private ImageView imgView;
    private Label fallbackIcon;

    public MiniPlayer(Stage mainStage, RadioModClient modClient) {
        this.mainStage  = mainStage;
        this.modClient  = modClient;
        this.miniStage  = new Stage();
        build();
        setupCallbacks();
    }

    private void build() {
        miniStage.initStyle(StageStyle.TRANSPARENT);
        miniStage.setAlwaysOnTop(modClient.isMiniPlayerAlwaysOnTop());
        miniStage.setTitle("Radio Mini");

        root = new HBox(8);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(0, 10, 0, 10));
        root.setPrefWidth(WIDTH);
        root.setPrefHeight(HEIGHT);
        applyRootStyle();

        // === GRAFIKA ALBUMU / LOGO ===
        StackPane imgPane = new StackPane();
        imgPane.setPrefSize(44, 44);
        fallbackIcon = new Label("🎵");
        fallbackIcon.setStyle("-fx-font-size: 24px; -fx-text-fill: #888;");
        imgView = new ImageView();
        imgView.setFitWidth(44);
        imgView.setFitHeight(44);
        imgView.setPreserveRatio(true);
        imgPane.getChildren().addAll(fallbackIcon, imgView);

        // === TEKSTY — klik przywraca główne okno ===
        VBox texts = new VBox(2);
        texts.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(texts, Priority.ALWAYS);
        texts.setCursor(Cursor.HAND);
        texts.setOnMouseClicked(e -> restoreMain());
        Tooltip.install(texts, new Tooltip(modClient.t("Kliknij aby przywrócić okno", "Click to restore window")));

        stationLbl = new Label(modClient.t("Brak stacji", "No station"));
        stationLbl.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
        stationLbl.setMaxWidth(180);

        songLbl = new Label("♪ ...");
        songLbl.setStyle("-fx-text-fill: #bbb; -fx-font-size: 11px; -fx-font-style: italic;");
        songLbl.setMaxWidth(180);

        texts.getChildren().addAll(stationLbl, songLbl);

        // === PRZYCISKI - Twarde rozmiary naprawiające błąd "..." ===
        playBtn = new Button(modClient.isPlaying() ? "⏹" : "▶");
        playBtn.setMinSize(40, 32);
        playBtn.setPadding(new Insets(0));
        playBtn.setStyle(playBtnStyle(modClient.isPlaying()));
        playBtn.setOnAction(e -> {
            if (modClient.isPlaying()) {
                modClient.stopRadio();
            } else {
                String lastUrl  = modClient.getCurrentStationUrl();
                String lastName = modClient.getCurrentStationName();
                if (lastUrl != null && !lastUrl.isEmpty()) {
                    modClient.playStation(lastName, lastUrl, modClient.getCurrentStationFavicon());
                } else {
                    restoreMain();
                }
            }
        });

        Slider volSlider = new Slider(0, 1, modClient.getVolume());
        volSlider.setPrefWidth(70);
        volSlider.setStyle("-fx-padding: 0;");
        volSlider.valueProperty().addListener((obs, o, nv) -> modClient.setVolume(nv.floatValue()));

        boolean pinned = modClient.isMiniPlayerAlwaysOnTop();
        pinBtn = new Button(pinned ? "📌" : "📍");
        pinBtn.setMinSize(34, 32);
        pinBtn.setPadding(new Insets(0));
        pinBtn.setStyle(pinBtnStyle(pinned));
        Tooltip.install(pinBtn, new Tooltip(modClient.t("Zawsze na wierzchu", "Always on top")));
        pinBtn.setOnAction(e -> {
            boolean nowPinned = !modClient.isMiniPlayerAlwaysOnTop();
            modClient.setMiniPlayerAlwaysOnTop(nowPinned);
            miniStage.setAlwaysOnTop(nowPinned);
            pinBtn.setText(nowPinned ? "📌" : "📍");
            pinBtn.setStyle(pinBtnStyle(nowPinned));
        });

        Button restoreBtn = new Button("⬆");
        restoreBtn.setMinSize(34, 32);
        restoreBtn.setPadding(new Insets(0));
        restoreBtn.setStyle(iconBtnStyle());
        restoreBtn.setOnMouseEntered(e -> restoreBtn.setStyle(iconBtnStyleHover()));
        restoreBtn.setOnMouseExited(e -> restoreBtn.setStyle(iconBtnStyle()));
        restoreBtn.setOnAction(e -> restoreMain());
        Tooltip.install(restoreBtn, new Tooltip(modClient.t("Przywróć okno", "Restore window")));

        Button closeBtn = new Button("✕");
        closeBtn.setMinSize(34, 32);
        closeBtn.setPadding(new Insets(0));
        closeBtn.setStyle("-fx-background-color: rgba(160,0,0,0.65); -fx-text-fill: white;" +
            "-fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 14px;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: rgba(220,0,0,0.95); -fx-text-fill: white;" +
            "-fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 14px;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: rgba(160,0,0,0.65); -fx-text-fill: white;" +
            "-fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 14px;"));
        closeBtn.setOnAction(e -> {
            modClient.stopRadio();
            miniStage.close();
            Platform.exit();
            System.exit(0);
        });
        Tooltip.install(closeBtn, new Tooltip(modClient.t("Zamknij aplikację", "Quit")));

        root.setOnMousePressed(e -> dragOffsetX = e.getSceneX());
        root.setOnMouseDragged(e -> {
            miniStage.setX(e.getScreenX() - dragOffsetX);
            snapToBottom();
        });

        root.getChildren().addAll(imgPane, texts, playBtn, volSlider, pinBtn, restoreBtn, closeBtn);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.setFill(Color.TRANSPARENT);
        miniStage.setScene(scene);
    }

    private void applyRootStyle() {
        String accent = modClient.getAccentColor();
        root.setStyle(
            "-fx-background-color: linear-gradient(to right, rgba(8,8,16,0.97), rgba(" + hexToRgb(accent) + ",0.82));" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;" +
            "-fx-border-color: rgba(" + hexToRgb(accent) + ",0.55);" +
            "-fx-border-width: 1.2;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 18, 0, 0, 5);"
        );
    }

    private void setupCallbacks() {
        modClient.addOnSongChanged(song -> Platform.runLater(() -> {
            stationLbl.setText("📻 " + truncate(modClient.getCurrentStationName(), 22));
            songLbl.setText("♪ " + truncate(song, 30));
            updatePlayBtn();
        }));
        modClient.addOnPlayStateChanged(() -> Platform.runLater(this::updatePlayBtn));
        modClient.addOnAlbumArtChanged(url -> Platform.runLater(() -> updateImage(url)));
    }
    
    private void updateImage(String albumUrl) {
        String target = (albumUrl != null && !albumUrl.isEmpty()) ? albumUrl : modClient.getCurrentStationFavicon();
        if (target != null && !target.isEmpty()) {
            try { imgView.setImage(new Image(target, 44, 44, true, true, true)); } 
            catch (Exception ignored) { imgView.setImage(null); }
        } else {
            imgView.setImage(null);
        }
    }

    private void updatePlayBtn() {
        boolean playing = modClient.isPlaying();
        playBtn.setText(playing ? "⏹" : "▶");
        playBtn.setStyle(playBtnStyle(playing));
        if (!playing) {
            stationLbl.setText(modClient.t("Brak stacji", "No station"));
            songLbl.setText("♪ ...");
        } else {
            updateImage(modClient.getCurrentAlbumArt());
        }
    }

    public void show() {
        if (visible) return;
        visible = true;
        miniStage.setAlwaysOnTop(modClient.isMiniPlayerAlwaysOnTop());
        applyRootStyle();
        positionAtBottom();
        miniStage.show();
        if (modClient.isPlaying()) {
            stationLbl.setText("📻 " + truncate(modClient.getCurrentStationName(), 22));
            String song = modClient.getLastSongName();
            songLbl.setText("♪ " + (song.isEmpty() ? "..." : truncate(song, 30)));
            updateImage(modClient.getCurrentAlbumArt());
        }
        updatePlayBtn();
        root.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(220), root);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    public void hide() {
        if (!visible) return;
        visible = false;
        FadeTransition ft = new FadeTransition(Duration.millis(180), root);
        ft.setFromValue(1); ft.setToValue(0);
        ft.setOnFinished(e -> miniStage.hide());
        ft.play();
    }

    private void restoreMain() {
        hide();
        mainStage.show();
        mainStage.setIconified(false);
        mainStage.toFront();
        mainStage.requestFocus();
    }

    private void positionAtBottom() {
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        miniStage.setX(screen.getMinX() + (screen.getWidth() - WIDTH) / 2.0);
        snapToBottom();
    }

    private void snapToBottom() {
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        miniStage.setY(screen.getMaxY() - HEIGHT - 6);
    }

    private String playBtnStyle(boolean playing) {
        String bg  = playing ? "rgba(160,0,0,0.75)"   : "rgba(0,110,0,0.75)";
        return "-fx-background-color: " + bg + "; -fx-text-fill: white;" +
               "-fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 16px;";
    }

    private String pinBtnStyle(boolean pinned) {
        String bg = pinned ? "rgba(180,120,0,0.75)" : "rgba(60,60,80,0.75)";
        return "-fx-background-color: " + bg + "; -fx-text-fill: white;" +
               "-fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 14px;";
    }

    private String iconBtnStyle() {
        return "-fx-background-color: rgba(255,255,255,0.12); -fx-text-fill: white;" +
               "-fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 14px;";
    }

    private String iconBtnStyleHover() {
        return "-fx-background-color: rgba(255,255,255,0.28); -fx-text-fill: white;" +
               "-fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 14px;";
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    private String hexToRgb(String hex) {
        try {
            return Integer.parseInt(hex.substring(1,3),16) + "," +
                   Integer.parseInt(hex.substring(3,5),16) + "," +
                   Integer.parseInt(hex.substring(5,7),16);
        } catch (Exception e) { return "83,52,131"; }
    }
}