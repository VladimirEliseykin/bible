package bible.app.ui.home

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** true после первого свайпа пользователя и навсегда — так автолистание останавливается после касания. */
@Composable
internal fun InteractionSource.collectIsDraggedAsStateOnce(): Boolean {
    var dragged by remember { mutableStateOf(false) }
    LaunchedEffect(this) {
        interactions.collect { if (it is DragInteraction.Start) dragged = true }
    }
    return dragged
}
