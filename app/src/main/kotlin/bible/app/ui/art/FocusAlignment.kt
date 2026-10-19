package bible.app.ui.art

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt

/**
 * Обрезка картинки вокруг точки интереса: [focusY] — доля высоты (0 — верх, 1 — низ), где главный сюжет.
 * Видимая область центрируется на ней, но не выходит за края картинки.
 */
class FocusAlignment(private val focusY: Float, private val focusX: Float = 0.5f) : Alignment {
    override fun align(size: IntSize, space: IntSize, layoutDirection: LayoutDirection): IntOffset =
        IntOffset(axis(size.width, space.width, focusX), axis(size.height, space.height, focusY))

    private fun axis(content: Int, space: Int, focus: Float): Int {
        if (content <= space) return ((space - content) / 2f).roundToInt()
        val offset = space / 2f - content * focus
        return offset.coerceIn((space - content).toFloat(), 0f).roundToInt()
    }
}
