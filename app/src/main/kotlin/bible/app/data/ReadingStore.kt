package bible.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("reading")

class ReadingStore(private val context: Context) {
    private val book = intPreferencesKey("book")
    private val chapter = intPreferencesKey("chapter")
    private val verse = intPreferencesKey("verse")
    private val size = intPreferencesKey("text_size")
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

    val textSize: Flow<Int> = context.dataStore.data.map { (it[size] ?: 2).coerceIn(0, 4) }

    suspend fun setTextSize(step: Int) {
        context.dataStore.edit { it[size] = step.coerceIn(0, 4) }
    }

    val lineSpacing: Flow<Int> = context.dataStore.data.map { (it[spacing] ?: 1).coerceIn(0, 2) }

    suspend fun setLineSpacing(step: Int) {
        context.dataStore.edit { it[spacing] = step.coerceIn(0, 2) }
    }
}
