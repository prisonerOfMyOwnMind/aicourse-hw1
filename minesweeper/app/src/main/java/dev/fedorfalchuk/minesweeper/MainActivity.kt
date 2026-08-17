package dev.fedorfalchuk.minesweeper

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.fedorfalchuk.minesweeper.data.GameStorage
import dev.fedorfalchuk.minesweeper.game.Clock
import dev.fedorfalchuk.minesweeper.game.Phase
import dev.fedorfalchuk.minesweeper.ui.GameScreen
import dev.fedorfalchuk.minesweeper.ui.MenuScreen
import dev.fedorfalchuk.minesweeper.ui.MinesweeperTheme
import kotlinx.coroutines.delay
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MinesweeperViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val storage = GameStorage(filesDir)
        val clock = Clock { SystemClock.elapsedRealtime() }

        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    MinesweeperViewModel(storage, clock) as T
            },
        )[MinesweeperViewModel::class.java]

        setContent {
            MinesweeperTheme {
                App(viewModel)
            }
        }
    }

    /**
     * Уход в фон фиксирует время и пишет партию на диск. Именно onStop, а не
     * onDestroy: процесс убивают без предупреждения, и onDestroy может не прийти.
     */
    override fun onStop() {
        viewModel.onScreenHidden()
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        viewModel.onScreenVisible()
    }
}

@Composable
private fun App(viewModel: MinesweeperViewModel) {
    when (viewModel.screen) {
        Screen.MENU -> MenuScreen(
            records = viewModel.records,
            hasSavedGame = viewModel.hasSavedGame,
            confirmingRestart = viewModel.confirmingRestart,
            onContinue = viewModel::continueGame,
            onPick = viewModel::pickDifficulty,
            onConfirmRestart = viewModel::confirmRestart,
            onCancelRestart = viewModel::cancelRestart,
        )

        Screen.GAME -> {
            val game = viewModel.game
            if (game == null) {
                viewModel.goToMenu()
                return
            }

            // Отрисовка таймера обновляется опросом монотонного времени,
            // а не накоплением тиков: тики в фоне соврали бы.
            var displayedMs by remember { mutableLongStateOf(viewModel.elapsedMs()) }
            LaunchedEffect(game.phase) {
                while (game.phase == Phase.PLAYING) {
                    displayedMs = viewModel.elapsedMs()
                    delay(200)
                }
                displayedMs = viewModel.elapsedMs()
            }

            // Системная «Назад» равносильна выходу в меню: партия уже на диске.
            BackHandler { viewModel.goToMenu() }

            GameScreen(
                game = game,
                elapsedMs = if (game.isOver) game.elapsedMs else displayedMs,
                isNewRecord = viewModel.lastRunWasRecord,
                onOpen = viewModel::openCell,
                onFlag = viewModel::toggleFlag,
                onRestart = viewModel::restartSameDifficulty,
                onMenu = viewModel::goToMenu,
            )
        }
    }
}
