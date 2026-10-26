package bible.commentary.tool

import bible.commentary.model.Reading
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingsTest {
    private val md = corpusText(
        "Мф 5:3",
        "Блаженны нищие духом, ибо их есть Царство Небесное.",
        listOf(
            OptinaSection("Блж. Феофилакт Болгарский", listOf("Блаженны нищие духом, ибо их есть Царствие Небесное", "Выставляет смирение, как основание жизни."), "Толкование на Евангелие от Матфея."),
            OptinaSection("Ориген", listOf("Современный перевод."), "Комментарии на Евангелие от Матфея."),
            OptinaSection("Лопухин А.П.", listOf("Нищие духом – смиренные."), ""),
        ),
    ) + "\nИсточник: https://bible.optina.ru/new:mf:05:03\n"

    private val authors = mapOf(
        "Блж. Феофилакт Болгарский" to OptinaAuthor(true, "Блж. Феофилакт Болгарский"),
        "Ориген" to OptinaAuthor(false, "Ориген"),
        "Лопухин А.П." to OptinaAuthor(true, "А. П. Лопухин"),
    )

    @Test fun readsBackSectionsFromCorpusFile() {
        val (verse, sections) = parseCorpus(md)
        assertEquals("Блаженны нищие духом, ибо их есть Царство Небесное.", verse)
        assertEquals(listOf("Блж. Феофилакт Болгарский", "Ориген", "Лопухин А.П."), sections.map { it.author })
        assertEquals("Толкование на Евангелие от Матфея.", sections[0].work)
        assertEquals(listOf("Нищие духом – смиренные."), sections[2].paragraphs)
    }

    @Test fun keepsOnlyPublicDomainAndDropsRepeatedVerse() {
        val r = readings(md, authors)
        assertEquals(listOf("Блж. Феофилакт Болгарский", "А. П. Лопухин"), r.map { it.author })
        assertEquals("Выставляет смирение, как основание жизни.", r[0].text)
        assertEquals("Толкование на Евангелие от Матфея", r[0].work)
    }

    @Test fun seeAlsoStubBecomesLinkToThatVerse() {
        val stub = corpusText("Мф 5:3", "Блаженны нищие духом", listOf(OptinaSection("Лопухин А.П.", listOf("См. Толкование на Мф. 5:2"), ""))) 
        assertEquals(listOf(Reading("А. П. Лопухин", "", "", see = 51005002)), readings(stub, authors))
    }

    @Test fun seeAlsoVariants() {
        assertEquals(27002001, seeAlso("См. Толкование на Прем. Сир. 2:1"))
        assertEquals(51021034, seeAlso("См. толкование на Мф. 21:34 и Мф. 21:35"))
        assertEquals(52001002, seeAlso("См. Толкование Мк. 1, 2"))
        assertEquals(null, seeAlso("Смирение есть основание жизни."))
    }

    @Test fun unknownAuthorIsLeftOut() = assertEquals(1, readings(md, authors - "Блж. Феофилакт Болгарский").size)
}
