package net.anonbot.radio;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gson.*;

public class RadioScreen {

    private final RadioModClient modClient;
    private final Stage stage;
    private final BorderPane root;
    private Runnable onSwitchToMiniPlayer;

    record StationItem(String name, String url, String favicon) {
        @Override public String toString() { return name; }
    }
    record StationRow(StationItem left, StationItem right) {}

    private ListView<StationRow> stationList;
    private TextField searchField;
    private ComboBox<String> countryBox;
    private ComboBox<String> genreBox;
    private Label statusLabel;
    private Label songLabel;
    private Label stationLabel;
    private Label countryLbl;
    private Label genreLbl;
    private Button playStopBtn;
    private Button recordBtn;
    private Button searchBtn;
    private Button favBtn;
    private Button settingsBtn;
    private Button blacklistBtn;
    private Button historyBtn;
    private Button sleepTimerBtn;
    private TitledPane manualPane;
    private Button manualPlayBtn;
    private Button manualFavBtn;
    private TextField manualUrlField;
    private TextField manualNameField;
    private boolean showFavoritesMode = false;
    private ToastManager toastManager;
    
    private ImageView albumArtView;
    private int sleepTimerIndex = 0; // 0=OFF, 1=15, 2=30, 3=45, 4=60

    public RadioScreen(RadioModClient modClient, Stage stage) {
        this.modClient = modClient;
        this.stage = stage;
        this.root = new BorderPane();
        this.toastManager = new ToastManager(stage);
        build();
        setupCallbacks();
        performSearch("");
    }

    public void setOnSwitchToMiniPlayer(Runnable r) { this.onSwitchToMiniPlayer = r; }
    public BorderPane getRoot() { return root; }

    private void build() {
        root.getStyleClass().add("main-root");

        VBox topArea = new VBox();
        topArea.getChildren().add(buildTitleBar());

        VBox topBox = new VBox(8);
        topBox.setPadding(new Insets(10, 16, 8, 16));
        topBox.getStyleClass().add("top-bar");

        HBox searchRow = new HBox(8);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchField = new TextField();
        searchField.setPromptText(modClient.t("🔍 Szukaj stacji...", "🔍 Search stations..."));
        searchField.getStyleClass().add("search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.setOnAction(e -> performSearch(searchField.getText()));

        searchBtn = new Button(modClient.t("Szukaj", "Search"));
        searchBtn.getStyleClass().add("btn-primary");
        searchBtn.setOnAction(e -> performSearch(searchField.getText()));

        favBtn = new Button(modClient.t("★ Ulubione", "★ Favorites"));
        favBtn.getStyleClass().add("btn-fav");
        favBtn.setOnAction(e -> toggleFavorites());

        searchRow.getChildren().addAll(searchField, searchBtn, favBtn);

        HBox filterRow = new HBox(8);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        countryBox = new ComboBox<>();
        updateCountryBox();
        countryBox.getStyleClass().add("filter-box");
        countryBox.setOnAction(e -> performSearch(searchField.getText()));

        genreBox = new ComboBox<>();
        updateGenreBox();
        genreBox.getStyleClass().add("filter-box");
        genreBox.setOnAction(e -> performSearch(searchField.getText()));

        countryLbl = new Label(modClient.t("Kraj:", "Country:"));
        countryLbl.getStyleClass().add("filter-label");
        genreLbl = new Label(modClient.t("Gatunek:", "Genre:"));
        genreLbl.getStyleClass().add("filter-label");

        filterRow.getChildren().addAll(countryLbl, countryBox, genreLbl, genreBox);

        manualPane = buildManualPane();
        topBox.getChildren().addAll(searchRow, filterRow, manualPane);
        
        topArea.getChildren().add(topBox);
        root.setTop(topArea);

        stationList = new ListView<>();
        stationList.getStyleClass().add("station-list");
        stationList.setCellFactory(lv -> new StationCell());
        root.setCenter(stationList);

        root.setBottom(buildPlayerBar());
    }

    private HBox buildTitleBar() {
        HBox bar = new HBox(10);
        bar.getStyleClass().add("custom-title-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 12, 6, 12));

        Label title = new Label(modClient.t("🎵 Radio Internetowe", "🎵 Internet Radio"));
        title.getStyleClass().add("window-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button miniBtn = new Button("▼");
        miniBtn.getStyleClass().add("window-btn");
        Tooltip.install(miniBtn, new Tooltip(modClient.t("Przełącz na MiniPlayer", "Switch to MiniPlayer")));
        miniBtn.setOnAction(e -> {
            stage.hide();
            if (onSwitchToMiniPlayer != null) onSwitchToMiniPlayer.run();
        });

        Button minBtn = new Button("—");
        minBtn.getStyleClass().add("window-btn");
        Tooltip.install(minBtn, new Tooltip(modClient.t("Minimalizuj", "Minimize")));
        minBtn.setOnAction(e -> stage.setIconified(true));

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("window-btn-close");
        Tooltip.install(closeBtn, new Tooltip(modClient.t("Zamknij", "Close")));
        closeBtn.setOnAction(e -> {
            modClient.stopRadio();
            Platform.exit();
            System.exit(0);
        });

        bar.getChildren().addAll(title, spacer, miniBtn, minBtn, closeBtn);

        class DragContext { double x, y; }
        final DragContext dragCtx = new DragContext();
        bar.setOnMousePressed(e -> {
            dragCtx.x = stage.getX() - e.getScreenX();
            dragCtx.y = stage.getY() - e.getScreenY();
        });
        bar.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() + dragCtx.x);
            stage.setY(e.getScreenY() + dragCtx.y);
        });

        return bar;
    }

    private TitledPane buildManualPane() {
        HBox manualRow = new HBox(8);
        manualRow.setAlignment(Pos.CENTER_LEFT);
        manualRow.setPadding(new Insets(4, 0, 4, 0));

        manualNameField = new TextField();
        manualNameField.setPromptText(modClient.t("Nazwa stacji...", "Station name..."));
        manualNameField.setPrefWidth(150);

        manualUrlField = new TextField();
        manualUrlField.setPromptText("URL (http://...)");
        HBox.setHgrow(manualUrlField, Priority.ALWAYS);

        manualPlayBtn = new Button(modClient.t("▶ Graj", "▶ Play"));
        manualPlayBtn.getStyleClass().add("btn-primary");
        manualPlayBtn.setOnAction(e -> {
            String url = manualUrlField.getText().trim();
            String name = manualNameField.getText().trim();
            if (url.isEmpty()) return;
            if (name.isEmpty()) name = modClient.t("Własna stacja", "Custom station");
            modClient.playStation(name, url, "");
        });

        manualFavBtn = new Button(modClient.t("★ Do ulubionych", "★ Add favorite"));
        manualFavBtn.getStyleClass().add("btn-fav");
        manualFavBtn.setOnAction(e -> {
            String url = manualUrlField.getText().trim();
            String name = manualNameField.getText().trim();
            if (url.isEmpty()) return;
            if (name.isEmpty()) name = modClient.t("Własna stacja", "Custom station");
            modClient.toggleFavorite(name, url, "");
            statusLabel.setText(modClient.t("Dodano do ulubionych: ", "Added to favorites: ") + name);
        });

        manualRow.getChildren().addAll(manualNameField, manualUrlField, manualPlayBtn, manualFavBtn);

        TitledPane tp = new TitledPane(modClient.t("➕ Dodaj własną stację (URL)", "➕ Add custom station (URL)"), manualRow);
        tp.setExpanded(false);
        tp.getStyleClass().add("manual-pane");
        return tp;
    }

    private VBox buildPlayerBar() {
        VBox bar = new VBox(5);
        bar.setPadding(new Insets(10, 16, 10, 16));
        bar.getStyleClass().add("player-bar");

        HBox infoRow = new HBox(16);
        infoRow.setAlignment(Pos.CENTER_LEFT);
        
        albumArtView = new ImageView();
        albumArtView.setFitWidth(55);
        albumArtView.setFitHeight(55);
        albumArtView.setPreserveRatio(true);
        StackPane albumPane = new StackPane(albumArtView);
        albumPane.setPrefSize(55, 55);
        albumPane.setStyle("-fx-background-color: #000; -fx-background-radius: 4; -fx-border-radius: 4; -fx-border-color: #333;");

        VBox textInfoBox = new VBox(2);
        stationLabel = new Label(modClient.t("Brak stacji", "No station"));
        stationLabel.getStyleClass().add("station-label");
        songLabel = new Label("♪ ...");
        songLabel.getStyleClass().add("song-label");
        textInfoBox.getChildren().addAll(stationLabel, songLabel);
        HBox.setHgrow(textInfoBox, Priority.ALWAYS);

        statusLabel = new Label(modClient.t("Gotowy", "Ready"));
        statusLabel.getStyleClass().add("status-label");

        infoRow.getChildren().addAll(albumPane, textInfoBox);

        HBox controlRow = new HBox(8);
        controlRow.setAlignment(Pos.CENTER_LEFT);

        playStopBtn = new Button(modClient.t("▶ Graj", "▶ Play"));
        playStopBtn.getStyleClass().add("btn-play");
        playStopBtn.setMinWidth(110);
        playStopBtn.setOnAction(e -> {
            if (modClient.isPlaying()) {
                modClient.stopRadio();
            } else {
                String lastUrl = modClient.getCurrentStationUrl();
                if (lastUrl != null && !lastUrl.isEmpty()) {
                    modClient.playStation(modClient.getCurrentStationName(), lastUrl, modClient.getCurrentStationFavicon());
                } else {
                    statusLabel.setText(modClient.t("Wybierz stację z listy!", "Select station from the list!"));
                }
            }
        });

        recordBtn = new Button(modClient.t("⏺ Nagraj", "⏺ Rec"));
        recordBtn.getStyleClass().add("btn-record");
        recordBtn.setOnAction(e -> toggleRecording());

        settingsBtn = new Button(modClient.t("⚙ Ustawienia", "⚙ Settings"));
        settingsBtn.getStyleClass().add("btn-secondary");
        settingsBtn.setOnAction(e -> new SettingsDialog(modClient, stage).show());

        blacklistBtn = new Button(modClient.t("🚫 Blacklist", "🚫 Blacklist"));
        blacklistBtn.getStyleClass().add("btn-secondary");
        blacklistBtn.setOnAction(e -> new BlacklistDialog(modClient, stage).show());

        historyBtn = new Button(modClient.t("📜 Historia", "📜 History"));
        historyBtn.getStyleClass().add("btn-secondary");
        historyBtn.setOnAction(e -> new HistoryDialog(modClient, stage).show());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        sleepTimerBtn = new Button();
        sleepTimerBtn.getStyleClass().add("btn-secondary");
        updateSleepTimerBtnText(); // Inicjalizacja tekstu
        sleepTimerBtn.setOnAction(e -> {
            sleepTimerIndex = (sleepTimerIndex + 1) % 5;
            int mins = sleepTimerIndex * 15; // 0, 15, 30, 45, 60
            modClient.setSleepTimer(mins);
            updateSleepTimerBtnText();
        });

        Label volLbl = new Label("🔊");
        volLbl.setStyle("-fx-font-size: 14px;");

        Slider volumeSlider = new Slider(0, 1, modClient.getVolume());
        volumeSlider.setPrefWidth(130);
        volumeSlider.getStyleClass().add("volume-slider");
        volumeSlider.valueProperty().addListener((obs, old, nv) -> modClient.setVolume(nv.floatValue()));

        Label volPctLbl = new Label(Math.round(modClient.getVolume() * 100) + "%");
        volPctLbl.setMinWidth(36);
        volPctLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12px;");
        volumeSlider.valueProperty().addListener((obs, old, nv) ->
                volPctLbl.setText(Math.round(nv.doubleValue() * 100) + "%"));

        Label resizeGrip = new Label("↘");
        resizeGrip.setStyle("-fx-font-size: 16px; -fx-text-fill: #666; -fx-cursor: se-resize; -fx-padding: 0 0 0 10;");
        class ResizeContext { double startW, startH, startX, startY; }
        final ResizeContext rCtx = new ResizeContext();
        resizeGrip.setOnMousePressed(e -> {
            rCtx.startX = e.getScreenX(); rCtx.startY = e.getScreenY();
            rCtx.startW = stage.getWidth(); rCtx.startH = stage.getHeight();
        });
        resizeGrip.setOnMouseDragged(e -> {
            double newW = rCtx.startW + (e.getScreenX() - rCtx.startX);
            double newH = rCtx.startH + (e.getScreenY() - rCtx.startY);
            if (newW >= 920) stage.setWidth(newW);
            if (newH >= 560) stage.setHeight(newH);
        });

        controlRow.getChildren().addAll(
            playStopBtn, recordBtn, settingsBtn, blacklistBtn, historyBtn,
            spacer, sleepTimerBtn, volLbl, volumeSlider, volPctLbl, resizeGrip
        );

        bar.getChildren().addAll(infoRow, controlRow, statusLabel);
        return bar;
    }
    
    // Zapewnia prawidłowe odświeżanie języka i kolorowania guzika Timer'a
    private void updateSleepTimerBtnText() {
        int mins = sleepTimerIndex * 15;
        String textKey = "Wyłącz za: OFF";
        String enKey = "Sleep timer: OFF";
        
        if (mins == 15) { textKey = "Wyłącz za: 15m"; enKey = "Sleep timer: 15m"; }
        else if (mins == 30) { textKey = "Wyłącz za: 30m"; enKey = "Sleep timer: 30m"; }
        else if (mins == 45) { textKey = "Wyłącz za: 45m"; enKey = "Sleep timer: 45m"; }
        else if (mins == 60) { textKey = "Wyłącz za: 60m"; enKey = "Sleep timer: 60m"; }
        
        sleepTimerBtn.setText("🌙 " + modClient.t(textKey, enKey));
        
        if (mins > 0) sleepTimerBtn.setStyle("-fx-text-fill: #55FF55; -fx-font-weight: bold;");
        else sleepTimerBtn.setStyle("-fx-text-fill: #ccc; -fx-font-weight: bold;");
    }

    private void setupCallbacks() {
        modClient.addOnSongChanged(song -> {
            songLabel.setText("♪ " + song);
            statusLabel.setText(modClient.t("▶ Gra: ", "▶ Playing: ") + modClient.getCurrentStationName());
            if (modClient.isShowToast()) {
                // getCurrentAlbumArt() jest już aktualne - onSongChanged odpala się PO załadowaniu okładki
                String img = modClient.getCurrentAlbumArt() != null
                        ? modClient.getCurrentAlbumArt()
                        : modClient.getCurrentStationFavicon();
                toastManager.showToast(modClient.getCurrentStationName(), song, img,
                        modClient.getToastDuration(), () -> {
                            modClient.addToBlacklist(song);
                            statusLabel.setText(modClient.t("🚫 Dodano do czarnej listy: ", "🚫 Blacklisted: ") + song);
                        });
            }
        });
        
        modClient.addOnAlbumArtChanged(url -> {
            if (url == null) albumArtView.setImage(null);
            else albumArtView.setImage(new Image(url, 55, 55, true, true, true));
        });

        modClient.setOnStatusChanged(status -> {
            statusLabel.setText(status);
            if (modClient.isPlaying()) stationLabel.setText("📻 " + modClient.getCurrentStationName());
        });
        modClient.setOnPlayStateChanged(this::updatePlayButton);
        modClient.setOnLanguageChanged(this::reloadLanguage);
    }

    private void updatePlayButton() {
        String lastUrl = modClient.getCurrentStationUrl();
        if (modClient.isPlaying()) {
            playStopBtn.setText(modClient.t("⏹ Stop", "⏹ Stop"));
            playStopBtn.getStyleClass().removeAll("btn-play");
            if (!playStopBtn.getStyleClass().contains("btn-stop")) playStopBtn.getStyleClass().add("btn-stop");
                
            stationLabel.setText("📻 " + modClient.getCurrentStationName());
            stationLabel.setStyle("-fx-text-fill: " + modClient.getAccentColor() + "; -fx-font-size: 20px; -fx-font-weight: bold;");
        } else {
            playStopBtn.setText(modClient.t("▶ Graj", "▶ Play"));
            playStopBtn.getStyleClass().removeAll("btn-stop");
            if (!playStopBtn.getStyleClass().contains("btn-play")) playStopBtn.getStyleClass().add("btn-play");
                
            if(lastUrl != null && !lastUrl.isEmpty()) {
                stationLabel.setText("📻 " + modClient.getCurrentStationName() + " (" + modClient.t("Wstrzymano", "Paused") + ")");
            } else {
                stationLabel.setText(modClient.t("Brak stacji", "No station"));
            }
            stationLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 20px; -fx-font-weight: bold;");
            songLabel.setText("♪ ...");
            albumArtView.setImage(null);
        }
    }

    public void applyAccentColors() {
        String a = modClient.getAccentColor();
        String al = modClient.getAccentColorLight();

        String aDark   = darken(a, 0.15);
        String aMid    = darken(a, 0.35);
        String aBorder = darken(a, 0.50);

        root.setStyle("-fx-background-color: " + aDark + ";");

        root.lookupAll(".custom-title-bar").forEach(n -> n.setStyle(
            "-fx-background-color: " + darken(a, 0.45) + ";" +
            "-fx-border-color: " + darken(a, 0.6) + ";" +
            "-fx-border-width: 0 0 1 0;"));

        root.lookupAll(".top-bar").forEach(n -> n.setStyle(
            "-fx-background-color: " + aMid + ";" +
            "-fx-border-color: " + aBorder + ";" +
            "-fx-border-width: 0 0 1 0;"));

        root.lookupAll(".player-bar").forEach(n -> n.setStyle(
            "-fx-background-color: " + aMid + ";" +
            "-fx-border-color: " + a + ";" +
            "-fx-border-width: 2 0 0 0;"));

        root.lookupAll(".station-list").forEach(n -> n.setStyle(
            "-fx-background-color: " + aDark + ";" +
            "-fx-border-color: " + aBorder + ";" +
            "-fx-border-width: 1 0 1 0;"));

        root.lookupAll(".search-field").forEach(n -> n.setStyle(
            "-fx-background-color: " + darken(a, 0.08) + ";" +
            "-fx-text-fill: white;" +
            "-fx-prompt-text-fill: #666;" +
            "-fx-border-color: " + aBorder + ";" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-font-size: 13px;" +
            "-fx-padding: 6 10 6 10;"));

        root.lookupAll(".filter-box").forEach(n -> n.setStyle(
            "-fx-background-color: " + darken(a, 0.08) + ";" +
            "-fx-text-fill: white;" +
            "-fx-border-color: " + aBorder + ";" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;"));

        root.lookupAll(".manual-pane > .title").forEach(n -> n.setStyle(
            "-fx-background-color: " + aMid + "; -fx-text-fill: #aaa;"));
        root.lookupAll(".manual-pane > .content").forEach(n -> n.setStyle(
            "-fx-background-color: " + aMid + ";" +
            "-fx-border-color: " + aBorder + ";"));

        root.lookupAll(".btn-secondary").forEach(n -> {
            n.setStyle("-fx-background-color: " + darken(a, 0.3) + "; -fx-text-fill: #ddd; " +
                "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 5 10 5 10;");
            n.setOnMouseEntered(e -> n.setStyle("-fx-background-color: " + darken(a, 0.2) + "; -fx-text-fill: white; " +
                "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 5 10 5 10;"));
            n.setOnMouseExited(e -> n.setStyle("-fx-background-color: " + darken(a, 0.3) + "; -fx-text-fill: #ddd; " +
                "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 5 10 5 10;"));
        });

        // Musimy powtórzyć nałożenie kolorów na Sleep Timer, ponieważ n.setStyle() je resetuje
        updateSleepTimerBtnText(); 

        root.lookupAll(".btn-primary").forEach(n -> {
            n.setStyle("-fx-background-color: " + a + "; -fx-text-fill: white; " +
                "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 5 10 5 10;");
            n.setOnMouseEntered(e -> n.setStyle("-fx-background-color: " + al + "; -fx-text-fill: white; " +
                "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 5 10 5 10;"));
            n.setOnMouseExited(e -> n.setStyle("-fx-background-color: " + a + "; -fx-text-fill: white; " +
                "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 5 10 5 10;"));
        });

        root.lookupAll(".volume-slider .track").forEach(n -> n.setStyle("-fx-background-color: " + aBorder + ";"));
        root.lookupAll(".volume-slider .thumb").forEach(n -> n.setStyle("-fx-background-color: " + a + ";"));

        if (stationList != null) stationList.refresh();
        updatePlayButton();
    }

    private String darken(String hex, double ratio) {
        try {
            int r = Integer.parseInt(hex.substring(1,3), 16);
            int g = Integer.parseInt(hex.substring(3,5), 16);
            int b = Integer.parseInt(hex.substring(5,7), 16);
            return String.format("#%02x%02x%02x", (int)(r * ratio), (int)(g * ratio), (int)(b * ratio));
        } catch (Exception e) { return hex; }
    }

    private void reloadLanguage() {
        stage.setTitle(modClient.t("🎵 Radio Internetowe", "🎵 Internet Radio"));
        searchField.setPromptText(modClient.t("🔍 Szukaj stacji...", "🔍 Search stations..."));
        searchBtn.setText(modClient.t("Szukaj", "Search"));
        favBtn.setText(showFavoritesMode
                ? modClient.t("🔙 Pokaż wszystkie", "🔙 Show all")
                : modClient.t("★ Ulubione", "★ Favorites"));
        countryLbl.setText(modClient.t("Kraj:", "Country:"));
        genreLbl.setText(modClient.t("Gatunek:", "Genre:"));
        manualPane.setText(modClient.t("➕ Dodaj własną stację (URL)", "➕ Add custom station (URL)"));
        manualNameField.setPromptText(modClient.t("Nazwa stacji...", "Station name..."));
        manualPlayBtn.setText(modClient.t("▶ Graj", "▶ Play"));
        manualFavBtn.setText(modClient.t("★ Do ulubionych", "★ Add favorite"));
        recordBtn.setText(modClient.isRecording()
                ? modClient.t("⏹ Stop", "⏹ Stop")
                : modClient.t("⏺ Nagraj", "⏺ Rec"));
        settingsBtn.setText(modClient.t("⚙ Ustawienia", "⚙ Settings"));
        blacklistBtn.setText(modClient.t("🚫 Blacklist", "🚫 Blacklist"));
        historyBtn.setText(modClient.t("📜 Historia", "📜 History"));
        statusLabel.setText(modClient.t("Gotowy", "Ready"));

        int savedCountry = countryBox.getSelectionModel().getSelectedIndex();
        int savedGenre = genreBox.getSelectionModel().getSelectedIndex();
        updateCountryBox();
        updateGenreBox();
        countryBox.getSelectionModel().select(savedCountry);
        genreBox.getSelectionModel().select(savedGenre);

        updateSleepTimerBtnText(); // Tłumaczymy Sleep Timer
        updatePlayButton();
        applyAccentColors();
    }

    private void updateCountryBox() {
        String[] countries = {
                modClient.t("🌍 Cały Świat", "🌍 Worldwide"),
                modClient.t("🇵🇱 Polska", "🇵🇱 Poland"),
                "🇺🇸 USA",
                modClient.t("🇬🇧 Wielka Brytania", "🇬🇧 United Kingdom"),
                modClient.t("🇩🇪 Niemcy", "🇩🇪 Germany"),
                modClient.t("🇫🇷 Francja", "🇫🇷 France"),
                modClient.t("🇮🇹 Włochy", "🇮🇹 Italy"),
                modClient.t("🇯🇵 Japonia", "🇯🇵 Japan")
        };
        countryBox.getItems().setAll(countries);
        countryBox.getSelectionModel().selectFirst();
    }

    private void updateGenreBox() {
        String[] genres = {
                modClient.t("🎵 Wszystkie gatunki", "🎵 All genres"),
                "Pop", "Rock", "Hip Hop", "Electronic", "Jazz", "Classical", "News"
        };
        genreBox.getItems().setAll(genres);
        genreBox.getSelectionModel().selectFirst();
    }

    private void performSearch(String query) {
        if (showFavoritesMode) { loadFavorites(); return; }
        stationList.getItems().clear();
        stationList.getItems().add(new StationRow(new StationItem(modClient.t("⏳ Wyszukiwanie...", "⏳ Searching..."), "", ""), null));

        String countryCode = getCountryCode(countryBox.getSelectionModel().getSelectedItem());
        String genre = getGenreValue(genreBox.getSelectionModel().getSelectedItem());

        CompletableFuture.runAsync(() -> {
            try {
                // Bezpieczne kodowanie URL ze spacjami jako %20, którego wymaga silnik URI.create() Javy
                String q = URLEncoder.encode(query, StandardCharsets.UTF_8).replace("+", "%20");
                
                StringBuilder urlBuilder = new StringBuilder("https://de1.api.radio-browser.info/json/stations/search?limit=80&hidebroken=true&order=clickcount&reverse=true");
                if (!q.isEmpty()) urlBuilder.append("&name=").append(q);
                if (!countryCode.isEmpty()) urlBuilder.append("&countrycode=").append(countryCode);
                if (!genre.isEmpty()) urlBuilder.append("&tag=").append(genre);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(urlBuilder.toString()))
                        .header("User-Agent", "RadioApp/1.0").build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

                JsonArray arr = JsonParser.parseString(resp.body()).getAsJsonArray();
                Platform.runLater(() -> {
                    stationList.getItems().clear();
                    if (arr.isEmpty()) {
                        stationList.getItems().add(new StationRow(new StationItem(modClient.t("Brak wyników", "No results"), "", ""), null));
                        return;
                    }
                    
                    StationItem tempLeft = null;
                    for (JsonElement el : arr) {
                        JsonObject o = el.getAsJsonObject();
                        String name = o.has("name") ? o.get("name").getAsString() : "?";
                        String stUrl = o.has("url_resolved") ? o.get("url_resolved").getAsString() : "";
                        String favicon = o.has("favicon") ? o.get("favicon").getAsString() : "";
                        
                        if (!stUrl.isEmpty()) {
                            StationItem current = new StationItem(name.trim(), stUrl, favicon);
                            if (tempLeft == null) { tempLeft = current; } 
                            else { stationList.getItems().add(new StationRow(tempLeft, current)); tempLeft = null; }
                        }
                    }
                    if (tempLeft != null) { stationList.getItems().add(new StationRow(tempLeft, null)); }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    stationList.getItems().clear();
                    stationList.getItems().add(new StationRow(new StationItem(modClient.t("❌ Błąd połączenia z API", "❌ API connection error"), "", ""), null));
                });
            }
        });
    }

    private void loadFavorites() {
        stationList.getItems().clear();
        List<RadioModClient.FavoriteStation> favs = modClient.getFavorites();
        if (favs.isEmpty()) {
            stationList.getItems().add(new StationRow(new StationItem(modClient.t("Brak ulubionych stacji", "No favorite stations"), "", ""), null));
        } else {
            StationItem tempLeft = null;
            for (RadioModClient.FavoriteStation fs : favs) {
                StationItem current = new StationItem(fs.name, fs.url, fs.favicon);
                if (tempLeft == null) { tempLeft = current; } 
                else { stationList.getItems().add(new StationRow(tempLeft, current)); tempLeft = null; }
            }
            if (tempLeft != null) { stationList.getItems().add(new StationRow(tempLeft, null)); }
        }
    }

    private void toggleFavorites() {
        showFavoritesMode = !showFavoritesMode;
        if (showFavoritesMode) {
            favBtn.setText(modClient.t("🔙 Pokaż wszystkie", "🔙 Show all"));
            favBtn.getStyleClass().remove("btn-fav");
            favBtn.getStyleClass().add("btn-primary");
            loadFavorites();
        } else {
            favBtn.setText(modClient.t("★ Ulubione", "★ Favorites"));
            favBtn.getStyleClass().remove("btn-primary");
            favBtn.getStyleClass().add("btn-fav");
            performSearch(searchField.getText());
        }
    }

    private void toggleRecording() {
        if (modClient.isRecording()) {
            modClient.stopRecording();
            recordBtn.setText(modClient.t("⏺ Nagraj", "⏺ Record"));
            recordBtn.getStyleClass().remove("btn-recording");
            recordBtn.getStyleClass().add("btn-record");
            statusLabel.setText(modClient.t("Nagrywanie zatrzymane.", "Recording stopped."));
        } else {
            if (!modClient.isPlaying()) {
                statusLabel.setText(modClient.t("Najpierw włącz stację!", "Start a station first!"));
                return;
            }
            FileChooser fc = new FileChooser();
            fc.setTitle(modClient.t("Zapisz nagranie", "Save recording"));
            String stationSafe = modClient.getCurrentStationName().replaceAll("[^a-zA-Z0-9 ]", "").trim().replace(" ", "_");
            if (stationSafe.isEmpty()) stationSafe = "radio";
            fc.setInitialFileName(stationSafe + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".wav");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("WAV Audio", "*.wav"));
            File file = fc.showSaveDialog(stage);
            
            if (file != null) {
                boolean success = modClient.startRecording(file.getAbsolutePath());
                if (success) {
                    recordBtn.setText(modClient.t("⏹ Stop nagrania", "⏹ Stop recording"));
                    recordBtn.getStyleClass().remove("btn-record");
                    recordBtn.getStyleClass().add("btn-recording");
                    statusLabel.setText(modClient.t("Nagrywanie do: ", "Recording to: ") + file.getName());
                } else {
                    statusLabel.setText(modClient.t("⚠️ Format strumienia nie obsługuje nagrywania!", "⚠️ Stream format doesn't support recording!"));
                }
            }
        }
    }

    private String getCountryCode(String selected) {
        if (selected == null) return "";
        if (selected.contains("Polska") || selected.contains("Poland")) return "PL";
        if (selected.contains("USA")) return "US";
        if (selected.contains("Brytan") || selected.contains("United Kingdom")) return "GB";
        if (selected.contains("Niemcy") || selected.contains("Germany")) return "DE";
        if (selected.contains("Francja") || selected.contains("France")) return "FR";
        if (selected.contains("Włochy") || selected.contains("Italy")) return "IT";
        if (selected.contains("Japonia") || selected.contains("Japan")) return "JP";
        return "";
    }

    private String getGenreValue(String selected) {
        if (selected == null) return "";
        if (selected.contains("Pop")) return "pop";
        if (selected.contains("Rock")) return "rock";
        if (selected.contains("Hip")) return "hiphop";
        if (selected.contains("Electronic")) return "electronic";
        if (selected.contains("Jazz")) return "jazz";
        if (selected.contains("Classical")) return "classical";
        if (selected.contains("News")) return "news";
        return "";
    }

    private class StationCard extends HBox {
        private final Label starLbl;
        private final ImageView iconView;
        private final Label fallbackIcon;
        private final StackPane imageStack;
        private final Label nameLbl;
        private StationItem currentItem;

        public StationCard() {
            setSpacing(10);
            setAlignment(Pos.CENTER_LEFT);
            setPadding(new Insets(8, 15, 8, 15));
            setStyle("-fx-background-radius: 8;");
            
            starLbl = new Label("☆");
            starLbl.setStyle("-fx-font-size: 20px; -fx-cursor: hand;");
            starLbl.setMinWidth(25);
            
            iconView = new ImageView();
            iconView.setFitWidth(24); iconView.setFitHeight(24); iconView.setPreserveRatio(true);
            
            fallbackIcon = new Label("🎵");
            fallbackIcon.setStyle("-fx-font-size: 20px; -fx-text-fill: #555;");
            
            imageStack = new StackPane(fallbackIcon, iconView);
            imageStack.setPrefSize(24, 24);
            
            nameLbl = new Label();
            nameLbl.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(nameLbl, Priority.ALWAYS);
            
            getChildren().addAll(starLbl, imageStack, nameLbl);

            setOnMouseEntered(e -> {
                if (currentItem != null && !currentItem.url().isEmpty()) {
                    String hexOpacity = modClient.getAccentColor() + "40";
                    setStyle("-fx-background-color: " + hexOpacity + "; -fx-background-radius: 8;");
                    nameLbl.setStyle("-fx-text-fill: #55FF55; -fx-font-size: 15px; -fx-font-weight: bold;");
                }
            });
            setOnMouseExited(e -> {
                if (currentItem != null) {
                    setStyle("-fx-background-color: transparent; -fx-background-radius: 8;");
                    nameLbl.setStyle("-fx-text-fill: " + (currentItem.url().isEmpty() ? "#888" : "white") + "; -fx-font-size: 14px;");
                }
            });

            setOnMouseClicked(e -> {
                if (currentItem != null && !currentItem.url().isEmpty() && e.getClickCount() == 2) {
                    modClient.playStation(currentItem.name(), currentItem.url(), currentItem.favicon());
                    e.consume();
                }
            });

            starLbl.setOnMouseClicked(e -> {
                if (currentItem != null && !currentItem.url().isEmpty()) {
                    modClient.toggleFavorite(currentItem.name(), currentItem.url(), currentItem.favicon());
                    updateItem(currentItem);
                    if (showFavoritesMode) loadFavorites();
                    e.consume();
                }
            });
        }

        public void updateItem(StationItem item) {
            this.currentItem = item;
            nameLbl.setText(item.name());
            setStyle("-fx-background-color: transparent; -fx-background-radius: 8;");
            
            if (item.url().isEmpty()) {
                starLbl.setText(""); iconView.setImage(null);
                fallbackIcon.setVisible(false);
                nameLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 14px;");
            } else {
                boolean isFav = modClient.isFavorite(item.url());
                starLbl.setText(isFav ? "★" : "☆");
                starLbl.setStyle("-fx-font-size: 20px; -fx-cursor: hand; -fx-text-fill: " + (isFav ? "#FFD700" : "#888") + ";");
                nameLbl.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                
                fallbackIcon.setVisible(true);
                if (item.favicon() != null && !item.favicon().isEmpty() && (item.favicon().startsWith("http://") || item.favicon().startsWith("https://"))) {
                    try { 
                        Image img = new Image(item.favicon(), 24, 24, true, true, true);
                        iconView.setImage(img); 
                        img.progressProperty().addListener((obs, old, prog) -> {
                            if (prog.doubleValue() == 1.0 && !img.isError()) fallbackIcon.setVisible(false);
                        });
                    }
                    catch (Exception ignored) { iconView.setImage(null); }
                } else { 
                    iconView.setImage(null); 
                }
            }
        }
    }

    private class StationCell extends ListCell<StationRow> {
        private final HBox rootBox;
        private final StationCard leftCard;
        private final StationCard rightCard;

        StationCell() {
            rootBox = new HBox(15);
            rootBox.setAlignment(Pos.CENTER_LEFT);
            leftCard = new StationCard();
            rightCard = new StationCard();

            HBox.setHgrow(leftCard, Priority.ALWAYS);
            HBox.setHgrow(rightCard, Priority.ALWAYS);
            leftCard.setMaxWidth(Double.MAX_VALUE);
            rightCard.setMaxWidth(Double.MAX_VALUE);
            
            leftCard.prefWidthProperty().bind(rootBox.widthProperty().divide(2).subtract(10));
            rightCard.prefWidthProperty().bind(rootBox.widthProperty().divide(2).subtract(10));

            rootBox.getChildren().addAll(leftCard, rightCard);
        }

        @Override
        protected void updateItem(StationRow row, boolean empty) {
            super.updateItem(row, empty);
            if (empty || row == null) { 
                setGraphic(null);
                setStyle("-fx-background-color: transparent;");
                return; 
            }
            leftCard.updateItem(row.left());
            
            if (row.right() != null) {
                rightCard.updateItem(row.right());
                rightCard.setVisible(true);
            } else {
                rightCard.setVisible(false);
            }
            
            String aDark = darken(modClient.getAccentColor(), 0.15);
            String aBorder = darken(modClient.getAccentColor(), 0.50);
            setStyle("-fx-background-color: " + aDark + ";" +
                     "-fx-padding: 2 10 2 10;" +
                     "-fx-border-color: transparent transparent " + aBorder + " transparent;" +
                     "-fx-border-width: 0 0 1 0;");
                     
            setGraphic(rootBox);
        }
    }
}