package bible.app.data

enum class BookGroup { LAW, HISTORY, WISDOM, PROPHETS, HISTORY_LATE, GOSPELS, ACTS, CATHOLIC_EPISTLES, PAUL, REVELATION }

data class Book(
    val id: Int,
    val newTestament: Boolean,
    val group: BookGroup,
    val title: String,
    val shortTitle: String,
    val abbr: String,
    val chapters: Int,
    val deuterocanonical: Boolean,
)

data class Verse(val id: Int, val number: Int, val text: String)

data class ChapterRef(val bookId: Int, val chapter: Int)

data class ReadingPosition(val ref: ChapterRef, val verse: Int)

/** Евангелие от Матфея, глава 1 — куда ведёт «Начать чтение». */
val FIRST_GOSPEL_CHAPTER = ChapterRef(bookId = 51, chapter = 1)
