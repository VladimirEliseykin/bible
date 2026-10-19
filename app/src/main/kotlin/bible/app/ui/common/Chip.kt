package bible.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bible.app.ui.theme.BibleColors
import bible.app.ui.theme.Literata

private val ChipShape = RoundedCornerShape(18.dp)

/** Плавающая кнопка-«пилюля» поверх арта и текста: «Бытие 1 ▾», «Аа», «‹ Бытие 1». */
@Composable
fun Chip(text: String, modifier: Modifier = Modifier, serif: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier
            .shadow(4.dp, ChipShape)
            .clip(ChipShape)
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
