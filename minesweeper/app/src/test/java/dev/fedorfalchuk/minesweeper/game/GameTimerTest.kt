package dev.fedorfalchuk.minesweeper.game

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Таймер на управляемых часах. Реальный SystemClock здесь не нужен и не годится:
 * проверяется именно то, что время не течёт, пока приложение в фоне.
 */
class GameTimerTest {

    private class FakeClock(var now: Long = 0L) : Clock {
        override fun nowMs(): Long = now
    }

    @Test
    fun `до resume время не идёт`() {
        val clock = FakeClock()
        val timer = GameTimer(clock)

        clock.now = 5_000
        assertEquals(0L, timer.elapsedMs())
    }

    @Test
    fun `время считается между resume и pause`() {
        val clock = FakeClock()
        val timer = GameTimer(clock)

        timer.resume()
        clock.now = 3_000
        timer.pause()

        assertEquals(3_000L, timer.elapsedMs())
    }

    @Test
    fun `в фоне время не растёт`() {
        val clock = FakeClock()
        val timer = GameTimer(clock)

        timer.resume()
        clock.now = 2_000
        timer.pause()

        // Минута в фоне: приложение свёрнуто, часы идут, таймер стоять.
        clock.now = 62_000
        assertEquals(2_000L, timer.elapsedMs())
    }

    @Test
    fun `после возврата из фона время продолжается с накопленного`() {
        val clock = FakeClock()
        val timer = GameTimer(clock)

        timer.resume()
        clock.now = 2_000
        timer.pause()
        clock.now = 62_000
        timer.resume()
        clock.now = 63_500

        assertEquals(3_500L, timer.elapsedMs())
    }

    @Test
    fun `повторный resume не сбрасывает накопленное и не удваивает счёт`() {
        val clock = FakeClock()
        val timer = GameTimer(clock)

        timer.resume()
        clock.now = 1_000
        timer.resume()
        clock.now = 2_000

        assertEquals(2_000L, timer.elapsedMs())
    }

    @Test
    fun `restore задаёт накопленное время восстановленной партии`() {
        val clock = FakeClock(now = 100_000)
        val timer = GameTimer(clock)

        timer.restore(7_000)
        assertEquals(7_000L, timer.elapsedMs())

        timer.resume()
        clock.now = 101_500
        assertEquals(8_500L, timer.elapsedMs())
    }

    @Test
    fun `pause без resume ничего не ломает`() {
        val clock = FakeClock()
        val timer = GameTimer(clock)

        timer.pause()
        clock.now = 5_000
        timer.pause()

        assertEquals(0L, timer.elapsedMs())
    }
}
