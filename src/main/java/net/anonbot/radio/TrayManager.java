package net.anonbot.radio;

import javafx.application.Platform;
import javafx.stage.Stage;

import java.awt.*;
import java.awt.image.BufferedImage;

public class TrayManager {

    private final Stage stage;
    private final RadioModClient modClient;
    private TrayIcon trayIcon;
    private boolean traySupported = false;

    public TrayManager(Stage stage, RadioModClient modClient) {
        this.stage = stage;
        this.modClient = modClient;
    }

    public boolean setup() {
        if (!SystemTray.isSupported()) return false;
        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image icon = createRadioIcon();

            PopupMenu popup = new PopupMenu();

            MenuItem showItem = new MenuItem(modClient.t("Pokaż okno", "Show window"));
            showItem.addActionListener(e -> restoreWindow());

            MenuItem stateItem = new MenuItem(modClient.t("Zatrzymaj radio", "Stop radio"));
            stateItem.addActionListener(e -> Platform.runLater(() -> modClient.stopRadio()));

            MenuItem quitItem = new MenuItem(modClient.t("Wyjdź", "Quit"));
            quitItem.addActionListener(e -> {
                modClient.stopRadio();
                tray.remove(trayIcon);
                Platform.runLater(() -> {
                    Platform.exit();
                    System.exit(0);
                });
            });

            popup.add(showItem);
            popup.addSeparator();
            popup.add(stateItem);
            popup.addSeparator();
            popup.add(quitItem);

            trayIcon = new TrayIcon(icon, modClient.t("Radio Internetowe", "Internet Radio"), popup);
            trayIcon.setImageAutoSize(true);

            // Dwuklik = przywróć okno
            trayIcon.addActionListener(e -> restoreWindow());

            tray.add(trayIcon);
            traySupported = true;
            return true;
        } catch (Exception e) {
            System.err.println("[RadioApp] Tray niedostępny: " + e.getMessage());
            return false;
        }
    }

    private void restoreWindow() {
        // Musi być w JavaFX thread, i potrzebujemy sekwencji kroków żeby działało na Windows
        Platform.runLater(() -> {
            stage.show();
            stage.setIconified(false);
            stage.setAlwaysOnTop(true);
            stage.toFront();
            stage.requestFocus();
            // Zdejmujemy alwaysOnTop po chwili — tylko żeby wymusić focus
            new Thread(() -> {
                try { Thread.sleep(200); }
                catch (InterruptedException ignored) {}
                Platform.runLater(() -> stage.setAlwaysOnTop(false));
            }).start();
        });
    }

    public void updateTooltip(String stationName, String songName) {
        if (!traySupported || trayIcon == null) return;
        String tooltip = modClient.t("Radio Internetowe", "Internet Radio");
        if (stationName != null && !stationName.isEmpty()) {
            tooltip = stationName;
            if (songName != null && !songName.isEmpty()) {
                String song = songName.length() > 40 ? songName.substring(0, 37) + "..." : songName;
                tooltip += "\n♪ " + song;
            }
        }
        trayIcon.setToolTip(tooltip);
    }

    public void showTrayMessage(String title, String message) {
        if (!traySupported || trayIcon == null) return;
        trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
    }

    public void remove() {
        if (traySupported && trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
    }

    private Image createRadioIcon() {
        int size = 32;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Tło
        g.setColor(new Color(26, 26, 46));
        g.fillRoundRect(0, 0, size, size, 8, 8);

        // Zewnętrzny pierścień
        g.setColor(new Color(85, 255, 85));
        g.setStroke(new BasicStroke(2.0f));
        g.drawOval(5, 5, 22, 22);

        // Środkowy punkt
        g.fillOval(11, 11, 10, 10);

        // Antena
        g.setStroke(new BasicStroke(2.0f));
        g.drawLine(23, 6, 29, 1);
        g.fillOval(28, 0, 4, 4);

        g.dispose();
        return img;
    }
}
