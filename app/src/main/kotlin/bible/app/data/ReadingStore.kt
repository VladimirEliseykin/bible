package bible.app.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(
    name = "reading",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

class ReadingStore(private val context: Context) {
    private val book = intPreferencesKey("book")
    private val chapter = intPreferencesKey("chapter")
    private val verse = intPreferencesKey("verse")
    private val legacySize = intPreferencesKey("text_size")
    private val sizeSp = floatPreferencesKey("text_size_sp")
    private val spacing = intPreferencesKey("line_spacing")

    val position: Flow<ReadingPosition?> = context.dataStore.data.map { p ->
        val b = p[book] ?: return@map null
        ReadingPosition(ChapterRef(b, p[chapter] ?: 1), p[verse] ?: 1)
    }

    suspend fun savePosition(pos: ReadingPosition) {
        context.dataStore.edit {
            it[book] = pos.ref.bookId
            it[chapter] = pos.ref.chapter
            it[verse] = pos.verse
        }
    }

    suspend fun clearPosition() {
        context.dataStore.edit {
            it.remove(book)
            it.remove(chapter)
            it.remove(verse)
        }
    }

    val textSizeSp: Flow<Float> = context.dataStore.data.map { resolveTextSizeSp(it[sizeSp], it[legacySize]) }

    suspend fun setTextSizeSp(sp: Float) {
        context.dataStore.edit {
            it[sizeSp] = resolveTextSizeSp(sp, null)
            it.remove(legacySize)
        }
    }

    val lineSpacing: Flow<Int> = context.dataStore.data.map { (it[spacing] ?: 1).coerceIn(0, 2) }

    suspend fun setLineSpacing(step: Int) {
        context.dataStore.edit { it[spacing] = step.coerceIn(0, 2) }
    }
}
