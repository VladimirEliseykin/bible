package bible.app.ui.art

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.DrawableRes
import bible.app.R
import bible.app.data.Book
import bible.app.data.BookGroup

@DrawableRes
fun artForGroup(group: BookGroup): Int = when (group) {
    BookGroup.LAW -> R.drawable.art_group_law
    BookGroup.HISTORY -> R.drawable.art_group_history
    BookGroup.WISDOM -> R.drawable.art_group_wisdom
    BookGroup.PROPHETS -> R.drawable.art_group_prophets
    BookGroup.HISTORY_LATE -> R.drawable.art_group_history_late
    BookGroup.GOSPELS -> R.drawable.art_group_gospels
    BookGroup.ACTS -> R.drawable.art_group_acts
    BookGroup.CATHOLIC_EPISTLES -> R.drawable.art_group_catholic_epistles
    BookGroup.PAUL -> R.drawable.art_group_paul
    BookGroup.REVELATION -> R.drawable.art_group_revelation
}

private val bookArtCache = HashMap<Int, Int>()

/** Есть ли у книги собственный арт (а не арт её группы). */
fun hasOwnArt(context: Context, book: Book): Boolean = artFor(context, book) != artForGroup(book.group)

/** Арт отдельной книги (art_book_<id>), если он добавлен, иначе арт её группы. */
@SuppressLint("DiscouragedApi")
@DrawableRes
fun artFor(context: Context, book: Book): Int = bookArtCache.getOrPut(book.id) {
    context.resources.getIdentifier("art_book_${book.id}", "drawable", context.packageName)
        .takeIf { it != 0 } ?: artForGroup(book.group)
}

// Пока своих артов для карусели нет — показываем обложки Пятикнижия.
val homeCarouselArts: List<Int> = listOf(
    R.drawable.art_book_2,
    R.drawable.art_book_1,
    R.drawable.art_book_4,
    R.drawable.art_book_3,
    R.drawable.art_book_5,
)

/**
 * Широкий арт карточки раздела: art_section_<группа>, если он добавлен,
 * иначе обложка первой книги раздела с собственным артом, иначе арт группы.
 */
@SuppressLint("DiscouragedApi")
@DrawableRes
fun artForSection(context: Context, group: BookGroup, books: List<Book>): Int =
    context.resources.getIdentifier("art_section_${group.name.lowercase()}", "drawable", context.packageName)
        .takeIf { it != 0 }
        ?: books.firstOrNull { hasOwnArt(context, it) }?.let { artFor(context, it) }
        ?: artForGroup(group)
