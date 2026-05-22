package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SophisticatedPurple,
    secondary = SophisticatedPurple,
    tertiary = SophisticatedGreen,
    background = SophisticatedBg,
    surface = SophisticatedSurface,
    onPrimary = Color(0xFF21005D),
    onSecondary = SophisticatedTextPrimary,
    onBackground = SophisticatedTextPrimary,
    onSurface = SophisticatedTextPrimary,
    error = SophisticatedError,
    surfaceVariant = SophisticatedCardBg,
    outline = SophisticatedBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force visual excellence as a high-tech darkness operational console
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
