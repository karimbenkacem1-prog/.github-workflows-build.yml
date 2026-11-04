# وقتك ثمين | Your Time Is Precious

Islamic screen-time reminder app. Kotlin · Jetpack Compose · Material 3 · MVVM · Room · DataStore · WorkManager.
Created by كريم بن قاسم | Karim Benkacem — WhatsApp +213 778 38 88 78.

## Build & install
1. Install Android Studio (Koala+ / JDK 17). **File → Open** this folder and let Gradle sync
   (if `gradlew` is missing, run `gradle wrapper --gradle-version 8.9` once, or just use Studio).
2. Connect a phone (Android 9+) or start an emulator and press **Run ▶**.
3. For an APK: **Build → Build APK(s)** → `app/build/outputs/apk/`.
4. On first launch follow onboarding: grant **Usage access** (system screen) and **Notifications**.

## Architecture
```
data/    DataStore prefs (Prefs, K), Room (DayStat reminders log), static content (activities, daily messages)
logic/   UsageRepo  – reads UsageStatsManager events; Reminders – milestone engine + quiet hours;
         Notifier – channels & notifications; TrackerService – foreground service; CheckWorker/SessionWorker; BootReceiver
ui/      Theme (colors, Islamic backgrounds, Quran illustration, charts) + Screens (Onboarding, Home, Stats, Invest, Settings, About)
MainVM   single ViewModel exposing settings + usage UiState
```
Screens: Onboarding → bottom nav (Home · Stats · Invest · Settings) → About (from Settings).

## How tracking works (and its limits)
* **Screen-on time** is reconstructed from `UsageStatsManager.queryEvents` (SCREEN_INTERACTIVE / SCREEN_NON_INTERACTIVE). It counts time the screen is on, including the lock screen.
* **Total reminder**: whenever cumulative screen-on time crosses a multiple of the interval (default 4 h) one notification is sent. Milestones are stored per date → no duplicates and an automatic reset at midnight.
* **Continuous reminder**: screen-on intervals separated by less than the *break length* (default 5 min, adjustable) are merged into one session; a notification is sent at each multiple of the interval (default 2 h).
* **Timeliness**: Android gives no public "continuous use" API. A lightweight foreground service (only active while the screen is on; one query/minute) gives ~1-minute accuracy. If the OEM kills it, a 15-minute WorkManager watchdog still catches up (reminders are computed from system events, so they are not lost, only delayed). Disable battery optimization for best results on aggressive OEMs (Xiaomi, Huawei, Oppo…).
* Per-app times come from `queryAndAggregateUsageStats`; Android only keeps usage history for a limited period.
* Quiet hours suppress notifications (milestones are consumed so nothing floods after quiet hours end).

## Customization
* Fonts: drop e.g. Amiri / Reem Kufi into `res/font/` and use them in `ui/Screens.kt` instead of `FontFamily.Serif`.
* Add content in `data/Data.kt` (activities, daily messages).
