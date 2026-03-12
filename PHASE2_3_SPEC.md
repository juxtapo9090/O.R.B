# PhantomAI — Notification Popup Fix + Phase 2 & 3
**Delegation spec for Gemini 2.5 Pro**
_Drafted: 2026-03-12_

---

## 0. QUICK FIX: `/notify` — Add System Popup

**File to modify:** `app/src/main/java/com/phantom/ai/service/BackSocketService.kt`

Currently `/notify` only injects into `ChatEventBus` (chat panel). Add a heads-up system notification too.

**Replace the `/notify` route:**
```kotlin
post("/notify") {
    val req = call.receive<NotifyRequest>()
    val msg = if (req.title.isNullOrBlank()) req.message else "${req.title}: ${req.message}"

    // 1. Chat panel injection (existing)
    ChatEventBus.postMessage(ChatMessage("📞 $msg", isUser = false))

    // 2. System popup notification (NEW)
    val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    val popupNotification = NotificationCompat.Builder(this@BackSocketService, CHANNEL_ID)
        .setContentTitle(req.title ?: "📞 Opus")
        .setContentText(req.message)
        .setSmallIcon(android.R.drawable.stat_sys_warning)
        .setPriority(NotificationCompat.PRIORITY_HIGH)   // ← triggers heads-up popup
        .setDefaults(NotificationCompat.DEFAULT_SOUND)
        .setAutoCancel(true)
        .build()
    notificationManager.notify(System.currentTimeMillis().toInt(), popupNotification)

    call.respond(mapOf("status" to "ok"))
}
```

> [!IMPORTANT]
> `PRIORITY_HIGH` + `DEFAULT_SOUND` = Android heads-up notification (pops from top of screen). Uses existing `CHANNEL_ID = "phantom_backsocket"` — but that channel was created as `IMPORTANCE_LOW`. **Change** `NotificationManager.IMPORTANCE_LOW` → `NotificationManager.IMPORTANCE_HIGH` in `createNotificationChannel()` so popups are actually allowed through.

Also create a **second channel** for popups to avoid polluting the status bar persistent notification channel:
```kotlin
// Add second channel for popup alerts
val alertChannel = NotificationChannel(
    "phantom_alerts",
    "Opus Alerts",
    NotificationManager.IMPORTANCE_HIGH
).apply {
    description = "Popup alerts from VPS agents"
    enableVibration(true)
}
manager?.createNotificationChannel(alertChannel)
```
Then use `"phantom_alerts"` instead of `CHANNEL_ID` in the popup builder.

---

## Phase 2: The Eyes + Ears 👁️👂

### Files to create:
- `service/PhantomAccessibilityService.kt`
- `service/PhantomNotificationListener.kt`
- New back socket endpoint: `GET /context` — returns current screen context

### New endpoint in `BackSocketService.kt`
```
GET /context
→ {"foreground_app": "com.whatsapp", "screen_text": "...", "last_notification": "..."}
```

---

### 2a. `service/PhantomAccessibilityService.kt`

An `AccessibilityService` that:
1. Tracks which app is in foreground (`TYPE_WINDOW_STATE_CHANGED` event)
2. Reads on-screen text (`getRootInActiveWindow()` → walk node tree → collect `text` values)
3. Stores latest state in a companion object (shared with BackSocketService via `PhantomContext`)

```kotlin
// Shared context singleton — use this pattern
object PhantomContext {
    var foregroundApp: String = "unknown"
    var screenText: String = ""
    var lastNotification: String = ""
    var lastNotificationApp: String = ""
}
```

**Key implementation notes:**
- Event filter: `typesMask = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED`
- `TYPE_WINDOW_CONTENT_CHANGED` fires too often — debounce: only update `screenText` max once every **2 seconds**
- Extract text: recurse `AccessibilityNodeInfo` tree, collect non-empty `text` values, join with `\n`, cap at 2000 chars
- Service config in `res/xml/accessibility_service_config.xml`:
```xml
<accessibility-service
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:notificationTimeout="2000"
    android:canRetrieveWindowContent="true"
    android:settingsActivity="com.phantom.ai.MainActivity"/>
```

**AndroidManifest.xml — add:**
```xml
<service
    android:name=".service.PhantomAccessibilityService"
    android:enabled="true"
    android:exported="true"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>
```

**Enable in settings:** Settings → Accessibility → Installed services → PhantomAI → Enable

---

### 2b. `service/PhantomNotificationListener.kt`

A `NotificationListenerService` that:
1. Captures every notification posted on device
2. Stores latest in `PhantomContext.lastNotification` + `PhantomContext.lastNotificationApp`

```kotlin
class PhantomNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        PhantomContext.lastNotification = if (title.isNotBlank()) "$title: $text" else text
        PhantomContext.lastNotificationApp = sbn.packageName
    }
}
```

**AndroidManifest.xml — add:**
```xml
<service
    android:name=".service.PhantomNotificationListener"
    android:exported="true"
    android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
    <intent-filter>
        <action android:name="android.service.notification.NotificationListenerService"/>
    </intent-filter>
</service>
```

**Enable:** Settings → Notification Access → PhantomAI → Allow

---

### 2c. `GET /context` endpoint in `BackSocketService.kt`

Add to routing:
```kotlin
get("/context") {
    call.respond(mapOf(
        "foreground_app" to PhantomContext.foregroundApp,
        "screen_text" to PhantomContext.screenText,
        "last_notification" to PhantomContext.lastNotification,
        "last_notification_app" to PhantomContext.lastNotificationApp
    ))
}
```

---

### 2d. Nudger `config.yaml` additions (PC side)
```yaml
phone_context:
  type: http
  url: "http://10.0.0.3:8300/context"
  method: GET
  platform: nix
  description: "Get what abang is currently doing on phone — foreground app + screen text + last notification"
  timeout: 5
```

---

## Phase 3: Logcat Bridge 🪲

### Prerequisite (one-time from PC)
```bash
adb shell pm grant com.phantom.ai android.permission.READ_LOGS
```

### New endpoint: `POST /logcat`
Body: `{"pkg": "com.someapp", "lines": 100}`
Returns: last N lines of logcat filtered to that package.

### `service/LogcatBridge.kt`
```kotlin
object LogcatBridge {
    fun capture(pkg: String, lines: Int = 100): String {
        return try {
            val process = Runtime.getRuntime().exec(
                arrayOf("logcat", "-d", "-t", lines.toString(), "-s", "*:V")
            )
            val output = process.inputStream.bufferedReader().readText()
            // Filter to package if specified
            if (pkg.isNotBlank()) {
                output.lines()
                    .filter { it.contains(pkg, ignoreCase = true) }
                    .joinToString("\n")
                    .takeLast(8000)  // cap response size
            } else {
                output.takeLast(8000)
            }
        } catch (e: Exception) {
            "logcat error: ${e.message}"
        }
    }
}
```

**Add to BackSocketService routing:**
```kotlin
post("/logcat") {
    val req = call.receive<LogcatRequest>()
    val output = withContext(Dispatchers.IO) {
        LogcatBridge.capture(req.pkg ?: "", req.lines ?: 100)
    }
    call.respond(mapOf("output" to output, "pkg" to (req.pkg ?: "all")))
}
```

**Add data class:**
```kotlin
@Serializable
data class LogcatRequest(val pkg: String? = null, val lines: Int? = null)
```

### Nudger `config.yaml`
```yaml
phone_logcat:
  type: http
  url: "http://10.0.0.3:8300/logcat"
  method: POST
  platform: nix
  description: "Stream logcat from phone. Args: <package_name> [line_count]. Needs READ_LOGS granted via ADB."
  timeout: 15
  accepts_args: true
```

---

## Build Order

1. **Fix `/notify`** → rebuild + install → test popup appears
2. **Phase 2a** (`AccessibilityService`) → enable in settings → test `/context` returns foreground app
3. **Phase 2b** (`NotificationListener`) → enable in settings → test phone notifications appear in `/context`
4. **Phase 3** (Logcat) → grant READ_LOGS via ADB → test `/logcat` with a known crashing app

```bash
# Build
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export ANDROID_SDK_ROOT=/home/juxtapo/Android/Sdk
cd /home/juxtapo/cee_v25_Pre_Final/PhantomAI
./gradlew assembleDebug 2>&1 | tail -20

# Install
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Grant logcat (Phase 3 only)
adb shell pm grant com.phantom.ai android.permission.READ_LOGS
```

## Smoke tests from VPS

```bash
# Phase 2
curl http://10.0.0.3:8300/context

# Phase 3
curl -X POST http://10.0.0.3:8300/logcat \
  -H "Content-Type: application/json" \
  -d '{"pkg": "com.whatsapp", "lines": 50}'
```

---

## What NOT to do in Phase 2+3
- ❌ No MediaProjection implementation yet (Phase 4)
- ❌ Don't auto-start Accessibility/Notification services programmatically — Android requires manual user toggle
- ❌ Don't store screen text persistently (privacy — in-memory only via `PhantomContext`)
- ❌ Don't filter WhatsApp/banking apps specifically — generic capture only

---

_Built by Abang & Luv 💜 — FAFO session 2026-03-12_
