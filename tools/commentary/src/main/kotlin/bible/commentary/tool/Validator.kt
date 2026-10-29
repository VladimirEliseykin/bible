package bible.commentary.tool

import bible.commentary.model.ChapterFile
import bible.commentary.model.Item
import bible.commentary.model.Link
import bible.commentary.model.VerseNote

const val MAX_LEAD = 300

/** Невидимые символы, которые попадают в текст при копировании и ломают поиск и переносы. */
private val INVISIBLE = setOf('\u200B', '\u200C', '\u200D', '\u2060', '\uFEFF', '\u00AD')

/** Ошибки в файлах глав. Пустой список — пакет можно собирать. */
fun validate(files: List<ChapterFile>, verseIds: Set<Int>, bookIds: Map<String, Int>, sources: Set<String>): List<String> {
    val errors = mutableListOf<String>()
    for (f in files) {
        val book = bookIds[f.book]
        if (book == null) {
            errors += "${f.book} ${f.chapter}: неизвестная книга"
            continue
        }
        val chapter = Check(verseIds, sources, errors)
        f.chapter_note.context.forEachIndexed { i, it -> chapter.item("${f.book} ${f.chapter}, контекст главы ${i + 1}", it) }
        f.chapter_note.links.forEachIndexed { i, it -> chapter.link("${f.book} ${f.chapter}, ссылка главы ${i + 1}", it) }
        for ((num, note) in f.verses) {
            val where = "${f.book} ${f.chapter}:$num"
            val id = num.toIntOrNull()?.let { book * 1_000_000 + f.chapter * 1_000 + it }
            if (id == null || id !in verseIds) {
                errors += "$where: такого стиха нет"
                continue
            }
            chapter.note(where, note)
        }
    }
    return errors
}

private class Check(val ids: Set<Int>, val sources: Set<String>, val errors: MutableList<String>) {
    fun note(where: String, n: VerseNote) {
        if (n.lead.isBlank()) errors += "$where: пустое «Кратко»"
        if (n.lead.length > MAX_LEAD) errors += "$where: «Кратко» длиннее $MAX_LEAD знаков"
        dash("$where, кратко", n.lead)
        n.meaning.forEachIndexed { i, it -> item("$where, смысл ${i + 1}", it) }
        n.context.forEachIndexed { i, it -> item("$where, контекст ${i + 1}", it) }
        n.life.forEachIndexed { i, it -> item("$where, для жизни ${i + 1}", it) }
        n.fathers.forEachIndexed { i, f ->
            val at = "$where, толкование ${i + 1}"
            if (f.author.isBlank() || f.quote.isBlank()) errors += "$at: пустой автор или цитата"
            if (f.source !in sources) errors += "$at: неизвестный источник «${f.source}»"
            if (f.url.isNotEmpty() && !f.url.startsWith("https://")) errors += "$at: адрес должен начинаться с https://"
        }
        n.links.forEachIndexed { i, it -> link("$where, ссылка ${i + 1}", it) }
    }

    fun item(where: String, item: Item) {
        if (item.title.isBlank() || item.text.isBlank()) errors += "$where: пустой заголовок или текст"
        dash(where, item.title + " " + item.text)
        item.refs.forEach { r ->
            val end = r.to
            val ok = r.verse_id in ids && (end == null || (end in ids && end >= r.verse_id))
            if (!ok) errors += "$where: ссылка ${r.verse_id}${r.to?.let { "–$it" } ?: ""} указывает на несуществующий стих"
        }
        if (item.src.isEmpty()) errors += "$where: пункт без источника"
        item.src.filter { it !in sources }.forEach { errors += "$where: неизвестный источник «$it»" }
    }

    fun link(where: String, l: Link) {
        if (l.title.isBlank() || l.site.isBlank()) errors += "$where: пустое название или сайт"
        if (!l.url.startsWith("https://")) errors += "$where: адрес должен начинаться с https://"
    }

    fun dash(where: String, text: String) {
        if ('—' in text) errors += "$where: длинное тире «—», нужно короткое «–»"
        if (text.any { it in INVISIBLE }) errors += "$where: невидимый символ в тексте (пробел нулевой ширины и т. п.)"
    }
}
