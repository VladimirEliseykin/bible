package bible.dbgen

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {
    @Test fun formsOfOneWordShareStem() {
        val a = TextNormalizer.stems("любовь")
        val b = TextNormalizer.stems("любовью")
        val c = TextNormalizer.stems("Любви")
        assertEquals(a, b)
        assertEquals(a.take(3), c.take(3))
    }

    @Test fun longDashBecomesShort() {
        assertEquals("Блаженны – ибо их; Иисус – Сын", TextNormalizer.dashes("Блаженны — ибо их; Иисус — Сын"))
    }

    @Test fun yoEqualsYe() {
        assertEquals(TextNormalizer.stems("её"), TextNormalizer.stems("ее"))
    }

    @Test fun dropsPunctuationAndDigits() {
        assertEquals(TextNormalizer.stems("свет мира"), TextNormalizer.stems("Свет, — мира! 14"))
    }

    @Test fun emptyForNoWords() {
        assertEquals("", TextNormalizer.stems("— 12 !"))
    }
}
