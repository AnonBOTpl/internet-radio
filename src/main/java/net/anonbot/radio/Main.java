package net.anonbot.radio;

public class Main {
    public static void main(String[] args) {
        // Ta klasa służy jako pośrednik ("Fat JAR fix").
        // Nie rozszerza ona javafx.application.Application,
        // dzięki czemu omija restrykcje ClassLoadera w nowej Javie.
        RadioApp.main(args);
    }
}