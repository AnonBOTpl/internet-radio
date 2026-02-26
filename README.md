# 🎵 Internet Radio App

A lightweight, modern, and feature-rich internet radio player written in Java 21 and JavaFX. It allows you to search and play thousands of radio stations worldwide, completely free.

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-UI-1366b5?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

*Read this in [Polish](README_pl.md)*

---

## ✨ Features

* **🌍 Global Station Search:** Powered by the open *Radio-Browser API*, search for thousands of stations by name, country, or genre.
* **🌗 Modern Dark UI:** Custom title bar, borderless design, and dynamic layout with a sleek color accent selector.
* **🎵 Auto Album Art:** Automatically fetches cover art for the currently playing song via the iTunes API.
* **📱 MiniPlayer Mode:** A compact, "always-on-top" overlay player that stays out of your way while you work.
* **⌨️ Global Hotkeys:** Control playback using your keyboard's media keys (Play/Pause/Stop) even when the app is minimized, thanks to `JNativeHook`.
* **⏺️ Live Recording:** Record your favorite live streams directly to `.wav` files.
* **🌙 Sleep Timer:** Set an automatic shutdown timer (15, 30, 45, or 60 minutes).
* **🚫 Notification & Blacklist System:** Non-intrusive desktop toasts show the current song. Tired of ads? Add specific keywords to the blacklist to ignore them.
* **🌐 Multilingual:** Fully translatable via external JSON files (defaults to English and Polish).

---

## 🚀 How to Run (Development)

**Prerequisites:**
* Java Development Kit (JDK) 21 or newer.
* Apache Maven.

Clone the repository and run the application using Maven:

```bash
git clone https://github.com/yourusername/InternetRadioApp.git
cd InternetRadioApp
mvn clean compile javafx:run
```

---

## 📦 How to Build Standalone (.exe)

You can build a fully standalone portable app or an `.exe` installer. The target user does **not** need to have Java installed on their machine!

Open a terminal in the project directory.

**For a Portable Version** (no installation required), run:

```bash
build_portable.bat
```

The result will be in the `release/InternetRadio` folder.

**For an Installer Version** (requires [WiX Toolset v3](https://github.com/wixtoolset/wix3/releases) installed), run:

```bash
build_installer.bat
```

---

## ⚙️ Customizing Translations

The app automatically generates language JSON files on the first run. To add your own language or change existing phrases:

1. Open the app and go to **Settings**.
2. Click **"Open language folder"**.
3. Duplicate the `en.json` file, rename it to your locale (e.g. `de.json`), and translate the right-side values.
4. The app will automatically detect it based on your system locale.

---

## 🤝 Dependencies

| Library | Purpose |
|---|---|
| JavaFX | UI Framework |
| Gson | JSON parsing |
| JNativeHook | Global keyboard listener |
| MP3SPI / JLayer | Legacy MP3 stream decoding support |
