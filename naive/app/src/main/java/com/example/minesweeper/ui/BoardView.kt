package com.example.minesweeper.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.example.minesweeper.game.Cell
import com.example.minesweeper.game.Difficulty
import com.example.minesweeper.game.GameState
import com.example.minesweeper.game.Minesweeper
import com.example.minesweeper.ui.theme.BoardColors
import com.example.minesweeper.ui.theme.MinesweeperTheme
import kotlin.random.Random

private val MIN_CELL = 26.dp
private val MAX_CELL = 44.dp

/**
 * Поле целиком. Клетки подгоняются под ширину экрана, но не мельче [MIN_CELL] —
 * широкие уровни («профессионал») просто прокручиваются.
 */
@Composable
fun BoardView(
    state: GameState,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val cellSize = (maxWidth / state.cols).coerceIn(MIN_CELL, MAX_CELL)
        Box(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .verticalScroll(rememberScrollState())
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .border(2.dp, BoardColors.Frame, RoundedCornerShape(6.dp))
                    .background(BoardColors.Grid)
                    .padding(2.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                for (row in 0 until state.rows) {
                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                        for (col in 0 until state.cols) {
                            val index = state.index(row, col)
                            CellView(
                                cell = state.cells[index],
                                size = cellSize,
                                exploded = index == state.explodedIndex,
                                wrongFlag = state.isWrongFlag(index),
                                onTap = { onTap(index) },
                                onLongPress = { onLongPress(index) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CellView(
    cell: Cell,
    size: Dp,
    exploded: Boolean,
    wrongFlag: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val fontSize = with(LocalDensity.current) { (size * 0.56f).toSp() }
    val interactionSource = remember { MutableInteractionSource() }

    val background: Brush = when {
        exploded -> SolidColor(BoardColors.Exploded)
        cell.revealed -> SolidColor(BoardColors.Revealed)
        // Лёгкий градиент вместо классического «объёмного» бордюра: тот же эффект
        // выпуклой кнопки, но одним модификатором.
        else -> Brush.verticalGradient(listOf(BoardColors.HiddenTop, BoardColors.HiddenBottom))
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(3.dp))
            .background(background)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTap,
                onLongClick = onLongPress,
            )
            .semantics { contentDescription = cell.describe(exploded, wrongFlag) },
        contentAlignment = Alignment.Center,
    ) {
        when {
            wrongFlag -> Text("❌", fontSize = fontSize, textAlign = TextAlign.Center)
            cell.flagged -> Text("🚩", fontSize = fontSize, textAlign = TextAlign.Center)
            !cell.revealed -> Unit
            cell.isMine -> Text("💣", fontSize = fontSize, textAlign = TextAlign.Center)
            cell.adjacent > 0 -> Text(
                text = cell.adjacent.toString(),
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = BoardColors.number(cell.adjacent),
                textAlign = TextAlign.Center,
            )
            else -> Unit
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 420)
@Composable
private fun BoardPreview() {
    val fresh = Minesweeper.newGame(Difficulty.BEGINNER)
    val opened = Minesweeper.reveal(fresh, fresh.index(4, 4), Random(7))
    val state = Minesweeper.toggleFlag(opened, fresh.index(0, 0))
    MinesweeperTheme {
        BoardView(state = state, onTap = {}, onLongPress = {})
    }
}

private fun Cell.describe(exploded: Boolean, wrongFlag: Boolean): String = when {
    wrongFlag -> "Ошибочный флажок"
    flagged -> "Флажок"
    !revealed -> "Закрытая клетка"
    exploded -> "Взорванная мина"
    isMine -> "Мина"
    adjacent > 0 -> "Рядом мин: $adjacent"
    else -> "Пустая клетка"
}
