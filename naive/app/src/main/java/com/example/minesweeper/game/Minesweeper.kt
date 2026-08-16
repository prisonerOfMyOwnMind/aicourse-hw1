package com.example.minesweeper.game

import kotlin.random.Random

/**
 * Правила игры. Чистые функции: на вход состояние — на выход новое состояние.
 * Никакой зависимости от Android, поэтому логику можно покрывать обычными unit-тестами.
 */
object Minesweeper {

    fun newGame(difficulty: Difficulty): GameState =
        newGame(difficulty.rows, difficulty.cols, difficulty.mines)

    fun newGame(rows: Int, cols: Int, mines: Int): GameState {
        require(rows > 0 && cols > 0) { "Размеры поля должны быть положительными" }
        require(mines in 1 until rows * cols) { "Мин должно быть меньше, чем клеток" }
        return GameState(
            rows = rows,
            cols = cols,
            mines = mines,
            cells = List(rows * cols) { Cell() },
            status = GameStatus.READY,
        )
    }

    /**
     * Открыть клетку. Первый в партии вызов расставляет мины так, чтобы клетка
     * и её соседи были пустыми, — первый ход никогда не проигрышный.
     * Нажатие на уже открытую цифру работает как «аккорд» (см. [chord]).
     */
    fun reveal(state: GameState, index: Int, random: Random = Random.Default): GameState {
        if (state.isFinished) return state
        val cell = state.cells[index]
        if (cell.flagged) return state
        if (cell.revealed) return chord(state, index)

        var next = state
        if (next.status == GameStatus.READY) {
            next = next.copy(
                cells = placeMines(next, index, random),
                status = GameStatus.RUNNING,
            )
        }

        if (next.cells[index].isMine) return explode(next, index)

        val cells = next.cells.toMutableList()
        floodReveal(cells, next.rows, next.cols, index)
        return checkWin(next.copy(cells = cells))
    }

    /** Поставить/снять флажок. Открытые клетки не трогаем. */
    fun toggleFlag(state: GameState, index: Int): GameState {
        if (state.isFinished) return state
        val cell = state.cells[index]
        if (cell.revealed) return state
        val cells = state.cells.toMutableList()
        cells[index] = cell.copy(flagged = !cell.flagged)
        return state.copy(cells = cells)
    }

    /**
     * «Аккорд»: если вокруг открытой цифры выставлено ровно столько флажков,
     * сколько она показывает, открываем все остальные соседние клетки.
     * Ошибся с флажками — подрываешься, это часть механики.
     */
    fun chord(state: GameState, index: Int): GameState {
        if (state.isFinished) return state
        val cell = state.cells[index]
        if (!cell.revealed || cell.isMine || cell.adjacent == 0) return state

        val neighbours = neighbours(index, state.rows, state.cols)
        if (neighbours.count { state.cells[it].flagged } != cell.adjacent) return state

        val targets = neighbours.filter { !state.cells[it].flagged && !state.cells[it].revealed }
        if (targets.isEmpty()) return state

        targets.firstOrNull { state.cells[it].isMine }?.let { return explode(state, it) }

        val cells = state.cells.toMutableList()
        targets.forEach { floodReveal(cells, state.rows, state.cols, it) }
        return checkWin(state.copy(cells = cells))
    }

    /** Индексы восьми соседей клетки с учётом границ поля. */
    fun neighbours(index: Int, rows: Int, cols: Int): List<Int> {
        val row = index / cols
        val col = index % cols
        val result = ArrayList<Int>(8)
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val r = row + dr
                val c = col + dc
                if (r in 0 until rows && c in 0 until cols) result.add(r * cols + c)
            }
        }
        return result
    }

    private fun placeMines(state: GameState, safeIndex: Int, random: Random): List<Cell> {
        val total = state.cellCount
        val mineCount = state.mines

        // Защищаем первый клик вместе с соседями; если мин столько, что места не
        // остаётся (крошечное поле), защищаем только саму клетку.
        var safe = (neighbours(safeIndex, state.rows, state.cols) + safeIndex).toSet()
        if (total - safe.size < mineCount) safe = setOf(safeIndex)

        val candidates = (0 until total).filterTo(ArrayList()) { it !in safe }
        candidates.shuffle(random)
        val mines = candidates.take(mineCount).toHashSet()

        return List(total) { i ->
            if (i in mines) {
                Cell(isMine = true)
            } else {
                Cell(adjacent = neighbours(i, state.rows, state.cols).count { it in mines })
            }
        }
    }

    /**
     * Открывает клетку и, если она пустая, лавиной раскрывает соседей.
     * Итеративно, чтобы не переполнить стек на больших пустых областях.
     */
    private fun floodReveal(cells: MutableList<Cell>, rows: Int, cols: Int, start: Int) {
        val stack = ArrayDeque<Int>()
        stack.addLast(start)
        while (stack.isNotEmpty()) {
            val i = stack.removeLast()
            val cell = cells[i]
            if (cell.revealed || cell.flagged) continue
            cells[i] = cell.copy(revealed = true)
            if (!cell.isMine && cell.adjacent == 0) {
                for (n in neighbours(i, rows, cols)) {
                    if (!cells[n].revealed && !cells[n].flagged) stack.addLast(n)
                }
            }
        }
    }

    private fun explode(state: GameState, index: Int): GameState {
        val cells = state.cells.mapIndexedTo(ArrayList()) { i, cell ->
            when {
                i == index -> cell.copy(revealed = true, flagged = false)
                // Мины под флажками не вскрываем: игрок их угадал, флажок нагляднее.
                cell.isMine && !cell.flagged -> cell.copy(revealed = true)
                else -> cell
            }
        }
        return state.copy(cells = cells, status = GameStatus.LOST, explodedIndex = index)
    }

    /** Победа — когда открыты все клетки без мин. Оставшиеся мины помечаем флажками. */
    private fun checkWin(state: GameState): GameState {
        val won = state.cells.none { !it.isMine && !it.revealed }
        if (!won) return state
        val cells = state.cells.map { if (it.isMine) it.copy(flagged = true) else it }
        return state.copy(cells = cells, status = GameStatus.WON)
    }
}
