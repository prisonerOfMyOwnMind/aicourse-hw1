package dev.fedorfalchuk.minesweeper.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Сериализация. Формат собственный текстовый именно для того, чтобы эти тесты
 * шли на голом JVM: org.json живёт в android.jar и в unit-тестах подменён заглушками.
 */
class CodecTest {

    private fun playedGame(): Game =
        newGame(Difficulty.INTERMEDIATE)
            .open(70, Random(42))
            .toggleFlag(0)
            .toggleFlag(1)

    @Test
    fun `партия переживает запись и чтение без потерь`() {
        val game = playedGame()

        val restored = GameCodec.decode(GameCodec.encode(game))

        assertEquals(game, restored)
    }

    @Test
    fun `партия без расставленных мин восстанавливается`() {
        val game = newGame(Difficulty.EXPERT).toggleFlag(5)

        val restored = GameCodec.decode(GameCodec.encode(game))

        assertEquals(game, restored)
        assertEquals(Phase.NOT_STARTED, restored?.phase)
        assertTrue(restored?.mines?.isEmpty() == true)
    }

    @Test
    fun `проигранная партия сохраняет клетку подрыва`() {
        val started = newGame(Difficulty.BEGINNER).open(40, Random(3))
        val lost = started.open(started.mines.first(), Random(3))

        val restored = GameCodec.decode(GameCodec.encode(lost))

        assertEquals(lost, restored)
        assertEquals(lost.explodedAt, restored?.explodedAt)
    }

    @Test
    fun `накопленное время переживает запись и чтение`() {
        val game = playedGame().copy(elapsedMs = 123_456L)

        assertEquals(123_456L, GameCodec.decode(GameCodec.encode(game))?.elapsedMs)
    }

    @Test
    fun `битый текст читается как отсутствие партии`() {
        assertNull(GameCodec.decode("это вообще не наш файл"))
    }

    @Test
    fun `пустой текст читается как отсутствие партии`() {
        assertNull(GameCodec.decode(""))
    }

    @Test
    fun `обрезанный файл читается как отсутствие партии`() {
        val full = GameCodec.encode(playedGame())
        val truncated = full.substring(0, full.length / 2)

        assertNull(GameCodec.decode(truncated))
    }

    @Test
    fun `неизвестная версия формата читается как отсутствие партии`() {
        val text = GameCodec.encode(playedGame()).replace("v=1", "v=99")

        assertNull(GameCodec.decode(text))
    }

    @Test
    fun `чужой уровень сложности читается как отсутствие партии`() {
        val text = GameCodec.encode(playedGame()).replace("difficulty=INTERMEDIATE", "difficulty=INSANE")

        assertNull(GameCodec.decode(text))
    }

    @Test
    fun `нечисловое время читается как отсутствие партии`() {
        val text = GameCodec.encode(playedGame()).replace("elapsed=0", "elapsed=скоро")

        assertNull(GameCodec.decode(text))
    }

    @Test
    fun `индекс за границами поля читается как отсутствие партии`() {
        val game = newGame(Difficulty.BEGINNER).toggleFlag(3)
        val text = GameCodec.encode(game).replace("flagged=3", "flagged=999")

        assertNull(GameCodec.decode(text))
    }

    @Test
    fun `рассогласованный признак расстановки мин читается как отсутствие партии`() {
        val game = newGame(Difficulty.BEGINNER)
        val text = GameCodec.encode(game).replace("minesPlaced=false", "minesPlaced=true")

        assertNull(GameCodec.decode(text))
    }

    @Test
    fun `рекорды переживают запись и чтение`() {
        val records = mapOf(Difficulty.BEGINNER to 42, Difficulty.EXPERT to 301)

        assertEquals(records, RecordsCodec.decode(RecordsCodec.encode(records)))
    }

    @Test
    fun `пустые рекорды читаются как пустая карта`() {
        assertEquals(emptyMap<Difficulty, Int>(), RecordsCodec.decode(RecordsCodec.encode(emptyMap())))
    }

    @Test
    fun `битые рекорды читаются как пустая карта а не как исключение`() {
        assertEquals(emptyMap<Difficulty, Int>(), RecordsCodec.decode("мусор\nи ещё мусор"))
        assertEquals(emptyMap<Difficulty, Int>(), RecordsCodec.decode(""))
    }

    @Test
    fun `в рекордах пропускается только испорченная строка`() {
        val text = "v=1\nBEGINNER=42\nEXPERT=нет\n"

        assertEquals(mapOf(Difficulty.BEGINNER to 42), RecordsCodec.decode(text))
    }
}
