package com.phantom.ai

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.phantom.ai.service.OverlayService
import com.phantom.ai.ui.screens.SettingsScreen
import com.phantom.ai.ui.theme.PhantomAITheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        setContent {
            PhantomAITheme {
                PhantomLauncher(
                    onLaunchBubble = { launchBubble() },
                    onOpenOverlaySettings = { openOverlaySettings() },
                    hasOverlayPermission = { Settings.canDrawOverlays(this) }
                )
            }
        }
    }

    private fun launchBubble() {
        if (Settings.canDrawOverlays(this)) {
            val intent = Intent(this, OverlayService::class.java)
            val socketIntent = Intent(this, com.phantom.ai.service.BackSocketService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
                startForegroundService(socketIntent)
            } else {
                startService(intent)
                startService(socketIntent)
            }
            moveTaskToBack(true)
        } else {
            openOverlaySettings()
        }
    }

    private fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhantomLauncher(
    onLaunchBubble: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    hasOverlayPermission: () -> Boolean
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var permissionGranted by remember { mutableStateOf(hasOverlayPermission()) }

    LaunchedEffect(Unit) {
        permissionGranted = hasOverlayPermission()
    }

    val gradientBg = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0D0D1A),
            Color(0xFF1A0D2E),
            Color(0xFF0D1A2E)
        )
    )

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0D0D1A),
                contentColor = Color(0xFFE0D4FF),
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.RocketLaunch, contentDescription = null) },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFBB86FC),
                        selectedTextColor = Color(0xFFBB86FC),
                        unselectedIconColor = Color(0xFF5C5C7A),
                        unselectedTextColor = Color(0xFF5C5C7A),
                        indicatorColor = Color(0xFF1A1A2E)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFBB86FC),
                        selectedTextColor = Color(0xFFBB86FC),
                        unselectedIconColor = Color(0xFF5C5C7A),
                        unselectedTextColor = Color(0xFF5C5C7A),
                        indicatorColor = Color(0xFF1A1A2E)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBg)
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> HomeTab(
                    permissionGranted = permissionGranted,
                    onLaunchBubble = onLaunchBubble,
                    onOpenOverlaySettings = onOpenOverlaySettings
                )
                1 -> SettingsScreen()
            }
        }
    }
}

@Composable
private fun HomeTab(
    permissionGranted: Boolean,
    onLaunchBubble: () -> Unit,
    onOpenOverlaySettings: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_trojan),
                contentDescription = "Trojan Horse",
                modifier = Modifier.size(96.dp).clip(androidx.compose.foundation.shape.CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Trojan Horse",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE0D4FF)
            )

            Text(
                text = "Already inside. Already watching.",
                fontSize = 14.sp,
                color = Color(0xFF8B7FB8),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Status card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (permissionGranted) Color(0xFF4CAF50) else Color(0xFFFF5722)
                        )
                        Column {
                            Text("Overlay Permission", color = Color(0xFFE0D4FF), fontWeight = FontWeight.Medium)
                            Text(
                                text = if (permissionGranted) "Granted ✓" else "Required — tap to enable",
                                color = if (permissionGranted) Color(0xFF4CAF50) else Color(0xFFFF8A65),
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (!permissionGranted) {
                        OutlinedButton(
                            onClick = { onOpenOverlaySettings() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFBB86FC))
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Open Permission Settings")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Launch button
            Button(
                onClick = onLaunchBubble,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = permissionGranted,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6C3CE0),
                    disabledContainerColor = Color(0xFF2A2A3E)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text("Deploy Horse", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "The horse is inside the gates.\nTap to unleash the soldiers.",
                color = Color(0xFF6B6B8D),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = "v0.1.0-trojan • FAFO Build",
            color = Color(0xFF3A3A5C),
            fontSize = 10.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }
}
