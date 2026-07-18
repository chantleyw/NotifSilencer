# PlayFilter

A sideload-only Android app that suppresses Google **Play Store promotional /
recommendation notifications** while letting **payment & billing notifications**
through. Google bundles both under a single notification channel in the settings
UI, so you can't disable one without losing the other — this app splits them
apart with a `NotificationListenerService`.

**Package:** `com.chantley.playfilter` · **minSdk 26 · targetSdk 34 · Kotlin · Gradle KTS · no Compose · no third-party libs.**

---

## How it decides (biased toward false negatives)

For every notification from `com.android.vending` (Play Store) or
`com.google.android.gms` (Play Services / billing), the service builds a
lower-cased haystack from `EXTRA_TITLE`, `EXTRA_TEXT`, `EXTRA_BIG_TEXT`,
`EXTRA_SUB_TEXT` and then:

1. **ALLOW keywords are checked first and always win.** If any match, the
   notification is kept and never cancelled (payment, purchase, receipt, refund,
   order, subscription, renew, billing, card, charged, declined, expire,
   invoice, transaction, …).
2. **BLOCK channel IDs** — exact match against the notification's `channelId`.
   Empty by default; fill in once you've observed real Play Store channel IDs in
   the log (channel-based blocking is more reliable than keywords).
3. **BLOCK keywords** — recommend, for you, deal, offer, sale, discount, top
   charts, trending, new game, editors' choice, free trial, promo, …
4. **Anything else is kept.** When in doubt, the notification goes through —
   missing a declined-card alert is far worse than seeing a promo.

All keyword lists and the channel-ID set are **editable at runtime** (in-app
screen, stored in `SharedPreferences`, seeded with the defaults above).

## Log-only mode (default ON)

`Prefs.isLogOnly` defaults to **true**: the service logs what it *would* have
cancelled but never actually cancels. Run it this way for a week, inspect real
Play Store notifications, tune the lists, then flip to **Enforce** in the UI.

Inspect via logcat:

```
adb logcat -s PlayFilter
```

Each line includes the package name, channel ID, full concatenated text, the
verdict, and the matched rule. The same records are also kept in-app (last 100,
persisted) so you don't need adb.

---

## Build

You need the Android SDK (API 34). Easiest path is **Android Studio** (Hedgehog+):

1. `File ▸ Open` this folder. On first sync Android Studio downloads Gradle and
   **regenerates `gradle/wrapper/gradle-wrapper.jar`** automatically.
2. `Build ▸ Build App Bundle(s) / APK(s) ▸ Build APK(s)`.
   Output: `app/build/outputs/apk/debug/app-debug.apk`.

### Command line

The wrapper scripts (`gradlew`, `gradlew.bat`) are included, but the binary
`gradle-wrapper.jar` is **not** (it can't be checked in as text). Generate it
once with a system Gradle 8.9:

```bash
gradle wrapper --gradle-version 8.9
```

Then build:

```bash
# macOS / Linux
./gradlew assembleDebug
# Windows
gradlew.bat assembleDebug
```

Create `local.properties` in the project root pointing at your SDK if Gradle
can't find it:

```
sdk.dir=C:\\Users\\USER-PC\\AppData\\Local\\Android\\Sdk
```

---

## Sideload

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

(or `adb install-multiple` from Android Studio's Run button with your phone in
USB-debugging mode).

---

## Grant permissions (one-time, on the phone)

Open the app, then use its buttons:

1. **Open notification access settings** → enable **PlayFilter**. This is the
   critical permission (`BIND_NOTIFICATION_LISTENER_SERVICE`); the status line at
   the top of the app shows GRANTED / NOT granted (read from
   `enabled_notification_listeners`).
2. **Disable battery optimisation for PlayFilter** → confirm the exemption.
   Aggressive OEM battery management (Samsung, Xiaomi, OPPO, etc.) will kill the
   listener otherwise; some OEMs also have a separate "auto-start" toggle you may
   need to enable manually.
3. Leave **Log-only** on for a week, watch the in-app log (or `adb logcat -s
   PlayFilter`), tune the keyword/channel lists via **Edit keyword / channel
   lists**, then turn **Log-only off** to start enforcing.

To confirm the listener via adb:

```bash
adb shell settings get secure enabled_notification_listeners
```

---

## Project layout

```
settings.gradle.kts
build.gradle.kts
gradle.properties
gradlew / gradlew.bat
gradle/wrapper/gradle-wrapper.properties      (jar generated on first build)
app/build.gradle.kts
app/proguard-rules.pro
app/src/main/AndroidManifest.xml
app/src/main/kotlin/com/chantley/playfilter/
    PlayFilterService.kt      NotificationListenerService — the core filter
    MainActivity.kt           status, log-only toggle, in-app log, battery prompt
    KeywordEditActivity.kt    runtime editor for ALLOW / BLOCK / channel lists
    Prefs.kt                  SharedPreferences-backed config + seed defaults
    LogStore.kt               persisted ring buffer of the last 100 interceptions
app/src/main/res/…            layouts, strings, theme, adaptive launcher icon
```

## Notes / caveats

- A `NotificationListenerService` can only *cancel* notifications it can see; it
  can't rewrite Google's channel grouping. Cancelling happens the moment the
  promo is posted, so you may see a brief flash on some OEMs.
- If you revoke and re-grant notification access, Android restarts the service.
- Keep an eye on the log after Google reworks their notification wording — that's
  what the runtime-editable lists are for.
