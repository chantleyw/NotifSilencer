# NotifSilencer

**Stop unwanted "Recommendations" and promotional notifications on Android — including the ones your phone gives you no way to turn off.**

NotifSilencer is a lightweight, open-source notification filter for Android. It watches incoming notifications and silently cancels the promotional / "Recommendations" junk while **always letting important things (payments, billing, OTP) through**. It was built to kill the **Honor / Huawei "Recommendations" notifications** that have no off switch in system settings — but it works as a general per-app notification filter for anything.

- **No internet permission. No servers. No tracking.** Your notifications never leave your phone.
- **Open source** — read exactly what it does.
- **Sideloaded** (GitHub release APK) — not on the Play Store.

---

## The problem it solves

Some phones — Honor and Huawei especially — push "Recommendations" spam through a **system push agent** (`com.hihonor.android.pushagent`) that you **cannot disable** in notification settings. The word that identifies them isn't even in the visible text; it's hidden in the notification's *channel ID* (`…RECOMMEND…`), with a rotating suffix. That's why:

- You can't turn them off in Settings — there's no toggle.
- Most general notification-blocker apps miss them — they match on visible text, not the channel ID, and they get killed by the phone's aggressive battery manager before the next one arrives.

NotifSilencer matches on the **channel ID**, survives aggressive battery management with a keep-alive service, and is preconfigured to catch these out of the box.

---

## Privacy & trust

This app reads your notifications, so you should be able to trust it. It's built so you don't have to take anyone's word for it:

- **No `INTERNET` permission is declared.** Android's OS blocks all network access for an app without it — so NotifSilencer *cannot* connect to the internet, send data anywhere, or be reached remotely. This is enforced by the operating system, not a promise.
- **No networking code, no analytics, no ad SDKs, no Firebase** — none. The source is here; check for yourself.
- **Everything stays on-device.** Intercepted notification info is stored only in the app's private storage (sandboxed to the app) and never transmitted.
- **Works fully offline** — put the phone in airplane mode and it still filters.
- **WhatsApp is ignored by default**, and you can exclude any other app with one tap so it's never even inspected.

The only URL in the entire app is the Ko-fi donation link, which — *only if you tap the donate button* — opens in your **browser**. The app itself makes no network connections.

Verify it yourself: decompile the APK and confirm there's no `INTERNET` permission, or just run it in airplane mode.

---

## Features

- Filters notifications from **any app** (with a per-app ignore list).
- **Allow-first safety:** a configurable ALLOW list (payment, receipt, refund, billing, declined, invoice, …) is checked first and **always wins** — those notifications are never cancelled.
- **Block by keyword** (recommend, offer, deal, sale, promo, …) matched against the notification text **and its channel ID**.
- **Block by channel ID** (substring match, so rotating suffixes don't matter).
- **Block by app** — cancel everything from a chosen app (ALLOW still protects payments).
- **Tap-to-block from the log** — tap any intercepted notification to block its channel or app, or ignore the app, without typing anything.
- **Managed block list** — add/remove blocked channels and apps from a list, plus a text editor for keywords/ALLOW.
- **Tap-to-ignore app picker** — pick apps to exclude entirely from a list of your installed apps; no typing package names.
- **Export / import settings** to a JSON file so your rules survive reinstalls.
- **Log-only mode (default on):** records what it *would* cancel without cancelling, so you can watch real notifications before enforcing.
- **In-app log** of the last 100 intercepted notifications (package, channel, text, verdict) — no adb needed.
- **Keep-alive foreground service** so aggressive OEM battery managers (Honor/Huawei/Xiaomi) can't silently kill the filter.
- **Runtime-editable** keyword, channel, and ignore lists — no rebuild needed to tune.
- Biased toward **false negatives**: when in doubt, it keeps the notification. Missing a promo is far better than losing a payment alert.

---

## How it decides (per notification)

1. **Ignored package?** (e.g. WhatsApp) → skipped entirely: never logged, matched, or cancelled.
2. **ALLOW keyword match?** (text + channel ID) → **KEEP**, always. Never cancels a payment/billing notification.
3. **BLOCK channel ID match?** (substring) → cancel.
4. **BLOCK keyword match?** (text + channel ID) → cancel.
5. **Otherwise** → keep.

In **log-only mode**, steps 3–4 log the decision as `WOULD-KILL` but don't cancel.

---

## Install (sideload)

1. Download the latest **`app-release.apk`** from the [Releases](../../releases) page.
2. On your phone, open the APK. Android will ask to allow installing from this source — enable **Install unknown apps** for your browser/file manager, then install.
3. Open **NotifSilencer** and complete the one-time setup below.

> No Google account or Play Store needed.

### One-time permission setup (on the phone)

The app's buttons take you to each screen:

1. **Open notification access settings** → enable **NotifSilencer**. This is the essential permission — nothing works without it. The status line at the top shows GRANTED / NOT granted.
2. **Disable battery optimisation for NotifSilencer** → confirm.
3. **Honor / Huawei only — protected apps (important):** the stock battery exemption isn't enough on these phones. Also do:
   - Settings → Battery → **App launch** → NotifSilencer → turn **off** "Manage automatically" → manually enable **Auto-launch**, **Secondary launch**, **Run in background**.
   - Lock the app in the recent-apps view (padlock icon).
4. Grant the **notification** permission prompt when it appears (for the keep-alive status notification).

You'll see a permanent low-key **"NotifSilencer is active"** notification — that's the keep-alive that stops the OS killing the filter. Leave it; it's what makes the app reliable.

---

## Using it

- **Choose apps to ignore** — tap apps you never want touched (WhatsApp is already ignored).
- **Edit keyword / channel lists** — tune ALLOW / BLOCK / channel / ignore lists as wording changes.
- **Log-only vs Enforce** — leave log-only ON for a few days, watch the log, confirm the junk shows as `WOULD-KILL` and nothing important does, then turn it **off** to start actually silencing.
- **Intercepted log** — the last 100 notifications with verdicts; use **Refresh** / **Clear**.

Advanced (optional) — inspect via adb:

```
adb logcat -s NotifSilencer
```

---

## Build from source

Requires **JDK 17** and the **Android SDK (API 34)**. Easiest with **Android Studio**:

1. `File ▸ Open` this folder. On first sync, Android Studio downloads Gradle and (if missing) regenerates `gradle/wrapper/gradle-wrapper.jar`.
2. **Run ▶** to build and install to a connected phone, or **Build ▸ Build APK(s)** for a debug APK at `app/build/outputs/apk/debug/app-debug.apk`.

Command line:

```bash
./gradlew assembleDebug        # macOS/Linux
gradlew.bat assembleDebug      # Windows
```

If Gradle can't find your SDK, add `local.properties`:

```
sdk.dir=C:\\Users\\YOU\\AppData\\Local\\Android\\Sdk
```

**Stack:** Kotlin · Gradle KTS · minSdk 26 · targetSdk 34 · no Compose · no third-party libraries. Package/applicationId `com.notifsilencer.app`.

---

## Building a signed release (maintainer)

Public APKs must be signed with a **stable release keystore** you reuse for every update — otherwise updates won't install over older versions.

1. Android Studio: **Build ▸ Generate Signed Bundle / APK ▸ APK ▸ Create new…** keystore. **Back up the `.jks` and passwords** — if lost, you can never update the app for existing users.
2. Build the release variant → `app/build/outputs/apk/release/app-release.apk`.
3. Attach that APK to a GitHub Release.

The keystore and `keystore.properties` are gitignored — **never commit them**.

---

## Project layout

```
app/src/main/kotlin/com/notifsilencer/app/
    NotifSilencerService.kt    NotificationListenerService — the core filter
    KeepAliveService.kt     foreground service that keeps the filter alive
    BootReceiver.kt         restarts keep-alive after reboot
    MainActivity.kt         status, log-only toggle, navigation, export/import, donate
    LogActivity.kt          intercepted log as a tappable list
    BlockLogActivity.kt     persistent blocked-only history (tappable)
    ManageBlockActivity.kt  add/remove blocked channels and apps
    IgnoreAppsActivity.kt   tap-to-ignore app picker
    KeywordEditActivity.kt  text editor for ALLOW / BLOCK / channel / ignore lists
    BlockActions.kt         "tap a log entry to block/ignore" dialog
    LogEntryAdapter.kt      list adapter for log rows
    LogRender.kt            shared coloured-verdict formatting
    Prefs.kt                SharedPreferences config, defaults, export/import
    LogStore.kt             persisted ring buffer of the last 100 interceptions
    BlockLog.kt             persistent block-decision history (500)
app/src/main/res/…          layouts, strings, theme, adaptive launcher icon
```

---

## Support

NotifSilencer is free and open source. If it saved your sanity, you can chip in:

☕ **[ko-fi.com/moersebene](https://ko-fi.com/moersebene)**

---

## Notes & caveats

- A notification listener can only *cancel* notifications it sees, the moment they're posted — on some OEMs you may see a brief flash before it's removed.
- It can't rewrite an app's notification channels; it cancels matching notifications instead.
- After a reboot, open the app once to be sure the listener is reconnected (some OEMs delay re-binding).
- Off-Play means **no auto-update** — grab new APKs from Releases.
