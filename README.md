# Smart Water Dispenser

Android app plus ESP32 firmware that starts and stops an existing electric water dispenser over Wi-Fi.

```
Mobile app  →  Wi-Fi / HTTP  →  ESP32  →  optocoupler  →  dispenser / pump
```

The phone does **not** measure millilitres. It sends a **duration in seconds**. 250 ml / 500 ml / 1 L buttons convert volume to time using a calibration value you set (seconds per 250 ml).

## Repository layout

| Path | What it is |
| --- | --- |
| `app/` | Kotlin / Jetpack Compose Android app (`com.smartdispenser`) |
| `firmware/esp32/smart_dispenser/` | Arduino sketch for the ESP32 HTTP server |
| `docs/` | Architecture notes, API contract, hardware assumptions |

Minimum Android: **API 29**. Phone and ESP32 must be on the **same Wi-Fi** (emulators often cannot reach a device on your LAN; use a physical phone).

## Run the Android app

1. Clone this repo and open the **project root** in Android Studio.
2. Let Gradle sync (JDK 17).
3. Run on a phone with USB debugging.

On first launch, Settings default to `http://192.168.1.100`. Until that address is a flashed ESP32, the status pill will show **Offline**. You can still create categories and timer presets locally.

### App settings

- **ESP32 Base URL** — `http://<esp32-ip>` (no trailing slash). Copy the IP from the ESP32 serial monitor after Wi-Fi connects.
- **Seconds for 250 ml** — time used for the 250 ml / 500 ml / 1 L buttons. Time a real 250 ml pour and put that number here.

Timer presets still send their own duration (1–180 seconds). **Stop** on the category screen calls `POST /stop`.

## Flash the ESP32

See `firmware/README.md` for the short checklist. In `firmware/esp32/smart_dispenser/smart_dispenser.ino`:

1. Set `WIFI_SSID` and `WIFI_PASSWORD`.
2. Set `PUMP_GPIO` to the pin that drives the optocoupler (default **GPIO 4**).
3. Set `PUMP_ACTIVE_HIGH` to match wiring (`true` = HIGH means ON).
4. Upload with the ESP32 board package in Arduino IDE.
5. Serial Monitor **115200 baud** — note the printed IP.

**First test without water:** LED + resistor on the pump GPIO. Confirm Start, Stop, and reboot leave the output **off**.

Safety built into this sketch:

- GPIO off before Wi-Fi starts
- Pour capped at **180 seconds**
- Wi-Fi drop during a pour turns the pump off
- Invalid `duration` is rejected

## How to test (app + ESP32)

1. Phone and ESP32 on the same Wi-Fi.
2. Browser on the phone: `http://<esp-ip>/status` should return JSON with `"connected":true`.
3. In the app, save that URL in Settings. Status should become **Connected**.
4. Create a category → open it → tap **250 ml** or a short timer preset (for example 3 seconds).
5. Status should show **DISPENSING** and a countdown. **Stop** should cut the GPIO immediately.
6. Letting a pour finish should show **Completed**.

HTTP details: `docs/http-api.md`.

## More documentation

- `docs/architecture.md` — how the app is structured
- `docs/current-problems.md` — issues found in the original tree (some are fixed in later commits)
- `docs/hardware-assumptions.md` — what the software assumes about the physical dispenser
