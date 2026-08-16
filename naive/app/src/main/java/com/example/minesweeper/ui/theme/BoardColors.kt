package com.example.minesweeper.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Палитра поля намеренно не зависит от светлой/тёмной темы: классический
 * «сапёр» узнаётся именно по этим цветам, а цифры должны читаться одинаково.
 */
object BoardColors {
    val Hidden = Color(0xFFBEC8D4)
    val HiddenTop = Color(0xFFE3E9F0)
    val HiddenBottom = Color(0xFF8E9AA8)
    val Revealed = Color(0xFFE8EBEF)
    val Grid = Color(0xFFA9B3BF)
    val Exploded = Color(0xFFE04B4B)
    val Frame = Color(0xFF8E9AA8)

    private val numberColors = listOf(
        Color(0xFF1976D2), // 1
        Color(0xFF388E3C), // 2
        Color(0xFFD32F2F), // 3
        Color(0xFF512DA8), // 4
        Color(0xFF8D4E00), // 5
        Color(0xFF00838F), // 6
        Color(0xFF37474F), // 7
        Color(0xFF6D6D6D), // 8
    )

    fun number(value: Int): Color = numberColors[(value - 1).coerceIn(0, numberColors.lastIndex)]
}
