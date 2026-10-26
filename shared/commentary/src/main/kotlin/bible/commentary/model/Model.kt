package bible.commentary.model

import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json as KJson

/** Формат JSON объяснений: неизвестные поля пропускаются, чтобы старое приложение читало новый пакет. */
val Json: KJson = KJson { ignoreUnknownKeys = true; encodeDefaults = false }

/**
 * Пункт раздела «Смысл», «Контекст» или «Для жизни»: заголовок с засечками и текст под ним.
 * [refs] – места Библии, о которых говорит пункт (карточки под ним); [src] – id источников, на которых он основан.
 */
@Serializable
data class Item(val title: String, val text: String, val refs: List<Ref> = emptyList(), val src: List<String> = emptyList())

/** Ссылка на другое место Библии: стих или диапазон стихов (`to` включительно). */
@Serializable data class Ref(val verse_id: Int, val to: Int? = null)

/** Толкование: дословная цитата, автор, источник (id из таблицы `source`), место в сочинении и адрес страницы. */
@Serializable
data class Father(val author: String, val quote: String, val source: String, val where: String = "", val url: String = "")

/** Статья или материал в интернете о стихе или главе: открывается в браузере. */
@Serializable data class Link(val title: String, val url: String, val site: String)

/**
 * Толкование целиком, дословно (с bible.optina.ru, только общественное достояние): автор, сочинение, абзацы через пустую строку.
 * [see] – вместо текста: автор толкует этот стих вместе с другим (id стиха), текст лежит там.
 */
@Serializable data class Reading(val author: String, val work: String, val text: String, val see: Int? = null)

/** Толкования главы для сборщика: номер стиха строкой → толкования по порядку. */
@Serializable data class ChapterReadings(val book: String, val chapter: Int, val verses: Map<String, List<Reading>>)

/** Объяснение одного стиха. */
@Serializable
data class VerseNote(
    val lead: String = "",
    val meaning: List<Item> = emptyList(),
    val context: List<Item> = emptyList(),
    val fathers: List<Father> = emptyList(),
    val life: List<Item> = emptyList(),
    val links: List<Link> = emptyList(),
)

/** Общее для главы: пункты «Контекст» и ссылки на материалы о главе – показываются у каждого её стиха. */
@Serializable data class ChapterNote(val context: List<Item> = emptyList(), val links: List<Link> = emptyList())

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

fun encodeText(text: String): ByteArray = deflate(text.toByteArray())

fun decodeText(bytes: ByteArray): String = String(inflate(bytes))

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
