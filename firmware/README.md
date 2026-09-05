# ESP32 firmware (Smart Dispenser)

Arduino sketch: `esp32/smart_dispenser/smart_dispenser.ino`

## What it does

- Turns the pump GPIO **off** before Wi-Fi starts.
- Serves the HTTP API used by the existing Android app.
- Starts a pour from `POST /dispense` and **returns immediately** (HTTP 202).
- Stops on `POST /stop`, at the duration cap, or if Wi-Fi drops while pouring.
- Clamps duration to 1–180 seconds.

## Setup

1. Install Arduino IDE (or Arduino CLI) with **esp32** board support.
2. Open `esp32/smart_dispenser/smart_dispenser.ino`.
3. Set `WIFI_SSID` and `WIFI_PASSWORD`.
4. Set `PUMP_GPIO` to the pin that drives the optocoupler.
5. Set `PUMP_ACTIVE_HIGH` to match wiring (`true` = HIGH means ON).
6. Flash, then copy the serial IP into the app Settings field (`http://x.x.x.x`).

API details: `docs/http-api.md`.
