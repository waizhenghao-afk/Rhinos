<div align="center">

# Rhinos 🦏

### A calm, offline-first journal for your investments.

Record trades, follow realized profit and loss, track dividends, and keep notes
about your investing decisions — all on your Android device.

![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4)
![Offline](https://img.shields.io/badge/Works-offline-2E8B57)

</div>

---

## At a glance

- **A personal journal, not a broker.** Rhinos does not place trades or provide
  investment advice.
- **Stored on your device.** There is no account, cloud sync, advertising, or
  analytics service.
- **Built for clear records.** Keep trades, daily P&L, dividends, expiry dates,
  and investing notes together.
- **Made for Android.** Native Kotlin app with a Jetpack Compose interface.

> Public source code does not make your app's records public. The app keeps its
> database in Android's private app storage. Backups are files you choose to
> export or import.

## Features

| Area | What you can do |
| --- | --- |
| Trades | Record buys and sells, including ticker, units, price, charges, date, and notes. |
| Profit and loss | See trade calculations and realized P&L using first-in, first-out (FIFO) matching. |
| Dividends | Record dividends and review monthly and yearly totals. |
| Daily journal | Enter daily portfolio P&L and review time-period summaries. |
| Reports | Review monthly or yearly results, trade counts, and winning/losing trades. |
| Expiry reminders | Track T+3, T+10, or permanent margin periods and optionally receive reminders. |
| Notes | Save and search strategies, trade ideas, lessons, and general notes. |
| Backup | Export or import records as JSON using Android's file picker. |
| Preferences | Choose a theme and display currency. |

## Project file guide

The app's Kotlin source is under
`app/src/main/java/com/rhinos/app/`. Here is what each source file is for.

### App startup

| File | Purpose |
| --- | --- |
| `MainActivity.kt` | Hosts the main Compose UI after the splash screen. |
| `SplashActivity.kt` | Android launcher activity and initial screen. |
| `RhinosApp.kt` | Application setup and the `AppContainer` that wires app dependencies together. |

### Calculations

| File | Purpose |
| --- | --- |
| `calculator/Calculators.kt` | Calculates trade totals, expiry dates, statistics, and report summaries. |
| `calculator/FifoEngine.kt` | Matches sell trades against the oldest eligible buy lots and calculates realized P&L. |

### Data and persistence

| File | Purpose |
| --- | --- |
| `data/model/Models.kt` | Trade, daily P&L, and note data models and their related enums. |
| `data/db/RhinosDatabase.kt` | Room database definition and database access point. |
| `data/db/Daos.kt` | Room queries and database operations for trades, daily P&L, and notes. |
| `data/db/Converters.kt` | Converts Kotlin types to and from values Room can store. |
| `data/repository/TradeRepository.kt` | Trade data access, validation, and trade-related business rules. |
| `data/repository/Repositories.kt` | Data access for daily P&L records and notes. |
| `settings/SettingsRepository.kt` | Reads and saves app preferences such as theme and currency. |

### Backup and reminders

| File | Purpose |
| --- | --- |
| `export/BackupManager.kt` | Serializes records to JSON and restores them from a backup. |
| `notifications/ExpiryScheduler.kt` | Schedules and cancels trade-expiry reminder alarms. |
| `notifications/ExpiryAlarmReceiver.kt` | Handles a reminder alarm and posts its notification. |
| `notifications/BootCompletedReceiver.kt` | Restores scheduled reminders after the device restarts. |

### User interface

| File | Purpose |
| --- | --- |
| `ui/navigation/RhinosNav.kt` | Defines app sections and connects screens with Compose navigation. |
| `ui/navigation/Onboarding.kt` | Displays the first-run welcome screen. |
| `ui/home/HomeScreen.kt` | Home dashboard and portfolio overview. |
| `ui/trades/TradesScreen.kt` | Displays and manages the trade list. |
| `ui/trades/TradeForm.kt` | Trade entry and editing form. |
| `ui/daily/DailyPnlScreen.kt` | Daily portfolio P&L journal screen. |
| `ui/reports/ReportsScreen.kt` | Monthly and yearly report screen. |
| `ui/notes/NotesScreen.kt` | Notes and strategy notebook screen. |
| `ui/margin/MarginScreen.kt` | Margin periods and expiry status screen. |
| `ui/more/MoreScreen.kt` | Additional tools and navigation options. |
| `ui/settings/SettingsScreen.kt` | Theme, currency, expiry-tip, and data-management settings. |
| `ui/components/CommonComponents.kt` | Shared Compose components used across screens. |
| `ui/components/Format.kt` | Shared display formatting for amounts and values. |
| `ui/theme/Color.kt` | App color palette. |
| `ui/theme/Theme.kt` | Compose Material theme setup, including light and dark modes. |

### Android resources

| Path | Purpose |
| --- | --- |
| `app/src/main/AndroidManifest.xml` | Declares app activities, reminder receivers, and required Android permissions. |
| `app/src/main/res/values/strings.xml` | App name and other string resources. |
| `app/src/main/res/values/themes.xml` | Base Android window and splash theme definitions. |
| `app/src/main/res/values-night/themes.xml` | Android theme overrides for dark mode. |
| `app/src/main/res/drawable/` | Vector artwork for the launcher background, foreground, and reminder icon. |
| `app/src/main/res/mipmap-*/` | Launcher icons at Android's supported screen densities. |

### Tests

| File | Purpose |
| --- | --- |
| `app/src/test/java/com/rhinos/app/CalculatorTests.kt` | Unit tests for calculation and FIFO behavior. |
| `app/src/test/java/com/rhinos/app/ReportAndBackupTests.kt` | Unit tests for reports and backup behavior. |
| `app/src/androidTest/java/com/rhinos/app/UiSmokeTest.kt` | Instrumented UI smoke test; requires an Android device or emulator. |

### Build and project files

| File | Purpose |
| --- | --- |
| `settings.gradle.kts` | Names the Gradle project and includes the `app` module. |
| `build.gradle.kts` | Declares shared Gradle plugin versions. |
| `gradle.properties` | Project-wide Gradle and Android build settings. |
| `gradle/wrapper/gradle-wrapper.properties` | Selects the Gradle version used by the wrapper. |
| `gradlew` / `gradlew.bat` | Gradle wrapper launchers for macOS/Linux and Windows. |
| `app/build.gradle.kts` | Android app configuration, SDK levels, and dependencies. |
| `app/proguard-rules.pro` | App-specific R8/ProGuard rules for release builds. |
| `.gitignore` | Excludes local build output, SDK paths, and signing files from Git. |
| `RHINOS_PROJECT_STATUS.md` | Project development/status notes. |
| `releases/Rhinos-1.0.0-debug.apk` | A debug APK snapshot for trying the app; it is not a Play Store release. |

## How trade matching works

Rhinos uses FIFO (first in, first out) to calculate realized trade P&L:

1. Each buy creates a lot with its share of the buy charges included in cost.
2. A sell consumes the oldest available lots for the same ticker first.
3. Sell charges are reflected in the net sale value.
4. A partial sell leaves the unused portion of a lot open.
5. A sell larger than the available position is rejected.

For example, buying 500 units at RM10.00 and 500 at RM10.50, then selling 700
at RM12.00, matches the sale to 500 units from the first lot and 200 from the
second. The remaining 300 units stay open from the second lot.

## Expiry reminders

- T+3 and T+10 dates use calendar days; they are personal reminders, not an
  official Bursa Malaysia settlement calculation.
- When enabled, Rhinos schedules a one-shot reminder for 09:00 on the day before
  expiry.
- Reminders are checked against the database before they are shown and are
  rescheduled after a device restart.
- Permanent trades do not get expiry reminders. Expiry tips can be turned off
  in Settings.

## Build and run

### Requirements

- Android Studio or Android SDK command-line tools
- JDK 21 (the app targets Java 17 bytecode)
- Android SDK Platform 35 and Build Tools 35.0.0

The Gradle wrapper downloads Gradle 8.10.2 when first run. Android Studio can
install the required SDK components. To install them with the command-line
tools:

```shell
sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

### Build a debug APK

From the project root:

```shell
# Windows
gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

The output is `app/build/outputs/apk/debug/app-debug.apk`. Open the project in
Android Studio and use **Run** to install it on a connected device or emulator.

### Run tests

```shell
# Unit tests
gradlew.bat testDebugUnitTest

# UI smoke test (requires a device or emulator)
gradlew.bat connectedDebugAndroidTest
```

Use `./gradlew` instead of `gradlew.bat` on macOS and Linux.

## Data and privacy

Trade records, notes, and daily P&L are stored locally in the Room database
inside Android's private app storage. Rhinos has no account or cloud service.
JSON backups are created only when you choose to export them. Reinstalling the
app or clearing its storage can remove local records, so keep a backup if you
need one.

## License

No license file is currently included. Making this repository public lets
people view its source; it does not by itself grant permission to reuse or
redistribute the code.
