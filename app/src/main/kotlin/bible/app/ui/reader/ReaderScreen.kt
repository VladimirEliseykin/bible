package bible.app.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import bible.app.data.BibleDatabase
import bible.app.data.ReadingPosition
import bible.app.data.ReadingStore
import bible.app.data.TEXT_SIZES_SP
import bible.app.ui.common.BottomTabsHeight
import bible.app.ui.common.Chip
import bible.app.ui.theme.BibleColors
import bible.app.ui.theme.StatusBarIcons
import kotlinx.coroutines.launch

private val PanelAnim = tween<Float>(durationMillis = 220)

/**
 * Экран чтения. [onImmersiveChange] сообщает, что пользователь читает (шапка и вкладки спрятаны):
 * вкладки рисует AppRoot, экран под ними не сжимается, поэтому текст не перестраивается.
 */
@Composable
fun ReaderScreen(
    db: BibleDatabase,
    store: ReadingStore,
    onImmersiveChange: (Boolean) -> Unit = {},
    onOpenToc: () -> Unit,
) {
    val vm: ReaderViewModel = viewModel(factory = viewModelFactory { initializer { ReaderViewModel(db, store) } })
    val start by vm.start.collectAsState()
    start?.let { Reader(vm, it, onImmersiveChange, onOpenToc) }
}

@Composable
private fun Reader(vm: ReaderViewModel, begin: ReadingPosition, onImmersiveChange: (Boolean) -> Unit, onOpenToc: () -> Unit) {
    val nav = vm.navigator
    val pager = rememberPagerState(initialPage = nav.indexOf(begin.ref)) { nav.totalChapters }
    val scope = rememberCoroutineScope()
    val textSizeSp by vm.textSizeSp.collectAsState()
    val spacing by vm.lineSpacing.collectAsState()
    var chromeVisible by remember { mutableStateOf(true) }
    var atEnd by remember { mutableStateOf(false) }
    var artVisible by remember { mutableStateOf(true) }
    var showTextSheet by remember { mutableStateOf(false) }
    val immersiveCallback by rememberUpdatedState(onImmersiveChange)
    StatusBarIcons(dark = !artVisible)

    val hideOnScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -4) chromeVisible = false else if (available.y > 4) chromeVisible = true
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(Unit) { snapshotFlow { chromeVisible }.collect { immersiveCallback(!it) } }
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect {
            chromeVisible = true
            atEnd = false
        }
    }
    val current = nav.refAt(pager.currentPage)
    val currentBook = nav.book(current.bookId)
    val barVisible = chromeVisible || atEnd

    // Плашка главы стоит над вкладками, когда они видны, и опускается к краю, когда спрятаны.
    val density = LocalDensity.current
    val navBottom = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val barLift by animateDpAsState(
        targetValue = if (chromeVisible) BottomTabsHeight + 1.dp + navBottom else navBottom,
        animationSpec = tween(220),
        label = "barLift",
    )

    Box(Modifier.fillMaxSize().background(BibleColors.Background).nestedScroll(hideOnScroll)) {
        HorizontalPager(pager, Modifier.fillMaxSize().testTag("pager"), beyondViewportPageCount = 1, key = { it }) { page ->
            val ref = nav.refAt(page)
            val content by remember(ref) { vm.chapter(ref) }.collectAsState()
            if (content == ChapterContent.Failed) {
                ChapterFailed { vm.retry(ref) }
                return@HorizontalPager
            }
            ChapterPage(
                book = nav.book(ref.bookId),
                chapter = ref.chapter,
                verses = (content as? ChapterContent.Loaded)?.verses.orEmpty(),
                initialVerse = if (ref == begin.ref) begin.verse else 1,
                textSizeSp = textSizeSp,
                lineHeight = LINE_HEIGHTS[spacing],
                listState = rememberLazyListState(),
                isCurrent = { pager.settledPage == page },
                onArtVisibleChanged = { artVisible = it },
                onAtEndChanged = { atEnd = it },
                onSettled = { verse -> vm.onPositionSettled(ref, verse) },
            )
        }

        if (!artVisible) {
            Box(
                Modifier.align(Alignment.TopCenter).fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(BibleColors.Background.copy(alpha = 0.94f)),
            )
        }

        AnimatedVisibility(
            chromeVisible,
            Modifier.align(Alignment.TopCenter),
            enter = slideInVertically { -it } + fadeIn(PanelAnim),
            exit = slideOutVertically { -it } + fadeOut(PanelAnim),
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
            barVisible,
            Modifier.align(Alignment.BottomCenter).offset { IntOffset(0, -barLift.roundToPx()) },
            enter = slideInVertically { it } + fadeIn(PanelAnim),
            exit = slideOutVertically { it } + fadeOut(PanelAnim),
        ) {
            Surface(
                Modifier.padding(horizontal = 14.dp, vertical = 8.dp).fillMaxWidth().heightIn(min = 40.dp).testTag("chapter-bar"),
                shape = RoundedCornerShape(20.dp),
                color = BibleColors.Background,
                shadowElevation = 4.dp,
            ) {
                Row(
                    Modifier.padding(horizontal = 4.dp),
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
                        fontSize = 14.sp,
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
        TextSizeSheet(
            size = TEXT_SIZES_SP.indexOf(textSizeSp).coerceAtLeast(0),
            spacing = spacing,
            onSize = { vm.setTextSizeSp(TEXT_SIZES_SP[it]) },
            onSpacing = vm::setLineSpacing,
        ) { showTextSheet = false }
    }
}

@Composable
private fun NavArrow(symbol: String, tag: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.defaultMinSize(36.dp, 36.dp).clip(RoundedCornerShape(18.dp)).clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp).testTag(tag),
        contentAlignment = Alignment.Center,
    ) { Text(symbol, Modifier.testTag("$tag-glyph"), fontSize = 20.sp, color = if (enabled) BibleColors.Ink else BibleColors.Line) }
}

@Composable
private fun ChapterFailed(onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp).testTag("chapter-failed"),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Не удалось открыть главу.", fontSize = 16.sp, color = BibleColors.Ink, textAlign = TextAlign.Center)
        Text(
            "Повторить",
            Modifier.clip(RoundedCornerShape(20.dp)).background(BibleColors.Tab).clickable(onClick = onRetry)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            fontWeight = FontWeight.SemiBold,
            color = BibleColors.Ink,
        )
    }
}
