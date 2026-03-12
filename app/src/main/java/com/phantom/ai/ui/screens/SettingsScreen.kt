package com.phantom.ai.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phantom.ai.data.SettingsRepository
import com.phantom.ai.network.StreamerClient
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settingsRepo = remember { SettingsRepository(context) }
    val settings by settingsRepo.settingsFlow.collectAsState(
        initial = com.phantom.ai.data.AppSettings()
    )
    val scope = rememberCoroutineScope()
    val client = remember { StreamerClient() }

    var streamerUrl by remember(settings.streamerUrl) { mutableStateOf(settings.streamerUrl) }
    var backSocketPort by remember(settings.backSocketPort) { mutableStateOf(settings.backSocketPort.toString()) }
    var selectedBackend by remember(settings.activeBackend) { mutableStateOf(settings.activeBackend) }
    var geminiKey by remember { mutableStateOf("") }
    var anthropicKey by remember { mutableStateOf("") }
    var termuxPackage by remember { mutableStateOf("com.termux") }
    var bubbleSize by remember { mutableStateOf(56f) }
    var bubbleOpacity by remember { mutableStateOf(0.9f) }
    var rootEnabled by remember { mutableStateOf(true) }
    var showApiKeys by remember { mutableStateOf(false) }
    var testingConnection by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ─── AI Backend ─────────────────────────────
        SettingsSection(title = "AI Backend", icon = Icons.Default.Psychology) {
            SettingsLabel("Active Backend")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                BackendChip("Gemini", selectedBackend == "gemini") {
                    selectedBackend = "gemini"
                    scope.launch { settingsRepo.saveActiveBackend("gemini") }
                }
                BackendChip("Claude", selectedBackend == "anthropic") {
                    selectedBackend = "anthropic"
                    scope.launch { settingsRepo.saveActiveBackend("anthropic") }
                }
                BackendChip("Custom", selectedBackend == "custom") {
                    selectedBackend = "custom"
                    scope.launch { settingsRepo.saveActiveBackend("custom") }
                }
            }

            Spacer(Modifier.height(12.dp))

            // API Keys (for direct mode)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsLabel("API Keys")
                IconButton(onClick = { showApiKeys = !showApiKeys }) {
                    Icon(
                        if (showApiKeys) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle visibility",
                        tint = Color(0xFF8B7FB8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            SettingsTextField(geminiKey, { geminiKey = it }, "Gemini API Key", isPassword = !showApiKeys)
            Spacer(Modifier.height(8.dp))
            SettingsTextField(anthropicKey, { anthropicKey = it }, "Anthropic API Key", isPassword = !showApiKeys)
        }

        // ─── Streamer (Custom / VPS Bridge) ─────────
        SettingsSection(title = "VPS Streamer", icon = Icons.Default.Hub) {
            SettingsLabel("Transcript Streamer URL")
            SettingsTextField(
                value = streamerUrl,
                onValueChange = {
                    streamerUrl = it
                    scope.launch { settingsRepo.saveStreamerUrl(it) }
                },
                placeholder = "http://10.0.0.1:8200"
            )

            Spacer(Modifier.height(8.dp))

            SettingsLabel("Back Socket Port (Restart required)")
            SettingsTextField(
                value = backSocketPort,
                onValueChange = {
                    backSocketPort = it
                    val port = it.toIntOrNull()
                    if (port != null && port in 1..65535) {
                        scope.launch { settingsRepo.saveBackSocketPort(port) }
                    }
                },
                placeholder = "8300"
            )

            Spacer(Modifier.height(8.dp))

            // Test Connection button
            Button(
                onClick = {
                    testingConnection = true
                    scope.launch {
                        val result = client.checkHealth(streamerUrl)
                        testingConnection = false
                        Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                    }
                },
                enabled = !testingConnection && streamerUrl.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3A1C71),
                    contentColor = Color(0xFFBB86FC)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (testingConnection) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color(0xFFBB86FC),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Testing...", fontSize = 13.sp)
                } else {
                    Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Test Connection", fontSize = 13.sp)
                }
            }
        }

        // ─── Termux Integration ─────────────────────
        SettingsSection(title = "Termux", icon = Icons.Default.Terminal) {
            SettingsTextField(termuxPackage, { termuxPackage = it }, "Termux package name")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("tmux Auto-attach", color = Color(0xFFE0D4FF), fontSize = 14.sp)
                    Text("Attach to 'phantom' session on launch", color = Color(0xFF6B6B8D), fontSize = 11.sp)
                }
                Switch(
                    checked = true, onCheckedChange = {},
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF6C3CE0),
                        checkedTrackColor = Color(0xFF3A1C71)
                    )
                )
            }
        }

        // ─── Root Access ────────────────────────────
        SettingsSection(title = "Root Access", icon = Icons.Default.Shield) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Enable Root Commands", color = Color(0xFFE0D4FF), fontSize = 14.sp)
                    Text("Allow AI to execute SU commands", color = Color(0xFF6B6B8D), fontSize = 11.sp)
                }
                Switch(
                    checked = rootEnabled, onCheckedChange = { rootEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFFF5722),
                        checkedTrackColor = Color(0xFF5D1F0D)
                    )
                )
            }
        }

        // ─── Bubble ──────────────────────────────────
        SettingsSection(title = "Bubble", icon = Icons.Default.Lens) {
            SettingsLabel("Size: ${bubbleSize.toInt()}dp")
            Slider(
                value = bubbleSize, onValueChange = { bubbleSize = it },
                valueRange = 40f..80f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF6C3CE0), activeTrackColor = Color(0xFF6C3CE0))
            )
            SettingsLabel("Opacity: ${(bubbleOpacity * 100).toInt()}%")
            Slider(
                value = bubbleOpacity, onValueChange = { bubbleOpacity = it },
                valueRange = 0.3f..1f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF6C3CE0), activeTrackColor = Color(0xFF6C3CE0))
            )
        }

        // ─── About ───────────────────────────────────
        SettingsSection(title = "About", icon = Icons.Default.Info) {
            SettingsRow("Version", "0.1.0-trojan")
            SettingsRow("Build", "FAFO")
            SettingsRow("Architecture", "Trojan Conductor 🐴")
            SettingsRow("Streamer", if (streamerUrl.isNotBlank()) "✅ configured" else "❌ not set")
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ─── Reusable Components ────────────────────────────

@Composable
private fun SettingsSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFFBB86FC), modifier = Modifier.size(20.dp))
                Text(title, color = Color(0xFFE0D4FF), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            content()
        }
    }
}

@Composable
private fun SettingsLabel(text: String) {
    Text(text, color = Color(0xFF8B7FB8), fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
}

@Composable
private fun SettingsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF8B7FB8), fontSize = 13.sp)
        Text(value, color = Color(0xFFE0D4FF), fontSize = 13.sp)
    }
}

@Composable
private fun SettingsTextField(value: String, onValueChange: (String) -> Unit, placeholder: String, isPassword: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF252540))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        if (value.isEmpty()) Text(placeholder, color = Color(0xFF5C5C7A), fontSize = 13.sp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = Color(0xFFE0D4FF), fontSize = 13.sp),
            cursorBrush = SolidColor(Color(0xFFBB86FC)),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
private fun BackendChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF6C3CE0),
            selectedLabelColor = Color.White,
            containerColor = Color(0xFF252540),
            labelColor = Color(0xFF8B7FB8)
        )
    )
}
