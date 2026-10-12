package bible.baselineprofile

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Время кадров (CPU) на главных жестах чтения — с профилем, как у пользователя после установки. */
@RunWith(AndroidJUnit4::class)
class ReadingBenchmark {
    @get:Rule val rule = MacrobenchmarkRule()

    private fun MacrobenchmarkScope.openReader() {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.res("tab-bible")), 10_000)
        device.findObject(By.res("tab-bible")).click()
        device.wait(Until.hasObject(By.res("pager")), 5_000)
    }

    private fun measure(block: MacrobenchmarkScope.() -> Unit) = rule.measureRepeated(
        packageName = "bible.app",
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        startupMode = StartupMode.WARM,
        iterations = 3,
        setupBlock = { openReader() },
        measureBlock = block,
    )

    @Test fun scrollChapter() = measure {
        val pager = device.findObject(By.res("pager"))
        repeat(4) { pager.fling(Direction.DOWN); device.waitForIdle() }
        repeat(2) { pager.fling(Direction.UP); device.waitForIdle() }
    }

    @Test fun swipeChapters() = measure {
        val pager = device.findObject(By.res("pager"))
        repeat(4) { pager.swipe(Direction.LEFT, 0.9f, 3000); device.waitForIdle() }
        repeat(2) { pager.swipe(Direction.RIGHT, 0.9f, 3000); device.waitForIdle() }
    }

    @Test fun scrollShelves() = measure {
        device.findObject(By.res("chapter-title")).click()
        device.wait(Until.hasObject(By.res("toc-list")), 5_000)
        val toc = device.findObject(By.res("toc-list"))
        repeat(3) { toc.fling(Direction.DOWN); device.waitForIdle() }
        device.findObject(By.res("shelf-PROPHETS"))?.fling(Direction.RIGHT)
        device.waitForIdle()
    }
}
