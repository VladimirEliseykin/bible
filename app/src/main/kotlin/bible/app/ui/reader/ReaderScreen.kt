package bible.app.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import bible.app.data.BibleDatabase
import bible.app.data.ReadingPosition
import bible.app.data.ReadingStore
import bible.app.ui.theme.BibleColors
import bible.app.ui.theme.Literata
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(db: BibleDatabase, store: ReadingStore, onOpenToc: () -> Unit) {
    val vm: ReaderViewModel = viewModel(factory = viewModelFactory { initializer { ReaderViewModel(db, store) } })
    val start by vm.start.collectAsState()
    start?.let { Reader(vm, db, it, onOpenToc) }
}

@Composable
private fun Reader(vm: ReaderViewModel, db: BibleDatabase, begin: ReadingPosition, onOpenToc: () -> Unit) {
    val nav = vm.navigator
    val pager = rememberPagerState(initialPage = nav.indexOf(begin.ref)) { nav.totalChapters }
    val scope = rememberCoroutineScope()
    val textSize by vm.textSize.collectAsState()
    val spacing by vm.lineSpacing.collectAsState()
    var chromeVisible by remember { mutableStateOf(true) }
    var showTextSheet by remember { mutableStateOf(false) }
    val hideOnScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -6) chromeVisible = false else if (available.y > 6) chromeVisible = true
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { chromeVisible = true }
    }
    val current = nav.refAt(pager.currentPage)
    val currentBook = db.books.first { it.id == current.bookId }

    Box(Modifier.fillMaxSize().background(BibleColors.Background).nestedScroll(hideOnScroll)) {
        HorizontalPager(pager, Modifier.fillMaxSize().testTag("pager"), beyondViewportPageCount = 1, key = { it }) { page ->
            val ref = nav.refAt(page)
            val book = db.books.first { it.id == ref.bookId }
            val verses by vm.chapter(ref).collectAsState()
            ChapterPage(
                book = book,
                chapter = ref.chapter,
                verses = verses,
                initialVerse = if (ref == begin.ref) begin.verse else 1,
                textSizeSp = TEXT_SIZES_SP[textSize],
                lineHeight = LINE_HEIGHTS[spacing],
                listState = rememberLazyListState(),
                isCurrent = { pager.settledPage == page },
                onSettled = { verse -> vm.onPositionSettled(ref, verse) },
            )
        }

        AnimatedVisibility(
            chromeVisible,
            Modifier.align(Alignment.TopCenter),
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
        ) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Chip("${currentBook.shortTitle} ${current.chapter} ▾", Modifier.weight(1f, fill = false).testTag("chapter-title"), onClick = onOpenToc)
                Spacer(Modifier.width(12.dp))
                Chip("Аа", Modifier.testTag("text-size"), serif = true) { showTextSheet = true }
            }
        }

        AnimatedVisibility(
            chromeVisible,
            Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it * 2 },
            exit = slideOutVertically { it * 2 },
        ) {
            Surface(
                Modifier.padding(horizontal = 14.dp, vertical = 10.dp).fillMaxWidth().heightIn(min = 46.dp),
                shape = RoundedCornerShape(23.dp),
                color = Color.White.copy(alpha = 0.95f),
                shadowElevation = 6.dp,
            ) {
                Row(
                    Modifier.padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    NavArrow("‹", "prev-chapter", enabled = pager.currentPage > 0) {
                        scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
                    }
                    Text(
                        "${currentBook.shortTitle} ${current.chapter}",
                        Modifier.weight(1f).padding(horizontal = 8.dp),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = BibleColors.Ink,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    NavArrow("›", "next-chapter", enabled = pager.currentPage < nav.totalChapters - 1) {
                        scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    }
                }
            }
        }
    }

    if (showTextSheet) {
        TextSizeSheet(textSize, spacing, vm::setTextSize, vm::setLineSpacing) { showTextSheet = false }
    }
}

@Composable
private fun Chip(text: String, modifier: Modifier = Modifier, serif: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier
            .shadow(4.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text,
            color = BibleColors.Ink,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (serif) 17.sp else 15.sp,
            fontFamily = if (serif) Literata else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NavArrow(symbol: String, tag: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.defaultMinSize(38.dp, 38.dp).clip(RoundedCornerShape(19.dp)).clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp).testTag(tag),
        contentAlignment = Alignment.Center,
    ) { Text(symbol, Modifier.testTag("$tag-glyph"), fontSize = 22.sp, color = if (enabled) BibleColors.Ink else BibleColors.Line) }
}
