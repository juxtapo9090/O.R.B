package com.phantom.ai.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
