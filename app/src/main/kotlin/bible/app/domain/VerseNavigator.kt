package bible.app.domain

/** Все стихи Библии по порядку: соседние стихи через границы глав и книг. */
class VerseNavigator(private val ids: IntArray) {
    val size: Int get() = ids.size

    fun indexOf(id: Int): Int = ids.binarySearch(id).also { require(it >= 0) { "нет стиха $id" } }

    fun idAt(index: Int): Int = ids[index]

    fun prev(id: Int): Int? = ids.getOrNull(indexOf(id) - 1)

    fun next(id: Int): Int? = ids.getOrNull(indexOf(id) + 1)

    /** Номер стиха в главе и число стихов в ней: «стих 2 из 3». */
    fun positionInChapter(id: Int): Pair<Int, Int> {
        val chapter = id / 1_000
        val i = indexOf(id)
        var first = i
        while (first > 0 && ids[first - 1] / 1_000 == chapter) first--
        var last = i
        while (last < ids.lastIndex && ids[last + 1] / 1_000 == chapter) last++
        return (i - first + 1) to (last - first + 1)
    }

    companion object {
        fun book(id: Int) = id / 1_000_000
        fun chapter(id: Int) = id / 1_000 % 1_000
        fun verse(id: Int) = id % 1_000
        fun label(id: Int) = "${chapter(id)}:${verse(id)}"
    }
}
