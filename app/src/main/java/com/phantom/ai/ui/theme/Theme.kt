package com.phantom.ai.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PhantomDarkColorScheme = darkColorScheme(
    primary = PhantomPurple,
    onPrimary = Color.White,
    primaryContainer = PhantomPurpleDark,
    secondary = PhantomPurpleLight,
    background = PhantomBackground,
    surface = PhantomSurface,
    onBackground = PhantomText,
    onSurface = PhantomText,
    error = PhantomRed
)

@Composable
fun PhantomAITheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PhantomDarkColorScheme,
        content = content
    )
}
