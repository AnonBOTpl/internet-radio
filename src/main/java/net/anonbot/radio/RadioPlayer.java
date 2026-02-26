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
    private ByteArrayOutputStream recordingBuffer;
    private String recordingPath;
    private AudioFormat decodedFormat;

    private volatile MediaPlayer fxPlayer;

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
                
                if (isRecording && recordingBuffer != null)
                    recordingBuffer.write(buf, 0, n);
            }

            decoded.close();
            if (isRecording) stopRecording();

        } catch (Exception e) {
            if (!stopRequested)
                System.err.println("[RadioApp] Błąd strumienia: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            if (fxPlayer == null) {
                isPlaying = false;
            }
            closeDataLine();
            disconnectConn();
        }
    }

    private void startFxPlayer() {
        Platform.runLater(() -> {
            try {
                Media media = new Media(streamUrl);
                fxPlayer = new MediaPlayer(media);
                fxPlayer.setVolume(currentVolume);
                
                fxPlayer.setOnPlaying(() -> {
                    isPlaying = true;
                    if (clientRef != null) clientRef.notifyConnected();
                });
                
                fxPlayer.setOnError(() -> {
                    System.err.println("[RadioApp] JavaFX Media błąd: " + fxPlayer.getError().getMessage());
                    isPlaying = false;
                });
                
                fxPlayer.setOnStopped(() -> isPlaying = false);
                fxPlayer.setOnEndOfMedia(() -> isPlaying = false);
                fxPlayer.setOnHalted(() -> isPlaying = false);
                
                fxPlayer.play();
            } catch (Exception ex) {
                System.err.println("[RadioApp] Błąd inicjalizacji JavaFX Media: " + ex.getMessage());
                isPlaying = false;
            }
        });
    }

    private HttpURLConnection openConnection(String url, int redirects) throws IOException {
        if (redirects > 5) return null;
        // Zastosowanie bezpiecznego URI zamiast przestarzałego new URL(url)
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
            if (loc != null && !loc.isEmpty()) return openConnection(loc, redirects + 1);
        }
        return conn;
    }

    public void stopRadio() {
        stopRequested = true;
        isPlaying = false;
        disconnectConn();
        closeDataLine();
        
        if (fxPlayer != null) {
            Platform.runLater(() -> {
                fxPlayer.stop();
                fxPlayer.dispose();
                fxPlayer = null;
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
        if (fxPlayer != null) {
            Platform.runLater(() -> fxPlayer.setVolume(currentVolume));
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
        if (fxPlayer != null) return false;
        if (isRecording || decodedFormat == null) return false;
        try {
            Path dir = Path.of(path).getParent();
            if (dir != null) Files.createDirectories(dir);
            recordingPath   = path;
            recordingBuffer = new ByteArrayOutputStream();
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