package bible.app.domain

import bible.app.data.Book
import bible.app.data.ChapterRef
import bible.app.data.ReadingPosition

class ChapterNavigator(private val books: List<Book>) {
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

    val totalChapters: Int get() = refs.size

    fun indexOf(ref: ChapterRef): Int = startIndex.getValue(ref.bookId) + ref.chapter - 1

    fun refAt(index: Int): ChapterRef = refs[index]

    fun next(ref: ChapterRef): ChapterRef? = refs.getOrNull(indexOf(ref) + 1)

    fun previous(ref: ChapterRef): ChapterRef? = refs.getOrNull(indexOf(ref) - 1)

    fun sanitize(pos: ReadingPosition): ReadingPosition {
        val book = books.firstOrNull { it.id == pos.ref.bookId } ?: return ReadingPosition(refs.first(), 1)
        if (pos.ref.chapter !in 1..book.chapters) return ReadingPosition(ChapterRef(book.id, 1), 1)
        return pos.copy(verse = pos.verse.coerceAtLeast(1))
    }
}
