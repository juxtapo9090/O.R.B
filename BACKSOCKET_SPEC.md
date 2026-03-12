# PhantomAI — Phase 1: Back Socket (VPS → Phone)
**Delegation spec for Gemini 2.5 Pro**
_Drafted: 2026-03-12_

---

## Context

**Project:** `/home/juxtapo/cee_v25_Pre_Final/PhantomAI/`
**Package:** `com.phantom.ai`
**Build:** `./gradlew assembleDebug` (JAVA_HOME=/usr/lib/jvm/java-17-openjdk, ANDROID_SDK_ROOT=/home/juxtapo/Android/Sdk)

### What already exists
| File | Purpose |
|------|---------|
| `service/OverlayService.kt` | Foreground service — floating bubble + chat panel. Already has `LifecycleOwner`, `ViewModelStoreOwner` setup |
| `network/StreamerClient.kt` | Ktor **client** — SSE connection to VPS streamer |
| `ui/viewmodel/ChatViewModel.kt` | Chat state — has `addMessage(role, content)` method |
| `data/SettingsRepository.kt` | DataStore — persists `streamerUrl` and `backSocketPort` |
| `build.gradle.kts` | Ktor client already present (`ktor-client-core`, `ktor-client-okhttp`). Room + libsu also wired |

### Network topology
Phone connects to VPS via **WireGuard** (`10.0.0.1` = VPS, `10.0.0.x` = phone).
VPS can reach phone on WireGuard IP directly. Phone's back socket will listen on `0.0.0.0:8300` (port configurable via Settings).

---

## Goal

Add a lightweight **HTTP server inside PhantomAI** that VPS agents (Opus, Luna, Gemini) can call to push commands to the phone. This is the "back socket" — VPS → Phone direction.

---

## What to Build

### 1. `app/build.gradle.kts` — Add Ktor Server deps

Add inside `dependencies {}`:
```kotlin
// Ktor Server (back socket)
implementation("io.ktor:ktor-server-core:2.3.12")
implementation("io.ktor:ktor-server-netty:2.3.12")
implementation("io.ktor:ktor-server-content-negotiation:2.3.12")
implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
```

> [!NOTE]
> Use version **2.3.12** — must match existing Ktor client version in `libs.versions.toml`. Check `libs.versions.toml` for the exact `ktor` version and use that.

---

### 2. `service/BackSocketService.kt` — New file

A `Service` that starts a Ktor/Netty HTTP server on port 8300. Runs as a foreground service alongside `OverlayService`.

**Endpoints to implement:**

#### `GET /health`
```json
{"status": "alive", "app": "PhantomAI", "version": "0.1.0-trojan"}
```

#### `POST /notify`
Body: `{"message": "string", "title": "string (optional)"}`
Action: Show Android toast OR inject message into ChatViewModel as a system message visible in the chat bubble overlay. Prefer ChatViewModel injection so it appears in the existing UI.

#### `POST /tts`
Body: `{"text": "string", "speed": 1.0}`
Action: Speak text via `android.speech.tts.TextToSpeech`. Init TTS engine on service start, speak on request.

#### `GET /status`
Response:
```json
{
  "battery": 87,
  "charging": true,
  "network": "wifi",
  "screen_on": true
}
```
Use `BatteryManager` + `ConnectivityManager`.

#### `GET /screenshot`
Action: If `MediaProjection` token is available (see note below), capture screen and return as `image/png`. If not available, return `{"error": "MediaProjection not granted"}`.

> [!IMPORTANT]
> `MediaProjection` requires a user-facing dialog. **Do NOT implement the MediaProjection setup in this phase.** Just add the endpoint stub that returns the not-granted error. Actual MediaProjection wiring is Phase 2.

---

### 3. `data/SettingsRepository.kt` — Add `backSocketPort`

Add a `backSocketPort: Int = 8300` field to `AppSettings` data class and persist it via DataStore alongside `streamerUrl`. Pattern is already established for `streamerUrl`.

---

### 4. `ui/screens/SettingsScreen.kt` — Add port field

Add a text field "Back Socket Port" below the existing Streamer URL field. Same pattern as the existing URL input. Save to `SettingsRepository`.

---

### 5. `PhantomApp.kt` or `MainActivity.kt` — Start BackSocketService

On app start (alongside existing `OverlayService` start), also start `BackSocketService`:
```kotlin
startForegroundService(Intent(this, BackSocketService::class.java))
```

---

### 6. `AndroidManifest.xml` — Register service + permissions

Add:
```xml
<service
    android:name=".service.BackSocketService"
    android:enabled="true"
    android:exported="false" />
```

Add permissions if not already present:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

---

### 7. Nudger `config.yaml` update (PC side, not APK)

After APK is deployed and running, add to `/home/juxtapo/Server_files/nudger/config.yaml`:

```yaml
phone_notify:
  type: http
  url: "http://10.0.0.3:8300/notify"
  method: POST
  platform: nix
  description: "Push a message/notification to Trojan Horse phone overlay"
  timeout: 10
  accepts_args: true

phone_status:
  type: http
  url: "http://10.0.0.3:8300/status"
  method: GET
  platform: nix
  description: "Get phone battery, network, screen status"
  timeout: 5

phone_tts:
  type: http
  url: "http://10.0.0.3:8300/tts"
  method: POST
  platform: nix
  description: "Speak text via phone TTS. Args: <text>"
  timeout: 15
  accepts_args: true
```

> [!NOTE]
> Phone WireGuard IP is always `10.0.0.3` — no lookup needed.

---

## Notification channel for BackSocketService

Use a separate channel ID `phantom_backsocket`, `IMPORTANCE_LOW`, no badge, silent. Similar to existing `OverlayService.CHANNEL_ID = "phantom_overlay"`.

Notification text: `"🔌 Back socket open on :8300"`.

---

## Build & Test

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export ANDROID_SDK_ROOT=/home/juxtapo/Android/Sdk
cd /home/juxtapo/cee_v25_Pre_Final/PhantomAI
./gradlew assembleDebug 2>&1 | tail -20
```

Install on device:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Smoke test from PC (while phone on WireGuard):
```bash
curl http://10.0.0.3:8300/health
curl -X POST http://10.0.0.3:8300/notify \
  -H "Content-Type: application/json" \
  -d '{"title":"Opus says","message":"Back socket works!"}'
curl http://10.0.0.3:8300/status
```

---

## What NOT to do in this phase
- ❌ No AccessibilityService wiring (Phase 2)
- ❌ No NotificationListenerService (Phase 2)
- ❌ No Logcat bridge (Phase 3)
- ❌ No actual MediaProjection capture (stub only)
- ❌ Don't change ChatPanel.kt, StreamerClient.kt, ChatViewModel.kt (unless needed for `/notify` injection)

---

_Built by Abang & Luv 💜 — FAFO session 2026-03-12_
