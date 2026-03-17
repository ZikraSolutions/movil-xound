package com.example.xound.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.example.xound.data.local.SessionManager

private val DarkColorScheme = darkColorScheme(
    primary = XoundNavy,
    secondary = XoundYellow,
    background = Color(0xFF2D3142),
    surface = Color(0xFF2D3142)
)

private val LightColorScheme = lightColorScheme(
    primary = XoundNavy,
    secondary = XoundYellow,
    background = Color.White,
    surface = Color.White
)

// Global dark mode state holder
object ThemeState {
    // "system", "on", "off"
    var darkModeOption by mutableStateOf("system")
        private set

    fun init() {
        darkModeOption = SessionManager.getDarkModeOption()
    }

    fun toggleDarkMode(systemIsDark: Boolean) {
        // Cycle: if currently following system → force on, if on → force off, if off → system
        val newOption = when (darkModeOption) {
            "system" -> if (systemIsDark) "off" else "on"
            "on" -> "off"
            "off" -> "on"
            else -> "system"
        }
        darkModeOption = newOption
        SessionManager.setDarkModeOption(newOption)
    }

    fun isDark(systemIsDark: Boolean): Boolean = when (darkModeOption) {
        "on" -> true
        "off" -> false
        else -> systemIsDark
    }
}

@Composable
fun XOUNDTheme(
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = ThemeState.isDark(systemDark)

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val xoundColors = if (isDark) DarkXoundColors else LightXoundColors

    CompositionLocalProvider(LocalXoundColors provides xoundColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
