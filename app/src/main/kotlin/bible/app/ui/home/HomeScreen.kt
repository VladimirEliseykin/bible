package bible.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bible.app.data.BibleDatabase
import bible.app.data.FIRST_GOSPEL_CHAPTER
import bible.app.data.ReadingPosition
import bible.app.data.ReadingStore
import bible.app.domain.ChapterNavigator
import bible.app.ui.art.artFor
import bible.app.ui.art.homeCarouselArts
import bible.app.ui.theme.BibleColors
import bible.app.ui.theme.Literata
import bible.app.ui.theme.StatusBarIcons
import java.util.Optional
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(db: BibleDatabase, store: ReadingStore, onContinue: () -> Unit) {
    val nav = remember(db) { ChapterNavigator(db.books) }
    // Optional.empty = позиции нет; null = DataStore ещё не ответил, кнопку не трогаем, чтобы не затереть позицию.
    val saved by remember(store) { store.position.map { Optional.ofNullable(it) } }.collectAsState(initial = null)
    val loaded = saved != null
    val position = saved?.orElse(null)?.let(nav::sanitize)
    val book = position?.let { nav.book(it.ref.bookId) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val carouselHeight = LocalConfiguration.current.screenHeightDp.dp * 0.55f
    StatusBarIcons(dark = false)

    Column(Modifier.fillMaxSize().background(BibleColors.Page).verticalScroll(rememberScrollState())) {
        ArtCarousel(homeCarouselArts, Modifier.fillMaxWidth().height(carouselHeight))
        Text(
            "Продолжить чтение",
            Modifier.padding(start = 18.dp, top = 22.dp, bottom = 10.dp),
            fontFamily = Literata,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            color = BibleColors.Ink,
        )
        Row(
            Modifier
                .padding(horizontal = 14.dp)
                .alpha(if (loaded) 1f else 0f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(BibleColors.Background)
                .clickable(enabled = loaded) {
                    if (position == null) {
                        scope.launch {
                            store.savePosition(ReadingPosition(FIRST_GOSPEL_CHAPTER, 1))
                            onContinue()
                        }
                    } else {
                        onContinue()
                    }
                }
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val artBook = book ?: nav.book(FIRST_GOSPEL_CHAPTER.bookId)
            Image(
                painterResource(artFor(context, artBook)),
                null,
                Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
            Column {
                Text(
                    if (book != null) "${book.shortTitle} ${position.ref.chapter}" else "Начать с Евангелия от Матфея",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = BibleColors.Ink,
                )
                Text(
                    if (book != null) "Вернитесь к последнему открытому стиху" else "Первая глава Нового Завета",
                    fontSize = 13.sp,
                    color = BibleColors.Muted,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
