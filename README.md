# Rhinos 🦏

**A simple, private, offline investment journal for Android.**

Rhinos is a personal investment record and analysis tool. It is NOT a brokerage
app, live trading app, or professional financial platform. No ads. No account.
No internet required.

> Simple. Clean. Fast. Private.

---

## Features

- **Record trades** — buy/sell with stock name, ticker, units, price, charges,
  dividend, date, notes, and margin period (T+3 / T+10 / Permanent).
- **Automatic calculations** — total buy cost, net sell value, and realized P&L
  shown live before you save.
- **FIFO engine** — first-in-first-out lot matching with partial sells, multiple
  lots, multiple stocks, and charges spread proportionally. Oversells are
  blocked with a friendly message.
- **Dividends** — recorded manually, tallied monthly/yearly, kept separate from
  trading P&L.
- **Daily P&L journal** — manually record each day's portfolio performance
  (+/- amounts). Today / this week / this month / total summaries.
- **Reports** — monthly and yearly conclusions with realized P&L, daily P&L,
  dividends, trade counts, winning/losing trades and win rate. Filter by this
  month, previous month, this year, previous year, or all.
- **Margin / Expiry** — automatic expiry date from trade date (calendar days),
  clear status labels (Active / Expires in X days / Expires Tomorrow / Expires
  Today / Expired), and a gentle notification one day before expiry at 09:00.
- **Notes & Strategies** — private notebook with Strategy / Trade Idea / Lesson /
  General categories, search, edit, delete.
- **Data safety** — JSON export/import via the system file picker, confirmation
  before any deletion, everything stored locally in a Room database.
- **Dark mode** — System / Light / Dark.
- **Currency** — RM default; USD / EUR / SGD selectable.

## Technology

- Kotlin 2.0, Jetpack Compose, Material 3
- Room 2.6 local database (offline, no server)
- MVVM-ish separation: `data/` (entities, DAOs, repositories), `calculator/`
  (pure business logic), `notifications/` (AlarmManager scheduling), `ui/`
  (Compose screens per feature)
- AlarmManager exact alarms for one-shot expiry reminders (rescheduled on boot)
- kotlinx.serialization for JSON backup
- minSdk 26 (Android 8.0), targetSdk 35 (Android 15)

## Project structure

```
app/src/main/java/com/rhinos/app/
├── MainActivity.kt            # single Activity hosting Compose
├── RhinosApp.kt               # Application + manual DI container
├── calculator/
│   ├── FifoEngine.kt          # FIFO lot matching (pure, unit-tested)
│   └── Calculators.kt         # buy/sell math, expiry, stats, reports
├── data/
│   ├── model/Models.kt        # Trade, DailyPnl, Note + enums
│   ├── db/                    # Room database, DAOs, converters
│   └── repository/            # single write path, validation
├── export/BackupManager.kt    # JSON export/import
├── notifications/             # AlarmManager scheduling + receivers
├── settings/                  # SharedPreferences-backed settings
└── ui/
    ├── navigation/            # bottom bar, NavHost, onboarding
    ├── components/            # shared cards, formatting, empty states
    └── home/ trades/ daily/ reports/ notes/ margin/ more/ settings/
```

## Build prerequisites

The JDK and Android SDK are not stored in this repository. Install JDK 21 and
Android SDK Platform 35 with Build Tools 35.0.0. Android Studio can install the
SDK components when you open the project. Alternatively, install the Android
SDK command-line tools from the [official Android Studio downloads](https://developer.android.com/studio)
and run:

```bash
sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

Install JDK 21 from a trusted distribution such as
[Eclipse Temurin](https://adoptium.net/temurin/releases/?version=21). Set
`JAVA_HOME` to the JDK installation and configure the Android SDK in Android
Studio or in your local, untracked `local.properties` file. Do not commit
machine-specific SDK paths.

The Gradle wrapper downloads Gradle 8.10.2 on first use.

## How to build

```bash
./gradlew assembleDebug
# On Windows:
gradlew.bat assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

Open the folder in Android Studio and press Run, or use the wrapper from a
terminal after installing the prerequisites above:

```bash
./gradlew assembleDebug
```

### Release APK

```bash
# 1. Create a keystore once:
keytool -genkeypair -v -keystore rhinos-release.keystore -alias rhinos \
  -keyalg RSA -keysize 2048 -validity 10000

# 2. Add to gradle.properties (not committed):
#    RHINOS_STORE_FILE=../rhinos-release.keystore
#    RHINOS_STORE_PASSWORD=...
#    RHINOS_KEY_ALIAS=rhinos
#    RHINOS_KEY_PASSWORD=...

# 3. Build:
./gradlew assembleRelease
```

Then read `app/build/outputs/apk/release/app-release-unsigned.apk` → sign with
`apksigner` (or configure signingConfig as above so it is signed automatically).
Alternatively use Android Studio → Build → Generate Signed Bundle / APK.

## How to install the APK

1. Copy `app-debug.apk` to your phone.
2. Open it and allow "Install unknown apps" for your file manager when prompted
   (debug APKs are not from the Play Store).
3. Open Rhinos. Tap **Get Started**. You are on the Home screen.

## How to run tests

```bash
./gradlew testDebugUnitTest          # FIFO, calculators, reports, backup
./gradlew connectedDebugAndroidTest  # UI smoke tests (device/emulator needed)
```

## How data is stored

Everything lives on the device in a Room database (`rhinos.db`, private app
storage). No account, no cloud, no analytics, no permissions beyond
notifications (only if you enable expiry tips). Dates are stored as ISO-8601
strings so the database and backups stay human-readable.

## How expiry reminders work

- When you save a trade with T+3 / T+10, the expiry date is computed with
  plain calendar-day arithmetic (your personal reminder system — not an official
  Bursa Malaysia settlement calculation).
- A single exact alarm is scheduled for 09:00 the day **before** expiry.
- The alarm re-checks the database before notifying, so edited/deleted trades
  never produce stale notifications. Only one reminder is ever sent per expiry.
- Permanent trades never schedule anything.
- Alarms are re-registered after device reboot. Turning off Expiry Tips in
  Settings cancels everything.

## How FIFO P&L works

1. Every BUY opens a "lot" with a per-unit cost that includes its share of the
   lot's charges.
2. Every SELL consumes the oldest open lots first, in trade-date order.
3. Realized P&L = net sale value (units × price − charges) − matched buy cost.
4. Partial sells leave the remainder of the lot open; each ticker is matched
   completely independently.
5. Selling more than you hold is rejected: "Not enough units available to sell."

Example: BUY 500 @ RM10.00, BUY 500 @ RM10.50, SELL 700 @ RM12.00
→ matches 500 from lot 1 (+RM1000) and 200 from lot 2 (+RM300) = **+RM1300**,
with 300 units still open at RM10.50.

## Future improvement ideas

- CSV export in addition to JSON
- Optional password/biometric lock
- Charts (monthly P&L bars, dividend timeline)
- Exchange-specific settlement calendars for T+3/T+10
- Multiple portfolios per stock exchange
- Home-screen widget with today's P&L

---

*Version 1.0.0 · Package `com.rhinos.app` · Built with love and one golden rhino.*
