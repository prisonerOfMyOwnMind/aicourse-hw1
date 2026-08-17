package dev.fedorfalchuk.minesweeper.game

/**
 * Источник монотонного времени. Ядро не знает про Android, поэтому получает
 * время через этот интерфейс: в приложении его реализует SystemClock,
 * в тестах — управляемое значение.
 */
fun interface Clock {
    fun nowMs(): Long
}

/**
 * Таймер партии. Считает накопленное время плюс разницу монотонных меток,
 * а не тиками раз в секунду: в фоне система усыпляет процесс и уводит в Doze,
 * и счётчик тиков соврал бы.
 */
class GameTimer(private val clock: Clock) {

    private var accumulatedMs = 0L
    private var startMark: Long? = null

    /** Экран партии виден — время идёт. Повторный вызов ничего не меняет. */
    fun resume() {
        if (startMark == null) startMark = clock.nowMs()
    }

    /** Уход в фон или переход в меню — накопленное фиксируется, время стоит. */
    fun pause() {
        val mark = startMark ?: return
        accumulatedMs += clock.nowMs() - mark
        startMark = null
    }

    fun elapsedMs(): Long = accumulatedMs + (startMark?.let { clock.nowMs() - it } ?: 0L)

    /** Восстановление партии с диска: накопленное берётся из файла, время стоит. */
    fun restore(elapsedMs: Long) {
        accumulatedMs = elapsedMs
        startMark = null
    }
}
