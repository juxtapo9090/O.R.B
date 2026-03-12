package com.phantom.ai.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class PhantomAccessibilityService : AccessibilityService() {

    private var lastScrapeTime = 0L
    private val debounceMs = 2000L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                event.packageName?.let {
                    PhantomContext.foregroundApp = it.toString()
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val now = System.currentTimeMillis()
                if (now - lastScrapeTime > debounceMs) {
                    lastScrapeTime = now
                    dumpScreenText()
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.w("PhantomAI", "Accessibility Service Interrupted")
    }

    private fun dumpScreenText() {
        val rootNode = rootInActiveWindow ?: return
        val texts = mutableListOf<String>()
        extractText(rootNode, texts)
        val fullText = texts.joinToString("\n").take(2000)
        PhantomContext.screenText = fullText
    }

    private fun extractText(node: AccessibilityNodeInfo?, texts: MutableList<String>) {
        if (node == null) return

        if (!node.text.isNullOrBlank()) {
            texts.add(node.text.toString())
        } else if (!node.contentDescription.isNullOrBlank()) {
            texts.add(node.contentDescription.toString())
        }

        for (i in 0 until node.childCount) {
            extractText(node.getChild(i), texts)
        }
    }
}
