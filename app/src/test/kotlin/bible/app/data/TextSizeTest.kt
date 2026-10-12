package bible.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TextSizeTest {
    @Test fun nineSizesFromElevenToTwentySeven() =
        assertEquals(listOf(11f, 13f, 15f, 17f, 19f, 21f, 23f, 25f, 27f), TEXT_SIZES_SP)

    @Test fun defaultIsNineteen() = assertEquals(19f, resolveTextSizeSp(storedSp = null, legacyIndex = null))

    @Test fun legacyFiveStepIndexKeepsSameSize() {
        assertEquals(15f, resolveTextSizeSp(null, 0))
        assertEquals(21f, resolveTextSizeSp(null, 3))
        assertEquals(23f, resolveTextSizeSp(null, 4))
    }

    @Test fun storedSizeWinsAndSnapsToScale() {
        assertEquals(11f, resolveTextSizeSp(11f, 4))
        assertEquals(27f, resolveTextSizeSp(40f, null))
        assertEquals(17f, resolveTextSizeSp(17.4f, null))
    }
}
