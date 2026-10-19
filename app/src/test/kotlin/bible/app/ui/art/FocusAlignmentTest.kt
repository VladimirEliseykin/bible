package bible.app.ui.art

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class FocusAlignmentTest {
    private val ltr = LayoutDirection.Ltr

    @Test fun centersFocusPointInWideFrame() {
        // картинка 1000×1000, видимая полоса 1000×200: точка 0.7 по высоте → верх полосы на 600
        assertEquals(IntOffset(0, -600), FocusAlignment(0.7f).align(IntSize(1000, 1000), IntSize(1000, 200), ltr))
    }

    @Test fun neverShowsBeyondTopEdge() =
        assertEquals(IntOffset(0, 0), FocusAlignment(0.05f).align(IntSize(1000, 1000), IntSize(1000, 200), ltr))

    @Test fun neverShowsBeyondBottomEdge() =
        assertEquals(IntOffset(0, -800), FocusAlignment(0.98f).align(IntSize(1000, 1000), IntSize(1000, 200), ltr))

    @Test fun centersHorizontallyWhenCroppingSides() =
        assertEquals(IntOffset(-150, 0), FocusAlignment(0.6f).align(IntSize(1000, 1000), IntSize(700, 1000), ltr))

    @Test fun centersSmallerContent() =
        assertEquals(IntOffset(50, 50), FocusAlignment(0.9f).align(IntSize(100, 100), IntSize(200, 200), ltr))
}
