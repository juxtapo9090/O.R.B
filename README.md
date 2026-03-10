# 🐴 Trojan Horse

> An open-source AI overlay for rooted Android — already infiltrated, already watching.

## What Is This?

Trojan Horse is a floating bubble that hovers over any app on your Android phone. Tap it to expand a chat panel connected to AI (Gemini, Claude, or any custom backend). With root access, it can take screenshots, execute shell commands, nudge Termux, and inspect apps underneath.

**It doesn't do all the things. It knows how to ask all the things.**

## Features

- 🐴 **Floating Bubble** — draggable overlay that stays on top of all apps
- 💬 **Chat Panel** — expandable AI chat interface
- 🧠 **Pluggable AI** — Gemini, Anthropic Claude, or custom proxy
- 📷 **Screenshot + Vision** — capture screen and ask AI about it
- 🔧 **Root Shell** — execute commands via SU
- 💻 **Termux Bridge** — nudge Termux for heavy lifting (Python, tmux, etc.)
- 📱 **App Inspector** — see what's running underneath

## Philosophy

```
Trojan Horse = The Conductor, not the Orchestra

"Need terminal?"     → nudges Termux
"Need screenshot?"   → nudges screencap (root)
"Need to check MT5?" → nudges MT5 app
"Need browser?"      → nudges Chrome
```

Lightweight ghost conductor, not full steroid bodybuilder.

## Requirements

- Android 8.0+ (API 26)
- Rooted device (Magisk/KernelSU) for root features
- "Draw over other apps" permission
- Termux (optional, for terminal bridge)

## Build

```bash
./gradlew assembleDebug
```

APK at `app/build/outputs/apk/debug/app-debug.apk`

## Status

🔬 **FAFO Build** — experimental, not for production use.

| Phase | Status |
|-------|--------|
| Phase 0: Skeleton | ✅ |
| Phase 1: Floating Bubble | ✅ |
| Phase 2: AI Brain | 🔜 |
| Phase 3: Root Tools + Termux Bridge | 🔜 |
| Phase 4: Persistence | 🔜 |

## License

MIT — do whatever you want with it. 🐴
