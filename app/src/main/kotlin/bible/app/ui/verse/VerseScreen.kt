package bible.app.ui.verse

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import bible.app.AppContainer
import bible.app.R
import bible.app.data.BibleDatabase
import bible.app.domain.VerseNavigator
import bible.app.ui.art.ArtImage
import bible.app.ui.art.artFor
import bible.app.ui.common.GoldPill
import bible.app.ui.theme.BibleColors
import bible.app.ui.theme.Literata
import bible.app.ui.theme.StatusBarIcons
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Cream = Color(0xFFFBF6EC)
private val Gold2 = Color(0xFF8A6A2C)
private val GoldLight = Color(0xFFF1DFB4)
private val GlassDark = Color(0xFF12141A)
private val Ponomar = FontFamily(Font(R.font.ponomar))

private val GlassStyle = HazeStyle(
    backgroundColor = GlassDark,
    tint = HazeTint(GlassDark.copy(alpha = 0.42f)),
    blurRadius = 14.dp,
    noiseFactor = 0f,
    fallbackTint = HazeTint(GlassDark.copy(alpha = 0.72f)),
)

/**
 * Страница объяснения стиха. Свайп листает стихи всей Библии; [onBack] получает стих, на котором остановились.
 */
@Composable
fun VerseScreen(
    db: BibleDatabase,
    container: AppContainer,
    startVerseId: Int,
    onBack: (verseId: Int) -> Unit,
    onOpenRef: (verseId: Int) -> Unit,
) {
    val nav = remember(db) { VerseNavigator(db.verseIds) }
    val books = remember(db) { db.books.associateBy { it.id } }
    val pager = rememberPagerState(initialPage = nav.indexOf(startVerseId)) { nav.size }
    val scope = rememberCoroutineScope()
    val current by remember { derivedStateOf { nav.idAt(pager.currentPage) } }
    StatusBarIcons(dark = false)
    BackHandler { onBack(current) }

    HorizontalPager(pager, Modifier.fillMaxSize().background(BibleColors.Background), key = { nav.idAt(it) }, beyondViewportPageCount = 1) { page ->
        val id = nav.idAt(page)
        val data by produceState<VerseData?>(null, id) {
            value = withContext(Dispatchers.IO) { loadVerse(db, books, container.commentary(), id) }
        }
        val d = data ?: return@HorizontalPager
        VersePage(
            d = d,
            prev = nav.prev(id),
            next = nav.next(id),
            onPrev = { scope.launch { pager.animateScrollToPage(page - 1) } },
            onNext = { scope.launch { pager.animateScrollToPage(page + 1) } },
            onBack = { onBack(id) },
            onOpenRef = onOpenRef,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VersePage(
    d: VerseData,
    prev: Int?,
    next: Int?,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onOpenRef: (Int) -> Unit,
) {
    val list = rememberLazyListState()
    val tabs = remember(d) { d.tabs() }
    var tab by rememberSaveable(d.id) { mutableStateOf(tabs.first()) }
    if (tab !in tabs) tab = tabs.first()
    // Плашка нужна, когда стрелки в шапке ушли из вида: шапка почти целиком прокручена.
    val density = LocalDensity.current
    val showBar by remember {
        derivedStateOf {
            val header = list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "header" }
            header == null || list.firstVisibleItemScrollOffset > header.size - with(density) { 72.dp.roundToPx() }
        }
    }
    val minContent = (LocalConfiguration.current.screenHeightDp * 0.8f).dp
    val pill = "${d.book.shortTitle} ${VerseNavigator.label(d.id)}"

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().testTag("verse-list-${d.id}"), state = list, contentPadding = PaddingValues(bottom = 96.dp)) {
            item(key = "header") { Header(d, pill, prev, next, onPrev, onNext, onBack) }
            item(key = "lead") {
                when {
                    d.note == null -> NoteMissing(d.id)
                    else -> Lead(d.note.lead)
                }
            }
            stickyHeader(key = "tabs") { Tabs(d.id, tabs, tab) { tab = it } }
            item(key = "content-${tab.name}") {
                Column(Modifier.fillMaxWidth().heightIn(min = minContent).padding(horizontal = 18.dp, vertical = 14.dp)) {
                    when (tab) {
                        VerseTab.Meaning -> d.note?.meaning.orEmpty().forEachIndexed { i, it -> Point(it.title, it.text, last = i == d.note!!.meaning.lastIndex) }
                        VerseTab.Context -> d.context.forEachIndexed { i, it -> Point(it.title, it.text, last = i == d.context.lastIndex) }
                        VerseTab.Refs -> d.refs.forEach { RefCard(it) { onOpenRef(it.verseId) } }
                        VerseTab.Translations -> {
                            Point("Синодальный перевод", d.synodal, serif = true, last = d.csl == null)
                            d.csl?.let { Point("Церковнославянский", it, family = Ponomar, last = true) }
                        }
                        VerseTab.Fathers -> d.fathers.forEachIndexed { i, f -> FatherPoint(f.author, f.quote, d.sourceUrls[f.source], last = i == d.fathers.lastIndex) }
                    }
                }
            }
        }

        AnimatedVisibility(
            showBar,
            Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(tween(200)),
            exit = slideOutVertically { it } + fadeOut(tween(200)),
        ) { BottomBar(d.id, pill, prev != null, next != null, onPrev, onNext) }
    }
}

@Composable
private fun Header(d: VerseData, pill: String, prev: Int?, next: Int?, onPrev: () -> Unit, onNext: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val haze = remember { HazeState() }
    Box(Modifier.fillMaxWidth()) {
        ArtImage(artFor(context, d.book), Modifier.matchParentSize().hazeSource(haze))
        Column(
            Modifier.fillMaxWidth().heightIn(min = 300.dp).statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassButton("‹ ${d.book.shortTitle} ${VerseNavigator.chapter(d.id)}", haze, Modifier.testTag("back-to-chapter-${d.id}"), onBack)
                Spacer(Modifier.weight(1f))
                ShareButton(d, pill, haze)
            }
            // Зазор не даёт плашке подойти к кнопкам: шапка растёт вниз, а не поверх них.
            Spacer(Modifier.height(28.dp))
            Box(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .hazeEffect(haze, GlassStyle)
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(18.dp))
                        .padding(start = 14.dp, end = 14.dp, top = 22.dp, bottom = 10.dp)
                        .testTag("verse-glass-${d.id}"),
                ) {
                    Text(d.text, Modifier.fillMaxWidth(), color = Color.White, fontFamily = Literata, fontSize = 17.sp, lineHeight = 26.sp, textAlign = TextAlign.Center)
                    HorizontalDivider(Modifier.padding(top = 12.dp), thickness = 1.dp, color = Color.White.copy(alpha = 0.16f))
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.Bottom) {
                        if (prev != null) NavLabel("Предыдущий", "‹ ${VerseNavigator.label(prev)}", Alignment.Start, Modifier.testTag("nav-prev-${d.id}"), onPrev)
                        Spacer(Modifier.weight(1f))
                        if (next != null) NavLabel("Следующий", "${VerseNavigator.label(next)} ›", Alignment.End, Modifier.testTag("nav-next-${d.id}"), onNext)
                    }
                }
                GoldPill(pill, Modifier.align(Alignment.TopCenter).testTag("verse-pill-${d.id}"), elevated = true)
            }
        }
    }
}

@Composable
private fun GlassButton(text: String, haze: HazeState, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(RoundedCornerShape(20.dp))
            .hazeEffect(haze, GlassStyle)
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

@Composable
private fun ShareButton(d: VerseData, pill: String, haze: HazeState) {
    val context = LocalContext.current
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .hazeEffect(haze, GlassStyle)
            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
            .clickable {
                val text = buildString {
                    append(pill).append(" – ").append(d.text)
                    d.note?.lead?.takeIf { it.isNotBlank() }?.let { append("\n\nКратко: ").append(it) }
                }
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                context.startActivity(Intent.createChooser(send, null))
            }
            .testTag("share-${d.id}"),
        contentAlignment = Alignment.Center,
    ) { Icon(painterResource(R.drawable.ic_share), "Поделиться", Modifier.size(18.dp), tint = Color.White) }
}

@Composable
private fun NavLabel(caption: String, label: String, align: Alignment.Horizontal, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.defaultMinSize(minWidth = 88.dp, minHeight = 44.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = align,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(caption.uppercase(), color = GoldLight, fontSize = 10.sp, letterSpacing = 0.06.em, fontWeight = FontWeight.Medium)
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Lead(text: String) {
    Column(Modifier.fillMaxWidth().background(Cream).padding(horizontal = 20.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("КРАТКО", color = Gold2, fontSize = 11.sp, letterSpacing = 0.14.em, fontWeight = FontWeight.SemiBold)
        Text(text, Modifier.padding(top = 6.dp), fontFamily = Literata, fontStyle = FontStyle.Italic, fontSize = 16.sp, lineHeight = 25.sp, color = BibleColors.Ink, textAlign = TextAlign.Center)
        Text("✦   ✦   ✦", Modifier.padding(top = 6.dp), color = BibleColors.Gold, fontSize = 12.sp)
    }
}

@Composable
private fun NoteMissing(id: Int) {
    Box(Modifier.fillMaxWidth().background(Cream).padding(horizontal = 20.dp, vertical = 16.dp).testTag("note-missing-$id"), contentAlignment = Alignment.Center) {
        Text("Объяснение этого стиха ещё готовится", fontFamily = Literata, fontStyle = FontStyle.Italic, fontSize = 15.sp, color = BibleColors.Muted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Tabs(id: Int, tabs: List<VerseTab>, selected: VerseTab, onSelect: (VerseTab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(BibleColors.Background)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
            tabs.forEach { t ->
                val on = t == selected
                Column(
                    Modifier.width(IntrinsicSize.Max).clickable { onSelect(t) }.testTag("tab-${t.tag}-$id").padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        t.title, Modifier.padding(horizontal = 10.dp),
                        fontSize = 14.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (on) BibleColors.Ink else BibleColors.Muted,
                    )
                    Box(Modifier.padding(top = 9.dp).height(2.dp).fillMaxWidth().background(if (on) BibleColors.Gold else Color.Transparent))
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = BibleColors.Line)
    }
}

@Composable
private fun Point(title: String, text: String, last: Boolean, serif: Boolean = false, family: FontFamily? = null) {
    Column(Modifier.fillMaxWidth().padding(bottom = if (last) 0.dp else 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(BibleColors.Gold))
            Text(title, Modifier.padding(start = 8.dp), fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = BibleColors.Ink)
        }
        Text(
            text, Modifier.padding(start = 14.dp, top = 4.dp),
            fontFamily = family ?: if (serif) Literata else null,
            fontSize = if (family != null) 18.sp else 15.sp,
            lineHeight = if (family != null) 28.sp else 24.sp,
            color = BibleColors.Ink,
        )
        if (!last) HorizontalDivider(Modifier.padding(start = 14.dp, top = 14.dp), thickness = 1.dp, color = BibleColors.Line)
    }
}

@Composable
private fun FatherPoint(author: String, quote: String, url: String?, last: Boolean) {
    val uri = LocalUriHandler.current
    var open by rememberSaveable(quote) { mutableStateOf(false) }
    val long = quote.length > 170
    Column(Modifier.fillMaxWidth().padding(bottom = if (last) 0.dp else 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(BibleColors.Gold))
            Text(author, Modifier.padding(start = 8.dp), fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = BibleColors.Ink)
        }
        Text(
            "«$quote»", Modifier.padding(start = 14.dp, top = 4.dp),
            fontFamily = Literata, fontSize = 15.sp, lineHeight = 24.sp, color = BibleColors.Ink,
            maxLines = if (long && !open) 3 else Int.MAX_VALUE, overflow = TextOverflow.Ellipsis,
        )
        if (long && !open) {
            Text("Читать полностью", Modifier.padding(start = 14.dp, top = 6.dp).clickable { open = true }, color = Gold2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        if (url != null) {
            Text("↗ Источник", Modifier.padding(start = 14.dp, top = 6.dp).clickable { uri.openUri(url) }, color = Color(0xFF2F5D8A), fontSize = 13.sp)
        }
        if (!last) HorizontalDivider(Modifier.padding(start = 14.dp, top = 14.dp), thickness = 1.dp, color = BibleColors.Line)
    }
}

@Composable
private fun RefCard(ref: RefView, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(14.dp)).background(BibleColors.Tab).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(ref.label, color = Gold2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(ref.text, Modifier.padding(top = 3.dp), fontFamily = Literata, fontSize = 15.sp, lineHeight = 23.sp, color = BibleColors.Ink)
        }
        Text("›", Modifier.padding(start = 8.dp), fontSize = 20.sp, color = BibleColors.Muted)
    }
}

/** Нижняя плашка — как плашка главы на экране чтения, номер стиха — золотой пилюлей. */
@Composable
private fun BottomBar(id: Int, pill: String, hasPrev: Boolean, hasNext: Boolean, onPrev: () -> Unit, onNext: () -> Unit) {
    Surface(
        Modifier.navigationBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp).fillMaxWidth().heightIn(min = 40.dp).testTag("verse-bottom-bar-$id"),
        shape = RoundedCornerShape(20.dp),
        color = BibleColors.Background,
        shadowElevation = 4.dp,
    ) {
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Arrow("‹", hasPrev, onPrev)
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { GoldPill(pill) }
            Arrow("›", hasNext, onNext)
        }
    }
}

@Composable
private fun Arrow(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.defaultMinSize(36.dp, 36.dp).clip(RoundedCornerShape(18.dp)).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) { Text(symbol, fontSize = 20.sp, color = if (enabled) BibleColors.Ink else BibleColors.Line) }
}
