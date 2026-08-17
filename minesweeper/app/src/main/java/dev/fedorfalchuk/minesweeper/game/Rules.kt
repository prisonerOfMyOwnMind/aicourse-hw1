package dev.fedorfalchuk.minesweeper.game

import kotlin.random.Random

/**
 * Правила игры чистыми функциями «состояние → состояние».
 * Здесь нет ни одного импорта android.*, поэтому всё проверяется JVM-тестами.
 */

fun newGame(difficulty: Difficulty): Game = Game(
    difficulty = difficulty,
    mines = emptySet(),
    opened = emptySet(),
    flagged = emptySet(),
    phase = Phase.NOT_STARTED,
    elapsedMs = 0L,
    explodedAt = null,
)

/**
 * Соседи клетки — от трёх в углу до восьми внутри поля.
 * Формулировка «все восемь соседей» верна только для внутренней клетки,
 * и на границе это классический источник выхода за массив.
 */
fun neighbors(size: Int, index: Int): List<Int> {
    val row = index / size
    val col = index % size
    val result = ArrayList<Int>(8)
    for (dRow in -1..1) {
        for (dCol in -1..1) {
            if (dRow == 0 && dCol == 0) continue
            val r = row + dRow
            val c = col + dCol
            if (r in 0 until size && c in 0 until size) result += r * size + c
        }
    }
    return result
}

fun Game.adjacentMines(index: Int): Int =
    neighbors(difficulty.size, index).count { it in mines }

/**
 * Мины расставляются при первом открытии, минуя нажатую клетку и её соседей.
 * Поэтому первый ход всегда раскрывает область, а не одинокую цифру.
 * Генератор передаётся извне: в тестах это фиксированное зерно.
 */
fun placeMines(difficulty: Difficulty, safeIndex: Int, random: Random): Set<Int> {
    val excluded = neighbors(difficulty.size, safeIndex).toSet() + safeIndex
    return (0 until difficulty.cellCount)
        .filter { it !in excluded }
        .shuffled(random)
        .take(difficulty.mines)
        .toSet()
}

/** Короткий тап: копает. */
fun Game.open(index: Int, random: Random): Game {
    if (isOver) return this
    // Клетка под флажком защищена: тап по ней не копает и флажок не снимает.
    if (index in opened || index in flagged) return this

    val started = if (phase == Phase.NOT_STARTED) {
        copy(mines = placeMines(difficulty, index, random), phase = Phase.PLAYING)
    } else {
        this
    }

    if (index in started.mines) {
        return started.copy(
            opened = started.opened + index,
            phase = Phase.LOST,
            explodedAt = index,
        )
    }

    val openedAfter = started.floodFrom(index)
    val safeCells = difficulty.cellCount - started.mines.size
    return if (openedAfter.size == safeCells) {
        // Победа: оставшиеся мины помечаются автоматически, счётчик обнуляется.
        started.copy(opened = openedAfter, flagged = started.mines, phase = Phase.WON)
    } else {
        started.copy(opened = openedAfter)
    }
}

/** Долгий тап: ставит или снимает флажок. Разрешён и до первого копания. */
fun Game.toggleFlag(index: Int): Game {
    if (isOver) return this
    if (index in opened) return this
    return copy(flagged = if (index in flagged) flagged - index else flagged + index)
}

/**
 * Раскрытие области итеративно через явный стек: глубина рекурсии зависела бы
 * от расстановки мин. Клетка с ненулевым числом открывается, но волну
 * дальше не пускает; клетка с флажком не открывается вовсе.
 */
private fun Game.floodFrom(start: Int): Set<Int> {
    val result = opened.toMutableSet()
    val stack = ArrayDeque<Int>()
    stack.addLast(start)
    while (stack.isNotEmpty()) {
        val current = stack.removeLast()
        if (current in result || current in flagged) continue
        result += current
        if (adjacentMines(current) == 0) {
            for (next in neighbors(difficulty.size, current)) {
                if (next !in result && next !in flagged) stack.addLast(next)
            }
        }
    }
    return result
}
