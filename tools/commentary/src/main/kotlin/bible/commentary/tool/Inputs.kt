package bible.commentary.tool

import bible.commentary.model.ChapterFile
import bible.commentary.model.ChapterNote
import bible.commentary.model.Json
import java.io.File
import java.sql.DriverManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

/** Источник из sources.json; [public_domain] разрешает длинные цитаты. */
@Serializable
data class SourceJson(val id: String, val title: String, val author: String, val url: String, val license: String, val public_domain: Boolean = false)

/** Входные данные сборки: стихи и книги из bible.db, источники, файлы глав. */
class Inputs(bibleDb: File, notesDir: File, sourcesFile: File) {
    val verseIds = HashSet<Int>()
    val bookIds = HashMap<String, Int>()
    val sources: List<SourceJson> = Json.decodeFromString(ListSerializer(SourceJson.serializer()), sourcesFile.readText())
    val files: List<ChapterFile>

    init {
        DriverManager.getConnection("jdbc:sqlite:${bibleDb.path}").use { c ->
            c.createStatement().use { s ->
                s.executeQuery("SELECT id FROM verse").use { r -> while (r.next()) verseIds += r.getInt(1) }
                s.executeQuery("SELECT id, abbr FROM book").use { r -> while (r.next()) bookIds[r.getString(2)] = r.getInt(1) }
            }
        }
        files = notesDir.walkTopDown().filter { it.isFile && it.extension == "json" }.sorted()
            .map { Json.decodeFromString(ChapterFile.serializer(), it.readText()) }.toList()
    }

    fun errors(): List<String> {
        val dup = sources.groupBy { it.id }.filter { it.value.size > 1 }.keys.map { "sources.json: источник «$it» повторяется" }
        val byChapter = files.groupBy { it.book to it.chapter }
        val twice = byChapter.flatMap { (k, fs) ->
            fs.flatMap { it.verses.keys }.groupBy { it }.filter { it.value.size > 1 }.keys.map { "${k.first} ${k.second}:$it: стих описан в нескольких файлах" } +
                listOfNotNull("${k.first} ${k.second}: общее для главы задано в нескольких файлах".takeIf { fs.count { f -> f.chapter_note != ChapterNote() } > 1 })
        }
        return dup + twice + validate(files, verseIds, bookIds, sources.map { it.id }.toSet())
    }
}
