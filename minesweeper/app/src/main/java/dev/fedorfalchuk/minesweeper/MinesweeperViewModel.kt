package dev.fedorfalchuk.minesweeper

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dev.fedorfalchuk.minesweeper.data.AsyncSaver
import dev.fedorfalchuk.minesweeper.data.GameStorage
import dev.fedorfalchuk.minesweeper.game.Clock
import dev.fedorfalchuk.minesweeper.game.Difficulty
import dev.fedorfalchuk.minesweeper.game.Game
import dev.fedorfalchuk.minesweeper.game.GameTimer
import dev.fedorfalchuk.minesweeper.game.Phase
import dev.fedorfalchuk.minesweeper.game.newGame
import dev.fedorfalchuk.minesweeper.game.open
import dev.fedorfalchuk.minesweeper.game.toggleFlag
import kotlin.random.Random

enum class Screen { MENU, GAME }

/**
 * Связывает чистое ядро игры с диском и экраном.
 *
 * Состояние партии пишется на диск после любого изменения, а не при выходе:
 * кнопки выхода на телефоне нет, процесс убивают без предупреждения.
 */
class MinesweeperViewModel(
    private val storage: GameStorage,
    clock: Clock,
    private val random: Random = Random.Default,
) : ViewModel() {

    private val timer = GameTimer(clock)
    private val saver = AsyncSaver<Game> { storage.saveGame(it) }

    var screen by mutableStateOf(Screen.MENU)
        private set

    var game by mutableStateOf<Game?>(null)
        private set

    var records by mutableStateOf(storage.loadRecords())
        private set

    /** Уровень, выбранный в меню при живой партии: ждёт подтверждения потери. */
    var confirmingRestart by mutableStateOf<Difficulty?>(null)
        private set

    var lastRunWasRecord by mutableStateOf(false)
        private set

    private var savedGame: Game? = storage.loadGame()

    val hasSavedGame: Boolean get() = savedGame != null

    /** Время для отрисовки. Считается по монотонным меткам, а не тиками. */
    fun elapsedMs(): Long = timer.elapsedMs()

    // --- меню ---

    fun pickDifficulty(difficulty: Difficulty) {
        if (savedGame != null) {
            confirmingRestart = difficulty
            return
        }
        startGame(difficulty)
    }

    fun confirmRestart() {
        val difficulty = confirmingRestart ?: return
        confirmingRestart = null
        startGame(difficulty)
    }

    fun cancelRestart() {
        confirmingRestart = null
    }

    fun continueGame() {
        val saved = savedGame ?: return
        game = saved
        timer.restore(saved.elapsedMs)
        lastRunWasRecord = false
        screen = Screen.GAME
        onScreenVisible()
    }

    private fun startGame(difficulty: Difficulty) {
        val fresh = newGame(difficulty)
        game = fresh
        savedGame = fresh
        timer.restore(0L)
        lastRunWasRecord = false
        screen = Screen.GAME
        persist(fresh)
        onScreenVisible()
    }

    // --- партия ---

    fun openCell(index: Int) {
        val current = game ?: return
        val next = current.open(index, random)
        if (next === current) return
        if (next.phase == Phase.PLAYING) timer.resume()
        applyMove(next)
    }

    fun toggleFlag(index: Int) {
        val current = game ?: return
        val next = current.toggleFlag(index)
        if (next === current) return
        applyMove(next)
    }

    private fun applyMove(next: Game) {
        if (next.isOver) {
            timer.pause()
            val finished = next.copy(elapsedMs = timer.elapsedMs())
            game = finished
            savedGame = null
            storage.clearGame()
            lastRunWasRecord = next.phase == Phase.WON &&
                storage.updateRecord(finished.difficulty, (finished.elapsedMs / 1000).toInt())
            records = storage.loadRecords()
            return
        }

        val withTime = next.copy(elapsedMs = timer.elapsedMs())
        game = withTime
        savedGame = withTime
        persist(withTime)
    }

    fun restartSameDifficulty() {
        val difficulty = game?.difficulty ?: return
        startGame(difficulty)
    }

    fun goToMenu() {
        onScreenHidden()
        screen = Screen.MENU
        records = storage.loadRecords()
    }

    // --- жизненный цикл ---

    /** Экран партии виден: время идёт. */
    fun onScreenVisible() {
        if (screen == Screen.GAME && game?.phase == Phase.PLAYING) timer.resume()
    }

    /**
     * Уход в фон или в меню. Время фиксируется и партия пишется на диск —
     * иначе промежуток между последним ходом и сворачиванием пропал бы,
     * и после перезапуска таймер показал бы меньше, чем игрок видел на экране.
     */
    fun onScreenHidden() {
        timer.pause()
        val current = game ?: return
        if (current.isOver) return
        val withTime = current.copy(elapsedMs = timer.elapsedMs())
        game = withTime
        savedGame = withTime
        persist(withTime)
    }

    private fun persist(value: Game) {
        saver.submit(value)
    }

    override fun onCleared() {
        saver.awaitIdle()
        super.onCleared()
    }
}
