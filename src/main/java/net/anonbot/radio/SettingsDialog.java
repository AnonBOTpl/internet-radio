package net.anonbot.radio;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;

public class SettingsDialog {

    private final RadioModClient modClient;
    private final Stage owner;
    private Stage dialog;
    private Scene scene;

    public SettingsDialog(RadioModClient modClient, Stage owner) {
        this.modClient = modClient;
        this.owner = owner;
    }

    public void show() {
        dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.setResizable(false);
        dialog.setMinWidth(520);

        buildContent();
        dialog.show();
    }

    private void buildContent() {
        dialog.setTitle(modClient.t("Ustawienia Radia", "Radio Settings"));

        VBox root = new VBox(0);
        root.getStyleClass().add("dialog-root");

        Label title = new Label(modClient.t("⚙ Ustawienia", "⚙ Settings"));
        title.getStyleClass().add("dialog-title");
        title.setPadding(new Insets(16, 16, 8, 16));
        root.getChildren().add(title);
        root.getChildren().add(new Separator());

        Label sectionNotif = new Label(modClient.t("Powiadomienia", "Notifications"));
        sectionNotif.getStyleClass().add("settings-section");
        sectionNotif.setPadding(new Insets(10, 16, 4, 16));
        root.getChildren().add(sectionNotif);

        Button toastToggle = buildToggleBtn(modClient.t("Toast: ", "Toast: "), modClient.isShowToast());
        toastToggle.setOnAction(e -> {
            modClient.setShowToast(!modClient.isShowToast());
            updateToggleBtn(toastToggle, modClient.t("Toast: ", "Toast: "), modClient.isShowToast());
        });
        toastToggle.setPadding(new Insets(6, 16, 6, 16));
        toastToggle.setMaxWidth(Double.MAX_VALUE);
        root.getChildren().add(toastToggle);

        Button pinToggle = buildToggleBtn(
                modClient.t("Mini player zawsze na wierzchu: ", "Mini player always on top: "),
                modClient.isMiniPlayerAlwaysOnTop());
        pinToggle.setOnAction(e -> {
            modClient.setMiniPlayerAlwaysOnTop(!modClient.isMiniPlayerAlwaysOnTop());
            updateToggleBtn(pinToggle,
                modClient.t("Mini player zawsze na wierzchu: ", "Mini player always on top: "),
                modClient.isMiniPlayerAlwaysOnTop());
        });
        pinToggle.setPadding(new Insets(6, 16, 6, 16));
        pinToggle.setMaxWidth(Double.MAX_VALUE);
        root.getChildren().add(pinToggle);

        HBox durationRow = new HBox(10);
        durationRow.setAlignment(Pos.CENTER_LEFT);
        durationRow.setPadding(new Insets(6, 16, 6, 16));
        Label durLbl = new Label(modClient.t("Czas wyświetlania: ", "Display duration: "));
        durLbl.getStyleClass().add("dialog-label");
        durLbl.setMinWidth(150);
        Slider durSlider = new Slider(2, 20, modClient.getToastDuration());
        durSlider.setPrefWidth(160);
        durSlider.setMajorTickUnit(2);
        durSlider.setSnapToTicks(true);
        Label durVal = new Label(modClient.getToastDuration() + " sek");
        durVal.setMinWidth(50);
        durVal.getStyleClass().add("dialog-label");
        durSlider.valueProperty().addListener((obs, old, nv) -> {
            int v = (int) Math.round(nv.doubleValue());
            modClient.setToastDuration(v);
            durVal.setText(v + " sek");
        });
        durationRow.getChildren().addAll(durLbl, durSlider, durVal);
        root.getChildren().add(durationRow);
        root.getChildren().add(new Separator());

        Label sectionTheme = new Label(modClient.t("Kolor akcentu", "Accent Color"));
        sectionTheme.getStyleClass().add("settings-section");
        sectionTheme.setPadding(new Insets(10, 16, 6, 16));
        root.getChildren().add(sectionTheme);

        HBox presetRow = new HBox(6);
        presetRow.setPadding(new Insets(0, 16, 6, 16));
        presetRow.setAlignment(Pos.CENTER_LEFT);
        Label presetLbl = new Label(modClient.t("Presetki: ", "Presets: "));
        presetLbl.getStyleClass().add("dialog-label");
        presetRow.getChildren().add(presetLbl);

        ColorPicker colorPicker = new ColorPicker(Color.web(modClient.getAccentColor()));
        colorPicker.setStyle("-fx-color-label-visible: false;");

        Object[][] presets = {
            {"🟣", "#533483", "#6a44a8"},
            {"🔵",  "#1a3a6e", "#2a5a9e"},
            {"🟢",   "#1a5c2a", "#257a38"},
            {"🔴",    "#7a1a1a", "#a02020"},
            {"🟠", "#7a4a00","#a06000"},
            {"⚫",       "#3a3a3a", "#555555"},
            {"🩷",      "#7a1a5a", "#a02080"},
            {"🩵",      "#0a5a6a", "#0a7a8a"},
        };

        for (Object[] preset : presets) {
            Button pb = new Button((String) preset[0]);
            String c1 = (String) preset[1];
            String c2 = (String) preset[2];
            pb.setStyle("-fx-background-color: " + c1 + "; -fx-text-fill: white; " +
                    "-fx-background-radius: 5; -fx-cursor: hand; -fx-font-size: 10px; -fx-padding: 3 5 3 5;");
            pb.setOnAction(e -> {
                modClient.setAccentColor(c1, c2);
                colorPicker.setValue(Color.web(c1));
            });
            presetRow.getChildren().add(pb);
        }
        root.getChildren().add(presetRow);

        HBox colorRow = new HBox(10);
        colorRow.setAlignment(Pos.CENTER_LEFT);
        colorRow.setPadding(new Insets(0, 16, 10, 16));
        Label colorLbl = new Label(modClient.t("Własny kolor: ", "Custom color: "));
        colorLbl.getStyleClass().add("dialog-label");

        colorPicker.setOnAction(e -> {
            Color c = colorPicker.getValue();
            modClient.setAccentColor(toHex(c), toHexLighter(c));
        });

        colorRow.getChildren().addAll(colorLbl, colorPicker);
        root.getChildren().add(colorRow);
        root.getChildren().add(new Separator());

        Label sectionLang = new Label(modClient.t("Język / Language", "Language / Język"));
        sectionLang.getStyleClass().add("settings-section");
        sectionLang.setPadding(new Insets(10, 16, 4, 16));
        root.getChildren().add(sectionLang);

        HBox langRow = new HBox(12);
        langRow.setAlignment(Pos.CENTER_LEFT);
        langRow.setPadding(new Insets(6, 16, 10, 16));

        ToggleGroup langGroup = new ToggleGroup();
        RadioButton autoBtn = new RadioButton(modClient.t("Auto (z systemu)", "Auto (system)"));
        RadioButton plBtn = new RadioButton("Polski");
        RadioButton enBtn = new RadioButton("English");
        autoBtn.setToggleGroup(langGroup); plBtn.setToggleGroup(langGroup); enBtn.setToggleGroup(langGroup);
        autoBtn.getStyleClass().add("dialog-radio"); plBtn.getStyleClass().add("dialog-radio"); enBtn.getStyleClass().add("dialog-radio");

        switch (modClient.getLanguage()) {
            case "pl" -> plBtn.setSelected(true);
            case "en" -> enBtn.setSelected(true);
            default -> autoBtn.setSelected(true);
        }

        langGroup.selectedToggleProperty().addListener((obs, old, nv) -> {
            if (nv == autoBtn) modClient.setLanguage("auto");
            else if (nv == plBtn) modClient.setLanguage("pl");
            else modClient.setLanguage("en");
            dialog.getScene().setRoot(buildRootAndReturn());
        });
        
        Button openLangBtn = new Button(modClient.t("Otwórz folder języków", "Open language folder"));
        openLangBtn.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 3 8 3 8;");
        openLangBtn.setOnAction(e -> {
            try {
                File dir = new File(System.getProperty("user.home"), ".radioapp/langs");
                if (dir.exists()) Desktop.getDesktop().open(dir);
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        langRow.getChildren().addAll(autoBtn, plBtn, enBtn, openLangBtn);
        root.getChildren().add(langRow);
        root.getChildren().add(new Separator());

        HBox btnRow = new HBox();
        btnRow.setPadding(new Insets(10, 16, 14, 16));
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        Button okBtn = new Button(modClient.t("✓ Zapisz i zamknij", "✓ Save & Close"));
        okBtn.getStyleClass().add("btn-primary");
        okBtn.setOnAction(e -> { modClient.saveConfig(); dialog.close(); });
        btnRow.getChildren().add(okBtn);
        root.getChildren().add(btnRow);

        if (scene == null) {
            scene = new Scene(root, 540, 480);
            try { scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm()); }
            catch (Exception ignored) {}
            dialog.setScene(scene);
        } else {
            scene.setRoot(root);
        }
    }

    private VBox buildRootAndReturn() {
        buildContent();
        return (VBox) scene.getRoot();
    }

    private String toHex(Color c) {
        return String.format("#%02x%02x%02x", (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
    }
    private String toHexLighter(Color c) {
        return String.format("#%02x%02x%02x",
            (int)(Math.min(c.getRed()*1.35, 1.0)*255), (int)(Math.min(c.getGreen()*1.35, 1.0)*255), (int)(Math.min(c.getBlue()*1.35, 1.0)*255));
    }

    private Button buildToggleBtn(String label, boolean state) {
        Button btn = new Button(label + (state ? "✅ ON" : "❌ OFF"));
        btn.getStyleClass().add(state ? "settings-toggle-on" : "settings-toggle-off");
        return btn;
    }

    private void updateToggleBtn(Button btn, String label, boolean state) {
        btn.setText(label + (state ? "✅ ON" : "❌ OFF"));
        btn.getStyleClass().removeAll("settings-toggle-on", "settings-toggle-off");
        btn.getStyleClass().add(state ? "settings-toggle-on" : "settings-toggle-off");
    }
}