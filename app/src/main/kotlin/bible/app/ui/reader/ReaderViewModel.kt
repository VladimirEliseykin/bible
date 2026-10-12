package bible.app.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bible.app.data.BibleDatabase
import bible.app.data.ChapterRef
import bible.app.data.FIRST_GOSPEL_CHAPTER
import bible.app.data.ReadingPosition
import bible.app.data.ReadingStore
import bible.app.data.Verse
import bible.app.domain.ChapterNavigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

val TEXT_SIZES_SP = listOf(15f, 17f, 19f, 21f, 23f)
val LINE_HEIGHTS = listOf(1.5f, 1.7f, 1.9f)

class ReaderViewModel(private val db: BibleDatabase, private val store: ReadingStore) : ViewModel() {
    val navigator = ChapterNavigator(db.books)

    private val _start = MutableStateFlow<ReadingPosition?>(null)
    val start: StateFlow<ReadingPosition?> = _start

    init {
        viewModelScope.launch {
            _start.value = store.position.first()?.let(navigator::sanitize) ?: ReadingPosition(FIRST_GOSPEL_CHAPTER, 1)
        }
    }

    private val cache = object : LinkedHashMap<ChapterRef, MutableStateFlow<List<Verse>>>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<ChapterRef, MutableStateFlow<List<Verse>>>) = size > 5
    }

    fun chapter(ref: ChapterRef): StateFlow<List<Verse>> = cache.getOrPut(ref) {
        MutableStateFlow<List<Verse>>(emptyList()).also { flow -> viewModelScope.launch { flow.value = db.chapter(ref) } }
    }

    fun onPositionSettled(ref: ChapterRef, firstVisibleVerse: Int) {
        viewModelScope.launch { store.savePosition(ReadingPosition(ref, firstVisibleVerse)) }
    }

    val textSize: StateFlow<Int> = store.textSize.stateIn(viewModelScope, SharingStarted.Eagerly, 2)
    val lineSpacing: StateFlow<Int> = store.lineSpacing.stateIn(viewModelScope, SharingStarted.Eagerly, 1)

    fun setTextSize(step: Int) {
        viewModelScope.launch { store.setTextSize(step) }
    }

    fun setLineSpacing(step: Int) {
        viewModelScope.launch { store.setLineSpacing(step) }
    }
}
