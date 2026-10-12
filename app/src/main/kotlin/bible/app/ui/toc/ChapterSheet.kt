package bible.app.ui.toc

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bible.app.data.Book
import bible.app.domain.chaptersLabel
import bible.app.ui.art.artFor
import bible.app.ui.theme.BibleColors
import bible.app.ui.theme.Literata

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterSheet(book: Book, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = BibleColors.Background,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Box(Modifier.fillMaxWidth().height(110.dp)) {
                Image(painterResource(artFor(context, book)), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)))))
                Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Text(book.shortTitle, fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, color = Color.White)
                    Text("${book.group.label()} · ${chaptersLabel(book.chapters)}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                }
            }
            LazyVerticalGrid(
                GridCells.Fixed(6),
                Modifier.fillMaxWidth().heightIn(max = 360.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                items(book.chapters) { i ->
                    Box(
                        Modifier.height(40.dp).clip(RoundedCornerShape(11.dp)).background(BibleColors.Tab)
                            .clickable { onPick(i + 1) }.testTag("chapter-${i + 1}"),
                        contentAlignment = Alignment.Center,
                    ) { Text("${i + 1}", fontSize = 15.sp, color = BibleColors.Ink) }
                }
            }
        }
    }
}
