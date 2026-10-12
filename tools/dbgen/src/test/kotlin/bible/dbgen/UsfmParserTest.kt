package bible.dbgen

import org.junit.Assert.assertEquals
import org.junit.Test

class UsfmParserTest {
    @Test fun parsesChaptersAndVerses() {
        val usfm = """
            \id MAT
            \mt Евангелие
            \c 1
            \p \v 1 Первый.
            \v 2 Второй.
            \c 2
            \p \v 1 Третий.
        """.trimIndent()
        assertEquals(
            listOf(ParsedVerse(1, 1, "Первый."), ParsedVerse(1, 2, "Второй."), ParsedVerse(2, 1, "Третий.")),
            UsfmParser.parse(usfm),
        )
    }

    @Test fun ignoresIntroductionBeforeFirstChapter() {
        val usfm = "\\ip Предисловие \\v 9 не стих\n\\c 1\n\\p \\v 1 Текст."
        assertEquals(listOf(ParsedVerse(1, 1, "Текст.")), UsfmParser.parse(usfm))
    }

    @Test fun removesFootnotesAndKeepsAddedWords() {
        val usfm = "\\c 21\n\\v 6 пред богов\\f + \\fr 21.6 \\ft Т. е. пред судей. \\fv Пс 81\\fv* \\f* и \\add поставит\\add* его."
        assertEquals("пред богов и поставит его.", UsfmParser.parse(usfm).single().text)
    }

    @Test fun removesWordsOfJesusMarkersAndEmphasis() {
        val usfm = "\\c 3\n\\p \\v 15 Иисус сказал: \\wj оставь теперь.\\wj* Тогда \\em он\\em* допускает."
        assertEquals("Иисус сказал: оставь теперь. Тогда он допускает.", UsfmParser.parse(usfm).single().text)
    }

    @Test fun lineWithoutVerseContinuesPreviousVerseAndDropsAltNumber() {
        val usfm = "\\c 13\n\\v 13 Начало.\n\\p \\va 14\\va* [Вставка.]\n\\p \\v 14 \\va 15\\va* Следующий."
        assertEquals(
            listOf(ParsedVerse(13, 13, "Начало. [Вставка.]"), ParsedVerse(13, 14, "Следующий.")),
            UsfmParser.parse(usfm),
        )
    }

    @Test fun collapsesWhitespace() {
        assertEquals("а б", UsfmParser.parse("\\c 1\n\\v 1   а \t  б  ").single().text)
    }

    @Test fun removesNestedMarkersInsideWordsOfJesus() {
        val usfm = "\\c 5\n\\v 12 \\wj так гнали \\+add и\\+add* пророков\\wj*"
        assertEquals("так гнали и пророков", UsfmParser.parse(usfm).single().text)
    }
}
