package bible.commentary.tool

import bible.commentary.model.ChapterFile
import bible.commentary.model.Father
import bible.commentary.model.Item
import bible.commentary.model.Ref
import bible.commentary.model.VerseNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorTest {
    private val ids = setOf(27002001, 27002002, 56001002, 56001003)
    private val books = mapOf("Сир" to 27, "Иак" to 56)
    private val sources = setOf("lopukhin")
    private val good = VerseNote(lead = "Кратко – суть.", meaning = listOf(Item("«слово»", "смысл")), refs = listOf(Ref(56001002, 56001003)), fathers = listOf(Father("Лопухин", "«цитата»", "lopukhin")))
    private fun check(vararg v: Pair<String, VerseNote>, book: String = "Сир") =
        validate(listOf(ChapterFile(book, 2, verses = mapOf(*v))), ids, books, sources)

    @Test fun goodFilePasses() = assertEquals(emptyList<String>(), check("1" to good))
    @Test fun unknownBook() = assertTrue(check("1" to good, book = "Xyz").single().contains("неизвестная книга"))
    @Test fun missingVerse() = assertTrue(check("40" to good).single().contains("Сир 2:40") )
    @Test fun badRef() = assertTrue(check("1" to good.copy(refs = listOf(Ref(56009099)))).single().contains("ссылка"))
    @Test fun badRangeEnd() = assertTrue(check("1" to good.copy(refs = listOf(Ref(56001002, 56001099)))).single().contains("ссылка"))
    @Test fun longDash() = assertTrue(check("1" to good.copy(lead = "Кратко — суть.")).single().contains("длинное тире"))
    @Test fun longDashInsideItem() = assertTrue(check("1" to good.copy(meaning = listOf(Item("a", "b — c")))).single().contains("длинное тире"))
    @Test fun emptyLead() = assertTrue(check("1" to good.copy(lead = " ")).single().contains("пуст"))
    @Test fun emptyItem() = assertTrue(check("1" to good.copy(context = listOf(Item("", "x")))).single().contains("пуст"))
    @Test fun leadTooLong() = assertTrue(check("1" to good.copy(lead = "а".repeat(301))).single().contains("300"))
    @Test fun unknownSource() = assertTrue(check("1" to good.copy(fathers = listOf(Father("X", "«y»", "nope")))).single().contains("источник"))
}
