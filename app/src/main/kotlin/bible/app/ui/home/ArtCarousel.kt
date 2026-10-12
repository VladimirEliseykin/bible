package bible.app.ui.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Карусель артов: листается сама, пока пользователь её не коснулся. */
@Composable
fun ArtCarousel(arts: List<Int>, modifier: Modifier = Modifier, autoScrollMillis: Long = 6_000) {
    val pager = rememberPagerState { arts.size }
    val touched = pager.interactionSource.collectIsDraggedAsStateOnce()
    LaunchedEffect(touched) {
        while (!touched) {
            delay(autoScrollMillis)
            pager.animateScrollToPage((pager.currentPage + 1) % arts.size)
        }
    }
    Box(modifier.clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)).testTag("carousel")) {
        HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
            Image(painterResource(arts[page]), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(arts.size) { i ->
                val selected = i == pager.currentPage
                val width by animateDpAsState(if (selected) 20.dp else 7.dp, label = "dot")
                Box(
                    Modifier.height(7.dp).width(width).clip(RoundedCornerShape(4.dp))
                        .background(if (selected) Color.White else Color.White.copy(alpha = 0.6f)),
                )
            }
        }
    }
}
