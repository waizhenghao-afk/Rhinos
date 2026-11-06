# Rhinos — Project Status (saved 2026-10-04)

Session handoff notes. Read this first when continuing work on the Rhinos app.

## What this is

**Rhinos** — offline, ad-free investment journal for Android (user's brief:
"simple, clean, fast, private"). Golden rhino logo on black background.
App name: **Rhinos** (NOT InvestLog — user renamed it).
Package: `com.rhinos.app`, version 1.0.0, minSdk 26, targetSdk 35.
Stack: Kotlin 2.0.21, Jetpack Compose (BOM 2024.12.01), Material 3, Room 2.6.1,
kotlinx.serialization, AlarmManager.

## Where everything is

| Item | Path |
|---|---|
| Project root | `C:\Users\waizh\.ilmucode\workspace\default\Rhinos\` |
| Debug APK | `C:\Users\waizh\.ilmucode\workspace\default\Rhinos-1.0.0-debug.apk` (copy also on Desktop: `C:\Users\waizh\OneDrive\Desktop\`) |
| Toolchain (JDK 21, Gradle 8.10.2, Android SDK 35) | `C:\Users\waizh\.ilmucode\workspace\default\.tools\` |
| Icon generator (Java, with previews) | `C:\Users\waizh\.ilmucode\workspace\default\.tools\icontool\` |
| Status file (this file) | `C:\Users\waizh\.ilmucode\workspace\default\RHINOS_PROJECT_STATUS.md` |

Note: `local.properties` in the project points `sdk.dir` at the `.tools` SDK.

## How to build / test

```bash
cd C:\Users\waizh\.ilmucode\workspace\default\Rhinos
gradlew.bat assembleDebug          # APK -> app\build\outputs\apk\debug\app-debug.apk
gradlew.bat testDebugUnitTest      # unit tests (29 tests, all passing)
```

(The wrappers set JAVA_HOME/ANDROID_HOME to the .tools paths automatically.
The system `java` is JDK 25 — do NOT use it for Gradle; JDK 21 is in .tools.)

## Current state: COMPLETE & VERIFIED

- `assembleDebug` → BUILD SUCCESSFUL, APK 17.5 MB, debug-signed.
- 29/29 unit tests pass: FIFO (12), Expiry (5), TradeCalc (3), Stats (3),
  Reports (3), Backup round-trip (3).
- APK verified with aapt: correct package/label/permissions (only
  POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM, RECEIVE_BOOT_COMPLETED).
- Launcher icon visually verified from inside the built APK (golden rhino,
  black bg, all densities + adaptive + monochrome + notification glyph).

## Architecture map (33 Kotlin files)

```
com.rhinos.app/
├── MainActivity.kt / RhinosApp.kt      # Activity + AppContainer (manual DI)
├── calculator/FifoEngine.kt            # FIFO lot matching (pure logic, tested)
├── calculator/Calculators.kt           # buy/sell math, expiry, stats, reports
├── data/model/Models.kt                # Trade, DailyPnl, Note + enums
├── data/db/                            # Room: RhinosDatabase, Daos, Converters
├── data/repository/                    # TradeRepository (FIFO validation),
│                                       # DailyPnl/Note repositories
├── export/BackupManager.kt             # JSON export/import
├── notifications/                      # ExpiryScheduler, alarm+boot receivers
├── settings/SettingsRepository.kt      # StateFlow<AppSettings> + prefs
└── ui/                                 # navigation, components, theme,
    └── home trades daily reports notes margin more settings
```

Bottom nav: Home, Trades, Daily P&L, Expiry, More (Reports/Notes/Settings
live behind "More"). Onboarding gate on first launch.

## PENDING FOR NEXT SESSION

- **User said: "there is something to change" — NOT YET SPECIFIED.**
  Ask what they want changed before doing anything else.
- Possible future ideas already in README: CSV export, biometric lock,
  charts, settlement calendars, widgets, multiple portfolios.

## Known quirks / notes

- Editing/deleting a BUY that sits before existing SELLs replays the ticker
  history and rejects edits that would break FIFO (oversell) — by design.
- Import appends rows (ids regenerated); invalid rows skipped, not fatal.
- Sample data: tap "Rhinos v1.0.0" row in Settings 3 times.
- The trade form shows available units hint when SELL is selected.
- The system `python` is the Windows Store stub — unusable; Java was used for
  icon generation instead.
