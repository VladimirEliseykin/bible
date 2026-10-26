package bible.app.ui.verse

import bible.app.commentary.CommentaryDatabase
import bible.app.data.BibleDatabase
import bible.app.data.Book
import bible.app.domain.VerseNavigator
import bible.commentary.model.Father
import bible.commentary.model.Item
import bible.commentary.model.VerseNote

/** Ссылка, готовая к показу: «Иакова 1:2–3» и текст стихов. */
data class RefView(val verseId: Int, val label: String, val text: String)

/** Всё, что показывает страница одного стиха. */
data class VerseData(
    val id: Int,
    val book: Book,
    val text: String,
    val packInstalled: Boolean,
    val note: VerseNote?,
    val context: List<Item>,
    val refs: List<RefView>,
    val synodal: String,
    val csl: String?,
    val fathers: List<Father>,
    val sourceUrls: Map<String, String>,
)

enum class VerseTab(val title: String, val tag: String) {
    Meaning("Смысл", "meaning"), Context("Контекст", "context"), Refs("Ссылки", "refs"),
    Translations("Переводы", "translations"), Fathers("Отцы", "fathers"),
}

/** Вкладки, в которых есть что показать; «Переводы» есть всегда. */
fun VerseData.tabs(): List<VerseTab> = buildList {
    if (!note?.meaning.isNullOrEmpty()) add(VerseTab.Meaning)
    if (context.isNotEmpty()) add(VerseTab.Context)
    if (refs.isNotEmpty()) add(VerseTab.Refs)
    add(VerseTab.Translations)
    if (fathers.isNotEmpty()) add(VerseTab.Fathers)
}

fun refLabel(books: Map<Int, Book>, from: Int, to: Int?): String {
    val b = books.getValue(VerseNavigator.book(from)).shortTitle
    val start = VerseNavigator.label(from)
    if (to == null || to == from) return "$b $start"
    return if (VerseNavigator.chapter(to) == VerseNavigator.chapter(from)) "$b $start–${VerseNavigator.verse(to)}" else "$b $start–${VerseNavigator.label(to)}"
}

fun loadVerse(db: BibleDatabase, books: Map<Int, Book>, pack: CommentaryDatabase?, id: Int): VerseData {
    val book = books.getValue(VerseNavigator.book(id))
    val text = db.verse(id)?.text.orEmpty()
    val note = pack?.note(id)
    val chapterContext = pack?.chapter(book.id, VerseNavigator.chapter(id))?.context.orEmpty()
    val refs = note?.refs.orEmpty().map { r ->
        val ids = if (r.to == null) listOf(r.verse_id) else db.verseIds.filter { it in r.verse_id..r.to!! }
        RefView(r.verse_id, refLabel(books, r.verse_id, r.to), ids.mapNotNull { db.verse(it)?.text }.joinToString(" "))
    }
    val fathers = note?.fathers.orEmpty()
    return VerseData(
        id = id,
        book = book,
        text = text,
        packInstalled = pack != null,
        note = note,
        context = note?.context.orEmpty() + chapterContext,
        refs = refs,
        synodal = text,
        csl = pack?.translation(id, "csl"),
        fathers = fathers,
        sourceUrls = fathers.map { it.source }.distinct().mapNotNull { s -> pack?.sourceUrl(s)?.let { s to it } }.toMap(),
    )
}
