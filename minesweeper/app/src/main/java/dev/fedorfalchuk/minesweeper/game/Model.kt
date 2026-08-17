package dev.fedorfalchuk.minesweeper.game

/**
 * Уровень сложности. Поля квадратные, поэтому одного размера достаточно.
 * Плотность мин растёт от уровня к уровню — см. SPEC.md, раздел «Уровни».
 */
enum class Difficulty(val size: Int, val mines: Int, val title: String) {
    BEGINNER(9, 10, "Новичок"),
    INTERMEDIATE(12, 20, "Любитель"),
    EXPERT(16, 40, "Эксперт");

    val cellCount: Int get() = size * size
}

/** Фаза партии. NOT_STARTED означает, что мины ещё не расставлены. */
enum class Phase { NOT_STARTED, PLAYING, WON, LOST }

/**
 * Состояние партии целиком. Клетки не хранятся объектами: поле описано
 * множествами индексов, поэтому состояние тривиально сериализуется
 * и сравнивается, а equals достаётся от data class.
 */
data class Game(
    val difficulty: Difficulty,
    val mines: Set<Int>,
    val opened: Set<Int>,
    val flagged: Set<Int>,
    val phase: Phase,
    val elapsedMs: Long,
    val explodedAt: Int?,
) {
    /** Счётчик мин на экране. Может быть отрицательным при перерасходе флажков. */
    val minesLeft: Int get() = difficulty.mines - flagged.size

    val isOver: Boolean get() = phase == Phase.WON || phase == Phase.LOST
}
