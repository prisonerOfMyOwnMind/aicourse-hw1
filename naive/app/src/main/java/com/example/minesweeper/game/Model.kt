package com.example.minesweeper.game

/** Пресеты сложности: размер поля и количество мин. */
enum class Difficulty(val rows: Int, val cols: Int, val mines: Int) {
    BEGINNER(9, 9, 10),
    INTERMEDIATE(16, 16, 40),
    EXPERT(16, 30, 99),
}

/** Одна клетка поля. */
data class Cell(
    val isMine: Boolean = false,
    /** Число мин в восьми соседних клетках. Для мины не имеет смысла и равно 0. */
    val adjacent: Int = 0,
    val revealed: Boolean = false,
    val flagged: Boolean = false,
)

enum class GameStatus {
    /** Поле создано, мины ещё не расставлены — ждём первый ход. */
    READY,
    RUNNING,
    WON,
    LOST,
}

/**
 * Полное состояние партии. Неизменяемое: каждый ход возвращает новый объект,
 * поэтому Compose корректно перерисовывает поле.
 *
 * Размеры хранятся числами, а не [Difficulty], — так логика не привязана к
 * пресетам и её удобно проверять на маленьких полях в тестах.
 */
data class GameState(
    val rows: Int,
    val cols: Int,
    val mines: Int,
    val cells: List<Cell>,
    val status: GameStatus,
    /** Индекс мины, на которой игрок подорвался, или -1. */
    val explodedIndex: Int = -1,
) {
    val cellCount: Int get() = rows * cols

    val flagsPlaced: Int get() = cells.count { it.flagged }

    /** Счётчик мин в шапке: может уйти в минус, если наставить лишних флажков. */
    val minesLeft: Int get() = mines - flagsPlaced

    val isFinished: Boolean get() = status == GameStatus.WON || status == GameStatus.LOST

    fun index(row: Int, col: Int): Int = row * cols + col

    fun cellAt(row: Int, col: Int): Cell = cells[index(row, col)]

    /** Флаг, поставленный не на мину, — подсвечивается после проигрыша. */
    fun isWrongFlag(index: Int): Boolean =
        status == GameStatus.LOST && cells[index].flagged && !cells[index].isMine
}
