package dev.fedorfalchuk.minesweeper.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.fedorfalchuk.minesweeper.game.Difficulty

@Composable
fun MenuScreen(
    records: Map<Difficulty, Int>,
    hasSavedGame: Boolean,
    confirmingRestart: Difficulty?,
    onContinue: () -> Unit,
    onPick: (Difficulty) -> Unit,
    onConfirmRestart: () -> Unit,
    onCancelRestart: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Сапёр",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(32.dp))

        if (hasSavedGame) {
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Продолжить")
            }
            Spacer(Modifier.height(24.dp))
        }

        for (difficulty in Difficulty.entries) {
            OutlinedButton(
                onClick = { onPick(difficulty) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Text(difficulty.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${difficulty.size}×${difficulty.size}, мин: ${difficulty.mines}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Блок рекордов показывает только уровни, где есть победа.
        if (records.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Лучшее время", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    for (difficulty in Difficulty.entries) {
                        val seconds = records[difficulty] ?: continue
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(difficulty.title, style = MaterialTheme.typography.bodyMedium)
                            Text(formatTime(seconds * 1000L), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    if (confirmingRestart != null) {
        AlertDialog(
            onDismissRequest = onCancelRestart,
            title = { Text("Начать заново?") },
            text = { Text("Текущая партия будет потеряна.") },
            confirmButton = {
                TextButton(onClick = onConfirmRestart) { Text("Начать заново") }
            },
            dismissButton = {
                TextButton(onClick = onCancelRestart) { Text("Отмена") }
            },
        )
    }
}
