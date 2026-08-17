package dev.fedorfalchuk.minesweeper.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Тесты правил игры. Ядро не зависит от Android, поэтому всё гоняется на JVM
 * за секунды — это и есть то доказательство, которое показывается выводом команды.
 */
class RulesTest {

    // --- соседи ---

    @Test
    fun `у внутренней клетки восемь соседей`() {
        assertEquals(8, neighbors(9, index = 4 * 9 + 4).size)
    }

    @Test
    fun `у угловой клетки три соседа`() {
        assertEquals(3, neighbors(9, index = 0).size)
        assertEquals(3, neighbors(9, index = 8).size)
        assertEquals(3, neighbors(9, index = 80).size)
    }

    @Test
    fun `у краевой клетки пять соседей`() {
        assertEquals(5, neighbors(9, index = 4).size)
    }

    @Test
    fun `соседи не выходят за границы поля`() {
        for (i in 0 until 81) {
            assertTrue("клетка $i", neighbors(9, i).all { it in 0 until 81 })
        }
    }

    // --- первый тап ---

    @Test
    fun `первый тап никогда не мина и открывает больше одной клетки`() {
        // По всем уровням, по угловой, краевой и внутренней клетке, с фиксированным зерном.
        for (difficulty in Difficulty.entries) {
            val n = difficulty.size
            val probes = listOf(0, n / 2, n * n - 1, n * (n / 2) + n / 2)
            for (first in probes) {
                for (seed in 1..25) {
                    val game = newGame(difficulty).open(first, Random(seed))
                    assertFalse(
                        "${difficulty.name} seed=$seed клетка=$first: мина под первым тапом",
                        first in game.mines
                    )
                    assertTrue(
                        "${difficulty.name} seed=$seed клетка=$first: соседи первого тапа заминированы",
                        neighbors(n, first).none { it in game.mines }
                    )
                    assertTrue(
                        "${difficulty.name} seed=$seed клетка=$first: открылась одна клетка",
                        game.opened.size > 1
                    )
                    assertEquals(difficulty.mines, game.mines.size)
                }
            }
        }
    }

    @Test
    fun `до первого тапа мин на поле нет`() {
        val game = newGame(Difficulty.BEGINNER)
        assertTrue(game.mines.isEmpty())
        assertEquals(Phase.NOT_STARTED, game.phase)
    }

    // --- раскрытие области ---

    @Test
    fun `волна не открывает клетку с флажком и не снимает его`() {
        val game = newGame(Difficulty.BEGINNER)
            .toggleFlag(1)
            .open(0, Random(7))

        assertTrue("флажок снят волной", 1 in game.flagged)
        assertFalse("клетка с флажком открыта волной", 1 in game.opened)
    }

    @Test
    fun `короткий тап по клетке с флажком ничего не делает`() {
        val started = newGame(Difficulty.BEGINNER).open(40, Random(3))
        val target = (0 until 81).first { it !in started.opened && it !in started.mines }
        val flagged = started.toggleFlag(target)

        val after = flagged.open(target, Random(3))

        assertEquals("клетка под флажком открылась", flagged.opened, after.opened)
        assertEquals(flagged.flagged, after.flagged)
        assertEquals(flagged.phase, after.phase)
    }

    @Test
    fun `короткий тап по открытой клетке ничего не меняет`() {
        val game = newGame(Difficulty.BEGINNER).open(40, Random(5))
        val opened = game.opened.first()

        assertEquals(game, game.open(opened, Random(5)))
    }

    // --- исход партии ---

    @Test
    fun `открытие мины проигрывает партию и запоминает клетку подрыва`() {
        val started = newGame(Difficulty.BEGINNER).open(0, Random(11))
        val mine = started.mines.first()

        val lost = started.open(mine, Random(11))

        assertEquals(Phase.LOST, lost.phase)
        assertEquals(mine, lost.explodedAt)
    }

    @Test
    fun `после поражения ходы игнорируются`() {
        val started = newGame(Difficulty.BEGINNER).open(0, Random(11))
        val lost = started.open(started.mines.first(), Random(11))
        val safe = (0 until 81).first { it !in lost.mines && it !in lost.opened }

        assertEquals(lost, lost.open(safe, Random(11)))
        assertEquals(lost, lost.toggleFlag(safe))
    }

    @Test
    fun `открытие всех безопасных клеток даёт победу и расставляет флажки на мины`() {
        var game = newGame(Difficulty.BEGINNER).open(40, Random(13))
        for (i in 0 until 81) {
            if (i !in game.mines) game = game.open(i, Random(13))
        }

        assertEquals(Phase.WON, game.phase)
        assertEquals("флажки расставлены не на все мины", game.mines, game.flagged)
        assertEquals("счётчик мин не обнулился", 0, game.minesLeft)
        assertNull(game.explodedAt)
    }

    // --- флажки и счётчик ---

    @Test
    fun `флажок ставится и снимается долгим тапом`() {
        val game = newGame(Difficulty.BEGINNER)

        assertTrue(3 in game.toggleFlag(3).flagged)
        assertFalse(3 in game.toggleFlag(3).toggleFlag(3).flagged)
    }

    @Test
    fun `флажок можно ставить до первого копания и таймер при этом не стартует`() {
        val game = newGame(Difficulty.BEGINNER).toggleFlag(5)

        assertTrue(5 in game.flagged)
        assertEquals(Phase.NOT_STARTED, game.phase)
        assertTrue(game.mines.isEmpty())
    }

    @Test
    fun `флажок на открытую клетку не ставится`() {
        val game = newGame(Difficulty.BEGINNER).open(40, Random(17))
        val opened = game.opened.first()

        assertEquals(game, game.toggleFlag(opened))
    }

    @Test
    fun `счётчик мин уходит в минус при перерасходе флажков`() {
        var game = newGame(Difficulty.BEGINNER)
        for (i in 0 until 12) game = game.toggleFlag(i)

        assertEquals(10 - 12, game.minesLeft)
    }

    @Test
    fun `флажок в исключаемой области остаётся на месте после расстановки мин`() {
        val game = newGame(Difficulty.BEGINNER)
            .toggleFlag(1)
            .open(0, Random(23))

        assertTrue(1 in game.flagged)
    }

    // --- инварианты уровней ---

    @Test
    fun `на каждом уровне мин меньше числа клеток за вычетом области первого тапа`() {
        for (d in Difficulty.entries) {
            assertTrue(d.name, d.mines <= d.size * d.size - 9)
        }
    }

    @Test
    fun `плотность мин растёт от уровня к уровню`() {
        val densities = Difficulty.entries.map { it.mines.toDouble() / (it.size * it.size) }
        assertEquals(densities.sorted(), densities)
    }
}
