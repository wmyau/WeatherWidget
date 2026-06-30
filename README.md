# Weather Widget

A clean, dark-themed Android weather app and home screen widget. Shows a 7-day forecast in a compact 7×1 widget, with a full detail view in the app. Built without Google Play Services — works on Huawei, AOSP, and standard Android devices.

## Features

- **7-day forecast widget** — day name, weather emoji, high/low temps across a single home screen row
- **Detailed app view** — full 7-day forecast cards with precipitation, wind speed, and UV index
- **Live clock** — widget header updates the time in real time without any manual refresh
- **Manual city search** — search and pin any city worldwide; no GPS required
- **Phone location fallback** — uses network-based location when GPS is unavailable or denied
- **Offline resilience** — last successful forecast cached locally; app and widget stay populated when the network is down
- **Auto-refresh** — background update every hour via WorkManager
- **No Google Play Services required** — works on Huawei EMUI, GrapheneOS, and any AOSP device
- **Fully accessible** — content descriptions on all interactive and informational elements

## Screenshots

| App — forecast view | Widget — 7×1 row |
|---|---|
| *(add screenshot)* | *(add screenshot)* |

## Architecture

```
app/
├── MainActivity.kt          # UI only — observes ViewModel state, dispatches events
├── WeatherViewModel.kt      # All business logic — location, fetch, cache, widget push
├── WeatherUiState.kt        # Sealed UI state interfaces
├── DayForecastAdapter.kt    # RecyclerView adapter for 7-day cards
├── WeatherWidgetProvider.kt # AppWidgetProvider — schedules WorkManager, builds RemoteViews
├── data/
│   ├── WeatherRepository.kt # HTTP + JSON parsing (kotlinx.serialization)
│   ├── WeatherModels.kt     # @Serializable data classes
│   ├── ForecastCache.kt     # SharedPreferences JSON cache for offline use
│   └── HttpClient.kt        # Shared OkHttpClient + Json singletons
├── utils/
│   ├── LocationHelper.kt    # GPS / network location, city search, Nominatim reverse geocoding
│   └── WeatherIconMapper.kt # WMO weather code → emoji + description
└── worker/
    └── WeatherUpdateWorker.kt # CoroutineWorker — runs hourly in background
```

**Pattern:** MVVM with `AndroidViewModel`, `StateFlow`, and `repeatOnLifecycle` for lifecycle-safe collection.

## Data Sources

| Source | Used for | Key required |
|---|---|---|
| [Open-Meteo](https://open-meteo.com) | 7-day weather forecast | None |
| [Open-Meteo Geocoding](https://open-meteo.com/en/docs/geocoding-api) | City search | None |
| [Nominatim (OpenStreetMap)](https://nominatim.org) | Reverse geocoding (lat/lon → city name) | None |

All APIs are free and keyless.

## Tech Stack

| Library | Version | Purpose |
|---|---|---|
| Kotlin | 1.9.22 | Language |
| Android Gradle Plugin | 8.2.2 | Build tooling |
| AndroidX AppCompat | 1.6.1 | Activity base |
| Material3 | 1.11.0 | UI components and theming |
| ViewModel + Lifecycle | 2.7.0 | MVVM architecture |
| WorkManager | 2.9.0 | Background hourly refresh |
| OkHttp | 4.12.0 | HTTP client |
| kotlinx.serialization | 1.6.3 | JSON parsing (compile-time, no reflection) |
| Coroutines | 1.7.3 | Async/await for network and location |

## Build Requirements

- Android Studio Hedgehog or newer
- JDK 11+
- `minSdk 26` (Android 8.0)
- `targetSdk 34` (Android 14)
- No API keys or `google-services.json` needed

```bash
git clone https://github.com/wmyau/WeatherWidget.git
cd WeatherWidget
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Permissions

| Permission | Why |
|---|---|
| `INTERNET` | Fetch weather and geocoding data |
| `ACCESS_COARSE_LOCATION` | Determine phone's approximate location for automatic weather |
| `RECEIVE_BOOT_COMPLETED` | Re-schedule WorkManager after device reboot |
| `SET_ALARM` | Tapping the widget clock opens the system alarm app |

Location permission is optional — you can search and pin a city manually without granting it.

## Huawei / No-GMS Notes

This app was developed and tested on a Huawei device without Google Mobile Services. Several constraints shaped the implementation and are worth knowing if you fork or extend it:

- **Flat widget layout** — Huawei's launcher inflater crashes on nested `LinearLayout > LinearLayout > View` in RemoteViews. The widget uses a maximum of one level of nesting.
- **No GPS in background** — Huawei's AppOps blocks background GPS access. Only `NETWORK_PROVIDER` and `PASSIVE_PROVIDER` are used; `ACCESS_FINE_LOCATION` is not declared.
- **Nominatim reverse geocoding fallback** — Android's `Geocoder` has no backend without Google Play Services. A Nominatim HTTP call is used as fallback when `Geocoder.isPresent()` returns false.
- **WorkManager one-time update omits network constraint** — Huawei's WorkManager implementation holds back jobs aggressively when constraints are set, preventing the initial fetch from ever running.

## License

MIT — see [LICENSE](LICENSE).
