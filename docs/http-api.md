# HTTP API (app ↔ ESP32)

Base URL is configured in the app (default `http://192.168.1.100`). Paths are absolute.

## `GET /status`

Returns device health and pour state. Polled by the app.

```json
{
  "connected": true,
  "state": "IDLE",
  "remainingMs": 0
}
```

`state` is one of: `IDLE`, `DISPENSING`, `STOPPED`, `COMPLETED`, `ERROR`.

The app still accepts a body that only contains `connected` (older firmware).

## `POST /dispense`

Starts a pour. Body:

```json
{ "duration": 12 }
```

`duration` is **seconds** of actuation. Firmware clamps to 1–180 and returns **202** without waiting for the pour to finish.

`409` if a pour is already running.

## `POST /stop`

Turns the actuator off immediately. Returns 200.

## Safety (firmware)

- GPIO off in `setup()` before Wi-Fi.
- Max pour 180 seconds.
- Wi-Fi disconnect while pouring → pump off, `ERROR`.
- Malformed `duration` → 400, pump stays off.
