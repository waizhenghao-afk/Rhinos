package com.rhinos.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.rhinos.app.settings.ThemeMode

private val LightColors = lightColorScheme(
    primary = Gold,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFFFF3C4),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF4A3B00),
    secondary = androidx.compose.ui.graphics.Color(0xFF5C5434),
    background = androidx.compose.ui.graphics.Color(0xFFFDFBF7),
    surface = androidx.compose.ui.graphics.Color(0xFFFDFBF7),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFEDE6CE)
)

private val DarkColors = darkColorScheme(
    primary = GoldDark,
    onPrimary = androidx.compose.ui.graphics.Color.Black,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF3F3100),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFFFE08D),
    secondary = androidx.compose.ui.graphics.Color(0xFFC9C5A6),
    background = androidx.compose.ui.graphics.Color(0xFF121212),
    surface = androidx.compose.ui.graphics.Color(0xFF121212),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF2A2A2A)
)

@Composable
fun RhinosTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}

/** P&L colors that also hold up in dark mode. */
@Composable
fun pnlColor(positive: Boolean): androidx.compose.ui.graphics.Color {
    val dark = when (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
        true -> true
        else -> false
    }
    return if (positive) {
        if (dark) GreenPnlDark else GreenPnl
    } else {
        if (dark) RedPnlDark else RedPnl
    }
}

private fun androidx.compose.ui.graphics.Color.luminance(): Float =
    (0.299f * red + 0.587f * green + 0.114f * blue)
