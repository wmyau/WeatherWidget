# Weather Widget

A 5×2 Android home-screen widget showing a 5-day weather forecast with weather icons, location, date and time.

## Features

- **5-column layout** — one column per day (Today → Day+4)
- **Weather data** — Open-Meteo API (free, no API key required)
- **Icons** — vector drawables styled after Erik Flowers' [weather-icons](https://github.com/erikflowers/weather-icons)
- **Auto-refresh** — every 30 minutes via WorkManager
- **Tap to refresh** — tapping the widget triggers an immediate update

## Widget preview

```
┌──────────────────────────────────────────────────────┐
│ 📍 London                  Mon 30 Jun          14:32 │
│ ─────────────────────────────────────────────────── │
│  TODAY      TUE       WED       THU       FRI        │
│   ☀️        🌤        🌧        ⛅        ☀️         │
│  25°        22°       18°       20°       24°        │
│  15°        14°       11°       13°       16°        │
└──────────────────────────────────────────────────────┘
```

## Setup

### 1. Open in Android Studio

Open the root `WeatherWidget/` folder in Android Studio (Hedgehog or newer). Gradle sync runs automatically.

### 2. Grant location permission

Launch the app once and tap **Grant Location Permission**, or approve the system permission dialog. The widget uses your device's last-known GPS/network location — no account needed.

### 3. Add the widget

Long-press your Android home screen → **Widgets** → scroll to **Weather Widget** → drag it onto the screen. It will occupy a 5×2 cell area.

## Weather icons

The vector drawables in `res/drawable/ic_weather_*.xml` are hand-crafted to match the style of Erik Flowers' weather-icons:

| File | Condition |
|---|---|
| `ic_weather_clear_day` | Clear / mainly clear (WMO 0–1) |
| `ic_weather_partly_cloudy_day` | Partly cloudy (WMO 2) |
| `ic_weather_cloudy` | Overcast (WMO 3) |
| `ic_weather_fog` | Fog / rime fog (WMO 45–48) |
| `ic_weather_drizzle` | Drizzle (WMO 51–57) |
| `ic_weather_rain` | Rain / showers (WMO 61–67, 80–82) |
| `ic_weather_snow` | Snow / snow showers (WMO 71–77, 85–86) |
| `ic_weather_thunderstorm` | Thunderstorm (WMO 95–99) |

## Architecture

```
WeatherWidgetProvider  ← AppWidgetProvider (schedules WorkManager)
WeatherUpdateWorker    ← CoroutineWorker (fetches location + weather)
WeatherRepository      ← OkHttp + Gson → Open-Meteo REST API
LocationHelper         ← LocationManager (no Play Services needed)
WeatherIconMapper      ← WMO code → drawable resource
```

## Permissions

| Permission | Purpose |
|---|---|
| `ACCESS_COARSE_LOCATION` | Resolve nearest city for weather lookup |
| `ACCESS_FINE_LOCATION` | Optional — improves location accuracy |
| `INTERNET` | Fetch forecast from Open-Meteo |
| `RECEIVE_BOOT_COMPLETED` | Re-schedule WorkManager after reboot |
