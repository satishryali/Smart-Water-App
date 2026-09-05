# Architecture overview

This document describes the **existing** Smart Water Dispenser repository as inspected on 2026-09-05. It does not propose a rewrite.

The repository contains an **Android (Kotlin / Jetpack Compose) mobile app** only. There is **no ESP32 firmware**, no hardware schematic, and no project README.

## Intended physical chain

```
Mobile App
    ↓  HTTP over Wi-Fi (LAN)
ESP32 (expected, not in this repo)
    ↓  GPIO (expected)
Optocoupler / electrical interface (not represented in software)
    ↓
Existing electric water dispenser
    ↓
Pump
```

## What this repository actually implements

```
Jetpack Compose UI
    ↓
ViewModels (Hilt)
    ↓
Repositories
    ├── Room (categories + timer presets)     ← local only
    └── DispenserRepository + Retrofit         ← ESP32 HTTP
            ↓
        GET  /status
        POST /dispense  { "duration": <seconds> }
            ↓
        ESP32 HTTP API (assumed, firmware missing)
            ↓
        Hardware control (missing)
            ↓
        Dispenser / pump (missing)
```

## Mobile application

| Layer | Location | Role |
| --- | --- | --- |
| Entry | `SmartDispenserApplication`, `MainActivity` | Hilt app + Compose host |
| Navigation | `navigation/AppNavHost.kt`, `NavRoutes.kt` | Home → category details → add/edit timer |
| UI | `ui/screens/`, `ui/components/` | Categories, presets, settings dialog, connection indicator |
| State | `viewmodel/` | Connection checks, CRUD, dispense trigger |
| Local data | Room `AppDatabase` (v1) | `categories`, `timer_presets` |
| Settings | DataStore `settings` | ESP32 base URL |
| Network | `DispenserApi`, `DispenserRepository`, `NetworkModule` | Status + dispense |

**Stack (do not replace):** Kotlin 2.0, Compose + Material 3, Navigation Compose, Hilt, Room, DataStore Preferences, Retrofit + OkHttp, Kotlinx Serialization.

**App identity:** `com.smartdispenser`, minSdk 29, target/compile SDK 35, cleartext HTTP allowed.

### Screen flow (as designed)

1. **Home** — list of user-defined categories; connection pill; settings (ESP32 URL).
2. **Category details** — list of timer presets for one category; Dispense button.
3. **Add/edit timer** — name + duration in **seconds**.

Navigation is already wired in `AppNavHost`. Category-details and timer screens exist on disk but are **inconsistent** (see `current-problems.md`).

## Network / API (app contract)

The app is a **thin HTTP client**. It does not discover devices.

| Item | Current value |
| --- | --- |
| Transport | HTTP (not HTTPS), `usesCleartextTraffic=true` |
| Default base URL | `http://192.168.1.100` (hardcoded in settings, Retrofit module, and UI placeholder) |
| URL storage | DataStore key `esp32_base_url`; trim + strip trailing slash |
| Timeouts | OkHttp connect/read/write **10 seconds** |
| Retry | None |
| Auth | None |
| Discovery | None (manual IP/URL only) |

### Endpoints used by the app

**`GET /status`**

- Response model: `{ "connected": boolean }` (`StatusResponse`).
- Used only as a reachability / “device ready” probe.
- Extra JSON keys would be ignored (`ignoreUnknownKeys = true`).

**`POST /dispense`**

- Body: `{ "duration": <int> }` (`DispenseRequest`).
- `duration` is **`TimerPreset.timerInSeconds`**, not millilitres.
- Success = HTTP 2xx. Empty body expected (`Response<Unit>`).
- Failure = non-2xx or exception (timeout, DNS, connection refused).

No `POST /stop`, no volume endpoint, no calibration endpoint, no WebSocket.

`DispenserRepository` **builds a new Retrofit client per call** from the DataStore URL. `NetworkModule` also provides a singleton `Retrofit`/`DispenserApi` pointed at the default IP; **nothing injects those singletons**.

## ESP32

**Not present in this repository.** The app assumes an ESP32 (or compatible board) already runs an HTTP server implementing `/status` and `/dispense`. GPIO, optocoupler polarity, pump wiring, and fail-safe behaviour are undefined in code.

## Hardware control

**Not present.** There is no pin map, no relay/optocoupler driver, no debounce, no watchdog-off-on-boot logic in this repo.

## Dispenser

**Not present.** The software never names a pump, valve, flow sensor, or dispenser model. Control is abstracted as “send a duration and hope the device runs for that many seconds.”

## State management

### Local UI / data state (complete enough to design around)

- Categories and presets live in Room (cascade delete presets when a category is deleted).
- Connection UI state: `ConnectionStatus` = `CHECKING` | `CONNECTED` | `OFFLINE`.
- Dispense UI state: `dispensingPresetId` + snackbar string `"Completed"` or error message.

### Device operational state (not implemented)

There is **no** shared machine for:

`IDLE` | `DISPENSING` | `STOPPED` | `COMPLETED` | `ERROR`

The app infers “dispensing” only while the **HTTP POST is in flight**. When the request returns, it shows **Completed** even if the hardware is still running, failed, or never started.

Connection is checked **once** when Home or Category Details appears. There is no polling of `/status` during a pour.

## Quantity / volume

The product concept (250 ml / 500 ml / 1 L) is **not in the data model**. Quantity is a **user-chosen timer in seconds**. Volume, flow rate, and calibration do not exist.

## Safety (software that exists)

The mobile app:

- Has **no Stop command**.
- Does **not** cap maximum duration (only “≥ 1 second” on the timer form).
- Does **not** cancel hardware work if the app is backgrounded or the HTTP call fails after the device has accepted the command.

ESP32-side safety cannot be verified because firmware is missing.

## Documentation that existed before this analysis

None (no `README`, no firmware notes, no API spec). This `docs/` folder is the first written architecture record.
