package bible.app.commentary

import android.database.sqlite.SQLiteDatabase
import bible.commentary.model.ChapterNote
import bible.commentary.model.VerseNote
import bible.commentary.model.decodeChapter
import bible.commentary.model.decodeNote
import java.io.File

/** База объяснений стихов (из APK), только чтение. Запросы короткие (по первичному ключу). */
class CommentaryDatabase(file: File) {
    private val db: SQLiteDatabase = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)

    val version: Int = db.rawQuery("SELECT value FROM meta WHERE key='version'", null).use { c -> if (c.moveToFirst()) c.getString(0).toIntOrNull() ?: 0 else 0 }

    fun note(verseId: Int): VerseNote? =
        db.rawQuery("SELECT body FROM verse_note WHERE verse_id=?", arrayOf(verseId.toString())).use { c ->
            if (c.moveToFirst()) decodeNote(c.getBlob(0)) else null
        }

    fun chapter(bookId: Int, chapter: Int): ChapterNote? =
        db.rawQuery("SELECT body FROM chapter_note WHERE book_id=? AND chapter=?", arrayOf(bookId.toString(), chapter.toString())).use { c ->
            if (c.moveToFirst()) decodeChapter(c.getBlob(0)) else null
        }

    /** Адреса источников для ссылки «↗» под толкованием. */
    fun sourceUrl(id: String): String? =
        db.rawQuery("SELECT url FROM source WHERE id=?", arrayOf(id)).use { c -> if (c.moveToFirst()) c.getString(0) else null }

    fun close() = db.close()
}
