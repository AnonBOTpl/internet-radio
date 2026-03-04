package net.anonbot.radio;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RadioApp extends Application implements NativeKeyListener {

    private RadioModClient modClient;
    private RadioScreen radioScreen;
    private MiniPlayer miniPlayer;

    @Override
    public void start(Stage primaryStage) {
        modClient = new RadioModClient();
        modClient.loadConfig();

        primaryStage.initStyle(StageStyle.UNDECORATED);

        // --- ŁADOWANIE IKONY APLIKACJI (PASEK ZADAŃ WINDOWS) ---
        try {
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icon.png")));
        } catch (Exception e) {
            System.err.println("[RadioApp] Ostrzeżenie: Nie znaleziono pliku icon.png w src/main/resources/");
        }

        radioScreen = new RadioScreen(modClient, primaryStage);
        Scene mainScene = new Scene(radioScreen.getRoot(), 960, 620);
        mainScene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        primaryStage.setTitle(modClient.t("🎵 Radio Internetowe", "🎵 Internet Radio"));
        primaryStage.setScene(mainScene);
        primaryStage.setMinWidth(920);
        primaryStage.setMinHeight(560);
        primaryStage.show();

        Platform.runLater(() -> radioScreen.applyAccentColors());

        miniPlayer = new MiniPlayer(primaryStage, modClient);
        radioScreen.setOnSwitchToMiniPlayer(() -> miniPlayer.show());

        primaryStage.setOnCloseRequest(e -> shutdown());
        modClient.setOnThemeChanged(() -> radioScreen.applyAccentColors());

        setupGlobalHotkeys();
    }

    private void setupGlobalHotkeys() {
        try {
            Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
            logger.setLevel(Level.OFF);
            logger.setUseParentHandlers(false);

            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
        } catch (NativeHookException ex) {
            System.err.println("[RadioApp] Błąd rejestracji klawiszy multimedialnych: " + ex.getMessage());
        }
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        if (e.getKeyCode() == NativeKeyEvent.VC_MEDIA_PLAY || e.getKeyCode() == 179) {
            Platform.runLater(() -> modClient.togglePlay());
        } else if (e.getKeyCode() == NativeKeyEvent.VC_MEDIA_STOP) {
            Platform.runLater(() -> modClient.stopRadio());
        }
    }

    private void shutdown() {
        try { GlobalScreen.unregisterNativeHook(); } catch (NativeHookException ignored) {}
        modClient.shutdownExecutors();
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}