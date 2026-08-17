package dev.fedorfalchuk.minesweeper.data

import dev.fedorfalchuk.minesweeper.game.Difficulty
import dev.fedorfalchuk.minesweeper.game.Game
import dev.fedorfalchuk.minesweeper.game.GameCodec
import dev.fedorfalchuk.minesweeper.game.RecordsCodec
import java.io.File

/**
 * Файловое хранилище. Принимает каталог, а не Context: так оно проверяется
 * на JVM во временной папке, без устройства.
 *
 * Партия и рекорды лежат в разных файлах и читаются независимо — порча одного
 * не уносит второе. Нечитаемый файл удаляется при первом же чтении, иначе
 * порча повторялась бы при каждом запуске.
 */
class GameStorage(private val dir: File) {

    companion object {
        const val GAME_FILE = "game.txt"
        const val RECORDS_FILE = "records.txt"
    }

    fun saveGame(game: Game) = writeAtomic(GAME_FILE, GameCodec.encode(game))

    fun loadGame(): Game? {
        val file = File(dir, GAME_FILE)
        if (!file.exists()) return null
        val game = runCatching { GameCodec.decode(file.readText()) }.getOrNull()
        if (game == null) file.delete()
        return game
    }

    fun clearGame() {
        File(dir, GAME_FILE).delete()
    }

    fun saveRecords(records: Map<Difficulty, Int>) =
        writeAtomic(RECORDS_FILE, RecordsCodec.encode(records))

    fun loadRecords(): Map<Difficulty, Int> {
        val file = File(dir, RECORDS_FILE)
        if (!file.exists()) return emptyMap()
        val records = runCatching { RecordsCodec.decode(file.readText()) }.getOrDefault(emptyMap())
        if (records.isEmpty()) file.delete()
        return records
    }

    /** Возвращает true, если время стало новым рекордом. Равное время рекордом не считается. */
    fun updateRecord(difficulty: Difficulty, seconds: Int): Boolean {
        val current = loadRecords()
        val best = current[difficulty]
        if (best != null && seconds >= best) return false
        saveRecords(current + (difficulty to seconds))
        return true
    }

    /**
     * Запись через временный файл и переименование: убийство процесса посреди
     * записи иначе оставило бы обрезанный файл вместо целого.
     */
    private fun writeAtomic(name: String, content: String) {
        if (!dir.exists()) dir.mkdirs()
        val target = File(dir, name)
        val tmp = File(dir, "$name.tmp")
        tmp.writeText(content)
        if (!tmp.renameTo(target)) {
            // Переименование может отказать. Терять состояние из-за этого нельзя,
            // поэтому пишем напрямую — ценой потери атомарности именно в этом случае.
            target.writeText(content)
            tmp.delete()
        }
    }
}
