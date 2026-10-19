package bible.app.ui.reader

import bible.app.data.ChapterRef
import bible.app.data.Verse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ChapterContent {
    data object Loading : ChapterContent
    data class Loaded(val verses: List<Verse>) : ChapterContent
    data object Failed : ChapterContent
}

/**
 * Загруженные главы для пейджера. Держит [capacity] последних глав — с запасом больше,
 * чем страниц одновременно на экране и рядом с ним, чтобы видимая глава не вытеснялась.
 */
class ChapterCache(
    private val scope: CoroutineScope,
    private val capacity: Int = 9,
    private val load: suspend (ChapterRef) -> List<Verse>,
) {
    private val entries = object : LinkedHashMap<ChapterRef, MutableStateFlow<ChapterContent>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<ChapterRef, MutableStateFlow<ChapterContent>>) = size > capacity
    }

    fun chapter(ref: ChapterRef): StateFlow<ChapterContent> =
        entries[ref] ?: MutableStateFlow<ChapterContent>(ChapterContent.Loading).also {
            entries[ref] = it
            fetch(ref, it)
        }

    fun retry(ref: ChapterRef) {
        val flow = entries[ref] ?: return
        flow.value = ChapterContent.Loading
        fetch(ref, flow)
    }

    private fun fetch(ref: ChapterRef, flow: MutableStateFlow<ChapterContent>) {
        scope.launch {
            flow.value = try {
                ChapterContent.Loaded(load(ref))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ChapterContent.Failed
            }
        }
    }
}
