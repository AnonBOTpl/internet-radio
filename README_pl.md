# 🎵 Radio Internetowe

Lekki, nowoczesny i bogaty w funkcje odtwarzacz radia internetowego napisany w Java 21 oraz JavaFX. Pozwala na wyszukiwanie i słuchanie tysięcy stacji radiowych z całego świata — całkowicie za darmo.

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-UI-1366b5?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

*Read in [English](README.md)*

---

## ✨ Główne funkcje

* **🌍 Globalna wyszukiwarka** — Integracja z *Radio-Browser API*. Szukaj stacji po nazwie, kraju lub gatunku muzycznym.
* **🌗 Nowoczesny interfejs (Dark Mode)** — Bezramkowy interfejs z selektorem koloru akcentu (presety + własny color picker).
* **🎵 Zsynchronizowane okładki albumów** — Okładka pobierana z iTunes API jest zawsze aktualna względem granego utworu. Utwory z czarnej listy nie są odpytywane.
* **📱 Tryb MiniPlayer** — Kompaktowy pasek przyklejony do dolnej krawędzi ekranu z głośnością i okładką. Idealny podczas pracy.
* **⌨️ Globalne klawisze multimedialne** — Steruj odtwarzaniem klawiszami Play/Pause/Stop nawet gdy aplikacja jest w tle (dzięki `JNativeHook`).
* **⏺️ Nagrywanie na żywo** — Zapisuj audycje do bezstratnych plików `.wav`. Nagrywanie zatrzymuje się automatycznie po osiągnięciu limitu 2 GB.
* **🌙 Wyłącznik czasowy (Sleep Timer)** — Automatyczne zamknięcie po 15, 30, 45 lub 60 minutach.
* **🚫 Czarna lista i powiadomienia** — Dyskretne powiadomienia Toast z okładką informują o nowych piosenkach. Dodaj frazę do czarnej listy, by wyciszyć reklamy.
* **📜 Trwała historia** — 50 ostatnich piosenek jest zapisywanych i przywracanych po restarcie aplikacji.
* **🌐 Wielojęzyczność** — Pełne wsparcie tłumaczeń przez pliki JSON w `~/.radioapp/langs/`. Domyślnie Polski i Angielski, wykrywane automatycznie z ustawień systemu.

---

## 🚀 Uruchamianie (dla programistów)

**Wymagania:** JDK 21+, Apache Maven.

```bash
git clone https://github.com/twojanazwa/InternetRadioApp.git
cd InternetRadioApp
mvn clean compile javafx:run
```

---

## 📦 Budowanie gotowej aplikacji (.exe)

Użytkownik końcowy **NIE MUSI** mieć zainstalowanej Javy.

**Wersja Portable** (bez instalacji):
```bash
build_portable.bat
```
Wynik: folder `release/InternetRadio/`

**Instalator (.exe)** — wymaga [WiX Toolset v3](https://github.com/wixtoolset/wix3/releases):
```bash
build_installer.bat
```
Wynik: `release/InternetRadio-1.0.0.exe`

---

## ⚙️ Własne tłumaczenia

Pliki językowe są generowane automatycznie przy pierwszym uruchomieniu w `~/.radioapp/langs/`.

1. Otwórz **Ustawienia → Otwórz folder języków**.
2. Skopiuj `en.json`, zmień nazwę na skrót swojego języka (np. `de.json`).
3. Przetłumacz wartości po prawej stronie każdego klucza.
4. Aplikacja automatycznie załaduje odpowiedni plik na podstawie języka systemu.

---

## 🔒 Bezpieczeństwo

* **Walidacja URL** — akceptowane są tylko publiczne adresy `http(s)://`; `localhost`, prywatne zakresy IP i inne protokoły są blokowane.
* **Ochrona przed CSS injection** — kolory akcentu z `config.json` są walidowane do formatu `#RRGGBB` przed użyciem.
* **Ochrona przed path traversal** — nazwy plików językowych są walidowane do bezpiecznych kodów locale.
* **Thread-safe czarna lista** — wszystkie operacje na czarnej liście są synchronizowane.

---

## 🤝 Użyte technologie i biblioteki

| Biblioteka | Zastosowanie |
|---|---|
| JavaFX | Framework interfejsu graficznego |
| Gson | Parsowanie JSON z API |
| JNativeHook | Globalne skróty klawiaturowe |
| MP3SPI / JLayer | Dekodowanie strumieni MP3 |
