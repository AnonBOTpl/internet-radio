# 🎵 Internet Radio App

A lightweight, modern, and feature-rich internet radio player written in Java 21 and JavaFX. Search and play thousands of radio stations worldwide, completely free.

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-UI-1366b5?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

*Przeczytaj po [Polsku](README_pl.md)*

---

## ✨ Features

* **🌍 Global Station Search** — Powered by the open *Radio-Browser API*. Search by name, country, or genre across thousands of stations.
* **🌗 Modern Dark UI** — Borderless, custom-themed interface with a dynamic accent color selector (presets + custom color picker).
* **🎵 Synchronized Album Art** — Cover art is fetched from the iTunes API and is always in sync with the currently playing song. Blacklisted tracks are never queried.
* **📱 MiniPlayer Mode** — A compact overlay bar that snaps to the bottom of your screen, with volume control and album art. Stays out of your way while you work.
* **⌨️ Global Media Keys** — Control playback with your keyboard's Play/Pause/Stop media keys even when the app is minimized, powered by `JNativeHook`.
* **⏺️ Live Recording** — Record live streams directly to lossless `.wav` files. Recording automatically stops at the 2 GB limit.
* **🌙 Sleep Timer** — Automatically shuts down the app after 15, 30, 45, or 60 minutes.
* **🚫 Blacklist & Notifications** — Non-intrusive toast notifications show the current song with cover art. Add any keyword to the blacklist to suppress ads or unwanted tracks.
* **📜 Persistent History** — The last 50 played songs are saved and restored between sessions.
* **🌐 Multilingual** — Fully translatable via external JSON files in `~/.radioapp/langs/`. Defaults to Polish and English, auto-detected from system locale.

---

## 🚀 Running in Development

**Prerequisites:** JDK 21+, Apache Maven.

```bash
git clone https://github.com/yourusername/InternetRadioApp.git
cd InternetRadioApp
mvn clean compile javafx:run
```

---

## 📦 Building a Standalone App (.exe)

The target user does **not** need Java installed.

**Portable version** (no installation required):
```bash
build_portable.bat
```
Output: `release/InternetRadio/`

**Installer (.exe)** — requires [WiX Toolset v3](https://github.com/wixtoolset/wix3/releases):
```bash
build_installer.bat
```
Output: `release/InternetRadio-1.0.0.exe`

---

## ⚙️ Custom Translations

Language files are generated automatically on first run at `~/.radioapp/langs/`.

1. Open **Settings → Open language folder**.
2. Duplicate `en.json`, rename it to your locale (e.g. `de.json`).
3. Translate the values on the right side of each key.
4. The app auto-detects the correct file based on your system locale.

---

## 🔒 Security

* **URL validation** — only public `http(s)://` URLs accepted; `localhost`, private IPs and non-HTTP protocols are blocked.
* **CSS injection protection** — accent colors from `config.json` are validated against `#RRGGBB` format before use.
* **Path traversal protection** — language file names are validated to safe locale codes only.
* **Thread-safe blacklist** — all blacklist operations are synchronized.

---

## 🤝 Dependencies

| Library | Purpose |
|---|---|
| JavaFX | UI Framework |
| Gson | JSON parsing |
| JNativeHook | Global keyboard listener |
| MP3SPI / JLayer | MP3 stream decoding |
