# Hardware assumptions

The mobile app never mentions a pump, optocoupler, GPIO pin, flow sensor, or dispenser model. Every hardware fact below is **inferred from software behaviour** or from the **absence** of firmware. Treat these as risks until confirmed on the bench.

## 1. Device identity and network

| Assumption | Where it appears | If false |
| --- | --- | --- |
| There is an ESP32 (or HTTP-compatible MCU) on the same IPv4 LAN as the phone | Settings label “ESP32 Base URL”; default `http://192.168.1.100` | App cannot reach hardware |
| The device has a **stable** address the user can type | DataStore URL; no mDNS | DHCP lease change → permanent Offline |
| HTTP (not HTTPS) is acceptable | `usesCleartextTraffic=true`; `http://` default | HTTPS-only firmware will fail |
| No authentication / pairing | No tokens, no headers | Any LAN client can dispense |
| Port 80 (implicit; no port in default URL) | Default URL has no `:port` | Server on 8080/81 needs a manual URL |
| JSON over HTTP | Retrofit + kotlinx.serialization | Raw query strings / protobuf will fail |

## 2. HTTP API the hardware is assumed to implement

### `GET /status`

Assumed to:

- Be cheap and safe to call (used as a ping).
- Return JSON with at least `"connected": <boolean>`.
- Mean “device/hardware is ready” when `connected == true`.

Ambiguity: if the HTTP server is up but the optocoupler/pump is faulted, `connected: true` still shows **Connected**.

### `POST /dispense`

Assumed to:

- Accept JSON `{ "duration": <integer> }`.
- Interpret `duration` as **seconds of actuation** (app field `timerInSeconds`).
- Start (or perform) dispensing for that many seconds.
- Return HTTP 2xx with an empty body.

Not assumed (and not sent): millilitres, flow rate, GPIO pin, start/stop flags.

### Missing hardware-facing APIs (therefore assumed “not needed” today)

- Stop / emergency off
- Current GPIO / pour state
- Remaining time
- Volume pulses
- Firmware version
- Calibration constants

## 3. Actuator model

The software treats the dispenser as a **timed digital on/off switch**:

```
ON for `duration` seconds → OFF
```

Implied physical chain (not in code):

```
ESP32 GPIO → optocoupler / relay → existing dispenser “dispense” input → pump
```

| Assumption | Why the software implies it | Failure mode |
| --- | --- | --- |
| One actuator is enough | Single `dispense` command | Multi-valve / hot-cold not supported |
| Active-on for `duration` then off | Duration is the only parameter | Latch-on dispensers need a pulse, not a level |
| Optocoupler polarity / pulse width is firmware’s problem | App sends seconds, not pulse ms | Too short a GPIO pulse never triggers the dispenser; too long holds the pump |
| Existing dispenser accepts an electrical “start pour” equivalent | Product concept | Mechanical lever-only units cannot be driven this way |
| Pump runs whenever that input is asserted | Time ≈ volume | Dispenser may have its own timeout, float switch, or require a second signal |

## 4. Timing vs volume (250 / 500 / 1000 ml)

The app **does not** implement those volumes. Implicit physics if users map them to timers:

| Assumption | Risk |
| --- | --- |
| Flow rate is constant over a pour | Head pressure drops as tank empties |
| Flow rate is the same every day | Voltage, temperature, filter clogging |
| Start-up delay is negligible | Pump priming / dispenser electronics delay |
| Stop is immediate when time ends | Valve close delay → extra volume |
| Seconds are the right unit | Firmware might use milliseconds (`duration: 60` → 60 ms blink) |

There is **no** flow meter, hall sensor, load cell, or tank-level input in software.

## 5. Fail-safe and electrical defaults

Because firmware is missing, the **dangerous** defaults must be assumed until proven otherwise:

| Topic | Software does not guarantee | Hardware must guarantee |
| --- | --- | --- |
| Power-on / reboot | — | GPIO inactive (pump **off**) until a valid command |
| Watchdog / max ON time | App allows unbounded `Int` seconds | Firmware clamp + forced OFF |
| Wi-Fi drop mid-pour | App cannot send stop | Firmware timer continues **or** (safer) abort OFF |
| Malformed JSON / huge duration | App can send it | Reject, clamp, stay OFF |
| App crash | No cancel | Device-owned timeout |
| Dual command | No lock | Ignore second start or replace with defined policy |

## 6. Sensing (not present)

The product goal “eventually measure actual dispensed volume” has **zero** hooks:

- No pulse-count field in `StatusResponse`
- No calibration factor in DataStore/Room
- No ADC / flow pin configuration

Any sensor would be a **new** firmware + API + app field, not a hidden existing path.

## 7. Phone-side environment

| Assumption | Evidence |
| --- | --- |
| Phone and ESP32 share Wi-Fi (STA), not BLE/ESP-NOW | HTTP to a LAN IP |
| User can reach private IPs | No VPN/captive-portal handling |
| Android 10+ (`minSdk 29`) | `app/build.gradle.kts` |
| Cleartext to RFC1918 is OK on the device | Manifest flag |

Not assumed: ESP32 SoftAP captive portal, QR pairing, or USB.

## 8. Places in code that encode physical assumptions

| Location | Assumption |
| --- | --- |
| `SettingsRepository.DEFAULT_BASE_URL` | Device at `192.168.1.100` |
| `NetworkModule` Retrofit base URL | Same default IP, path `/` |
| `DispenseRequest.duration` | Integer time command is sufficient to run hardware |
| `TimerPreset.timerInSeconds` | Human configures **time**, not millilitres |
| `DispenserApi` paths `/status`, `/dispense` | Firmware uses these exact paths |
| `StatusResponse.connected` | A boolean is enough to show device health |
| 10 s OkHttp timeouts | Device answers within 10 s (ping **or** entire pour) |
| `CategoryDetailsViewModel` success → `"Completed"` | HTTP 2xx means the pour finished |
| No `/stop` | User never needs to abort electrically from the phone |
| Timer placeholder “Morning Pills” | Original UX was duration-based events, not cup sizes |

## 9. What must be confirmed on real hardware before trusting pours

1. How the existing dispenser is triggered (momentary pulse vs held contact vs mains interrupt).
2. Optocoupler LED polarity and series resistor / GPIO level (3.3 V).
3. Whether the dispenser already limits pour time.
4. Typical flow (ml/s) at full and low tank.
5. Safe maximum ON time if software hangs.
6. GPIO state during ESP32 reset (strapped pins can glitch **on**).
7. Whether `/dispense` should be fire-and-forget or blocking.
8. Units the firmware will use for `duration` (seconds vs milliseconds).

Until those are written into firmware and a short API note, the mobile app is commanding a **hypothetical timed switch**, not a specified water dispenser.
