package bible.commentary.tool

import bible.commentary.model.ChapterFile
import bible.commentary.model.Item
import bible.commentary.model.VerseNote

const val MAX_LEAD = 300

/** Ошибки в файлах глав. Пустой список — пакет можно собирать. */
fun validate(files: List<ChapterFile>, verseIds: Set<Int>, bookIds: Map<String, Int>, sources: Set<String>): List<String> {
    val errors = mutableListOf<String>()
    for (f in files) {
        val book = bookIds[f.book]
        if (book == null) {
            errors += "${f.book} ${f.chapter}: неизвестная книга"
            continue
        }
        f.chapter_note.context.forEachIndexed { i, it -> checkItem("${f.book} ${f.chapter}, контекст главы ${i + 1}", it, errors) }
        for ((num, note) in f.verses) {
            val where = "${f.book} ${f.chapter}:$num"
            val id = num.toIntOrNull()?.let { book * 1_000_000 + f.chapter * 1_000 + it }
            if (id == null || id !in verseIds) {
                errors += "$where: такого стиха нет"
                continue
            }
            checkNote(where, note, verseIds, sources, errors)
        }
    }
    return errors
}

private fun checkNote(where: String, n: VerseNote, ids: Set<Int>, sources: Set<String>, errors: MutableList<String>) {
    if (n.lead.isBlank()) errors += "$where: пустое «Кратко»"
    if (n.lead.length > MAX_LEAD) errors += "$where: «Кратко» длиннее $MAX_LEAD знаков"
    checkDash("$where, кратко", n.lead, errors)
    n.meaning.forEachIndexed { i, it -> checkItem("$where, смысл ${i + 1}", it, errors) }
    n.context.forEachIndexed { i, it -> checkItem("$where, контекст ${i + 1}", it, errors) }
    n.refs.forEach { r ->
        val end = r.to
        val ok = r.verse_id in ids && (end == null || (end in ids && end >= r.verse_id))
        if (!ok) errors += "$where: ссылка ${r.verse_id}${r.to?.let { "–$it" } ?: ""} указывает на несуществующий стих"
    }
    n.fathers.forEachIndexed { i, f ->
        if (f.author.isBlank() || f.quote.isBlank()) errors += "$where, толкование ${i + 1}: пустой автор или цитата"
        if (f.source !in sources) errors += "$where, толкование ${i + 1}: неизвестный источник «${f.source}»"
    }
}

private fun checkItem(where: String, item: Item, errors: MutableList<String>) {
    if (item.title.isBlank() || item.text.isBlank()) errors += "$where: пустой заголовок или текст"
    checkDash(where, item.title + " " + item.text, errors)
}

private fun checkDash(where: String, text: String, errors: MutableList<String>) {
    if ('—' in text) errors += "$where: длинное тире «—», нужно короткое «–»"
}
