package bible.commentary.tool

import bible.commentary.model.ChapterFile
import bible.commentary.model.Father
import bible.commentary.model.VerseNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuotesTest {
    private val corpus = """
        ## Блж. Феофилакт Болгарский
        Выставляет смирение, как основание жизни. Так как Адам пал от гордости, то Христос восстановляет нас через смирение.
        «Нищие   духом» – смиренные.
    """.trimIndent()
    private val pd = mapOf("feofilakt" to true, "modern" to false)
    private fun file(vararg q: Father) = ChapterFile("Мф", 5, verses = mapOf("3" to VerseNote(lead = "x", fathers = q.toList())))

    @Test fun exactQuotePasses() = assertEquals(emptyList<String>(), checkQuotes(file(Father("Феофилакт", "Выставляет смирение, как основание жизни.", "feofilakt")), corpus, pd))
    @Test fun whitespaceQuotesAndYoAreNormalized() =
        assertEquals(emptyList<String>(), checkQuotes(file(Father("Ф", "\"Нищие духом\" – смиренные.", "feofilakt")), corpus, pd))
    @Test fun thinAndNoBreakSpacesCountAsSpaces() =
        assertEquals(emptyList<String>(), checkQuotes(file(Father("Ф", "Иов 2, 9 – 10 и далее", "feofilakt")), "Иов\u00A02,\u20099\u202F–\u200910 и далее", pd))

    @Test fun ellipsisJoinsFragmentsInOrder() =
        assertEquals(emptyList<String>(), checkQuotes(file(Father("Ф", "Так как Адам пал от гордости, … через смирение.", "feofilakt")), corpus, pd))
    @Test fun inventedQuoteFails() =
        assertTrue(checkQuotes(file(Father("Ф", "Смирение есть мать всех добродетелей.", "feofilakt")), corpus, pd).single().contains("не найдена"))
    @Test fun fragmentsOutOfOrderFail() =
        assertTrue(checkQuotes(file(Father("Ф", "через смирение. … Выставляет смирение", "feofilakt")), corpus, pd).single().contains("не найдена"))
    @Test fun longQuoteFromCopyrightedSourceFails() =
        assertTrue(checkQuotes(file(Father("М", "а".repeat(MAX_QUOTE_COPYRIGHTED + 1), "modern")), "а".repeat(1000), pd).any { it.contains("длиннее") })
}
