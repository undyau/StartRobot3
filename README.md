# StartRobot3

A native Android (Kotlin + Jetpack Compose) talking start clock for orienteering events. It runs on a single device at the start line and plays timed audio announcements — start times, runner names, countdowns, and start beeps — synchronized to GPS time.

## Features

- **GPS-synchronized clock** — uses GPS time (not network/system time) so the device stays accurate even offline.
- **Configurable start interval** — 30 seconds, 1 minute, or 2 minutes between starts.
- **Announcement chains** — each interval can play a sequence of announcements (TTS time, runner names, custom text, recorded clips, countdown beeps, a start beep). One announcement in each chain is the *anchor*, pinned to an exact offset from the interval boundary; everything else in the chain is scheduled before/after it and is truncated if it overruns, so the anchor always fires exactly on time.
- **Text-to-speech time announcements** — built-in Android TTS, decomposed into individual words and calibrated per-device on first run so scheduling estimates are accurate.
- **IOF XML start lists** — load a start list (file or URL) in IOF XML 3.0 format and the app will announce upcoming runners' names at their scheduled start time.
- **Adjustable delay** — shift all remaining start times forward or backward (e.g. ±1/±5 minutes) without restarting the event; the clock engine re-anchors to the new boundary.
- **Outdoor-readable run screen** — high-contrast black background with yellow/green text for visibility in direct sunlight.
- **Runs as a foreground service** so announcements keep playing while the screen is locked or the app is backgrounded.

## Screens

- **Run** — the live clock, next start time, upcoming starters, start/stop control, and delay adjustment.
- **Start List** — load an IOF XML start list from a file or URL.
- **Config** — configure the start interval and announcement chains.

## Building

Requires Android Studio (or the Gradle wrapper directly) with a JDK matching the project's Kotlin/AGP toolchain.

```bash
./gradlew assembleDebug
./gradlew installDebug
```

On Windows, if `JAVA_HOME` isn't set, point it at Android Studio's bundled JBR before invoking Gradle, e.g.:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat installDebug
```

- **Package:** `com.undy.startrobot3`
- **Min SDK:** 24 · **Target/Compile SDK:** 36

## Permissions

- `ACCESS_FINE_LOCATION` — GPS-based time source.
- `RECORD_AUDIO` — recording custom announcement clips.
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MEDIA_PLAYBACK` — keeps the clock and announcements running in the background.
- `POST_NOTIFICATIONS` — declared but not proactively requested; the foreground service still works without it, just without a visible notification.
- `INTERNET` — loading IOF XML start lists from a URL.

## Project structure

```
app/src/main/java/com/undy/startrobot3/
├── data/
│   ├── db/            Room database (announcement chains + announcements)
│   ├── model/          Announcement, Starter, TimeSpeech, etc.
│   ├── prefs/          DataStore-backed event preferences
│   └── repository/      Repository over the Room DAOs
├── engine/
│   ├── AudioEngine.kt   TTS, MediaPlayer, MediaRecorder wrapper
│   ├── BeepGenerator.kt AudioTrack-based countdown/start beeps
│   ├── ClockEngine.kt   Coroutine-based scheduler (start/stop/adjustDelay)
│   └── GpsTimeProvider.kt
├── iof/                IOF XML 3.0 start list parser
├── service/             Foreground service keeping the engine alive
└── ui/                  Compose screens: run, startlist, config
```

## How scheduling works

Each interval boundary triggers every configured announcement chain. Within a chain, the anchor announcement is scheduled at a fixed offset from the boundary; other announcements in the chain are scheduled backward/forward from the anchor using their estimated durations (measured TTS/clip durations plus a small overhead margin for real-world playback startup latency). If a non-anchor announcement is still playing when the anchor's time arrives, it's truncated so the anchor never slips.
