package bible.app.ui.toc

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bible.app.data.BibleDatabase
import bible.app.data.Book
import bible.app.data.BookGroup
import bible.app.data.ChapterRef
import bible.app.data.ReadingPosition
import bible.app.data.ReadingStore
import bible.app.domain.ChapterNavigator
import bible.app.domain.booksLabel
import bible.app.domain.chaptersLabel
import bible.app.ui.art.ArtImage
import bible.app.ui.art.artFor
import bible.app.ui.art.artForSection
import bible.app.ui.art.hasOwnArt
import bible.app.ui.common.Chip
import bible.app.ui.theme.BibleColors
import bible.app.ui.theme.Literata
import bible.app.ui.theme.StatusBarIcons
import kotlinx.coroutines.launch

/** Подпись группы на экране чтения (над «Глава N»). */
internal fun BookGroup.label(): String = when (this) {
    BookGroup.LAW -> "Пятикнижие"
    BookGroup.HISTORY, BookGroup.HISTORY_LATE -> "Исторические книги"
    BookGroup.WISDOM -> "Учительные книги"
    BookGroup.PROPHETS -> "Пророческие книги"
    BookGroup.GOSPELS -> "Евангелия"
    BookGroup.ACTS -> "Деяния"
    BookGroup.CATHOLIC_EPISTLES -> "Соборные послания"
    BookGroup.PAUL -> "Послания Павла"
    BookGroup.REVELATION -> "Откровение"
}

/** Название карточки раздела: у двух исторических разделов оно должно различаться. */
private fun BookGroup.sectionTitle(): String = when (this) {
    BookGroup.HISTORY_LATE -> "Маккавейские книги"
    else -> label()
}

private data class Section(val group: BookGroup, val books: List<Book>)

/** Книги одного Завета, разбитые на разделы по группам подряд. */
private fun sections(books: List<Book>): List<Section> =
    books.fold(mutableListOf<MutableList<Book>>()) { acc, b ->
        if (acc.isEmpty() || acc.last().first().group != b.group) acc += mutableListOf(b) else acc.last() += b
        acc
    }.map { Section(it.first().group, it) }

/**
 * Оглавление в два уровня: разделы Ветхого и Нового Завета, раздел открывает свою страницу с обложками книг.
 * Системная «Назад» со страницы раздела возвращает к разделам.
 */
@Composable
fun TocScreen(
    db: BibleDatabase,
    store: ReadingStore,
    onBackToReading: (() -> Unit)? = null,
    bottomSpace: Dp = 0.dp,
    onOpen: (ChapterRef) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var sheetBook by remember { mutableStateOf<Book?>(null) }
    var openGroup by rememberSaveable { mutableStateOf<String?>(null) }
    StatusBarIcons(dark = true)
    val open: (ChapterRef) -> Unit = { ref ->
        sheetBook = null
        scope.launch {
            store.savePosition(ReadingPosition(ref, 1))
            onOpen(ref)
        }
    }
    val testaments = remember(db) {
        listOf(
            "Ветхий Завет" to db.books.filter { !it.newTestament },
            "Новый Завет" to db.books.filter { it.newTestament },
        )
    }
    val section = remember(openGroup) {
        openGroup?.let { name -> testaments.flatMap { sections(it.second) }.firstOrNull { it.group.name == name } }
    }
    BackHandler(enabled = section != null) { openGroup = null }

    Box(Modifier.fillMaxSize().background(BibleColors.Page)) {
        AnimatedContent(
            targetState = section,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally(tween(280)) { it } + fadeIn(tween(200)))
                        .togetherWith(slideOutHorizontally(tween(280)) { -it / 4 } + fadeOut(tween(200)))
                } else {
                    (slideInHorizontally(tween(280)) { -it / 4 } + fadeIn(tween(200)))
                        .togetherWith(slideOutHorizontally(tween(280)) { it } + fadeOut(tween(200)))
                }
            },
            label = "toc",
        ) { current ->
            if (current == null) {
                Sections(testaments, db, store, onBackToReading, bottomSpace) { s ->
                    // Раздел из одной книги (Деяния, Откровение) сразу открывает выбор главы.
                    if (s.books.size == 1) sheetBook = s.books.single() else openGroup = s.group.name
                }
            } else {
                SectionPage(current, bottomSpace, onBack = { openGroup = null }) { b ->
                    if (b.chapters == 1) open(ChapterRef(b.id, 1)) else sheetBook = b
                }
            }
        }
        // Подложка под статус-бар: карточки не уходят под значки времени и сети.
        Box(
            Modifier.align(Alignment.TopCenter).fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(BibleColors.Page.copy(alpha = 0.94f)),
        )
        sheetBook?.let { b -> ChapterSheet(b, onDismiss = { sheetBook = null }, onPick = { open(ChapterRef(b.id, it)) }) }
    }
}

@Composable
private fun Sections(
    testaments: List<Pair<String, List<Book>>>,
    db: BibleDatabase,
    store: ReadingStore,
    onBackToReading: (() -> Unit)?,
    bottomSpace: Dp,
    onSection: (Section) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize().testTag("toc-sections"), contentPadding = PaddingValues(bottom = 24.dp + bottomSpace)) {
        item { Spacer(Modifier.statusBarsPadding().height(12.dp)) }
        if (onBackToReading != null) {
            item(key = "back") { BackToReading(db, store, onBackToReading) }
        }
        testaments.forEach { (title, books) ->
            item(key = title) { TestamentHeader(title, books.size) }
            sections(books).forEach { s ->
                item(key = "section-${s.group}") { SectionCard(s) { onSection(s) } }
            }
        }
    }
}

@Composable
private fun TestamentHeader(title: String, count: Int) {
    Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 12.dp)) {
        Text(title, fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, color = BibleColors.Ink)
        Text(booksLabel(count), fontSize = 13.sp, color = BibleColors.Muted)
        Box(Modifier.padding(top = 8.dp).size(width = 36.dp, height = 2.dp).clip(RoundedCornerShape(1.dp)).background(BibleColors.Gold))
    }
}

@Composable
private fun SectionCard(section: Section, onClick: () -> Unit) {
    val context = LocalContext.current
    val books = section.books
    Box(
        Modifier
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .fillMaxWidth()
            .height(116.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("section-${section.group}"),
    ) {
        ArtImage(artForSection(context, section.group, books), Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(0f to Color.Black.copy(alpha = 0.5f), 0.7f to Color.Black.copy(alpha = 0.05f)),
            ),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
            Text(
                section.group.sectionTitle(),
                fontFamily = Literata,
                fontWeight = FontWeight.SemiBold,
                fontSize = 21.sp,
                color = Color.White,
                style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.3f), blurRadius = 8f)),
            )
            Text(
                if (books.size == 1) booksLabel(1) else "${booksLabel(books.size)} · ${books.first().shortTitle} — ${books.last().shortTitle}",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.92f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SectionPage(section: Section, bottomSpace: Dp, onBack: () -> Unit, onBook: (Book) -> Unit) {
    LazyVerticalGrid(
        GridCells.Fixed(2),
        Modifier.fillMaxSize().background(BibleColors.Page).testTag("section-page-${section.group}"),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 24.dp + bottomSpace),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(span = { GridItemSpan(2) }) {
            Column(Modifier.statusBarsPadding().padding(top = 12.dp)) {
                Chip("‹ Разделы", Modifier.testTag("back-to-sections"), onClick = onBack)
                Text(
                    section.group.sectionTitle(),
                    Modifier.padding(start = 4.dp, top = 16.dp, bottom = 2.dp),
                    fontFamily = Literata,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 28.sp,
                    color = BibleColors.Ink,
                )
                Text(booksLabel(section.books.size), Modifier.padding(start = 4.dp, bottom = 4.dp), fontSize = 13.sp, color = BibleColors.Muted)
            }
        }
        items(section.books, key = { it.id }) { BookCover(it) { onBook(it) } }
    }
}

@Composable
private fun BookCover(book: Book, onClick: () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).testTag("book-${book.id}"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(4f / 5f).clip(RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            ArtImage(artFor(context, book), Modifier.fillMaxSize())
            if (!hasOwnArt(context, book)) {
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.18f)))))
                Text(
                    book.abbr,
                    style = TextStyle(
                        fontFamily = Literata,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 26.sp,
                        color = Color.White,
                        shadow = Shadow(Color.Black.copy(alpha = 0.35f), blurRadius = 10f),
                    ),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                book.shortTitle,
                Modifier.weight(1f, fill = false),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = BibleColors.Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (book.deuterocanonical) {
                Box(Modifier.padding(start = 5.dp).size(5.dp).clip(CircleShape).background(BibleColors.Gold))
            }
        }
        Text(chaptersLabel(book.chapters), fontSize = 12.sp, color = BibleColors.Muted)
    }
}

/** Возврат к месту чтения: такая же «пилюля», как заголовок главы на экране чтения. */
@Composable
private fun BackToReading(db: BibleDatabase, store: ReadingStore, onBack: () -> Unit) {
    val nav = remember(db) { ChapterNavigator(db.books) }
    val saved by store.position.collectAsState(initial = null)
    val label = saved?.let(nav::sanitize)?.let { "‹ ${nav.book(it.ref.bookId).shortTitle} ${it.ref.chapter}" } ?: "‹ К чтению"
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 4.dp)) {
        Chip(label, Modifier.testTag("back-to-reading"), onClick = onBack)
    }
}
