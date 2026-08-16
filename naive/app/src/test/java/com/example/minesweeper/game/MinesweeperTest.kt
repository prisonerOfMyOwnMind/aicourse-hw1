package com.example.minesweeper.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MinesweeperTest {

    /**
     * Собирает поле из текстовой схемы: '*' — мина, любой другой символ — пустая клетка.
     * Числа соседей считаются автоматически, статус — RUNNING (мины уже расставлены).
     */
    private fun board(vararg lines: String): GameState {
        val rows = lines.size
        val cols = lines.first().length
        require(lines.all { it.length == cols }) { "Строки схемы разной длины" }

        val mines = buildSet {
            lines.forEachIndexed { r, line ->
                line.forEachIndexed { c, ch -> if (ch == '*') add(r * cols + c) }
            }
        }
        val cells = List(rows * cols) { i ->
            if (i in mines) {
                Cell(isMine = true)
            } else {
                Cell(adjacent = Minesweeper.neighbours(i, rows, cols).count { it in mines })
            }
        }
        return GameState(rows, cols, mines.size, cells, GameStatus.RUNNING)
    }

    @Test
    fun `новая игра пустая и ждёт первого хода`() {
        val state = Minesweeper.newGame(Difficulty.BEGINNER)

        assertEquals(GameStatus.READY, state.status)
        assertEquals(81, state.cells.size)
        assertEquals(10, state.minesLeft)
        assertTrue(state.cells.none { it.isMine || it.revealed || it.flagged })
    }

    @Test
    fun `первый ход никогда не попадает на мину`() {
        repeat(200) { seed ->
            val start = Minesweeper.newGame(Difficulty.INTERMEDIATE)
            val index = seed % start.cellCount
            val after = Minesweeper.reveal(start, index, Random(seed))

            assertFalse("seed=$seed", after.status == GameStatus.LOST)
            assertTrue("seed=$seed", after.cells[index].revealed)
            // Соседи первого хода тоже свободны, поэтому открывается целая область.
            assertEquals("seed=$seed", 0, after.cells[index].adjacent)
            assertEquals("seed=$seed", 40, after.cells.count { it.isMine })
        }
    }

    @Test
    fun `открытие пустой клетки лавиной раскрывает область`() {
        val state = board(
            "*...",
            "....",
            "....",
        )
        val mine = state.index(0, 0)

        val after = Minesweeper.reveal(state, state.index(2, 3))

        assertTrue(after.cells.filterIndexed { i, _ -> i != mine }.all { it.revealed })
        assertFalse(after.cells[mine].revealed)
    }

    @Test
    fun `лавина останавливается на цифрах`() {
        val state = board(
            "....",
            ".*..",
            "....",
        )

        val after = Minesweeper.reveal(state, state.index(2, 3))

        // Кольцо цифр вокруг мины открылось, а угол за ним — нет.
        assertTrue(after.cellAt(2, 2).revealed)
        assertFalse(after.cellAt(0, 0).revealed)
        assertEquals(GameStatus.RUNNING, after.status)
    }

    @Test
    fun `клетка с цифрой открывается одна`() {
        val state = board(
            "..*",
            "...",
            "*..",
        )

        val after = Minesweeper.reveal(state, state.index(1, 1))

        assertEquals(2, after.cellAt(1, 1).adjacent)
        assertEquals(1, after.cells.count { it.revealed })
        assertEquals(GameStatus.RUNNING, after.status)
    }

    @Test
    fun `победа проставляет флажки на все мины`() {
        val state = board(
            "*...",
            "....",
            "....",
        )

        val after = Minesweeper.reveal(state, state.index(2, 3))

        assertEquals(GameStatus.WON, after.status)
        assertTrue(after.cellAt(0, 0).flagged)
        assertEquals(0, after.minesLeft)
    }

    @Test
    fun `подрыв вскрывает остальные мины и запоминает точку взрыва`() {
        val state = board(
            "*..",
            "...",
            "..*",
        )
        val flagged = Minesweeper.toggleFlag(state, state.index(2, 2))

        val after = Minesweeper.reveal(flagged, state.index(0, 0))

        assertEquals(GameStatus.LOST, after.status)
        assertEquals(state.index(0, 0), after.explodedIndex)
        assertTrue(after.cellAt(0, 0).revealed)
        // Угаданную мину оставляем под флажком — так нагляднее.
        assertFalse(after.cellAt(2, 2).revealed)
        assertTrue(after.cellAt(2, 2).flagged)
    }

    @Test
    fun `ошибочный флажок подсвечивается после проигрыша`() {
        val state = board(
            "*..",
            "...",
            "...",
        )
        val flagged = Minesweeper.toggleFlag(state, state.index(2, 2))

        val after = Minesweeper.reveal(flagged, state.index(0, 0))

        assertTrue(after.isWrongFlag(state.index(2, 2)))
        assertFalse(after.isWrongFlag(state.index(0, 0)))
    }

    @Test
    fun `флажок защищает клетку от открытия`() {
        val state = board(
            "*..",
            "...",
            "...",
        )
        val flagged = Minesweeper.toggleFlag(state, state.index(0, 0))

        val after = Minesweeper.reveal(flagged, state.index(0, 0))

        assertEquals(GameStatus.RUNNING, after.status)
        assertFalse(after.cellAt(0, 0).revealed)
        assertEquals(0, after.minesLeft)
    }

    @Test
    fun `флажок нельзя поставить на открытую клетку`() {
        val state = board(
            "..*",
            "...",
            "*..",
        )
        val opened = Minesweeper.reveal(state, state.index(1, 1))

        val after = Minesweeper.toggleFlag(opened, state.index(1, 1))

        assertEquals(0, after.flagsPlaced)
    }

    @Test
    fun `аккорд открывает соседей когда флажки расставлены верно`() {
        val state = board(
            ".*.",
            "...",
            "...",
        )
        val opened = Minesweeper.reveal(state, state.index(1, 1))
        val flagged = Minesweeper.toggleFlag(opened, state.index(0, 1))

        // Повторный тап по открытой цифре — это и есть аккорд.
        val after = Minesweeper.reveal(flagged, state.index(1, 1))

        assertEquals(GameStatus.WON, after.status)
    }

    @Test
    fun `аккорд с неверным флажком приводит к взрыву`() {
        val state = board(
            ".*.",
            "...",
            "...",
        )
        val opened = Minesweeper.reveal(state, state.index(1, 1))
        val flagged = Minesweeper.toggleFlag(opened, state.index(0, 0))

        val after = Minesweeper.reveal(flagged, state.index(1, 1))

        assertEquals(GameStatus.LOST, after.status)
        assertEquals(state.index(0, 1), after.explodedIndex)
    }

    @Test
    fun `аккорд без нужного числа флажков ничего не делает`() {
        val state = board(
            ".*.",
            "...",
            "...",
        )
        val opened = Minesweeper.reveal(state, state.index(1, 1))

        val after = Minesweeper.reveal(opened, state.index(1, 1))

        assertEquals(opened, after)
    }

    @Test
    fun `после конца партии ходы игнорируются`() {
        val state = board(
            "*..",
            "...",
            "...",
        )
        val lost = Minesweeper.reveal(state, state.index(0, 0))

        assertEquals(lost, Minesweeper.reveal(lost, state.index(2, 2)))
        assertEquals(lost, Minesweeper.toggleFlag(lost, state.index(2, 2)))
    }

    @Test
    fun `на крошечном поле мины расставляются даже без свободной зоны`() {
        // 2x2 и 3 мины: защитить соседей невозможно, но первый ход всё равно безопасен.
        val start = Minesweeper.newGame(rows = 2, cols = 2, mines = 3)

        val after = Minesweeper.reveal(start, 0, Random(1))

        assertEquals(3, after.cells.count { it.isMine })
        assertFalse(after.cells[0].isMine)
        assertEquals(GameStatus.WON, after.status)
    }
}
