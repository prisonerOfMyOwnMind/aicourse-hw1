package com.example.minesweeper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minesweeper.GameViewModel
import com.example.minesweeper.game.Difficulty
import com.example.minesweeper.game.GameStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(viewModel: GameViewModel) {
    val state = viewModel.state
    val haptics = LocalHapticFeedback.current
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сапёр") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                actions = {
                    Box {
                        TextButton(onClick = { menuOpen = true }) {
                            Text(
                                text = viewModel.difficulty.title,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            Difficulty.entries.forEach { difficulty ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(difficulty.title)
                                            Text(
                                                text = "${difficulty.rows}×${difficulty.cols}, " +
                                                    "мин: ${difficulty.mines}" +
                                                    (viewModel.bestTime(difficulty)
                                                        ?.let { " · рекорд ${formatTime(it)}" } ?: ""),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                    onClick = {
                                        menuOpen = false
                                        viewModel.newGame(difficulty)
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.toggleFlagMode()
                },
                containerColor = if (viewModel.flagMode) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (viewModel.flagMode) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ) {
                Text(if (viewModel.flagMode) "🚩 Флажки" else "⛏ Копать")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StatusPanel(
                minesLeft = state.minesLeft,
                seconds = viewModel.elapsedSeconds,
                status = state.status,
                onReset = { viewModel.newGame() },
            )

            BoardView(
                state = state,
                onTap = { index ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.onCellTap(index)
                },
                onLongPress = { index ->
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.onCellLongPress(index)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            Text(
                text = "Долгое нажатие — флажок. Тап по цифре с нужным числом флажков открывает соседей.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }
    }

    if (viewModel.resultShown && state.isFinished) {
        ResultDialog(
            won = state.status == GameStatus.WON,
            seconds = viewModel.elapsedSeconds,
            isRecord = viewModel.isNewRecord(),
            onRestart = { viewModel.newGame() },
            onDismiss = { viewModel.dismissResult() },
        )
    }
}

@Composable
private fun StatusPanel(
    minesLeft: Int,
    seconds: Int,
    status: GameStatus,
    onReset: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Counter(label = "💣", value = minesLeft.toString())

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFD54F))
                    .clickable(onClick = onReset),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when (status) {
                        GameStatus.WON -> "😎"
                        GameStatus.LOST -> "😵"
                        else -> "🙂"
                    },
                    fontSize = 26.sp,
                )
            }

            Counter(label = "⏱", value = formatTime(seconds))
        }
    }
}

@Composable
private fun Counter(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, fontSize = 20.sp)
        Text(
            text = " $value",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ResultDialog(
    won: Boolean,
    seconds: Int,
    isRecord: Boolean,
    onRestart: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (won) "Победа!" else "Взрыв!") },
        text = {
            Text(
                if (won) {
                    "Поле очищено за ${formatTime(seconds)}." +
                        if (isRecord) "\nЭто новый рекорд для уровня." else ""
                } else {
                    "Вы наступили на мину. Попробуем ещё раз?"
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onRestart) { Text("Новая игра") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Посмотреть поле") }
        },
    )
}

private val Difficulty.title: String
    get() = when (this) {
        Difficulty.BEGINNER -> "Новичок"
        Difficulty.INTERMEDIATE -> "Любитель"
        Difficulty.EXPERT -> "Профессионал"
    }

private fun formatTime(seconds: Int): String =
    "%d:%02d".format(seconds / 60, seconds % 60)
