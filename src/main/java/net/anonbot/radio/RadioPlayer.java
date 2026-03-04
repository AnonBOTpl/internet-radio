package net.anonbot.radio;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import javax.sound.sampled.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class RadioPlayer implements Runnable {

    private final String streamUrl;
    private final RadioModClient clientRef;

    private volatile SourceDataLine  dataLine;
    private volatile HttpURLConnection activeConn;
    private volatile boolean isPlaying     = false;
    private volatile boolean stopRequested = false;
    private volatile boolean isRecording   = false;

    private float currentVolume = 0.5f;
    private static final long MAX_RECORDING_BYTES = 2L * 1024 * 1024 * 1024; // 2 GB
    private long recordingBytesWritten = 0;
    private ByteArrayOutputStream recordingBuffer;
    private String recordingPath;
    private AudioFormat decodedFormat;

    // Osobny lock dla fxPlayer — jest tworzony/niszczony na wątku JavaFX,
    // ale odczytywany (isPlaying, setVolume) z innych wątków.
    private final Object fxLock = new Object();
    private MediaPlayer fxPlayer;

    public RadioPlayer(String streamUrl, RadioModClient clientRef) {
        this.streamUrl  = streamUrl;
        this.clientRef  = clientRef;
    }

    public boolean isPlaying()   { return isPlaying; }
    public boolean isRecording() { return isRecording; }

    @Override
    public void run() {
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        isPlaying = true;

        try {
            if (stopRequested) return;

            HttpURLConnection conn = openConnection(streamUrl, 0);
            if (conn == null || stopRequested) return;
            activeConn = conn;

            if (stopRequested) { conn.disconnect(); return; }

            InputStream raw = new BufferedInputStream(conn.getInputStream(), 65536);

            AudioInputStream ais;
            try {
                ais = AudioSystem.getAudioInputStream(raw);
            } catch (UnsupportedAudioFileException e) {
                System.out.println("[RadioApp] Omijanie AudioSystem. Uruchamianie silnika JavaFX (HLS/AAC)...");
                disconnectConn();
                startFxPlayer();
                return; 
            }

            AudioFormat baseFormat = ais.getFormat();
            decodedFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                baseFormat.getSampleRate() > 0 ? baseFormat.getSampleRate() : 44100f,
                16,
                baseFormat.getChannels() > 0 ? baseFormat.getChannels() : 2,
                (baseFormat.getChannels() > 0 ? baseFormat.getChannels() : 2) * 2,
                baseFormat.getSampleRate() > 0 ? baseFormat.getSampleRate() : 44100f,
                false
            );

            AudioInputStream decoded;
            try {
                decoded = AudioSystem.getAudioInputStream(decodedFormat, ais);
            } catch (Exception e) {
                System.err.println("[RadioApp] Nie można dekodować formatu MP3/WAV: " + baseFormat);
                conn.disconnect();
                return;
            }

            if (stopRequested) { decoded.close(); conn.disconnect(); return; }

            DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
            dataLine = (SourceDataLine) AudioSystem.getLine(info);
            dataLine.open(decodedFormat);
            dataLine.start();
            updateVolumeControl();

            if (clientRef != null) clientRef.notifyConnected();

            byte[] buf = new byte[8192];
            int n;
            while (!stopRequested && (n = decoded.read(buf, 0, buf.length)) != -1) {
                dataLine.write(buf, 0, n);
                
                if (isRecording && recordingBuffer != null) {
                    if (recordingBytesWritten + n <= MAX_RECORDING_BYTES) {
                        recordingBuffer.write(buf, 0, n);
                        recordingBytesWritten += n;
                    } else {
                        // Przekroczono limit 2 GB — automatycznie zatrzymujemy nagrywanie
                        System.err.println("[RadioApp] Nagrywanie zatrzymane — osiągnięto limit 2 GB.");
                        stopRecording();
                    }
                }
            }

            decoded.close();
            if (isRecording) stopRecording();

        } catch (Exception e) {
            if (!stopRequested)
                System.err.println("[RadioApp] Błąd strumienia: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            synchronized (fxLock) {
                if (fxPlayer == null) isPlaying = false;
            }
            closeDataLine();
            disconnectConn();
        }
    }

    private void startFxPlayer() {
        Platform.runLater(() -> {
            try {
                Media media = new Media(streamUrl);
                MediaPlayer player = new MediaPlayer(media);
                player.setVolume(currentVolume);

                player.setOnPlaying(() -> {
                    isPlaying = true;
                    if (clientRef != null) clientRef.notifyConnected();
                });
                player.setOnError(() -> {
                    System.err.println("[RadioApp] JavaFX Media błąd: " + player.getError().getMessage());
                    isPlaying = false;
                });
                player.setOnStopped(() -> isPlaying = false);
                player.setOnEndOfMedia(() -> isPlaying = false);
                player.setOnHalted(() -> isPlaying = false);

                synchronized (fxLock) { fxPlayer = player; }
                player.play();
            } catch (Exception ex) {
                System.err.println("[RadioApp] Błąd inicjalizacji JavaFX Media: " + ex.getMessage());
                isPlaying = false;
            }
        });
    }

    private HttpURLConnection openConnection(String url, int redirects) throws IOException {
        if (redirects > 5) return null;
        // Walidacja URL przed połączeniem (ochrona przed SSRF i przekierowaniami na file://)
        if (!RadioModClient.isUrlSafe(url)) return null;
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setConnectTimeout(7000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        conn.setRequestProperty("Accept", "*/*");
        conn.setInstanceFollowRedirects(true);
        conn.connect();
        int code = conn.getResponseCode();
        if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
            String loc = conn.getHeaderField("Location");
            conn.disconnect();
            // Walidujemy też URL przekierowania przed podążeniem za nim
            if (loc != null && !loc.isEmpty() && RadioModClient.isUrlSafe(loc))
                return openConnection(loc, redirects + 1);
        }
        return conn;
    }

    public void stopRadio() {
        stopRequested = true;
        isPlaying = false;
        disconnectConn();
        closeDataLine();

        MediaPlayer playerToStop;
        synchronized (fxLock) {
            playerToStop = fxPlayer;
            fxPlayer = null;
        }
        if (playerToStop != null) {
            Platform.runLater(() -> {
                playerToStop.stop();
                playerToStop.dispose();
            });
        }
    }

    private void disconnectConn() {
        HttpURLConnection c = activeConn;
        activeConn = null;
        if (c != null) try { c.disconnect(); } catch (Exception ignored) {}
    }

    private void closeDataLine() {
        SourceDataLine dl = dataLine;
        dataLine = null;
        if (dl != null && dl.isOpen()) {
            try { dl.flush(); dl.stop(); dl.close(); } catch (Exception ignored) {}
        }
    }

    public void setVolume(float v) {
        currentVolume = Math.max(0f, Math.min(v, 1f));
        synchronized (fxLock) {
            if (fxPlayer != null) {
                final float vol = currentVolume;
                Platform.runLater(() -> fxPlayer.setVolume(vol));
            }
        }
        updateVolumeControl();
    }

    private void updateVolumeControl() {
        SourceDataLine dl = dataLine;
        if (dl != null && dl.isOpen() && dl.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl fc = (FloatControl) dl.getControl(FloatControl.Type.MASTER_GAIN);
            float db = currentVolume == 0f ? fc.getMinimum()
                     : (float)(Math.log10(currentVolume) * 20.0);
            fc.setValue(Math.max(fc.getMinimum(), Math.min(db, fc.getMaximum())));
        }
    }

    public boolean startRecording(String path) {
        synchronized (fxLock) { if (fxPlayer != null) return false; }
        if (isRecording || decodedFormat == null) return false;
        try {
            Path dir = Path.of(path).getParent();
            if (dir != null) Files.createDirectories(dir);
            recordingPath   = path;
            recordingBuffer = new ByteArrayOutputStream();
            recordingBytesWritten = 0;
            isRecording     = true;
            return true;
        } catch (IOException e) { return false; }
    }

    public void stopRecording() {
        if (!isRecording) return;
        isRecording = false;
        if (recordingBuffer != null && recordingPath != null && decodedFormat != null) {
            try {
                byte[] pcm = recordingBuffer.toByteArray();
                AudioInputStream pcmStream = new AudioInputStream(
                    new ByteArrayInputStream(pcm), decodedFormat,
                    pcm.length / decodedFormat.getFrameSize());
                AudioSystem.write(pcmStream, AudioFileFormat.Type.WAVE, new File(recordingPath));
            } catch (IOException e) { System.err.println("[RadioApp] Błąd zapisu WAV: " + e.getMessage()); }
            recordingBuffer = null;
        }
    }

    public static String fetchCurrentSong(String streamUrl) {
        if (!RadioModClient.isUrlSafe(streamUrl)) return null;
        HttpURLConnection conn = null;
        try {
            // Zastosowanie bezpiecznego URI
            conn = (HttpURLConnection) URI.create(streamUrl).toURL().openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            conn.setRequestProperty("Icy-MetaData", "1");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.connect();

            int metaInt = conn.getHeaderFieldInt("icy-metaint", 0);
            if (metaInt == 0) return null;

            InputStream is = conn.getInputStream();
            int toSkip = metaInt;
            while (toSkip > 0) {
                long s = is.skip(toSkip);
                if (s <= 0) { if (is.read() == -1) break; toSkip--; }
                else toSkip -= (int)s;
            }
            int metaLen = is.read() * 16;
            if (metaLen <= 0 || metaLen > 4096) return null;

            byte[] meta = new byte[metaLen];
            int read = 0;
            while (read < metaLen) {
                int r = is.read(meta, read, metaLen - read);
                if (r == -1) break;
                read += r;
            }
            String s = new String(meta, StandardCharsets.UTF_8);
            int start = s.indexOf("StreamTitle='");
            if (start >= 0) {
                start += 13;
                int end = s.indexOf("';", start);
                if (end >= 0) return s.substring(start, end).trim();
            }
        } catch (Exception ignored) {}
        finally { if (conn != null) try { conn.disconnect(); } catch (Exception ignored) {} }
        return null;
    }
}