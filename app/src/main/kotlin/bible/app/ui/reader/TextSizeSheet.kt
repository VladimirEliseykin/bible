package bible.app.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bible.app.data.TEXT_SIZES_SP
import bible.app.ui.theme.BibleColors
import bible.app.ui.theme.Literata

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextSizeSheet(size: Int, spacing: Int, onSize: (Int) -> Unit, onSpacing: (Int) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = BibleColors.Background,
    ) {
        Column(
            Modifier.navigationBarsPadding().padding(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("Размер текста", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = BibleColors.Ink)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StepButton("А−", 15, "text-smaller") { onSize((size - 1).coerceAtLeast(0)) }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    repeat(TEXT_SIZES_SP.size) { i ->
                        Box(
                            Modifier.size(if (i == size) 10.dp else 6.dp).clip(CircleShape)
                                .background(if (i <= size) BibleColors.Gold else BibleColors.Line),
                        )
                    }
                }
                StepButton("А+", 21, "text-larger") { onSize((size + 1).coerceAtMost(TEXT_SIZES_SP.lastIndex)) }
            }
            Text("Интервал", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = BibleColors.Ink)
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(BibleColors.Tab).padding(4.dp)) {
                listOf("Плотно", "Обычно", "Свободно").forEachIndexed { i, label ->
                    Box(
                        Modifier.weight(1f).heightIn(min = 38.dp).clip(RoundedCornerShape(11.dp))
                            .background(if (i == spacing) BibleColors.Background else BibleColors.Tab)
                            .clickable { onSpacing(i) }
                            .testTag("spacing-$i"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            Modifier.padding(vertical = 8.dp),
                            fontSize = 14.sp,
                            color = if (i == spacing) BibleColors.Ink else BibleColors.Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Box(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StepButton(label: String, sizeSp: Int, tag: String, onClick: () -> Unit) {
    Box(
        Modifier.defaultMinSize(48.dp, 48.dp).clip(CircleShape).background(BibleColors.Tab).clickable(onClick = onClick)
            .padding(6.dp).testTag(tag),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontFamily = Literata, fontWeight = FontWeight.SemiBold, fontSize = sizeSp.sp, color = BibleColors.Ink) }
}
