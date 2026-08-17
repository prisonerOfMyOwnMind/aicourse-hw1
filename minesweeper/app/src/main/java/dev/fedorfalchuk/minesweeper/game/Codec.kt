package dev.fedorfalchuk.minesweeper.game

/**
 * Сериализация состояния в собственный текстовый формат «ключ=значение».
 *
 * Почему не JSON: встроенный в Android org.json живёт в android.jar и в JVM-тестах
 * подменён заглушками, поэтому тест «записал и прочитал обратно» с ним невозможен
 * без Robolectric или внешней библиотеки, а зависимости запрещены спекой.
 *
 * Чтение строгое: любая несогласованность — отсутствующий ключ, чужая версия,
 * индекс за границами поля, рассогласованный признак расстановки мин — трактуется
 * как отсутствие файла. Приложение при этом не падает, а открывает пустое меню.
 */
object GameCodec {

    private const val VERSION = 1

    fun encode(game: Game): String = buildString {
        appendLine("v=$VERSION")
        appendLine("difficulty=${game.difficulty.name}")
        appendLine("phase=${game.phase.name}")
        appendLine("elapsed=${game.elapsedMs}")
        appendLine("exploded=${game.explodedAt ?: ""}")
        appendLine("minesPlaced=${game.mines.isNotEmpty()}")
        appendLine("mines=${game.mines.sorted().joinToString(",")}")
        appendLine("opened=${game.opened.sorted().joinToString(",")}")
        appendLine("flagged=${game.flagged.sorted().joinToString(",")}")
    }

    fun decode(text: String): Game? {
        val fields = parseFields(text)

        if (fields["v"]?.toIntOrNull() != VERSION) return null

        val difficulty = Difficulty.entries.firstOrNull { it.name == fields["difficulty"] } ?: return null
        val phase = Phase.entries.firstOrNull { it.name == fields["phase"] } ?: return null

        val elapsed = fields["elapsed"]?.toLongOrNull() ?: return null
        if (elapsed < 0) return null

        val explodedRaw = fields["exploded"] ?: return null
        val exploded = when {
            explodedRaw.isEmpty() -> null
            else -> explodedRaw.toIntOrNull()?.takeIf { it in 0 until difficulty.cellCount } ?: return null
        }

        val minesPlaced = when (fields["minesPlaced"]) {
            "true" -> true
            "false" -> false
            else -> return null
        }

        val mines = parseIndices(fields["mines"], difficulty) ?: return null
        val opened = parseIndices(fields["opened"], difficulty) ?: return null
        val flagged = parseIndices(fields["flagged"], difficulty) ?: return null

        // Признак расстановки не источник истины, а контроль целостности файла.
        if (minesPlaced != mines.isNotEmpty()) return null
        if (minesPlaced && mines.size != difficulty.mines) return null
        if (!minesPlaced && phase != Phase.NOT_STARTED) return null

        return Game(
            difficulty = difficulty,
            mines = mines,
            opened = opened,
            flagged = flagged,
            phase = phase,
            elapsedMs = elapsed,
            explodedAt = exploded,
        )
    }

    private fun parseIndices(raw: String?, difficulty: Difficulty): Set<Int>? {
        if (raw == null) return null
        if (raw.isEmpty()) return emptySet()
        val result = HashSet<Int>()
        for (part in raw.split(',')) {
            val value = part.toIntOrNull() ?: return null
            if (value !in 0 until difficulty.cellCount) return null
            result += value
        }
        return result
    }

    internal fun parseFields(text: String): Map<String, String> =
        text.lineSequence()
            .mapNotNull { line ->
                val separator = line.indexOf('=')
                if (separator <= 0) null else line.substring(0, separator) to line.substring(separator + 1)
            }
            .toMap()
}

/**
 * Рекорды. Читаются мягче партии: испорченная строка пропускается, остальные
 * сохраняются. Терять весь список рекордов из-за одной битой строки незачем.
 */
object RecordsCodec {

    private const val VERSION = 1

    fun encode(records: Map<Difficulty, Int>): String = buildString {
        appendLine("v=$VERSION")
        for (difficulty in Difficulty.entries) {
            records[difficulty]?.let { appendLine("${difficulty.name}=$it") }
        }
    }

    fun decode(text: String): Map<Difficulty, Int> {
        val lines = text.lineSequence().toList()
        if (lines.firstOrNull()?.removePrefix("v=")?.toIntOrNull() != VERSION) return emptyMap()

        val result = LinkedHashMap<Difficulty, Int>()
        for (line in lines.drop(1)) {
            val separator = line.indexOf('=')
            if (separator <= 0) continue
            val difficulty = Difficulty.entries.firstOrNull { it.name == line.substring(0, separator) } ?: continue
            val seconds = line.substring(separator + 1).toIntOrNull() ?: continue
            if (seconds >= 0) result[difficulty] = seconds
        }
        return result
    }
}
