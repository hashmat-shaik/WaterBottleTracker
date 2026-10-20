# 💧 Water Bottle Tracker

A simple, offline Android app for tracking your daily water intake. Log water in one tap, watch your progress ring fill up, review your history on a calendar, and add a home screen widget so you can log a drink without even opening the app.

Built with **Kotlin**, **Jetpack Compose**, and **Jetpack Glance**.

---

## ✨ Features

- **Progress ring dashboard**: see how much you've drunk today, your daily goal, the percentage completed, and how much is left.
- **Quick Add chips**: log 250 ml, 500 ml, 750 ml, or 1 L with one tap. The last amount you used stays highlighted.
- **Bottle controls**: a dedicated `+` button adds one bottle (your configured bottle size), and `−` undoes it.
- **History calendar**: a compact view of recent days that expands into a full month view you can page through. Today is highlighted, and tapping any day shows what you consumed, your goal that day, and your progress.
- **Accurate history**: each day stores the goal that was active at the time, so changing your goal later doesn't rewrite the past.
- **Home screen widget** (2×2, resizable): shows a progress ring with a `+` button (adds your last logged amount) and a `−` button (undoes it). The layout adapts between square and wide sizes.
- **Automatic daily reset**: a background worker resets your total at midnight and archives the previous day. The app also handles day rollover if you open it after midnight.
- **Customizable settings**: set your daily goal and bottle size in millilitres. Changes save instantly.
- **Local-only storage**: no account, no network access, no permissions required.

## 🛠 Tech Stack

| Area | Technology |
| --- | --- |
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose (BOM 2024.09.00) + Material 3 |
| Widget | Jetpack Glance 1.1.1 (`glance-appwidget`, `glance-material3`) |
| Persistence | Jetpack DataStore (Preferences) 1.0.0 |
| Background work | WorkManager 2.9.0 (`CoroutineWorker`) |
| Build | Gradle (Kotlin DSL), Android Gradle Plugin 8.13.2 |
| Min / Target / Compile SDK | 26 / 35 / 35 |

## 📁 Project Structure

```
app/src/main/java/com/example/waterbottletracker/
├── MainActivity.kt              # Main screen: dashboard, quick add, bottle controls, settings
├── data/
│   └── WaterRepository.kt       # DataStore access, WaterState, DayRecord, logging/undo/reset logic
├── ui/
│   ├── history/
│   │   └── Historycalender.kt   # Collapsible calendar + day detail dialog
│   └── theme/                   # Color, Theme, Type
├── util/
│   └── Formatvolume.kt          # Formats ml values (e.g. 500ml, 1.5L)
├── widget/
│   └── WaterWidget.kt           # Glance widget, receiver, and +/- action callbacks
└── workers/
    └── MidnightResetWorker.kt   # Daily reset scheduled with WorkManager
```

## ⚙️ How It Works

- **State** lives in a DataStore `Preferences` file (`water_settings`). `WaterRepository` exposes it as Kotlin `Flow`s that the Compose UI and the widget both collect, so they stay in sync.
- **History** is stored as one preference per day (`history_<epochDay>` → `"mlDrank,goalMl"`) and exposed as a list of `DayRecord`s for the calendar.
- **Day rollover** is handled in two places: when a drink is logged after midnight, and by `MidnightResetWorker`, a periodic 24-hour job that archives the finished day and zeroes the counter.
- **Widget actions** (`IncrementWaterAction` / `DecrementWaterAction`) call the same repository functions as the app, then refresh all widget instances.

## 🚀 Getting Started

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (a recent version that supports AGP 8.13.x)
- JDK 11 or newer
- An Android device or emulator running **Android 8.0 (API 26)** or higher

### Build and run

```bash
# Clone the repository
git clone https://github.com/<your-username>/WaterBottleTracker.git
cd WaterBottleTracker
```

1. Open the project in Android Studio and let Gradle sync.
2. Select a device or emulator.
3. Click **Run ▶**, or build from the command line:

```bash
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### Add the widget

1. Long-press an empty area of your home screen.
2. Open **Widgets** and find **WaterBottleTracker**.
3. Drag the 2×2 widget onto your home screen. Tap `+` to log water and `−` to undo.

## 🔧 Configuration

Defaults can be changed in the app's **Settings** section:

| Setting | Default |
| --- | --- |
| Daily goal | 5000 ml |
| Bottle size | 500 ml |

The Quick Add amounts (250 / 500 / 750 / 1000 ml) are defined in `MainActivity.kt` (`QuickAddSection`) if you want to customize them.

## 🗺 Ideas for the Future

- Reminder notifications
- Custom Quick Add amounts
- Weekly and monthly statistics
- Light theme / dynamic color support
- Data export and backup

## 🤝 Contributing

Contributions, issues, and feature requests are welcome. Feel free to open an issue or submit a pull request.

## 📄 License

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
