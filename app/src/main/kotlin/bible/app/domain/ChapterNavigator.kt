package bible.app.domain

import bible.app.data.Book
import bible.app.data.ChapterRef
import bible.app.data.FIRST_GOSPEL_CHAPTER
import bible.app.data.ReadingPosition

class ChapterNavigator(books: List<Book>) {
    private val byId: Map<Int, Book> = books.associateBy { it.id }
    private val startIndex: Map<Int, Int>
    private val refs: List<ChapterRef>

    init {
        val list = ArrayList<ChapterRef>()
        val starts = HashMap<Int, Int>()
        for (b in books) {
            starts[b.id] = list.size
            for (c in 1..b.chapters) list += ChapterRef(b.id, c)
        }
        refs = list
        startIndex = starts
    }

    /** Куда вести, если сохранённая книга неизвестна: Мф 1, а без него — начало Библии. */
    private val fallback: ChapterRef = FIRST_GOSPEL_CHAPTER.takeIf { it.bookId in byId } ?: refs.first()

    val totalChapters: Int get() = refs.size

    fun book(id: Int): Book = byId.getValue(id)

    fun indexOf(ref: ChapterRef): Int = startIndex.getValue(ref.bookId) + ref.chapter - 1

    fun refAt(index: Int): ChapterRef = refs[index]

    fun next(ref: ChapterRef): ChapterRef? = refs.getOrNull(indexOf(ref) + 1)

    fun previous(ref: ChapterRef): ChapterRef? = refs.getOrNull(indexOf(ref) - 1)

    /** Ближайшая существующая позиция: глава за пределами книги → её крайняя глава с начала. */
    fun sanitize(pos: ReadingPosition): ReadingPosition {
        val book = byId[pos.ref.bookId] ?: return ReadingPosition(fallback, 1)
        val chapter = pos.ref.chapter.coerceIn(1, book.chapters)
        if (chapter != pos.ref.chapter) return ReadingPosition(ChapterRef(book.id, chapter), 1)
        return pos.copy(verse = pos.verse.coerceAtLeast(1))
    }
}
