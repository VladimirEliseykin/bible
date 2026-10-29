package bible.commentary.tool

import bible.commentary.model.ChapterFile
import bible.commentary.model.ChapterNote
import bible.commentary.model.Father
import bible.commentary.model.Item
import bible.commentary.model.Link
import bible.commentary.model.Ref
import bible.commentary.model.VerseNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorTest {
    private val ids = setOf(27002001, 27002002, 56001002, 56001003)
    private val books = mapOf("Сир" to 27, "Иак" to 56)
    private val sources = setOf("lopukhin")
    private val item = Item("«слово»", "смысл", refs = listOf(Ref(56001002, 56001003)), src = listOf("lopukhin"))
    private val good = VerseNote(
        lead = "Кратко – суть.",
        meaning = listOf(item),
        life = listOf(Item("Терпеть", "Как жить.", src = listOf("lopukhin"))),
        fathers = listOf(Father("Лопухин", "«цитата»", "lopukhin")),
        links = listOf(Link("Статья о стихе", "https://azbyka.ru/x", "Азбука веры")),
    )
    private fun check(vararg v: Pair<String, VerseNote>, book: String = "Сир", chapter: ChapterNote = ChapterNote()) =
        validate(listOf(ChapterFile(book, 2, chapter_note = chapter, verses = mapOf(*v))), ids, books, sources)

    @Test fun goodFilePasses() = assertEquals(emptyList<String>(), check("1" to good))
    @Test fun unknownBook() = assertTrue(check("1" to good, book = "Xyz").single().contains("неизвестная книга"))
    @Test fun missingVerse() = assertTrue(check("40" to good).single().contains("Сир 2:40"))
    @Test fun badRefInsideMeaning() = assertTrue(check("1" to good.copy(meaning = listOf(item.copy(refs = listOf(Ref(56009099)))))).single().contains("ссылка"))
    @Test fun badRangeEnd() = assertTrue(check("1" to good.copy(meaning = listOf(item.copy(refs = listOf(Ref(56001002, 56001099)))))).single().contains("ссылка"))
    @Test fun longDash() = assertTrue(check("1" to good.copy(lead = "Кратко — суть.")).single().contains("длинное тире"))
    @Test fun longDashInsideItem() = assertTrue(check("1" to good.copy(meaning = listOf(item.copy(text = "b — c")))).single().contains("длинное тире"))
    @Test fun longDashInsideLife() = assertTrue(check("1" to good.copy(life = listOf(item.copy(text = "b — c")))).single().contains("длинное тире"))
    @Test fun invisibleCharacters() = assertTrue(check("1" to good.copy(meaning = listOf(item.copy(text = "муж\u200Bу")))).single().contains("невидимый"))
    @Test fun mixedScriptWord() = assertTrue(check("1" to good.copy(meaning = listOf(item.copy(text = "в беснованrepresent ничего")))).single().contains("латиниц"))
    @Test fun emptyLead() = assertTrue(check("1" to good.copy(lead = " ")).single().contains("пуст"))
    @Test fun emptyItem() = assertTrue(check("1" to good.copy(context = listOf(item.copy(title = "")))).single().contains("пуст"))
    @Test fun leadTooLong() = assertTrue(check("1" to good.copy(lead = "а".repeat(301))).single().contains("300"))
    @Test fun unknownFatherSource() = assertTrue(check("1" to good.copy(fathers = listOf(Father("X", "«y»", "nope")))).single().contains("источник"))
    @Test fun itemWithoutSource() = assertTrue(check("1" to good.copy(meaning = listOf(item.copy(src = emptyList())))).single().contains("без источника"))
    @Test fun itemWithUnknownSource() = assertTrue(check("1" to good.copy(life = listOf(item.copy(src = listOf("nope"))))).single().contains("источник"))
    @Test fun linkMustBeHttps() = assertTrue(check("1" to good.copy(links = listOf(Link("a", "http://x.ru", "x")))).single().contains("https"))
    @Test fun linkNeedsTitleAndSite() = assertTrue(check("1" to good.copy(links = listOf(Link("", "https://x.ru", "x")))).single().contains("пуст"))
    @Test fun chapterLinksAndContextAreChecked() {
        val errors = check("1" to good, chapter = ChapterNote(context = listOf(item.copy(src = emptyList())), links = listOf(Link("a", "ftp://x", "x"))))
        assertEquals(2, errors.size)
    }
}
