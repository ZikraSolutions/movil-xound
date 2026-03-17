package com.example.xound.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Xound brand colors (always the same)
val XoundNavy = Color(0xFF1A2642)
val XoundYellow = Color(0xFFF0B42A)

// Theme-aware color palette
data class XoundColorScheme(
    val screenBackground: Color,
    val cardBackground: Color,
    val inputBackground: Color,
    val inputBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textHint: Color,
    val textOnNavy: Color,
    val recentCardBackground: Color,
    val searchBackground: Color,
    val searchBorder: Color,
    val chipUnselectedBg: Color,
    val chipUnselectedText: Color,
    val dialogBackground: Color,
    val divider: Color,
    val errorColor: Color,
    val successColor: Color,
    val chordColor: Color,
    val lyricsText: Color,
    val navyCardDark: Color,
)

val LightXoundColors = XoundColorScheme(
    screenBackground = Color(0xFFF5F0E8),    // XoundCream
    cardBackground = Color.White,
    inputBackground = Color.White,
    inputBorder = Color(0xFFE5E5E5),
    textPrimary = Color.Black,
    textSecondary = Color(0xFF888888),
    textHint = Color(0xFF999999),
    textOnNavy = Color.White,
    recentCardBackground = Color.White,
    searchBackground = Color.White,
    searchBorder = Color(0xFFE0E0E0),
    chipUnselectedBg = Color.Transparent,
    chipUnselectedText = XoundNavy,
    dialogBackground = Color.White,
    divider = Color(0xFFE5E5E5),
    errorColor = Color.Red,
    successColor = Color(0xFF4CAF50),
    chordColor = Color(0xFFE5A100),
    lyricsText = Color.Black,
    navyCardDark = XoundNavy,
)

val DarkXoundColors = XoundColorScheme(
    screenBackground = Color(0xFF2D3142),     // Dark gray-blue from Figma
    cardBackground = Color(0xFF1E2538),       // Slightly lighter than navy
    inputBackground = Color(0xFF232839),      // Dark input bg
    inputBorder = Color(0xFF3D4257),          // Subtle border
    textPrimary = Color.White,
    textSecondary = Color(0xFFAAAAAA),
    textHint = Color(0xFF777777),
    textOnNavy = Color.White,
    recentCardBackground = Color(0xFF1E2538),
    searchBackground = Color(0xFF232839),
    searchBorder = Color(0xFF3D4257),
    chipUnselectedBg = Color.Transparent,
    chipUnselectedText = Color.White,
    dialogBackground = Color(0xFF2D3142),
    divider = Color(0xFF3D4257),
    errorColor = Color(0xFFFF6B6B),
    successColor = Color(0xFF4CAF50),
    chordColor = Color(0xFFE5A100),
    lyricsText = Color.White,
    navyCardDark = Color(0xFF1A2642),
)

// Login/Register screens use white bg in light, dark bg in dark
val LightAuthColors = XoundColorScheme(
    screenBackground = Color.White,
    cardBackground = Color.White,
    inputBackground = Color(0xFFFAFAFA),
    inputBorder = Color(0xFFE5E5E5),
    textPrimary = Color.Black,
    textSecondary = Color(0xFF888888),
    textHint = Color(0xFF999999),
    textOnNavy = Color.White,
    recentCardBackground = Color.White,
    searchBackground = Color.White,
    searchBorder = Color(0xFFE0E0E0),
    chipUnselectedBg = Color.Transparent,
    chipUnselectedText = XoundNavy,
    dialogBackground = Color.White,
    divider = Color(0xFFE5E5E5),
    errorColor = Color.Red,
    successColor = Color(0xFF4CAF50),
    chordColor = Color(0xFFE5A100),
    lyricsText = Color.Black,
    navyCardDark = XoundNavy,
)

val LocalXoundColors = staticCompositionLocalOf { LightXoundColors }
