# Security Policy

## Reporting a vulnerability

If you find a security vulnerability in NotifSilencer, please report it **privately** rather than opening a public issue.

- Use GitHub's **[Private vulnerability reporting](https://github.com/chantleyw/NotifSilencer/security/advisories/new)** ("Report a vulnerability" under the repository's **Security** tab). This keeps the details private until a fix is available.

Please include:

- what the issue is and where in the app/code it occurs,
- steps to reproduce it, and
- the app version (see **⋮ menu** in the app, or the release tag).

This is a small, single-maintainer, open-source project, so responses are best-effort — but security reports are taken seriously and will be looked at as soon as possible.

## Scope / good to know

NotifSilencer is designed to minimise its own attack surface:

- It declares **no `INTERNET` permission**, so it cannot make network connections or exfiltrate data. This is enforced by Android.
- It has no analytics, ads, or third-party runtime libraries.
- All data (settings, intercepted-notification log) stays in the app's private on-device storage and is never transmitted.

Because it uses a `NotificationListenerService`, it can read notifications from other apps while notification access is granted — that is its core function. Reports about how that data is handled **on-device** are in scope; the app never sends it anywhere.

## Supported versions

Only the **latest release** is supported. Please update to the newest version (from [Releases](https://github.com/chantleyw/NotifSilencer/releases)) before reporting an issue.
