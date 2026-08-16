package com.example.minesweeper.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF3F6FB0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7E3F7),
    onPrimaryContainer = Color(0xFF0F2B4F),
    secondary = Color(0xFF546070),
    background = Color(0xFFF7F9FC),
    surface = Color(0xFFF7F9FC),
    surfaceVariant = Color(0xFFDFE3EB),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA8C7F0),
    onPrimary = Color(0xFF0F2B4F),
    primaryContainer = Color(0xFF27456E),
    onPrimaryContainer = Color(0xFFD7E3F7),
    secondary = Color(0xFFBBC7DB),
    background = Color(0xFF111418),
    surface = Color(0xFF111418),
    surfaceVariant = Color(0xFF42474E),
)

@Composable
fun MinesweeperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
