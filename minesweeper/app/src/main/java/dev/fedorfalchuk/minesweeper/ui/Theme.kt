package dev.fedorfalchuk.minesweeper.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F5D50),
    onPrimary = Color.White,
    secondary = Color(0xFF7A5C2E),
    background = Color(0xFFF4F1EA),
    onBackground = Color(0xFF1B1B18),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1B18),
    surfaceVariant = Color(0xFFDCD7CC),
    onSurfaceVariant = Color(0xFF44403A),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FD0BC),
    onPrimary = Color(0xFF0B231C),
    secondary = Color(0xFFDCC08C),
    background = Color(0xFF12130F),
    onBackground = Color(0xFFE6E2D8),
    surface = Color(0xFF1C1E1A),
    onSurface = Color(0xFFE6E2D8),
    surfaceVariant = Color(0xFF3A3D37),
    onSurfaceVariant = Color(0xFFC7C3B8),
    error = Color(0xFFF2B8B5),
)

/**
 * Цвета цифр на поле. Держатся отдельно от схемы Material: это не роли темы,
 * а фиксированный код «сколько мин рядом», знакомый по классическому сапёру.
 */
object CellPalette {
    private val light = listOf(
        Color(0xFF1D4ED8), Color(0xFF15803D), Color(0xFFB91C1C), Color(0xFF6D28D9),
        Color(0xFF9A3412), Color(0xFF0E7490), Color(0xFF334155), Color(0xFF78716C),
    )
    private val dark = listOf(
        Color(0xFF7FA8FF), Color(0xFF6DD48F), Color(0xFFFF8F87), Color(0xFFC4A2FF),
        Color(0xFFFFB27A), Color(0xFF6BD5E8), Color(0xFFC9D3E0), Color(0xFFBDB6AE),
    )

    fun forCount(count: Int, isDark: Boolean): Color {
        val palette = if (isDark) dark else light
        return palette[(count - 1).coerceIn(0, palette.lastIndex)]
    }
}

@Composable
fun MinesweeperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Цвет иконок статус-бара не трогаем намеренно: WindowCompat потребовал бы
    // зависимости androidx.core, а добавлять зависимости без вопроса запрещено.
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
