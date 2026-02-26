module net.anonbot.radio {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.desktop;
    requires java.net.http;
    requires java.logging;
    requires com.google.gson;
    requires com.github.kwhat.jnativehook; // Wymagane dla klawiszy multimedialnych

    opens net.anonbot.radio to javafx.fxml;
    exports net.anonbot.radio;
}