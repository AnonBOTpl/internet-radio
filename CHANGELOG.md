# Changelog

## [1.1.0] — 2026-03-04

### 🐛 Bug Fixes

- **Album art & Toast out of sync** — the Toast notification now always displays the correct cover art. Previously, the iTunes response arrived after the toast was already shown, causing it to fall back to the station favicon instead of the album cover.
- **Stale album art from previous track** — if the current song changed while an iTunes request was still in flight, the result is now discarded. No more cover art from the previous track bleeding into the next one.
- **Blacklist did not block album art fetching** — blacklisted tracks still triggered iTunes API requests and updated `currentAlbumArt` in the background. Fixed: blacklisted tracks now clear the cover art and skip the iTunes query entirely.
- **"Clear history" button had no effect** — `getHistory()` returns a copy of the internal list, so calling `.clear()` on it did nothing. Fixed via a dedicated `clearHistory()` method that operates on the original list.
- **BlacklistDialog modified internal list without synchronization** — direct access to the blacklist from the dialog could cause a `ConcurrentModificationException` during playback. All operations now go through synchronized methods.
- **Race condition on `fxPlayer`** — the `fxPlayer` field was assigned on the JavaFX thread but read from other threads without synchronization. Fixed by introducing a dedicated `fxLock` object.

### ✨ New Features

- **Persistent song history** — the last 50 played songs are now saved to `config.json` and restored on the next launch.
- **2 GB recording limit** — live recording now automatically stops when the buffer reaches 2 GB, preventing an `OutOfMemoryError` during long sessions.
- **Volume tooltip in MiniPlayer** — the volume slider in MiniPlayer mode now shows the current percentage in a tooltip, updated in real time.

### 🔒 Security

- **SSRF / URL validation** — introduced `isUrlSafe()` which blocks connections to `localhost`, `127.x`, `10.x`, `192.168.x`, `169.254.x`, IPv6 loopback (`::1`), and non-HTTP protocols (`file://`, `javascript:`, `data:`). Applied to stream playback, HTTP redirects, and the song metadata checker.
- **CSS injection protection** — accent color values loaded from `config.json` are now validated against the `#RRGGBB` format via `sanitizeColor()` before being applied to the UI.
- **Path traversal protection** — language file names are now validated against `[a-z]{2,10}` and the resolved path is checked to stay within the `langs/` directory.
- **Thread-safe blacklist** — added `blacklistLock`; `isBlacklisted()`, `addToBlacklist()`, and `removeFromBlacklist()` are now fully synchronized.

### 🧹 Refactoring

- **Removed `TrayManager.java`** — the class was unused (system tray was replaced by MiniPlayer). Removing it eliminates dead code and reduces project size.
- **Removed static `INSTANCE` singleton** — `RadioModClient.INSTANCE` and `getInstance()` were unused outside the class itself and have been removed.
- **Proper executor shutdown** — new `shutdownExecutors()` method in `RadioModClient` correctly shuts down `sleepTimerExecutor` and `songChecker` when the app closes.
- **Translated `BlacklistDialog`** — all hardcoded Polish strings replaced with `modClient.t()` calls for proper multilingual support.
- **New app icon** — replaced with a new 256×256 PNG/ICO featuring a music note and sound waves, fully filling the icon area and matching the app's purple accent color.

---

## [1.0.0] — initial release

- Station search via Radio-Browser API
- MP3 / AAC / HLS stream playback
- MiniPlayer, Sleep Timer, WAV recording
- Song history, blacklist, Toast notifications
- Album art via iTunes API
- Global media key support
- Multilingual support (PL/EN) via JSON files
