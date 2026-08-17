package dev.fedorfalchuk.minesweeper.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.fedorfalchuk.minesweeper.game.Game
import dev.fedorfalchuk.minesweeper.game.Phase
import dev.fedorfalchuk.minesweeper.game.adjacentMines

/** Формат mm:ss. Минуты не ограничены двумя разрядами, потолок 999:59. */
fun formatTime(elapsedMs: Long): String {
    val totalSeconds = (elapsedMs / 1000).coerceIn(0, 999 * 60 + 59)
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Composable
fun GameScreen(
    game: Game,
    elapsedMs: Long,
    isNewRecord: Boolean,
    onOpen: (Int) -> Unit,
    onFlag: (Int) -> Unit,
    onRestart: () -> Unit,
    onMenu: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StatusBar(
                minesLeft = game.minesLeft,
                elapsedMs = elapsedMs,
                onMenu = onMenu,
            )
            Spacer(Modifier.height(12.dp))
            Board(
                game = game,
                enabled = !game.isOver,
                onOpen = onOpen,
                onFlag = onFlag,
                modifier = Modifier.weight(1f),
            )
        }

        if (game.isOver) {
            ResultOverlay(
                won = game.phase == Phase.WON,
                elapsedMs = game.elapsedMs,
                isNewRecord = isNewRecord,
                onRestart = onRestart,
                onMenu = onMenu,
            )
        }
    }
}

@Composable
private fun StatusBar(minesLeft: Int, elapsedMs: Long, onMenu: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Ширина счётчика рассчитана под «-99»: перерасход флажков уводит его в минус.
        Text(
            text = "⚑ $minesLeft",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = formatTime(elapsedMs),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        TextButton(onClick = onMenu) { Text("В меню") }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Board(
    game: Game,
    enabled: Boolean,
    onOpen: (Int) -> Unit,
    onFlag: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val size = game.difficulty.size
    val gap = 1.dp

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        // Клетка считается по меньшей стороне: иначе в альбомной ориентации
        // поле, посчитанное по ширине, не поместилось бы по высоте.
        val available = minOf(maxWidth, maxHeight)
        val cell = (available - gap * (size - 1)) / size

        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            for (row in 0 until size) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (col in 0 until size) {
                        val index = row * size + col
                        Cell(
                            game = game,
                            index = index,
                            side = cell,
                            enabled = enabled,
                            onOpen = onOpen,
                            onFlag = onFlag,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Cell(
    game: Game,
    index: Int,
    side: Dp,
    enabled: Boolean,
    onOpen: (Int) -> Unit,
    onFlag: (Int) -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val opened = index in game.opened
    val flagged = index in game.flagged
    val isMine = index in game.mines
    val exploded = game.explodedAt == index
    val lost = game.phase == Phase.LOST

    // После поражения показываются все мины, кроме тех, что под верными флажками.
    val revealMine = lost && isMine && !flagged
    val wrongFlag = lost && flagged && !isMine

    val background = when {
        exploded -> MaterialTheme.colorScheme.error
        opened || revealMine -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    // Размер цифры производен от размера клетки, а не задан в sp:
    // при системном шрифте 200 % цифры иначе вылезли бы за клетку и поле развалилось.
    val fontSize = with(LocalDensity.current) { (side * 0.55f).toSp() }

    Box(
        modifier = Modifier
            .size(side)
            .clip(RoundedCornerShape(2.dp))
            .background(background)
            .combinedClickable(
                enabled = enabled,
                onClick = { onOpen(index) },
                onLongClick = { onFlag(index) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            flagged -> Text(
                text = "⚑",
                style = TextStyle(
                    fontSize = fontSize,
                    color = if (wrongFlag) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                    textDecoration = if (wrongFlag) TextDecoration.LineThrough else null,
                ),
            )

            exploded -> Text("✳", style = TextStyle(fontSize = fontSize, color = Color.White))

            revealMine -> Text(
                text = "✳",
                style = TextStyle(fontSize = fontSize, color = MaterialTheme.colorScheme.onSurface),
            )

            opened -> {
                val count = game.adjacentMines(index)
                if (count > 0) {
                    Text(
                        text = count.toString(),
                        style = TextStyle(
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            color = CellPalette.forCount(count, isDark),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultOverlay(
    won: Boolean,
    elapsedMs: Long,
    isNewRecord: Boolean,
    onRestart: () -> Unit,
    onMenu: () -> Unit,
) {
    // Тап мимо карточки оверлей не закрывает: выход только кнопкой.
    // Отдельный перехват касаний не нужен — поле в завершённой партии
    // уже не принимает ввод (enabled = false у клеток).
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center,
    ) {
        Card(modifier = Modifier.padding(32.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (won) "Победа" else "Поражение",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Text("Время: ${formatTime(elapsedMs)}", style = MaterialTheme.typography.bodyLarge)
                if (won && isNewRecord) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Новый рекорд",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(20.dp))
                Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("Заново") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onMenu, modifier = Modifier.fillMaxWidth()) { Text("В меню") }
            }
        }
    }
}
