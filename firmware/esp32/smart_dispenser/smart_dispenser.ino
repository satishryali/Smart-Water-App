#include <WiFi.h>
#include <WebServer.h>

// --- User config ---
const char* WIFI_SSID = "YOUR_WIFI_SSID";
const char* WIFI_PASSWORD = "YOUR_WIFI_PASSWORD";

// GPIO that drives the optocoupler LED (through a resistor).
// Confirm polarity on the bench. Default: HIGH = pump/dispenser ON.
const int PUMP_GPIO = 4;
const bool PUMP_ACTIVE_HIGH = true;

const uint32_t MIN_DURATION_MS = 1000UL;
const uint32_t MAX_DURATION_MS = 180000UL;  // 180 s safety cap, matches the app

WebServer server(80);

enum DispenserState {
  STATE_IDLE,
  STATE_DISPENSING,
  STATE_STOPPED,
  STATE_COMPLETED,
  STATE_ERROR
};

DispenserState deviceState = STATE_IDLE;
uint32_t pourEndMs = 0;
bool pumpOn = false;

const char* stateName(DispenserState state) {
  switch (state) {
    case STATE_DISPENSING: return "DISPENSING";
    case STATE_STOPPED: return "STOPPED";
    case STATE_COMPLETED: return "COMPLETED";
    case STATE_ERROR: return "ERROR";
    case STATE_IDLE:
    default: return "IDLE";
  }
}

void setPump(bool on) {
  pumpOn = on;
  digitalWrite(PUMP_GPIO, (on == PUMP_ACTIVE_HIGH) ? HIGH : LOW);
}

void abortPour(DispenserState nextState) {
  setPump(false);
  pourEndMs = 0;
  deviceState = nextState;
}

void handleCors() {
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.sendHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  server.sendHeader("Access-Control-Allow-Headers", "Content-Type");
}

int parseDurationSeconds(const String& body) {
  int key = body.indexOf("\"duration\"");
  if (key < 0) {
    return -1;
  }
  int colon = body.indexOf(':', key);
  if (colon < 0) {
    return -1;
  }
  int i = colon + 1;
  while (i < (int)body.length() && (body[i] == ' ' || body[i] == '\t')) {
    i++;
  }
  if (i >= (int)body.length() || body[i] < '0' || body[i] > '9') {
    return -1;
  }
  long value = 0;
  while (i < (int)body.length() && body[i] >= '0' && body[i] <= '9') {
    value = value * 10 + (body[i] - '0');
    if (value > 100000) {
      return -1;
    }
    i++;
  }
  return (int)value;
}

void sendStatus(int code) {
  handleCors();
  uint32_t remainingMs = 0;
  if (deviceState == STATE_DISPENSING && pourEndMs > millis()) {
    remainingMs = pourEndMs - millis();
  }
  String json = "{";
  json += "\"connected\":true,";
  json += "\"state\":\"";
  json += stateName(deviceState);
  json += "\",";
  json += "\"remainingMs\":";
  json += remainingMs;
  json += "}";
  server.send(code, "application/json", json);
}

void handleOptions() {
  handleCors();
  server.send(204);
}

void handleStatus() {
  sendStatus(200);
}

void handleDispense() {
  handleCors();
  if (deviceState == STATE_DISPENSING) {
    server.send(409, "application/json", "{\"error\":\"already_dispensing\"}");
    return;
  }

  String body = server.arg("plain");
  int durationSeconds = parseDurationSeconds(body);
  if (durationSeconds < 0) {
    abortPour(STATE_ERROR);
    server.send(400, "application/json", "{\"error\":\"invalid_duration\"}");
    deviceState = STATE_IDLE;
    return;
  }

  uint32_t durationMs = (uint32_t)durationSeconds * 1000UL;
  if (durationMs < MIN_DURATION_MS) {
    durationMs = MIN_DURATION_MS;
  }
  if (durationMs > MAX_DURATION_MS) {
    durationMs = MAX_DURATION_MS;
  }

  pourEndMs = millis() + durationMs;
  deviceState = STATE_DISPENSING;
  setPump(true);
  server.send(202, "text/plain", "");
}

void handleStop() {
  handleCors();
  abortPour(STATE_STOPPED);
  server.send(200, "text/plain", "");
}

void setup() {
  pinMode(PUMP_GPIO, OUTPUT);
  setPump(false);
  deviceState = STATE_IDLE;

  Serial.begin(115200);
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  uint32_t start = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - start < 20000UL) {
    delay(250);
  }

  server.on("/status", HTTP_GET, handleStatus);
  server.on("/status", HTTP_OPTIONS, handleOptions);
  server.on("/dispense", HTTP_POST, handleDispense);
  server.on("/dispense", HTTP_OPTIONS, handleOptions);
  server.on("/stop", HTTP_POST, handleStop);
  server.on("/stop", HTTP_OPTIONS, handleOptions);
  server.onNotFound([]() {
    handleCors();
    server.send(404, "application/json", "{\"error\":\"not_found\"}");
  });
  server.begin();

  Serial.print("IP: ");
  Serial.println(WiFi.localIP());
}

void loop() {
  server.handleClient();

  if (WiFi.status() != WL_CONNECTED && pumpOn) {
    abortPour(STATE_ERROR);
  }

  if (deviceState == STATE_DISPENSING) {
    if ((int32_t)(millis() - pourEndMs) >= 0) {
      abortPour(STATE_COMPLETED);
    }
  }
}
