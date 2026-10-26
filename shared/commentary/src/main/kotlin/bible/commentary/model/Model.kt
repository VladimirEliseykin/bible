package bible.commentary.model

import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json as KJson

/** Формат JSON объяснений: неизвестные поля пропускаются, чтобы старое приложение читало новый пакет. */
val Json: KJson = KJson { ignoreUnknownKeys = true; encodeDefaults = false }

/** Пункт раздела «Смысл» или «Контекст»: заголовок с засечками и текст под ним. */
@Serializable data class Item(val title: String, val text: String)

/** Ссылка на другое место Библии: стих или диапазон стихов (`to` включительно). */
@Serializable data class Ref(val verse_id: Int, val to: Int? = null)

/** Толкование: дословная цитата, автор и источник (id из таблицы `source`). */
@Serializable data class Father(val author: String, val quote: String, val source: String)

/** Объяснение одного стиха. */
@Serializable
data class VerseNote(
    val lead: String = "",
    val meaning: List<Item> = emptyList(),
    val context: List<Item> = emptyList(),
    val refs: List<Ref> = emptyList(),
    val fathers: List<Father> = emptyList(),
)

/** Общее для главы: пункты «Контекст», если у стиха своих нет. */
@Serializable data class ChapterNote(val context: List<Item> = emptyList())

/** Входной файл главы для сборщика пакета: ключ `verses` — номер стиха строкой. */
@Serializable
data class ChapterFile(
    val book: String,
    val chapter: Int,
    val chapter_note: ChapterNote = ChapterNote(),
    val verses: Map<String, VerseNote>,
)

fun encodeNote(note: VerseNote): ByteArray = deflate(Json.encodeToString(VerseNote.serializer(), note).toByteArray())

fun decodeNote(bytes: ByteArray): VerseNote = Json.decodeFromString(VerseNote.serializer(), String(inflate(bytes)))

fun encodeChapter(note: ChapterNote): ByteArray = deflate(Json.encodeToString(ChapterNote.serializer(), note).toByteArray())

fun decodeChapter(bytes: ByteArray): ChapterNote = Json.decodeFromString(ChapterNote.serializer(), String(inflate(bytes)))

private fun deflate(input: ByteArray): ByteArray {
    val d = Deflater(Deflater.BEST_COMPRESSION).apply { setInput(input); finish() }
    val out = ByteArrayOutputStream(input.size / 2 + 16)
    val buf = ByteArray(8192)
    while (!d.finished()) out.write(buf, 0, d.deflate(buf))
    d.end()
    return out.toByteArray()
}

private fun inflate(input: ByteArray): ByteArray {
    val i = Inflater().apply { setInput(input) }
    val out = ByteArrayOutputStream(input.size * 3)
    val buf = ByteArray(8192)
    while (!i.finished()) {
        val n = i.inflate(buf)
        if (n == 0 && (i.needsInput() || i.needsDictionary())) break
        out.write(buf, 0, n)
    }
    i.end()
    return out.toByteArray()
}
