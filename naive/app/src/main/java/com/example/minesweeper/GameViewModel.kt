package com.example.minesweeper

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minesweeper.game.Difficulty
import com.example.minesweeper.game.GameStatus
import com.example.minesweeper.game.Minesweeper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("minesweeper", Context.MODE_PRIVATE)

    var difficulty by mutableStateOf(Difficulty.BEGINNER)
        private set

    var state by mutableStateOf(Minesweeper.newGame(Difficulty.BEGINNER))
        private set

    /** Режим «флажок»: тап ставит флажок вместо открытия клетки. */
    var flagMode by mutableStateOf(false)
        private set

    var elapsedSeconds by mutableStateOf(0)
        private set

    /** Показывать ли диалог с итогом партии (закрывается, чтобы посмотреть поле). */
    var resultShown by mutableStateOf(false)
        private set

    private var timerJob: Job? = null

    fun bestTime(difficulty: Difficulty): Int? =
        prefs.getInt(bestTimeKey(difficulty), 0).takeIf { it > 0 }

    fun newGame(difficulty: Difficulty = this.difficulty) {
        stopTimer()
        elapsedSeconds = 0
        resultShown = false
        this.difficulty = difficulty
        state = Minesweeper.newGame(difficulty)
    }

    fun onCellTap(index: Int) {
        val before = state.status
        state = if (flagMode) {
            Minesweeper.toggleFlag(state, index)
        } else {
            Minesweeper.reveal(state, index)
        }
        onStatusChanged(before)
    }

    fun onCellLongPress(index: Int) {
        val before = state.status
        // Долгий тап — всегда флажок, независимо от режима: так привычнее.
        state = Minesweeper.toggleFlag(state, index)
        onStatusChanged(before)
    }

    fun toggleFlagMode() {
        flagMode = !flagMode
    }

    fun dismissResult() {
        resultShown = false
    }

    private fun onStatusChanged(before: GameStatus) {
        if (before == state.status) return
        when (state.status) {
            GameStatus.RUNNING -> startTimer()
            GameStatus.WON -> {
                stopTimer()
                saveBestTime()
                resultShown = true
            }
            GameStatus.LOST -> {
                stopTimer()
                resultShown = true
            }
            GameStatus.READY -> Unit
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && state.status == GameStatus.RUNNING) {
                delay(1000)
                if (state.status == GameStatus.RUNNING) elapsedSeconds++
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun saveBestTime() {
        val key = bestTimeKey(difficulty)
        val current = prefs.getInt(key, 0)
        if (current == 0 || elapsedSeconds < current) {
            prefs.edit().putInt(key, elapsedSeconds).apply()
        }
    }

    /** Новый рекорд показываем в диалоге победы. */
    fun isNewRecord(): Boolean =
        state.status == GameStatus.WON && bestTime(difficulty) == elapsedSeconds

    private fun bestTimeKey(difficulty: Difficulty) = "best_${difficulty.name}"

    override fun onCleared() {
        stopTimer()
        super.onCleared()
    }
}
