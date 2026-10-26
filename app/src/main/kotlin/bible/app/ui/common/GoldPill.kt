package bible.app.ui.common

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

val GoldPillBackground = Color(0xFFE9D3A0)
val GoldPillText = Color(0xFF3B2C10)

private val PillShape = RoundedCornerShape(12.dp)

/** Золотая «пилюля» с местом в Библии: «СИРАХ 2:1», «ЛЕВИТ 3». */
@Composable
fun GoldPill(text: String, modifier: Modifier = Modifier, elevated: Boolean = false) {
    Text(
        text.uppercase(),
        modifier
            .then(if (elevated) Modifier.shadow(6.dp, PillShape) else Modifier)
            .clip(PillShape)
            .background(GoldPillBackground)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        color = GoldPillText,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.08.em,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
