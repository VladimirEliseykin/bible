package bible.commentary.tool

import bible.commentary.model.ChapterFile

/** Цитата из источника под авторским правом – только короткая (право цитирования, ст. 1274 ГК РФ). */
const val MAX_QUOTE_COPYRIGHTED = 400

/** Цитата из общественного достояния – не длиннее, чтобы страница оставалась объяснением, а не перепечаткой. */
const val MAX_QUOTE = 1200

/**
 * Цитаты толкователей должны дословно найтись в корпусе главы. Сравнение без учёта пробелов, вида кавычек, тире и «ё»;
 * многоточие «…» разделяет фрагменты, которые ищутся по порядку.
 * [publicDomain] – id источника → общественное достояние ли он.
 */
fun checkQuotes(file: ChapterFile, corpus: String, publicDomain: Map<String, Boolean>): List<String> {
    val haystack = normalize(corpus)
    val errors = mutableListOf<String>()
    for ((num, note) in file.verses) {
        note.fathers.forEachIndexed { i, f ->
            val where = "${file.book} ${file.chapter}:$num, толкование ${i + 1} (${f.author})"
            val limit = if (publicDomain[f.source] == true) MAX_QUOTE else MAX_QUOTE_COPYRIGHTED
            if (f.quote.length > limit) errors += "$where: цитата длиннее $limit знаков"
            if (!found(haystack, f.quote)) errors += "$where: цитата не найдена в источниках дословно"
        }
    }
    return errors
}

private fun found(haystack: String, quote: String): Boolean {
    var from = 0
    for (part in quote.split('…').map(::normalize).map { it.trim(' ', '.', ',', ';', ':') }.filter { it.isNotEmpty() }) {
        val at = haystack.indexOf(part, from)
        if (at < 0) return false
        from = at + part.length
    }
    return true
}

private fun normalize(s: String): String = s
    .replace('ё', 'е').replace('Ё', 'Е')
    .replace(Regex("[«»„“”\"]"), "\"")
    .replace(Regex("[—–‑-]"), "-")
    .replace(Regex("[\\s\\u00A0\\u2007\\u2009\\u200A\\u202F]+"), " ")
    .trim()
