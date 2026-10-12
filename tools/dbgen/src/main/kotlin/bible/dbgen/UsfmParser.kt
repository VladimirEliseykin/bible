package bible.dbgen

data class ParsedVerse(val chapter: Int, val verse: Int, val text: String)

object UsfmParser {
    private val footnote = Regex("""\\f\s.*?\\f\*""")
    private val altVerse = Regex("""\\va\s.*?\\va\*""")
    private val inlineMarker = Regex("""\\(add|wj|em)\*?""")
    private val paragraphMarker = Regex("""^\\(p|m)\b""")
    private val chapterLine = Regex("""^\\c\s+(\d+)""")
    private val verseStart = Regex("""\\v\s+(\d+)\s*""")
    private val spaces = Regex("""\s+""")

    fun parse(usfm: String): List<ParsedVerse> {
        val result = mutableListOf<ParsedVerse>()
        var chapter = 0
        var verse = 0
        val buffer = StringBuilder()

        fun flush() {
            if (chapter > 0 && verse > 0) {
                val text = buffer.toString().replace(inlineMarker, "").replace(spaces, " ").trim()
                result += ParsedVerse(chapter, verse, text)
            }
            buffer.clear()
        }

        for (raw in usfm.lines()) {
            val line = raw.trim()
            val chapterMatch = chapterLine.find(line)
            if (chapterMatch != null) {
                flush()
                verse = 0
                chapter = chapterMatch.groupValues[1].toInt()
                continue
            }
            if (chapter == 0) continue
            val body = line.replace(footnote, "").replace(altVerse, "").replace(paragraphMarker, "")
            var cursor = 0
            for (m in verseStart.findAll(body)) {
                buffer.append(' ').append(body, cursor, m.range.first)
                flush()
                verse = m.groupValues[1].toInt()
                cursor = m.range.last + 1
            }
            buffer.append(' ').append(body, cursor, body.length)
        }
        flush()
        return result
    }
}
