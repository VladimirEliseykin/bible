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

/**
 * Где по высоте главный сюжет арта (0 — верх, 1 — низ). Узкие шапки (лист глав, глава, карусель)
 * обрезают картинку вокруг этой точки. Не указан — середина.
 */
val ART_FOCUS_Y: Map<String, Float> = mapOf(
    "art_book_1" to 0.55f, // Эдем: древо и лев
    "art_book_2" to 0.76f, // Исход: народ на дне моря
    "art_book_3" to 0.72f, // Левит: скиния и священник
    "art_book_4" to 0.48f, // Числа: медный змий и Моисей
    "art_book_5" to 0.66f, // Второзаконие: Моисей над долиной
    "art_book_6" to 0.67f, // Иисус Навин: стены и ковчег
    "art_book_7" to 0.66f, // Судьи: Гедеон и руно
    "art_book_8" to 0.66f, // Руфь в поле
    "art_book_9" to 0.5f, // Давид и Голиаф
    "art_book_10" to 0.6f, // ковчег идёт в Иерусалим
    "art_book_11" to 0.55f, // храм Соломона
    "art_book_12" to 0.36f, // огненная колесница
    "art_book_13" to 0.64f, // левиты перед скинией
    "art_book_14" to 0.56f, // храм и огонь на жертвеннике
    "art_book_15" to 0.62f, // основание храма
    "art_book_16" to 0.6f, // стены и строители
    "art_book_17" to 0.7f, // караван
    "art_book_18" to 0.66f, // Товия и Рафаил
    "art_book_19" to 0.62f, // Ветилуя и две жены
    "art_book_20" to 0.62f, // Есфирь перед троном
    "art_book_21" to 0.62f, // Иов на пепелище под бурей
    "art_book_22" to 0.6f, // Давид с арфой под звёздами
    "art_book_23" to 0.52f, // дом Премудрости о семи столбах
    "art_book_24" to 0.62f, // царь осенью над долиной
    "art_book_25" to 0.62f, // сад запертый и серна
    "art_book_26" to 0.55f, // Соломон молится о премудрости
    "art_book_27" to 0.6f, // сын Сирахов учит юношей
    "art_book_29" to 0.55f, // дом горшечника
    "art_book_30" to 0.6f, // пророк над руинами
    "art_book_31" to 0.55f, // идолы с птицами
    "art_book_32" to 0.62f, // Варух читает у реки
    "art_book_33" to 0.6f, // оживающие кости
    "art_book_34" to 0.6f, // Даниил среди львов
)
