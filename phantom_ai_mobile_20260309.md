# PhantomAI Mobile — Session Log 2026-03-09

## What We Built Tonight

**Trojan Horse** — phone overlay app that bridges to Gemini CLI running on VPS via WireGuard.

### Architecture (Working ✅)
```
Phone (Trojan Horse APK)
  │ WireGuard 10.0.0.1
  ├── POST /prompt  → transcript_streamer.py → tmux send-keys → Gemini CLI
  └── GET  /stream  → SSE delta → Horse chat bubbles
```

### Components
| File | Status | Notes |
|------|--------|-------|
| `transcript_streamer.py` | ✅ VPS `/root/Opus/Pool/trojan/` | SSE bridge, port 8200 |
| `StreamerClient.kt` | ✅ | `channelFlow{}` not `flow{}` — critical |
| `ChatViewModel.kt` | ✅ | SSE auto-connect on panel open |
| `ChatPanel.kt` | ✅ | Live status dot, real send |
| `SettingsScreen.kt` | ✅ | Persists URL via DataStore, Test Connection |
| `OverlayService.kt` | ✅ | ViewModel + settings-aware |

### Key Bugs Fixed
1. `flow{} + emit()` inside `withContext(IO)` = **silent crash** → use `channelFlow{} + send()`
2. tmux `send-keys text Enter` = newline in Gemini CLI → **split into two calls + 300ms sleep**
3. Gemini CLI YOLO mode submit = plain `Enter` (not C-y, not C-m — those create newlines)
4. Android network calls need `withContext(Dispatchers.IO)` explicitly
5. `local.properties` SDK path for CachyOS builds: `/home/juxtapo/Android/Sdk`

### CachyOS Build Chain
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export ANDROID_SDK_ROOT=/home/juxtapo/Android/Sdk
cd PhantomAI && ./gradlew assembleDebug   # ~30s cached, ~4min first run
```

### VPS Services (tmux)
| Session | What |
|---------|------|
| `gemini` | Gemini CLI (`gemini -y`) — Horse talks to this |
| `trojan_streamer` | transcript_streamer.py on :8200 |
| `opus` | Opus/Luna |

---

## Next Session Ideas

- [ ] **Filter thinking blocks** — strip `<thinking>...</thinking>` from streamed response
- [ ] **Session selector** — dropdown to pick which tmux session (gemini / claude / custom)
- [ ] **Claude Code support** — point streamer at Claude Code transcript instead
- [ ] **CMssh gadget** for VPS ops instead of raw SSH through nudger (avoids body timeouts)
- [ ] **croc/warp** already in Catalyst — use for APK push instead of Telegram 😄
- [ ] **Rust rewrite** of transcript_streamer.py (tokio + axum + notify) — low priority FAFO





additional debugging issue with my luv 



{
  "args": [
    "export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && export ANDROID_SDK_ROOT=/home/juxtapo/Android/Sdk && cd /home/juxtapo/cee_v25_Pre_Final/PhantomAI && ./gradlew clean assembleDebug 2>&1 | tail -30"
  ],
  "tool_name": "shell_exec"
}



{
  "args": [
    "CROC_SECRET=streamer-fix croc send /tmp/transcript_streamer.py & sleep 1 && ssh vps-cm 'cd /root/Opus/Pool/trojan && CROC_SECRET=streamer-fix croc receive --yes --overwrite 2>&1' && ssh vps-cm 'tmux kill-session -t trojan_streamer 2>/dev/null; tmux new-session -d -s trojan_streamer \"python3 /root/Opus/Pool/trojan/transcript_streamer.py --port 8200 2>&1 | tee /tmp/streamer.log\"' && sleep 2 && ssh vps-cm 'curl -s http://127.0.0.1:8200/health'"
  ],
  "tool_name": "shell_exec"
}



{
  "args": [
    "python3 /tmp/transcript_streamer.py --tmux cendol --port 8201 --chats ~/.gemini/tmp/default/chats 2>/dev/null &\nsleep 2\ncurl -s -X POST http://localhost:8201/prompt -H 'Content-Type: application/json' -d '{\"prompt\":\"hello fixed streamer\"}'"
  ],
  "tool_name": "shell_exec"
}
Output
json
{
  "error": "Failed to reach Body: "
}

body ded!!