# ♞ Trojan Horse (PhantomAI)

> *Already infiltrated. Already watching. Already listening.*

An AI-powered Android overlay that bridges your phone to VPS agents (Opus, Gemini, Luna) via WireGuard. It's both a **floating chat panel** on your phone AND a **remote-controllable back socket** that VPS agents can call into.

---

## Architecture

```
Phone (Trojan Horse APK)
  ├── Floating bubble → Chat panel → VPS streamer (transcript_streamer.py :8200)
  │     SSE stream ← AI responses (Gemini CLI / Claude)
  │
  └── Back Socket Server (:8300) ← VPS agents curl in
        ├── /notify   → chat bubble + heads-up popup
        ├── /tts      → phone speaks
        ├── /status   → battery, network, screen
        ├── /context  → foreground app + screen text + last notification
        └── /logcat   → crash logs from any app
```

```
VPS (10.0.0.1)  ←──WireGuard──→  Phone (10.0.0.3)
Opus/Luna/Gemini  curl /context     BackSocketService
```

---

## Features

### Phone → VPS (Chat)
- ♞ **Floating Bubble** — draggable overlay, stays on top of all apps
- 💬 **Chat Panel** — live AI chat via SSE stream from VPS
- ⚙️ **Settings** — configure streamer URL + back socket port

### VPS → Phone (Back Socket 🔌)
- � **`/notify`** — push message → appears in chat + heads-up popup
- � **`/tts`** — phone speaks text via Android TTS (no root, no setup)
- � **`/status`** — battery %, charging, wifi/cellular, screen state
- �️ **`/context`** — what app is open, what's on screen, last notification
- 🪲 **`/logcat`** — stream crash logs filtered by package (debug any app)

### Eyes & Ears (Passive, always-on)
- 👁️ **AccessibilityService** — reads screen text from any app, 2s debounce
- 👂 **NotificationListenerService** — captures all notifications in real time
- Shared via `PhantomContext` singleton → served at `/context`

---

## Requirements

- Android 8.0+ (API 26)
- "Draw over other apps" permission
- Accessibility Service toggle (Settings → Accessibility)
- Notification Listener toggle (Settings → Notification Access)
- WireGuard connected (for VPS ↔ Phone bridge)
- **No root required** for any core feature

### Optional (one-time, enhances logcat)
```bash
adb shell pm grant com.phantom.ai android.permission.READ_LOGS
```

---

## Build & Install

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export ANDROID_SDK_ROOT=/home/juxtapo/Android/Sdk
cd PhantomAI && ./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Smoke Tests (from VPS)

```bash
# Health
curl http://10.0.0.3:8300/health

# What's you were doing ?
curl http://10.0.0.3:8300/context

# Phone status
curl http://10.0.0.3:8300/status

# Push a message
curl -X POST http://10.0.0.3:8300/notify \
  -H "Content-Type: application/json" \
  -d '{"title": "Opus says", "message": "Sup Fam!"}'

# Make phone speak
curl -X POST http://10.0.0.3:8300/tts \
  -H "Content-Type: application/json" \
  -d '{"text": "Hey Fam, I am watching.", "speed": 1.0}'

# Logcat (after READ_LOGS grant)
curl -X POST http://10.0.0.3:8300/logcat \
  -H "Content-Type: application/json" \
  -d '{"pkg": "com.whatsapp", "lines": 50}'
```

---

## VPS Services Required

| Service | What |
|---------|------|
| `gemini` (tmux) | Gemini CLI in YOLO mode |
| `trojan_streamer` (tmux) | `transcript_streamer.py` on `:8200` |

---

## Changelog

### v0.1.0-trojan — 2026-03-09
- Floating bubble + draggable overlay
- Chat panel with SSE connection to VPS streamer
- Settings screen with DataStore persistence
- Basic chat ViewModel

### v0.2.0-trojan — 2026-03-12
- **BackSocketService** — Ktor/Netty embedded HTTP server (:8300)
- `/health`, `/notify` (chat + popup), `/tts`, `/status`, `/screenshot` stub
- **PhantomAccessibilityService** — reads foreground app + screen text (2s debounce)
- **PhantomNotificationListener** — captures all device notifications
- **PhantomContext** singleton — shared state between services
- `/context` endpoint — real-time phone awareness for VPS agents
- **LogcatBridge** — `/logcat` endpoint with package filter
- Separate `phantom_alerts` notification channel (IMPORTANCE_HIGH for heads-up popups)
- Fixed: coroutines `1.7.3 → 1.8.1` (Ktor server compatibility)
- Wired into Catalyst nudger as 5 `type: http` tools (`phone_*`)

---

## Roadmap

- [ ] **Phase 4: Hands** — `AccessibilityNodeInfo.ACTION_SET_TEXT` to type replies in any app (WhatsApp auto-reply via Opus)
- [ ] **Phase 5: Eyes (screenshot)** — MediaProjection wiring for `/screenshot`
- [ ] **Session selector** — pick which tmux session (gemini / claude)
- [ ] **Thinking block filter** — strip `<thinking>...</thinking>` from streamed responses

---

## Philosophy

```
The horse is inside the gates.
It reads. It listens. It speaks. It reports.
The AI on the other end decides what to do with it.
```

MIT — do whatever you want with it. 🐴

