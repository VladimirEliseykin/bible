package bible.app.ui.toc

import bible.app.ui.art.ArtImage
import bible.app.ui.common.Chip
import bible.app.domain.ChapterNavigator
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bible.app.data.BibleDatabase
import bible.app.data.Book
import bible.app.data.BookGroup
import bible.app.data.ChapterRef
import bible.app.data.ReadingPosition
import bible.app.data.ReadingStore
import bible.app.domain.booksLabel
import bible.app.domain.chaptersLabel
import bible.app.ui.art.artFor
import bible.app.ui.art.hasOwnArt
import bible.app.ui.theme.BibleColors
import bible.app.ui.theme.Literata
import bible.app.ui.theme.StatusBarIcons
import kotlinx.coroutines.launch

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

/** Книги одного завета, разбитые на полки по группам подряд. */
private fun shelves(books: List<Book>): List<List<Book>> =
    books.fold(mutableListOf<MutableList<Book>>()) { acc, b ->
        if (acc.isEmpty() || acc.last().first().group != b.group) acc += mutableListOf(b) else acc.last() += b
        acc
    }

@Composable
fun TocScreen(
    db: BibleDatabase,
    store: ReadingStore,
    onBackToReading: (() -> Unit)? = null,
    onOpen: (ChapterRef) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var sheetBook by remember { mutableStateOf<Book?>(null) }
    StatusBarIcons(dark = true)
    val open: (ChapterRef) -> Unit = { ref ->
        sheetBook = null
        scope.launch {
            store.savePosition(ReadingPosition(ref, 1))
            onOpen(ref)
        }
    }
    val testaments = remember(db) {
        listOf("Ветхий Завет" to shelves(db.books.filter { !it.newTestament }), "Новый Завет" to shelves(db.books.filter { it.newTestament }))
    }

    Box(Modifier.fillMaxSize().background(BibleColors.Page)) {
        LazyColumn(Modifier.fillMaxSize().testTag("toc-list"), contentPadding = PaddingValues(bottom = 24.dp)) {
            item { Spacer(Modifier.statusBarsPadding().height(if (onBackToReading != null) 12.dp else 8.dp)) }
            if (onBackToReading != null) {
                item(key = "back") { BackToReading(db, store, onBackToReading) }
            }
            testaments.forEach { (title, testamentShelves) ->
                item(key = title) {
                    Text(
                        title,
                        Modifier.padding(start = 18.dp, top = 14.dp, bottom = 4.dp),
                        fontFamily = Literata,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
                        color = BibleColors.Ink,
                    )
                }
                testamentShelves.forEach { shelf ->
                    item(key = "shelf-${shelf.first().group}") {
                        Shelf(shelf) { b -> if (b.chapters == 1) open(ChapterRef(b.id, 1)) else sheetBook = b }
                    }
                }
            }
        }
        sheetBook?.let { b -> ChapterSheet(b, onDismiss = { sheetBook = null }, onPick = { open(ChapterRef(b.id, it)) }) }
    }
}

@Composable
private fun Shelf(books: List<Book>, onBook: (Book) -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(books.first().group.label(), Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = BibleColors.Ink)
            Text(booksLabel(books.size), fontSize = 12.sp, color = BibleColors.Muted)
        }
        LazyRow(
            Modifier.testTag("shelf-${books.first().group}"),
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(books, key = { it.id }) { BookCover(it) { onBook(it) } }
        }
    }
}

@Composable
private fun BookCover(book: Book, onClick: () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier.width(96.dp).clickable(onClick = onClick).testTag("book-${book.id}"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(96.dp, 128.dp).clip(RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            ArtImage(artFor(context, book), Modifier.fillMaxSize())
            if (!hasOwnArt(context, book)) {
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.18f)))))
                Text(
                book.abbr,
                style = TextStyle(
                    fontFamily = Literata,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
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
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = BibleColors.Ink,
                lineHeight = 15.sp,
            )
            if (book.deuterocanonical) {
                Box(Modifier.padding(start = 4.dp).size(5.dp).clip(CircleShape).background(BibleColors.Gold))
            }
        }
        Text(chaptersLabel(book.chapters), fontSize = 11.sp, color = BibleColors.Muted)
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
