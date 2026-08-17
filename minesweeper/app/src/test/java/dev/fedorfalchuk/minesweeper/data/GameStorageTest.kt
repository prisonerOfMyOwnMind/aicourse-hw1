package dev.fedorfalchuk.minesweeper.data

import dev.fedorfalchuk.minesweeper.game.Difficulty
import dev.fedorfalchuk.minesweeper.game.GameCodec
import dev.fedorfalchuk.minesweeper.game.newGame
import dev.fedorfalchuk.minesweeper.game.open
import dev.fedorfalchuk.minesweeper.game.toggleFlag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.random.Random

/**
 * Хранилище принимает каталог, а не Context, поэтому проверяется на JVM
 * во временной папке — без устройства и без Robolectric.
 */
class GameStorageTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun storage(): GameStorage = GameStorage(temp.root)

    private fun playedGame() = newGame(Difficulty.BEGINNER)
        .open(40, Random(9))
        .toggleFlag(0)

    @Test
    fun `на чистом каталоге партии нет и рекордов нет`() {
        val storage = storage()

        assertNull(storage.loadGame())
        assertEquals(emptyMap<Difficulty, Int>(), storage.loadRecords())
    }

    @Test
    fun `партия сохраняется и читается обратно`() {
        val storage = storage()
        val game = playedGame()

        storage.saveGame(game)

        assertEquals(game, storage.loadGame())
    }

    @Test
    fun `повторное сохранение перезаписывает партию`() {
        val storage = storage()
        storage.saveGame(playedGame())
        val second = newGame(Difficulty.EXPERT).toggleFlag(7)

        storage.saveGame(second)

        assertEquals(second, storage.loadGame())
    }

    @Test
    fun `после записи временный файл не остаётся`() {
        val storage = storage()

        storage.saveGame(playedGame())

        val leftovers = temp.root.listFiles()?.filter { it.name.endsWith(".tmp") } ?: emptyList()
        assertTrue("остался временный файл: $leftovers", leftovers.isEmpty())
    }

    @Test
    fun `удаление партии убирает файл`() {
        val storage = storage()
        storage.saveGame(playedGame())

        storage.clearGame()

        assertNull(storage.loadGame())
    }

    @Test
    fun `испорченный файл партии читается как отсутствие и удаляется`() {
        val storage = storage()
        storage.saveGame(playedGame())
        File(temp.root, GameStorage.GAME_FILE).writeText("мусор без ключей")

        assertNull(storage.loadGame())
        assertFalse(
            "нечитаемый файл не удалён, порча повторится при каждом запуске",
            File(temp.root, GameStorage.GAME_FILE).exists()
        )
    }

    @Test
    fun `обрезанный файл партии читается как отсутствие`() {
        val storage = storage()
        val full = GameCodec.encode(playedGame())
        File(temp.root, GameStorage.GAME_FILE).writeText(full.substring(0, full.length / 2))

        assertNull(storage.loadGame())
    }

    @Test
    fun `порча файла партии не уносит рекорды`() {
        val storage = storage()
        storage.saveRecords(mapOf(Difficulty.BEGINNER to 55))
        storage.saveGame(playedGame())

        File(temp.root, GameStorage.GAME_FILE).writeText("сломано")

        assertNull(storage.loadGame())
        assertEquals(mapOf(Difficulty.BEGINNER to 55), storage.loadRecords())
    }

    @Test
    fun `порча файла рекордов не уносит партию`() {
        val storage = storage()
        val game = playedGame()
        storage.saveGame(game)
        storage.saveRecords(mapOf(Difficulty.BEGINNER to 55))

        File(temp.root, GameStorage.RECORDS_FILE).writeText("сломано")

        assertEquals(emptyMap<Difficulty, Int>(), storage.loadRecords())
        assertEquals(game, storage.loadGame())
    }

    @Test
    fun `рекорд обновляется только на лучшее время`() {
        val storage = storage()

        assertTrue(storage.updateRecord(Difficulty.BEGINNER, 100))
        assertFalse("равное время не рекорд", storage.updateRecord(Difficulty.BEGINNER, 100))
        assertFalse("худшее время не рекорд", storage.updateRecord(Difficulty.BEGINNER, 101))
        assertTrue(storage.updateRecord(Difficulty.BEGINNER, 99))

        assertEquals(mapOf(Difficulty.BEGINNER to 99), storage.loadRecords())
    }

    @Test
    fun `рекорды уровней не мешают друг другу`() {
        val storage = storage()

        storage.updateRecord(Difficulty.BEGINNER, 10)
        storage.updateRecord(Difficulty.EXPERT, 900)

        assertEquals(
            mapOf(Difficulty.BEGINNER to 10, Difficulty.EXPERT to 900),
            storage.loadRecords()
        )
    }
}
