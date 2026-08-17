package dev.fedorfalchuk.minesweeper.data

import dev.fedorfalchuk.minesweeper.game.Difficulty
import dev.fedorfalchuk.minesweeper.game.Game
import dev.fedorfalchuk.minesweeper.game.newGame
import dev.fedorfalchuk.minesweeper.game.toggleFlag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections

/**
 * Записи на диск обязаны быть сериализованы между собой: иначе быстрая серия
 * тапов может оставить в файле более старое состояние, а убийство процесса
 * сразу после этого даст откат на ход назад.
 */
class AsyncSaverTest {

    @Test
    fun `последним записано последнее переданное состояние`() {
        val written = Collections.synchronizedList(mutableListOf<Game>())
        val saver = AsyncSaver<Game> { written += it }

        var game = newGame(Difficulty.BEGINNER)
        repeat(200) { index ->
            game = game.toggleFlag(index % 81)
            saver.submit(game)
        }
        saver.awaitIdle()

        assertTrue("не записано ничего", written.isNotEmpty())
        assertEquals("на диске осталось не последнее состояние", game, written.last())
    }

    @Test
    fun `записи не выполняются одновременно`() {
        val concurrent = java.util.concurrent.atomic.AtomicInteger()
        val maxSeen = java.util.concurrent.atomic.AtomicInteger()
        val saver = AsyncSaver<Int> {
            val now = concurrent.incrementAndGet()
            maxSeen.updateAndGet { max -> maxOf(max, now) }
            Thread.sleep(1)
            concurrent.decrementAndGet()
        }

        repeat(50) { saver.submit(it) }
        saver.awaitIdle()

        assertEquals("записи шли параллельно", 1, maxSeen.get())
    }

    @Test
    fun `промежуточные состояния схлопываются`() {
        val written = Collections.synchronizedList(mutableListOf<Int>())
        val saver = AsyncSaver<Int> {
            Thread.sleep(2)
            written += it
        }

        repeat(100) { saver.submit(it) }
        saver.awaitIdle()

        assertTrue("схлопывания не произошло: записей ${written.size}", written.size < 100)
        assertEquals(99, written.last())
    }
}
