package dev.fedorfalchuk.minesweeper.data

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Последовательная запись вне главного потока со схлопыванием промежуточных
 * состояний: на диск всегда уезжает последнее переданное.
 *
 * Почему один поток: без сериализации две записи, стартовавшие в порядке A, B,
 * могут завершиться в порядке B, A, и в файле останется более старое состояние.
 * Убийство процесса сразу после этого даёт откат на ход назад — воспроизводится
 * редко, отлаживается тяжело.
 */
class AsyncSaver<T>(private val write: (T) -> Unit) {

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "minesweeper-saver").apply { isDaemon = true }
    }

    private val pending = AtomicReference<T?>(null)

    fun submit(value: T) {
        pending.set(value)
        executor.execute {
            // Если предыдущая задача уже забрала это значение, писать нечего.
            val next = pending.getAndSet(null) ?: return@execute
            write(next)
        }
    }

    /** Дождаться, пока очередь опустеет. Нужно тестам и закрытию экрана. */
    fun awaitIdle(timeoutMs: Long = 5_000) {
        val latch = CountDownLatch(1)
        executor.execute { latch.countDown() }
        latch.await(timeoutMs, TimeUnit.MILLISECONDS)
    }
}
