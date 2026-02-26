package net.anonbot.radio;

import com.google.gson.*;
import javafx.application.Platform;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class RadioModClient {

    private static RadioModClient INSTANCE;

    private final Object playerLock = new Object();
    private RadioPlayer currentPlayer = null;
    private Thread radioThread = null;
    private volatile long playRequestId = 0;

    private float globalVolume = 0.5f;
    private boolean showToast = true;
    private int toastDuration = 5;
    private String language = "auto";
    private String accentColor = "#533483";
    private String accentColorLight = "#6a44a8";
    private boolean miniPlayerAlwaysOnTop = true;

    private final List<String> blacklist = new ArrayList<>();
    private final List<FavoriteStation> favorites = new ArrayList<>();
    private final List<HistoryEntry> history = new ArrayList<>();

    private String currentStationName = "";
    private String currentStationUrl = "";
    private String currentStationFavicon = "";
    private String lastSongName = "";
    private String currentAlbumArt = null;

    private final Map<String, String> langMap = new HashMap<>();

    private final List<Consumer<String>> onSongChangedListeners = new ArrayList<>();
    private final List<Consumer<String>> onAlbumArtListeners = new ArrayList<>();
    private Consumer<String> onStatusChanged;
    private final List<Runnable> onPlayStateListeners = new ArrayList<>();
    private Runnable onLanguageChanged;
    private Runnable onThemeChanged;
    
    private ScheduledExecutorService songChecker;
    private ScheduledExecutorService sleepTimerExecutor;
    private ScheduledFuture<?> sleepTimerFuture;

    public static class FavoriteStation {
        public String name, url, favicon;
        public FavoriteStation(String name, String url, String favicon) {
            this.name = name; this.url = url; this.favicon = favicon;
        }
    }
    public static class HistoryEntry {
        public String song, station, time;
        public HistoryEntry(String song, String station, String time) {
            this.song = song; this.station = station; this.time = time;
        }
    }

    public RadioModClient() { INSTANCE = this; }
    public static RadioModClient getInstance() { return INSTANCE; }

    public void initLanguageSystem() {
        Path langDir = Path.of(System.getProperty("user.home"), ".radioapp", "langs");
        try { Files.createDirectories(langDir); } catch(Exception ignored){}
        
        Path plFile = langDir.resolve("pl.json");
        Path enFile = langDir.resolve("en.json");
        try {
            if (!Files.exists(plFile) || !Files.exists(enFile)) {
                JsonObject pl = new JsonObject();
                JsonObject en = new JsonObject();
                
                String[][] translations = {
                    {"🎵 Radio Internetowe", "🎵 Internet Radio"},
                    {"🔍 Szukaj stacji...", "🔍 Search stations..."},
                    {"Szukaj", "Search"},
                    {"★ Ulubione", "★ Favorites"},
                    {"🔙 Pokaż wszystkie", "🔙 Show all"},
                    {"Kraj:", "Country:"},
                    {"Gatunek:", "Genre:"},
                    {"➕ Dodaj własną stację (URL)", "➕ Add custom station (URL)"},
                    {"Nazwa stacji...", "Station name..."},
                    {"▶ Graj", "▶ Play"},
                    {"★ Do ulubionych", "★ Add favorite"},
                    {"▶ Gra: ", "▶ Playing: "},
                    {"⏹ Radio zatrzymane", "⏹ Radio stopped"},
                    {"⏳ Łączenie z: ", "⏳ Connecting to: "},
                    {"Brak stacji", "No station"},
                    {"Gotowy", "Ready"},
                    {"⏹ Stop", "⏹ Stop"},
                    {"⏺ Nagraj", "⏺ Rec"},
                    {"⚙ Ustawienia", "⚙ Settings"},
                    {"🚫 Blacklist", "🚫 Blacklist"},
                    {"📜 Historia", "📜 History"},
                    {"Wybierz stację z listy!", "Select station from the list!"},
                    {"Wstrzymano", "Paused"},
                    {"Nagrywanie zatrzymane.", "Recording stopped."},
                    {"Najpierw włącz stację!", "Start a station first!"},
                    {"Zapisz nagranie", "Save recording"},
                    {"⏹ Stop nagrania", "⏹ Stop recording"},
                    {"Nagrywanie do: ", "Recording to: "},
                    {"⚠️ Format strumienia nie obsługuje nagrywania!", "⚠️ Stream format doesn't support recording!"},
                    {"Brak wyników", "No results"},
                    {"❌ Błąd połączenia z API", "❌ API connection error"},
                    {"Brak ulubionych stacji", "No favorite stations"},
                    {"Audycja na żywo", "Live broadcast"},
                    {"🚫 Dodano do czarnej listy: ", "🚫 Blacklisted: "},
                    {"Powiadomienia", "Notifications"},
                    {"Toast: ", "Toast: "},
                    {"Mini player zawsze na wierzchu: ", "Mini player always on top: "},
                    {"Czas wyświetlania: ", "Display duration: "},
                    {"Kolor akcentu", "Accent Color"},
                    {"Presetki: ", "Presets: "},
                    {"Własny kolor: ", "Custom color: "},
                    {"Język / Language", "Language / Język"},
                    {"Auto (z systemu)", "Auto (system)"},
                    {"Otwórz folder języków", "Open language folder"},
                    {"✓ Zapisz i zamknij", "✓ Save & Close"},
                    {"Przełącz na MiniPlayer", "Switch to MiniPlayer"},
                    {"Minimalizuj", "Minimize"},
                    {"Zamknij", "Close"},
                    {"Wyłącz za: OFF", "Sleep timer: OFF"},
                    {"Wyłącz za: 15m", "Sleep timer: 15m"},
                    {"Wyłącz za: 30m", "Sleep timer: 30m"},
                    {"Wyłącz za: 45m", "Sleep timer: 45m"},
                    {"Wyłącz za: 60m", "Sleep timer: 60m"}
                };
                
                for (String[] pair : translations) {
                    pl.addProperty(pair[0], pair[0]);
                    en.addProperty(pair[0], pair[1]);
                }
                
                if (!Files.exists(plFile)) Files.writeString(plFile, new GsonBuilder().setPrettyPrinting().create().toJson(pl));
                if (!Files.exists(enFile)) Files.writeString(enFile, new GsonBuilder().setPrettyPrinting().create().toJson(en));
            }
        } catch(Exception ignored){}
        loadLanguageStrings();
    }

    private void loadLanguageStrings() {
        langMap.clear();
        String activeLang = language.equals("auto") ? Locale.getDefault().getLanguage() : language;
        Path target = Path.of(System.getProperty("user.home"), ".radioapp", "langs", activeLang + ".json");
        
        if (Files.exists(target)) {
            try {
                JsonObject j = JsonParser.parseString(Files.readString(target)).getAsJsonObject();
                for (String key : j.keySet()) {
                    langMap.put(key, j.get(key).getAsString());
                }
            } catch (Exception e) { System.err.println("[RadioApp] Błąd wczytywania języka: " + e.getMessage()); }
        }
    }

    public String t(String plKey, String fallbackEn) {
        if (langMap.containsKey(plKey)) return langMap.get(plKey);
        String activeLang = language.equals("auto") ? Locale.getDefault().getLanguage() : language;
        return activeLang.equals("pl") ? plKey : fallbackEn;
    }

    // ===== CALLBACKI =====
    public void setOnSongChanged(Consumer<String> c)   { onSongChangedListeners.clear(); onSongChangedListeners.add(c); }
    public void addOnSongChanged(Consumer<String> c)   { onSongChangedListeners.add(c); }
    public void addOnAlbumArtChanged(Consumer<String> c) { onAlbumArtListeners.add(c); }
    public void setOnAlbumArtChanged(Consumer<String> c){ onAlbumArtListeners.clear(); onAlbumArtListeners.add(c); }
    public void setOnStatusChanged(Consumer<String> c) { onStatusChanged = c; }
    public void setOnPlayStateChanged(Runnable r)      { onPlayStateListeners.clear(); onPlayStateListeners.add(r); }
    public void addOnPlayStateChanged(Runnable r)      { onPlayStateListeners.add(r); }
    public void setOnLanguageChanged(Runnable r)       { onLanguageChanged = r; }
    public void setOnThemeChanged(Runnable r)          { onThemeChanged = r; }

    private void firePlayState() {
        if (!onPlayStateListeners.isEmpty()) Platform.runLater(() -> onPlayStateListeners.forEach(Runnable::run));
    }
    private void fireStatus(String msg) {
        if (onStatusChanged != null) Platform.runLater(() -> onStatusChanged.accept(msg));
    }

    // ===== ODTWARZANIE =====
    private void stopAndWait() {
        RadioPlayer playerToStop;
        Thread threadToStop;
        synchronized (playerLock) {
            playerToStop = currentPlayer;
            threadToStop = radioThread;
            currentPlayer = null;
            radioThread = null;
        }
        if (playerToStop != null) playerToStop.stopRadio();
        if (threadToStop != null && threadToStop.isAlive()) {
            try { threadToStop.join(3000); } 
            catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
    }

    public void stopRadio() {
        stopSongChecker();
        playRequestId++;
        Thread stopThread = new Thread(() -> {
            stopAndWait();
            fireStatus(t("⏹ Radio zatrzymane", "⏹ Radio stopped"));
            firePlayState();
        }, "Radio-Stopper");
        stopThread.setDaemon(true);
        stopThread.start();
        
        lastSongName = "";
        currentAlbumArt = null;
        Platform.runLater(() -> onAlbumArtListeners.forEach(l -> l.accept(null)));
    }

    public void togglePlay() {
        if (isPlaying()) stopRadio();
        else if (currentStationUrl != null && !currentStationUrl.isEmpty()) {
            playStation(currentStationName, currentStationUrl, currentStationFavicon);
        }
    }

    public void playStation(String name, String url, String favicon) {
        if (url == null || (!url.startsWith("http://") && !url.startsWith("https://"))) return;

        final long myRequestId = ++playRequestId;
        currentStationUrl = url;
        currentStationName = name;
        currentStationFavicon = favicon;
        
        lastSongName = "";
        currentAlbumArt = null;
        Platform.runLater(() -> {
            onSongChangedListeners.forEach(l -> l.accept("..."));
            onAlbumArtListeners.forEach(l -> l.accept(null));
        });

        fireStatus(t("⏳ Łączenie z: ", "⏳ Connecting to: ") + name);
        firePlayState();

        Thread switchThread = new Thread(() -> {
            stopSongChecker();
            stopAndWait();
            if (myRequestId != playRequestId) return;

            RadioPlayer player = new RadioPlayer(url, this);
            player.setVolume(globalVolume);

            Thread thread = new Thread(player, "Radio-Player");
            thread.setDaemon(true);
            synchronized (playerLock) {
                currentPlayer = player;
                radioThread = thread;
            }
            thread.start();
            startSongChecker();
        }, "Radio-Switcher-" + myRequestId);
        switchThread.setDaemon(true);
        switchThread.start();
    }

    public void notifyConnected() {
        fireStatus(t("▶ Gra: ", "▶ Playing: ") + currentStationName);
        firePlayState();
    }

    public boolean isPlaying() {
        synchronized (playerLock) { return currentPlayer != null && currentPlayer.isPlaying(); }
    }

    // ===== SLEEP TIMER =====
    public void setSleepTimer(int minutes) {
        if (sleepTimerFuture != null) {
            sleepTimerFuture.cancel(false);
            sleepTimerFuture = null;
        }
        if (sleepTimerExecutor == null) {
            // Optymalizacja: Używamy wątku "Daemon", by po zamknięciu programu z X nie wisiał w tle menedżera zadań.
            sleepTimerExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "Sleep-Timer");
                t.setDaemon(true);
                return t;
            });
        }
        
        if (minutes > 0) {
            sleepTimerFuture = sleepTimerExecutor.schedule(() -> {
                Platform.runLater(() -> {
                    stopRadio();
                    Platform.exit();
                    System.exit(0);
                });
            }, minutes, TimeUnit.MINUTES);
        }
    }

    // ===== SONG CHECKER & ALBUM ART =====
    private void startSongChecker() {
        stopSongChecker();
        songChecker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Radio-SongChecker");
            t.setDaemon(true);
            return t;
        });
        songChecker.scheduleAtFixedRate(this::checkCurrentSong, 3, 10, TimeUnit.SECONDS);
    }

    private void stopSongChecker() {
        if (songChecker != null && !songChecker.isShutdown()) {
            songChecker.shutdownNow();
            songChecker = null;
        }
    }

    private void checkCurrentSong() {
        String url = currentStationUrl;
        if (url == null || url.isEmpty()) return;
        CompletableFuture.runAsync(() -> {
            String fetched = RadioPlayer.fetchCurrentSong(url);
            if (fetched == null) return;
            
            String newSong = fetched.trim().isEmpty() ? t("Audycja na żywo", "Live broadcast") : formatSongTitle(fetched.trim());
            if (!newSong.equals(lastSongName)) {
                lastSongName = newSong;
                fetchAlbumArt(newSong); 
                String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd.MM"));
                synchronized (history) {
                    history.add(0, new HistoryEntry(newSong, currentStationName, timeStr));
                    if (history.size() > 200) history.remove(history.size() - 1);
                }
                if (!isBlacklisted(newSong) && !onSongChangedListeners.isEmpty()) {
                    Platform.runLater(() -> onSongChangedListeners.forEach(l -> l.accept(newSong)));
                }
            }
        });
    }

    private void fetchAlbumArt(String songName) {
        if (songName == null || songName.contains("Audycja") || songName.contains("Live")) {
            currentAlbumArt = null;
            Platform.runLater(() -> onAlbumArtListeners.forEach(l -> l.accept(null)));
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                String query = songName.replaceAll("\\[.*?\\]", "").replaceAll("\\(.*?\\)", "").trim();
                String urlStr = "https://itunes.apple.com/search?term=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&entity=song&limit=1";
                
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(urlStr)).build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

                JsonObject j = JsonParser.parseString(resp.body()).getAsJsonObject();
                if (j.has("results") && j.getAsJsonArray("results").size() > 0) {
                    JsonObject first = j.getAsJsonArray("results").get(0).getAsJsonObject();
                    String rawArtwork = first.has("artworkUrl100") ? first.get("artworkUrl100").getAsString() : null;
                    
                    final String finalArtwork = rawArtwork != null ? rawArtwork.replace("100x100bb", "200x200bb") : null;
                    
                    currentAlbumArt = finalArtwork;
                    Platform.runLater(() -> onAlbumArtListeners.forEach(l -> l.accept(finalArtwork)));
                } else {
                    currentAlbumArt = null;
                    Platform.runLater(() -> onAlbumArtListeners.forEach(l -> l.accept(null))); 
                }
            } catch (Exception e) {
                currentAlbumArt = null;
                Platform.runLater(() -> onAlbumArtListeners.forEach(l -> l.accept(null)));
            }
        });
    }

    private String formatSongTitle(String title) {
        if (title == null || title.isEmpty()) return title;
        long upper = title.chars().filter(Character::isUpperCase).count();
        long letters = title.chars().filter(Character::isLetter).count();
        if (letters > 0 && (double) upper / letters > 0.6) {
            StringBuilder sb = new StringBuilder();
            for (String word : title.toLowerCase().split(" ")) {
                if (word.isEmpty()) continue;
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
            return sb.toString().trim();
        }
        return title;
    }

    private boolean isBlacklisted(String song) {
        String lower = song.toLowerCase();
        return blacklist.stream().anyMatch(w -> lower.contains(w.toLowerCase()));
    }

    // ===== GETTERY / SETTERY =====
    public String getCurrentStationName() { return currentStationName; }
    public String getCurrentStationUrl()  { return currentStationUrl; }
    public String getCurrentStationFavicon() { return currentStationFavicon; }
    public String getLastSongName()       { return lastSongName; }
    public String getCurrentAlbumArt()    { return currentAlbumArt; }
    public float  getVolume()             { return globalVolume; }
    public void   setVolume(float v) {
        globalVolume = Math.max(0f, Math.min(v, 1f));
        synchronized (playerLock) {
            if (currentPlayer != null) currentPlayer.setVolume(globalVolume);
        }
        saveConfig();
    }
    public boolean isShowToast()          { return showToast; }
    public void    setShowToast(boolean v){ showToast = v; saveConfig(); }
    public int     getToastDuration()     { return toastDuration; }
    public void    setToastDuration(int v){ toastDuration = v; saveConfig(); }
    public String  getLanguage()          { return language; }
    public void    setLanguage(String l)  {
        language = l; saveConfig();
        loadLanguageStrings();
        if (onLanguageChanged != null) Platform.runLater(onLanguageChanged);
    }
    public String  getAccentColor()       { return accentColor; }
    public String  getAccentColorLight()  { return accentColorLight; }
    public void    setAccentColor(String c, String cl) {
        accentColor = c; accentColorLight = cl; saveConfig();
        if (onThemeChanged != null) Platform.runLater(onThemeChanged);
    }
    public boolean isMiniPlayerAlwaysOnTop()        { return miniPlayerAlwaysOnTop; }
    public void    setMiniPlayerAlwaysOnTop(boolean v){ miniPlayerAlwaysOnTop = v; saveConfig(); }

    public List<String>       getBlacklist() { return blacklist; }
    public List<FavoriteStation> getFavorites() { return favorites; }
    public List<HistoryEntry>  getHistory()  { synchronized (history) { return new ArrayList<>(history); } }

    public boolean isFavorite(String url) { return favorites.stream().anyMatch(f -> f.url.equals(url)); }
    public void toggleFavorite(String name, String url, String favicon) {
        if (isFavorite(url)) favorites.removeIf(f -> f.url.equals(url));
        else favorites.add(new FavoriteStation(name, url, favicon));
        saveConfig();
    }
    public void addToBlacklist(String song) {
        if (!blacklist.contains(song)) { blacklist.add(song); saveConfig(); }
    }

    public boolean startRecording(String path) {
        synchronized (playerLock) { return currentPlayer != null && currentPlayer.startRecording(path); }
    }
    public void stopRecording() {
        synchronized (playerLock) { if (currentPlayer != null) currentPlayer.stopRecording(); }
    }
    public boolean isRecording() {
        synchronized (playerLock) { return currentPlayer != null && currentPlayer.isRecording(); }
    }

    // ===== CONFIG =====
    public void saveConfig() {
        try {
            Path p = Path.of(System.getProperty("user.home"), ".radioapp", "config.json");
            Files.createDirectories(p.getParent());
            JsonObject j = new JsonObject();
            j.addProperty("volume", globalVolume);
            j.addProperty("showToast", showToast);
            j.addProperty("toastDuration", toastDuration);
            j.addProperty("language", language);
            j.addProperty("accentColor", accentColor);
            j.addProperty("accentColorLight", accentColorLight);
            j.addProperty("miniPlayerAlwaysOnTop", miniPlayerAlwaysOnTop);

            JsonArray fa = new JsonArray();
            for (FavoriteStation fs : favorites) {
                JsonObject o = new JsonObject();
                o.addProperty("name", fs.name); o.addProperty("url", fs.url); o.addProperty("favicon", fs.favicon);
                fa.add(o);
            }
            j.add("favorites", fa);

            JsonArray ba = new JsonArray();
            for (String w : blacklist) ba.add(w);
            j.add("blacklist", ba);

            Files.writeString(p, new GsonBuilder().setPrettyPrinting().create().toJson(j));
        } catch (Exception e) { System.err.println("[RadioApp] Błąd zapisu: " + e.getMessage()); }
    }

    public void loadConfig() {
        initLanguageSystem();
        try {
            Path p = Path.of(System.getProperty("user.home"), ".radioapp", "config.json");
            if (Files.exists(p)) {
                JsonObject j = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
                if (j.has("volume"))               globalVolume         = j.get("volume").getAsFloat();
                if (j.has("showToast"))            showToast            = j.get("showToast").getAsBoolean();
                if (j.has("toastDuration"))        toastDuration        = j.get("toastDuration").getAsInt();
                if (j.has("language"))             language             = j.get("language").getAsString();
                if (j.has("accentColor"))          accentColor          = j.get("accentColor").getAsString();
                if (j.has("accentColorLight"))     accentColorLight     = j.get("accentColorLight").getAsString();
                if (j.has("miniPlayerAlwaysOnTop"))miniPlayerAlwaysOnTop= j.get("miniPlayerAlwaysOnTop").getAsBoolean();

                favorites.clear();
                if (j.has("favorites")) {
                    for (JsonElement e : j.getAsJsonArray("favorites")) {
                        JsonObject o = e.getAsJsonObject();
                        String u = o.has("url") ? o.get("url").getAsString() : "";
                        if (!u.isEmpty()) favorites.add(new FavoriteStation(
                            o.has("name") ? o.get("name").getAsString() : "?", u,
                            o.has("favicon") ? o.get("favicon").getAsString() : ""));
                    }
                }
                blacklist.clear();
                if (j.has("blacklist"))
                    for (JsonElement e : j.getAsJsonArray("blacklist")) blacklist.add(e.getAsString());
                else { blacklist.add("reklama"); blacklist.add("ad"); }
            } else { blacklist.add("reklama"); blacklist.add("ad"); }
        } catch (Exception e) { System.err.println("[RadioApp] Błąd odczytu: " + e.getMessage()); }
        loadLanguageStrings();
    }
}