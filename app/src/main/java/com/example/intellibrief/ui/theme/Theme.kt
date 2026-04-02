package com.example.intellibrief.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AgencyGold,
    secondary = AgencySlate,
    tertiary = AgencyCrimson,
    background = AgencyNavy,
    surface = AgencyBlue,
    onPrimary = AgencyNavy,
    onSecondary = AgencyWhite,
    onTertiary = AgencyWhite,
    onBackground = AgencyWhite,
    onSurface = AgencyWhite,
    error = AgencyCrimson
)

private val LightColorScheme = lightColorScheme(
    primary = AgencyNavy,
    secondary = AgencySlate,
    tertiary = AgencyGold,
    background = AgencyWhite,
    surface = AgencyWhite,
    onPrimary = AgencyWhite,
    onSecondary = AgencyWhite,
    onTertiary = AgencyNavy,
    onBackground = AgencyNavy,
    onSurface = AgencyNavy,
    error = AgencyCrimson
)

@Composable
fun IntelliBriefTheme(
    darkTheme: Boolean = true, // Default to dark theme for agency look
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
