package com.phantom.ai.network

import com.phantom.ai.ui.overlay.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * StreamerClient — SSE client for transcript_streamer.py
 *
 * Reads GET /stream as Server-Sent Events and emits ChatMessage objects.
 * Handles keepalive pings silently. Auto-reconnects on drop.
 */
class StreamerClient {

    companion object {
        private const val RECONNECT_DELAY_MS = 3000L
        private const val CONNECT_TIMEOUT_MS = 5000
        private const val READ_TIMEOUT_MS = 30000
    }

    /**
     * Connect to SSE stream and emit ChatMessages.
     * Automatically reconnects on disconnection.
     */
    fun stream(baseUrl: String): Flow<ChatMessage> = channelFlow {
        val streamUrl = "$baseUrl/stream".trimEnd('/')
            .replace("//stream", "/stream")

        while (true) {
            try {
                withContext(Dispatchers.IO) {
                    val conn = (URL(streamUrl).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("Accept", "text/event-stream")
                        setRequestProperty("Cache-Control", "no-cache")
                        connectTimeout = CONNECT_TIMEOUT_MS
                        readTimeout = READ_TIMEOUT_MS
                    }
                    BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                        for (line in reader.lineSequence()) {
                            when {
                                line.startsWith("data: ") -> {
                                    val data = line.removePrefix("data: ").trim()
                                    parseMessage(data)?.let { msg -> send(msg) }
                                }
                                line.startsWith(": ") -> { /* keepalive ping — ignore */ }
                                else -> { /* blank line or event boundary */ }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                delay(RECONNECT_DELAY_MS)
            }
        }
    }

    /**
     * POST /prompt — inject user message into VPS Gemini session.
     * Returns true if server acknowledged.
     */
    suspend fun sendPrompt(baseUrl: String, prompt: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val conn = (URL("$baseUrl/prompt").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = CONNECT_TIMEOUT_MS
                }
                val body = JSONObject().put("prompt", prompt).toString()
                OutputStreamWriter(conn.outputStream).use { it.write(body) }
                conn.responseCode == 200
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * GET /health — quick connectivity check.
     * Returns status string or null on failure.
     */
    suspend fun checkHealth(baseUrl: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val conn = (URL("$baseUrl/health").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = CONNECT_TIMEOUT_MS
                }
                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    val session = json.optString("session", "unknown")
                    "✅ Connected — session: $session"
                } else {
                    "❌ HTTP ${conn.responseCode}"
                }
            } catch (e: Exception) {
                "❌ ${e.message ?: "Connection failed"}"
            }
        }
    }

    private fun parseMessage(data: String): ChatMessage? {
        return try {
            val json = JSONObject(data)
            // Skip system events like session_start
            if (json.has("event")) return null
            val role = json.optString("role", "unknown")
            val text = json.optString("text", "")
            if (text.isBlank()) return null
            ChatMessage(
                content = text,
                isUser = role == "user"
            )
        } catch (e: Exception) {
            null
        }
    }
}
