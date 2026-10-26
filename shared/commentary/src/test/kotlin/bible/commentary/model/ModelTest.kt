package bible.commentary.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelTest {
    private val note = VerseNote(
        lead = "Кратко – о главном.",
        meaning = listOf(Item("«искушение»", "испытание", refs = listOf(Ref(56001002, 56001003)), src = listOf("lopukhin"))),
        context = listOf(Item("Кто и когда", "Иисус, сын Сирахов", src = listOf("lopukhin"))),
        fathers = listOf(Father("А. П. Лопухин", "«Решившийся…»", "lopukhin", where = "Сир 2", url = "https://azbyka.ru/x")),
        life = listOf(Item("Не бояться трудностей", "…", src = listOf("lopukhin"))),
        links = listOf(Link("Статья", "https://azbyka.ru/y", "Азбука веры")),
    )

    @Test fun roundTrip() = assertEquals(note, decodeNote(encodeNote(note)))

    @Test fun compresses() {
        val big = note.copy(meaning = List(40) { Item("слово $it", "длинное объяснение ".repeat(10)) })
        assertEquals(true, encodeNote(big).size < Json.encodeToString(VerseNote.serializer(), big).toByteArray().size / 2)
    }

    @Test fun ignoresUnknownFields() {
        val parsed = Json.decodeFromString(VerseNote.serializer(), """{"lead":"x","future":1}""")
        assertEquals("x", parsed.lead)
    }

    @Test fun chapterFileParses() {
        val f = Json.decodeFromString(ChapterFile.serializer(), """{"book":"Сир","chapter":2,"verses":{"1":{"lead":"a"}}}""")
        assertEquals("a", f.verses.getValue("1").lead)
    }
}
