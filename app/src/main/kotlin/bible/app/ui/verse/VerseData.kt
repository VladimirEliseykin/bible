package bible.app.ui.verse

import bible.app.commentary.CommentaryDatabase
import bible.app.data.BibleDatabase
import bible.app.data.Book
import bible.app.domain.VerseNavigator
import bible.commentary.model.Father
import bible.commentary.model.Item
import bible.commentary.model.Link
import bible.commentary.model.Reading
import bible.commentary.model.VerseNote

/** Ссылка, готовая к показу: «Иакова 1:2–3» и текст стихов. */
data class RefView(val verseId: Int, val label: String, val text: String)

/** Пункт раздела с местами Библии, о которых он говорит. */
data class PointView(val title: String, val text: String, val refs: List<RefView>)

/** Авторы, которые толкуют этот стих вместе с другим: «… толкуют этот стих вместе с Мф 5:2». */
data class SeeAlsoView(val verseId: Int, val label: String, val authors: List<String>)

/** Всё, что показывает страница одного стиха. */
data class VerseData(
    val id: Int,
    val book: Book,
    val text: String,
    val note: VerseNote?,
    val meaning: List<PointView>,
    val context: List<PointView>,
    /** Цитаты толкователей из объяснения, которых нет внутри [readings]. */
    val fathers: List<Father>,
    /** Толкования целиком, дословно. */
    val readings: List<Reading>,
    val seeAlso: List<SeeAlsoView>,
    val life: List<PointView>,
    val links: List<Link>,
    val sourceUrls: Map<String, String>,
)

enum class VerseTab(val title: String, val tag: String) {
    Meaning("Смысл", "meaning"), Context("Контекст", "context"), Fathers("Толкования", "fathers"),
    Life("Для жизни", "life"), Links("Ссылки", "links"),
}

/** Вкладки, в которых есть что показать. */
fun VerseData.tabs(): List<VerseTab> = buildList {
    if (meaning.isNotEmpty()) add(VerseTab.Meaning)
    if (context.isNotEmpty()) add(VerseTab.Context)
    if (fathers.isNotEmpty() || readings.isNotEmpty() || seeAlso.isNotEmpty()) add(VerseTab.Fathers)
    if (life.isNotEmpty()) add(VerseTab.Life)
    if (links.isNotEmpty()) add(VerseTab.Links)
}

fun refLabel(books: Map<Int, Book>, from: Int, to: Int?): String {
    val b = books.getValue(VerseNavigator.book(from)).shortTitle
    val start = VerseNavigator.label(from)
    if (to == null || to == from) return "$b $start"
    return if (VerseNavigator.chapter(to) == VerseNavigator.chapter(from)) "$b $start–${VerseNavigator.verse(to)}" else "$b $start–${VerseNavigator.label(to)}"
}

fun loadVerse(db: BibleDatabase, books: Map<Int, Book>, commentary: CommentaryDatabase, id: Int): VerseData {
    val book = books.getValue(VerseNavigator.book(id))
    val note = commentary.note(id)
    val chapter = commentary.chapter(book.id, VerseNavigator.chapter(id))
    fun point(it: Item) = PointView(it.title, it.text, it.refs.map { r ->
        val end = r.to
        val ids = if (end == null) listOf(r.verse_id) else db.verseIds.filter { v -> v in r.verse_id..end }
        RefView(r.verse_id, refLabel(books, r.verse_id, end), ids.mapNotNull { v -> db.verse(v)?.text }.joinToString(" "))
    })
    val all = commentary.readings(id)
    val readings = all.filter { it.see == null }
    val seeAlso = all.mapNotNull { r -> r.see?.takeIf { db.verse(it) != null }?.let { it to r.author } }.groupBy({ it.first }, { it.second })
        .map { (see, authors) -> SeeAlsoView(see, refLabel(books, see, null), authors.distinct()) }
    // Цитата, которая уже есть внутри полного толкования, не повторяется.
    val full = readings.map { squash(it.text) }
    val fathers = note?.fathers.orEmpty().filter { f -> squash(f.quote.substringBefore('…')).let { q -> full.none { q in it } } }
    return VerseData(
        id = id,
        book = book,
        text = db.verse(id)?.text.orEmpty(),
        note = note,
        meaning = note?.meaning.orEmpty().map(::point),
        context = (note?.context.orEmpty() + chapter?.context.orEmpty()).map(::point),
        fathers = fathers,
        readings = readings,
        seeAlso = seeAlso,
        life = note?.life.orEmpty().map(::point),
        links = (note?.links.orEmpty() + chapter?.links.orEmpty()).distinctBy { it.url },
        sourceUrls = fathers.map { it.source }.distinct().mapNotNull { s -> commentary.sourceUrl(s)?.let { s to it } }.toMap(),
    )
}

private fun squash(s: String) = s.lowercase().replace('ё', 'е').filter { it.isLetterOrDigit() }
