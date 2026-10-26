package bible.app.data

import android.database.sqlite.SQLiteDatabase
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BibleDatabase(file: File) {
    private val db: SQLiteDatabase = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)

    val books: List<Book> = db.rawQuery(
        "SELECT id, testament, grp, title, short_title, abbr, chapters, deutero FROM book ORDER BY id",
        null,
    ).use { c ->
        buildList {
            while (c.moveToNext()) {
                add(
                    Book(
                        id = c.getInt(0),
                        newTestament = c.getInt(1) == 1,
                        group = BookGroup.valueOf(c.getString(2)),
                        title = c.getString(3),
                        shortTitle = c.getString(4),
                        abbr = c.getString(5),
                        chapters = c.getInt(6),
                        deuterocanonical = c.getInt(7) == 1,
                    ),
                )
            }
        }
    }

    fun close() = db.close()

    /** Номера всех стихов Библии по порядку (≈ 37 тыс., около 150 КБ в памяти). */
    val verseIds: IntArray by lazy {
        db.rawQuery("SELECT id FROM verse ORDER BY id", null).use { c ->
            IntArray(c.count).also { a -> var i = 0; while (c.moveToNext()) a[i++] = c.getInt(0) }
        }
    }

    /** Один стих по номеру. */
    fun verse(id: Int): Verse? =
        db.rawQuery("SELECT id, verse, text FROM verse WHERE id = ?", arrayOf(id.toString())).use { c ->
            if (c.moveToFirst()) Verse(c.getInt(0), c.getInt(1), c.getString(2)) else null
        }

    suspend fun chapter(ref: ChapterRef): List<Verse> = withContext(Dispatchers.IO) {
        db.rawQuery(
            "SELECT id, verse, text FROM verse WHERE book_id = ? AND chapter = ? ORDER BY verse",
            arrayOf(ref.bookId.toString(), ref.chapter.toString()),
        ).use { c -> buildList { while (c.moveToNext()) add(Verse(c.getInt(0), c.getInt(1), c.getString(2))) } }
    }
}
