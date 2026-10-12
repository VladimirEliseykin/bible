package bible.app.ui.reader

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import bible.app.data.Book
import bible.app.data.Verse
import bible.app.ui.art.artFor
import bible.app.ui.theme.BibleColors
import bible.app.ui.theme.Golos
import bible.app.ui.theme.Literata
import bible.app.ui.toc.label
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * Одна глава: арт-шапка (индекс 0), затем стихи — индекс элемента списка равен номеру стиха.
 * Сохранённый стих восстанавливается один раз за жизнь страницы; [onSettled] получает первый видимый стих,
 * когда страница текущая и прокрутка остановилась — в том числе при возврате на страницу свайпом.
 */
@OptIn(FlowPreview::class)
@Composable
fun ChapterPage(
    book: Book,
    chapter: Int,
    verses: List<Verse>,
    initialVerse: Int,
    textSizeSp: Float,
    lineHeight: Float,
    listState: LazyListState,
    isCurrent: () -> Boolean,
    onArtVisibleChanged: (Boolean) -> Unit,
    onSettled: (Int) -> Unit,
) {
    val context = LocalContext.current
    var restored by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(listState, verses.isNotEmpty()) {
        if (verses.isEmpty()) return@LaunchedEffect
        if (!restored) {
            if (initialVerse > 1) listState.scrollToItem(initialVerse.coerceAtMost(verses.size))
            restored = true
        }
        snapshotFlow { Triple(isCurrent(), listState.isScrollInProgress, listState.firstVisibleItemIndex) }
            .filter { (current, scrolling, _) -> current && !scrolling }
            .debounce(300)
            .collect { (_, _, index) -> onSettled(index.coerceIn(1, verses.size)) }
    }
    val artLimitPx = with(LocalDensity.current) { 150.dp.roundToPx() }
    LaunchedEffect(listState) {
        snapshotFlow { isCurrent() to (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < artLimitPx) }
            .filter { it.first }
            .map { it.second }
            .distinctUntilChanged()
            .collect { onArtVisibleChanged(it) }
    }
    LazyColumn(
        Modifier.fillMaxSize().background(BibleColors.Background).testTag("chapter-list-${book.id}-$chapter"),
        state = listState,
        contentPadding = PaddingValues(bottom = 140.dp),
    ) {
        item(key = "header") {
            Box(Modifier.fillMaxWidth().height(210.dp)) {
                Image(painterResource(artFor(context, book)), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.15f),
                            0.35f to Color.Transparent,
                            0.45f to Color.Transparent,
                            1f to BibleColors.Background,
                        ),
                    ),
                )
                Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 22.dp, vertical = 18.dp)) {
                    Text(
                        book.group.label().uppercase(),
                        fontSize = 11.sp,
                        letterSpacing = 0.12.em,
                        fontWeight = FontWeight.SemiBold,
                        color = BibleColors.Gold,
                    )
                    Text("Глава $chapter", fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, color = BibleColors.Ink)
                }
            }
        }
        items(verses, key = { it.id }) { v ->
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 22.dp).testTag("verse-${v.number}")) {
                Text(
                    "${v.number}",
                    Modifier.widthIn(min = 24.dp).padding(top = 19.dp).testTag("verse-number-${v.number}"),
                    fontFamily = Golos,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = BibleColors.Gold,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                )
                Column(Modifier.padding(start = 8.dp).weight(1f)) {
                    Text(
                        v.text,
                        Modifier.padding(vertical = 14.dp),
                        fontFamily = Literata,
                        fontSize = textSizeSp.sp,
                        lineHeight = (textSizeSp * lineHeight).sp,
                        color = BibleColors.Ink,
                    )
                    HorizontalDivider(thickness = 1.dp, color = BibleColors.Line)
                }
            }
        }
    }
}
