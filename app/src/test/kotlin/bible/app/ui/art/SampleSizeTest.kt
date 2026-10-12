package bible.app.ui.art

import org.junit.Assert.assertEquals
import org.junit.Test

class SampleSizeTest {
    @Test fun fullSizeWhenTargetIsLarger() = assertEquals(1, sampleSizeFor(1080, 1350, 1080, 1350))
    @Test fun halvesForCover() = assertEquals(4, sampleSizeFor(1080, 1350, 252, 336))
    @Test fun neverBelowTarget() = assertEquals(2, sampleSizeFor(1080, 1350, 400, 300))
    @Test fun unknownTargetMeansFull() = assertEquals(1, sampleSizeFor(1080, 1350, 0, 0))
}
