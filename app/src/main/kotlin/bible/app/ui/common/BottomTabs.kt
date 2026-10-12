package bible.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bible.app.ui.theme.BibleColors

/** Высота вкладок без системной полосы жестов. */
val BottomTabsHeight: Dp = 56.dp

data class TabItem(val label: String, val icon: Int, val tag: String, val selected: Boolean, val onClick: () -> Unit)

/** Компактные нижние вкладки: иконка и подпись вплотную, без лишней высоты. */
@Composable
fun BottomTabs(items: List<TabItem>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(BibleColors.Background).testTag("bottom-tabs")) {
        HorizontalDivider(thickness = 1.dp, color = BibleColors.Line)
        Row(
            Modifier.fillMaxWidth().height(BottomTabsHeight).testTag("bottom-tabs-content"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                Column(
                    Modifier.weight(1f).fillMaxHeight()
                        .clickable(remember { MutableInteractionSource() }, indication = null, onClick = item.onClick)
                        .testTag(item.tag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                ) {
                    Box(
                        Modifier.clip(RoundedCornerShape(12.dp))
                            .background(if (item.selected) BibleColors.Tab else BibleColors.Background)
                            .padding(horizontal = 14.dp, vertical = 3.dp),
                    ) {
                        Icon(
                            painterResource(item.icon),
                            null,
                            Modifier.size(20.dp),
                            tint = if (item.selected) BibleColors.Ink else BibleColors.Muted,
                        )
                    }
                    Text(
                        item.label,
                        fontSize = 11.sp,
                        fontWeight = if (item.selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (item.selected) BibleColors.Ink else BibleColors.Muted,
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().navigationBarsPadding())
    }
}
