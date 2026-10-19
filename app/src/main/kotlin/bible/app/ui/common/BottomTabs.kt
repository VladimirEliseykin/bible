package bible.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bible.app.ui.theme.BibleColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/** Высота «пилюли» вкладок. */
val BottomTabsHeight: Dp = 60.dp

/** Отступ пилюли от краёв экрана и от полосы жестов. */
val BottomTabsMargin: Dp = 18.dp

private val BottomTabsBottomGap: Dp = 12.dp

/** Сколько места снизу оставить контенту, чтобы последний элемент не прятался под пилюлей. */
@Composable
fun bottomTabsSpace(): Dp =
    BottomTabsHeight + BottomTabsBottomGap + 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/** Расстояние от низа экрана до верха пилюли — над ней встаёт плашка главы. */
@Composable
fun bottomTabsTop(): Dp =
    BottomTabsHeight + BottomTabsBottomGap + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

data class TabItem(val label: String, val icon: Int, val tag: String, val selected: Boolean, val onClick: () -> Unit)

private val PillShape = RoundedCornerShape(30.dp)

/**
 * Стеклянная пилюля вкладок, висящая над контентом: размывает то, что под ней (Android 12+),
 * на старых версиях — просто полупрозрачная.
 */
@Composable
fun BottomTabs(items: List<TabItem>, hazeState: HazeState, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = BottomTabsMargin, end = BottomTabsMargin, bottom = BottomTabsBottomGap),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(BottomTabsHeight)
                .shadow(16.dp, PillShape, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.25f))
                .clip(PillShape)
                .hazeEffect(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = BibleColors.Page,
                        tint = HazeTint(Color.White.copy(alpha = 0.58f)),
                        blurRadius = 18.dp,
                        noiseFactor = 0f,
                        fallbackTint = HazeTint(Color.White.copy(alpha = 0.92f)),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.7f), PillShape)
                .testTag("bottom-tabs"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.fillMaxWidth().height(BottomTabsHeight).testTag("bottom-tabs-content")) {
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
                                .background(if (item.selected) Color.White.copy(alpha = 0.78f) else Color.Transparent)
                                .padding(horizontal = 14.dp, vertical = 3.dp),
                        ) {
                            Icon(
                                painterResource(item.icon),
                                null,
                                Modifier.size(21.dp),
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
        }
    }
}
