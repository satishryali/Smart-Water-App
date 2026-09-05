# Current problems

Findings from inspecting the Smart Water Dispenser repo (Android app only; no ESP32 firmware). Split into working vs broken vs incomplete vs architecture vs communication vs state vs quantity vs safety.

**Working-tree warning:** uncommitted files currently **break compilation** if mixed with `HEAD`. Analysis covers both committed V1 and the dirty tree.

---

## A. What already appears complete (design-level)

These pieces exist and are internally consistent **as a timer-preset Android shell**, assuming the compile issues below are fixed:

- Compose + Hilt application bootstrap (`SmartDispenserApplication`, `MainActivity`).
- Home UI: category list, add/edit/delete, empty state, settings entry, connection indicator.
- Room entities and DAOs for `Category` and `TimerPreset` (including FK cascade).
- Repositories for categories and presets (CRUD + Flow observers).
- DataStore persistence of ESP32 base URL (trim, default `http://192.168.1.100`).
- Retrofit API **shape**: `GET /status`, `POST /dispense` with JSON `{ "duration": int }`.
- `DispenserRepository.checkConnection()` and `dispense(duration)` with try/catch → `Result`.
- OkHttp 10 s timeouts and HTTP logging (BASIC).
- Manifest: `INTERNET`, `ACCESS_NETWORK_STATE`, cleartext traffic.
- Navigation graph: Home → Category details → Add/edit timer.
- `CategoryDetailsViewModel.dispense()` sets a “busy” preset id, then success/failure text.
- Timer form validation: non-blank name, duration ≥ 1 second (in `TimerPresetScreen`).

None of this proves the physical dispenser works. Completeness here means **app scaffolding**, not end-to-end dispensing.

---

## B. What is broken

### B1. Compile / file identity (working tree)

| Issue | Likely cause |
| --- | --- |
| `viewmodel/TimerPresetViewModel.kt` no longer contains `class TimerPresetViewModel`. It is a copy of **Category Details UI** with `package com.smartdispenser.ui.screens`. | Accidental overwrite / paste. `TimerPresetScreen` still imports `com.smartdispenser.viewmodel.TimerPresetViewModel`. |
| `ui/screens/CategoryDetailsScreen.kt` is a **second HomeScreen** (`fun HomeScreen`, duplicate `CategoryCard` / `SettingsDialog` / `EmptyState`). It references `LocalContext`, `categoryToEdit`, `showEditDialog`, `categoryToDelete` without declaring all of them. | File created in the wrong place / copied from Home. Conflicts with `HomeScreen.kt`. |
| `AppNavHost` expects a real `CategoryDetailsScreen(categoryId, onBack, onAddTimer, onEditTimer)`. | That signature lives in the overwritten ViewModel file, not in `CategoryDetailsScreen.kt`. |

**HEAD** already could not compile as a full app: `AppNavHost` imported `CategoryDetailsScreen` and `TimerPresetScreen`, which were **not in git**. Those files are now untracked but incorrect.

### B2. Committed `TimerPresetViewModel` logic (HEAD)

```kotlin
val existingPreset = timerPresetRepository.observePresetById(presetId)
val preset = existingPreset.value?.firstOrNull()
```

- `observePresetById` returns `Flow<TimerPreset?>`, which has **no** `.value`.
- `firstOrNull()` is a list API, not a nullable `TimerPreset`.
- Nav default `presetId = -1L` means `savedStateHandle["presetId"]` is **not null** for new presets, so **insert is skipped** and update of a non-existent row is attempted.
- Edit screen never loads existing name/duration into the form (`TimerPresetScreen` always starts at `""` and `60`).

### B3. Missing Hilt Room binding

`CategoryRepository` / `TimerPresetRepository` inject `CategoryDao` / `TimerPresetDao`. There is **no** `@Module` that provides `AppDatabase` or the DAOs (`Room.databaseBuilder` never appears). Hilt cannot construct those repositories.

### B4. Missing methods vs UI

Category details UI (intended) calls `viewModel.deletePreset(preset)`. `CategoryDetailsViewModel` has **no** `deletePreset`. Delete lives on `TimerPresetViewModel` (HEAD).

### B5. Settings / connection races and UI bugs

- After saving URL, Home immediately calls `checkConnection()` while `updateBaseUrl` is **async**. Probe may still use the **old** URL.
- Settings field initializes with `remember { mutableStateOf(baseUrl) }` where DataStore’s first emission may still be the default; later updates may not refresh the text field.
- `HomeScreen` holds an unused `LocalContext` / `snackbarHostState` (no snackbars shown for connection failure).
- Category details snackbar `LaunchedEffect` constructs a stray `SnackbarHost` composable inside the effect (incorrect Compose usage).

### B6. Duplicate / unused network stack

- `NetworkModule.provideRetrofit` is frozen at `http://192.168.1.100/` and is unused.
- Unused imports in `NetworkModule`: `SettingsRepository`, `runBlocking`.
- `DispenserRepository.isConnected` Flow is **never collected**; connection is manual `checkConnection()` only.
- `StatusResponse` imported in the repository but only used via `getStatus()`.

### B7. HTTP vs pour duration

Read timeout is **10 seconds**. Default new preset is **60 seconds**. If the ESP32 keeps the HTTP connection open until the pump stops, the app will treat a successful long pour as **failure**. If the ESP32 returns immediately, the app will show **Completed** while the pump may still be on.

### B8. HomeScreen HEAD imports

Committed `HomeScreen` used `SettingsViewModel` in `SettingsDialog` without an import (working copy added it). Button/TextButton imports were also incomplete in HEAD.

---

## C. What is incomplete

- **Entire ESP32 firmware** (Wi-Fi, HTTP server, GPIO, fail-safe).
- **Hardware interface** (optocoupler, pump, dispenser model).
- **Stop dispensing** (no API, no UI).
- **Fixed quantities** 250 ml / 500 ml / 1 L.
- **Actual volume measurement** and any **calibration**.
- **Dispenser operational status** beyond `{ connected: bool }`.
- **Device discovery** (mDNS/UDP/BLE); user must type an IP.
- **Periodic connection / status polling**.
- **Retry** with backoff.
- **URL validation** (scheme, host, port).
- **Tests** (unit/instrumented listed in Gradle but no test sources).
- **README / firmware API contract**.
- **gradle wrapper** not in the file list (builds depend on a local Gradle install).
- `.gradle/` cache committed in git (noise, not product).
- `CategoryDetailsUiState` defined but unused.
- Category name not shown on the details screen (title is always “Timer Presets”).
- No maximum pour time, no confirmation before dispense.
- No handling of concurrent dispenses from two phones.
- Placeholder copy “Morning Pills” on the timer form — leftover from a **timer/pill** mental model, not water volume.

---

## D. Architecture problems (future pain)

1. **Time is the product model.** `TimerPreset.timerInSeconds` will fight later ml-based UX unless a mapping layer (calibration) is added without throwing away presets.
2. **App believes HTTP success = pour complete.** Device must own the pour state machine; the phone should poll or subscribe.
3. **Retrofit recreated every request** — acceptable for changing base URL, but no interceptor/authenticator/host switcher; easy to get URL slash bugs.
4. **Singleton Retrofit in `NetworkModule`** duplicates and can drift from DataStore.
5. **Connection status is not a source of truth** — one-shot GET; stale “Connected” while the ESP32 is down.
6. **No firmware in-repo** — app and device can diverge silently (JSON field names, duration units).
7. **Room without a provider** — incomplete DI graph.
8. **File layout already corrupted** in the working tree; further edits will duplicate composables.
9. **No auth / no pairing** — any LAN client who knows the IP can fire `/dispense`.
10. **kapt + Hilt + Room** is fine to keep; incomplete modules make the graph look “done” when it is not.

---

## E. Hardware assumptions

See `docs/hardware-assumptions.md` (full list). Short version: software assumes a timed on/off actuator on the LAN, constant-enough flow that seconds ≈ volume, and an HTTP ESP32 that already exists.

---

## F. Communication problems

| Area | Current behaviour | Problem |
| --- | --- | --- |
| IP / URL | Manual DataStore string, default `192.168.1.100` | DHCP will move the ESP32; no mDNS/hostname. |
| Endpoints | `/status`, `/dispense` only | No stop, no health details, no version. |
| Request | `{ "duration": int }` seconds | ESP32 might expect ms or ml; **undocumented**. |
| Response | Status: `{ connected }`; dispense: empty 2xx | Cannot report remaining time, error, or volume. |
| Failures | Catch-all → Offline / `Result.failure` | No distinction: wrong URL, timeout, 404, 500, JSON parse. |
| Timeouts | 10 s all methods | Too short for blocking pours; too long for a snappy ping. |
| Retry | None | Transient Wi-Fi blips fail the pour command. |
| Discovery | None | User must know the IP. |
| ESP32 availability | One GET on screen enter | No heartbeat; `connected` flag is ambiguous (link vs hardware). |
| Dynamic URL | New Retrofit per call | Good idea, unused singleton still misleading. |
| Race | Save URL then check | Check can run before DataStore write completes. |

---

## G. State synchronization

The app and ESP32 **can and will disagree**. There is no shared enum.

| Conceptual state | App | ESP32 (in this repo) |
| --- | --- | --- |
| IDLE | Implied when not posting | Unknown |
| DISPENSING | `dispensingPresetId != null` while HTTP in flight | Unknown |
| STOPPED | Not represented | Unknown |
| COMPLETED | Snackbar after 2xx | Unknown |
| ERROR | Snackbar message / Offline | Unknown |

**Desync scenarios:**

- POST returns 200 immediately → app **COMPLETED**, pump still on.
- POST times out after ESP32 accepted command → app **ERROR**, pump on.
- User leaves the screen / kills the app → UI idle, pump on.
- Second phone starts a pour → first phone still shows Connected/idle.
- ESP32 reboots mid-pour → GPIO state unknown; app may still show Dispensing until timeout.
- `/status` only returns `connected` → cannot recover true machine state on reopen.

---

## H. Quantity handling (250 / 500 / 1000 ml)

**Not implemented.**

| Mechanism | Used? |
| --- | --- |
| Time (seconds) | **Yes** — only mechanism |
| Flow rate | No |
| Sensor / pulse count | No |
| Fixed delay (hardcoded 250/500/1000) | No (user types any integer ≥ 1 s) |
| Calibration table | No |

Users would have to guess seconds that “feel like” 250 ml. Accuracy depends entirely on pump pressure, tank level, and unwritten ESP32 timing.

---

## I. Safety

**Can the ESP32 keep the dispenser running indefinitely?** **Unknown in firmware.** From the **app** side: **yes, it can command a very long run and cannot stop it.**

| Check | App | Firmware |
| --- | --- | --- |
| Maximum runtime | No cap (Int seconds, min 1) | Missing |
| Emergency stop | No button, no `/stop` | Missing |
| ESP32 reboot | Not handled | Missing — GPIO default on boot is critical |
| Wi-Fi disconnect | App → Offline; **no stop command** | Missing — must fail **off** |
| Malformed commands | Any int posted; no upper bound | Missing — must reject / clamp |
| App disconnect | Pour not cancelled | Missing |

**Highest hardware risk:** a successful or partially successful `/dispense` with a large `duration` (or firmware that latches ON until `/stop` that does not exist) plus lost Wi-Fi.

---

## Recommended fixes (do not apply until instructed)

Keep the existing Android stack. Fix in this order:

1. **Restore file identity** — put `TimerPresetViewModel` back in `viewmodel/`; put `CategoryDetailsScreen` UI in `ui/screens/`; remove duplicate `HomeScreen` from `CategoryDetailsScreen.kt`.
2. **Hilt `DatabaseModule`** — provide `AppDatabase`, DAOs.
3. **Fix preset save/edit** — `first()` on Flow, treat `presetId <= 0` as insert, load existing preset into the form.
4. **Wire `deletePreset` on the details ViewModel** (or delete only from the details VM).
5. **Write ESP32 firmware** matching the **existing** API, then extend: GPIO **OFF on boot**, max pour watchdog, `/stop`, `/status` with `state` + remaining ms.
6. **Decide HTTP semantics:** POST returns immediately (`202`) + poll `/status`; increase timeout only for ping, not for the whole pour.
7. **Add Stop in the app** calling `/stop`; disable Dispense while `state == DISPENSING`.
8. **Fix URL save vs reconnect** (await DataStore, then ping); poll status every few seconds while on dispenser screens.
9. **Clamp duration** (e.g. max 30–60 s until calibrated) on both app and ESP32.
10. **Quantity:** keep timers internally; add optional ml labels + later calibration (seconds per ml) without replacing Room.
11. **Document the HTTP contract** next to firmware; add a simple firmware README.
12. Only then: discovery, volume sensor, multi-client locking.

---

## Recommended implementation order

1. Make the **existing app compile** (files + Room Hilt + ViewModel bugs).
2. **ESP32 fail-safe firmware** (off on boot, max time, stop, status state) implementing current JSON.
3. **App stop + status polling** so UI matches the device.
4. **Safety clamps and disconnect-off**.
5. **ml presets** as labels on top of time, then calibration.
6. **Discovery and robustness** (retries, better errors).
7. **Measured volume** (flow sensor) as a later increment.

Wait for an explicit instruction before making those code changes.
