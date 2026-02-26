# 🎵 Radio Internetowe

Lekki, nowoczesny i bogaty w funkcje odtwarzacz radia internetowego napisany w Java 21 oraz JavaFX. Pozwala na bezproblemowe wyszukiwanie i słuchanie tysięcy stacji radiowych z całego świata.

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-UI-1366b5?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

*Przeczytaj w języku [Angielskim](README.md)*

---

## ✨ Główne funkcje

* **🌍 Globalna wyszukiwarka:** Dzięki integracji z *Radio-Browser API*, możesz szukać stacji po nazwie, kraju lub gatunku muzycznym.
* **🌗 Nowoczesny interfejs (Dark Mode):** Autorski, bezramkowy interfejs okna z możliwością wyboru własnego koloru wiodącego (akcentu).
* **🎵 Okładki albumów:** Aplikacja automatycznie pobiera okładkę właśnie odtwarzanego utworu korzystając z API iTunes.
* **📱 Tryb MiniPlayer:** Kompaktowy, dyskretny odtwarzacz z funkcją "zawsze na wierzchu", idealny podczas pracy na komputerze.
* **⌨️ Globalne skróty klawiszowe:** Kontroluj radio przy pomocy przycisków multimedialnych na klawiaturze (Play/Pauza/Stop), nawet gdy aplikacja jest w tle (dzięki `JNativeHook`).
* **⏺️ Nagrywanie na żywo:** Zapisuj ulubione audycje i piosenki bezpośrednio do bezstratnych plików `.wav`.
* **🌙 Wyłącznik czasowy (Sleep Timer):** Zasypiaj przy radiu — aplikacja wyłączy się sama po 15, 30, 45 lub 60 minutach.
* **🚫 Powiadomienia i Czarna Lista:** Delikatne powiadomienia (Toast) informują o nowych piosenkach. Słyszysz reklamy? Dodaj frazę do czarnej listy, a powiadomienie nie zostanie wyświetlone.
* **🌐 Wielojęzyczność:** Pełne wsparcie dla własnych tłumaczeń przez pliki JSON (domyślnie język Polski i Angielski).

---

## 🚀 Uruchamianie (dla programistów)

**Wymagania:**
* Java Development Kit (JDK) 21 lub nowszy.
* Apache Maven.

Sklonuj repozytorium i uruchom aplikację za pomocą Mavena:

```bash
git clone https://github.com/twojanazwa/InternetRadioApp.git
cd InternetRadioApp
mvn clean compile javafx:run
```

---

## 📦 Budowanie gotowej aplikacji (.exe)

Możesz zbudować samodzielną wersję Portable lub Instalator `.exe`. Użytkownik końcowy **NIE MUSI** posiadać zainstalowanej Javy na swoim komputerze!

Otwórz konsolę w głównym katalogu projektu.

**Aby zbudować wersję Portable** (gotowy folder bez instalacji), uruchom:

```bash
build_portable.bat
```

Wynik znajdziesz w folderze `release/InternetRadio`.

**Aby zbudować Instalator** (wymaga zainstalowanego [WiX Toolset v3](https://github.com/wixtoolset/wix3/releases)), uruchom:

```bash
build_installer.bat
```

---

## ⚙️ Własne tłumaczenia

Aplikacja automatycznie generuje pliki językowe przy pierwszym uruchomieniu. Aby dodać nowy język lub edytować obecny:

1. Otwórz aplikację i wejdź w **Ustawienia**.
2. Kliknij **"Otwórz folder języków"**.
3. Skopiuj plik `en.json`, zmień jego nazwę na skrót Twojego języka (np. `de.json`) i przetłumacz wartości po prawej stronie.
4. Aplikacja automatycznie załaduje język zgodny z ustawieniami systemu Windows.

---

## 🤝 Użyte technologie i biblioteki

| Biblioteka | Zastosowanie |
|---|---|
| JavaFX | Framework interfejsu graficznego |
| Gson | Parsowanie danych JSON z API |
| JNativeHook | Obsługa globalnych skrótów klawiaturowych |
| MP3SPI / JLayer | Wsparcie dla dekodowania strumieni MP3 |
