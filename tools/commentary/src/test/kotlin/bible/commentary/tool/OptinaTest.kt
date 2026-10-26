package bible.commentary.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OptinaTest {
    private val html = """
        <div class="toc"><h3>Содержание</h3><ul><li><a href="#lopuxin_ap">Лопухин А.П.</a></li></ul></div>
        <h1 class="sectionedit1" id="t">Толкования на Прем. Сираха 2:1</h1>
        <div class="level1"></div>
        <h2 class="sectionedit2" id="prav_ioann_kronshtadtskij">Прав. Иоанн Кронштадтский</h2>
        <div class="level2">
        <p><strong>Сын мой! если ты приступаешь</strong></p>
        <p>То есть испытай себя – есть ли у тебя <em>живая вера</em>? <a href="/old:prsir:18:23">(Сир. 18,23)</a>.</p>
        <p><strong><em>Дневник. Том III.</em></strong></p>
        </div>
        <h2 class="sectionedit4" id="lopuxin_ap">Лопухин А.П.</h2>
        <div class="level2"><p>Решившийся   служить Богу.</p></div>
    """.trimIndent()

    @Test fun parsesAuthorsParagraphsAndWorks() {
        val s = parseOptina(html)
        assertEquals(listOf("Прав. Иоанн Кронштадтский", "Лопухин А.П."), s.map { it.author })
        assertEquals("То есть испытай себя – есть ли у тебя живая вера? (Сир. 18,23).", s[0].paragraphs[1])
        assertEquals("Дневник. Том III.", s[0].work)
        assertEquals(listOf("Решившийся служить Богу."), s[1].paragraphs)
        assertEquals("", s[1].work)
    }

    @Test fun missingPageHasNoSections() = assertTrue(parseOptina("<h1>Эта страница ещё не существует</h1>").isEmpty())

    @Test fun corpusFileIsReadable() {
        val text = corpusText("Сир 2:1", "Сын мой!", parseOptina(html))
        assertTrue(text.startsWith("# Сир 2:1\n\n> Сын мой!\n\n## Прав. Иоанн Кронштадтский\n_Дневник. Том III._\n\n"))
        assertTrue("## Лопухин А.П.\n\nРешившийся служить Богу." in text)
    }
}

class PageTextTest {
    @Test fun keepsAllBlocksAsParagraphsWithoutMenus() {
        val doc = org.jsoup.Jsoup.parse("<nav>Меню</nav><div class='a'>Блок <b>один</b></div><div><span>Блок два</span><br>строка</div><script>x()</script>")
        assertEquals("Блок один\n\nБлок два\n\nстрока", pageText(doc))
    }
}
