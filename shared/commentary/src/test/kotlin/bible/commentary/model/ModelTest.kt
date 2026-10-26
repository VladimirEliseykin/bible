package bible.commentary.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelTest {
    private val note = VerseNote(
        lead = "Кратко – о главном.",
        meaning = listOf(Item("«искушение»", "испытание")),
        context = listOf(Item("Кто и когда", "Иисус, сын Сирахов")),
        refs = listOf(Ref(56001002, 56001003)),
        fathers = listOf(Father("А. П. Лопухин", "«Решившийся…»", "lopukhin")),
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
