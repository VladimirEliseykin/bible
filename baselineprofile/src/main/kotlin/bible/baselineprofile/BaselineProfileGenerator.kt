package bible.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

/** Проходит главные сценарии: запуск, чтение с прокруткой и листанием глав, страница стиха, оглавление. */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule val rule = BaselineProfileRule()

    @Test fun generate() = rule.collect(packageName = "bible.app") {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.res("tab-bible")), 10_000)
        device.findObject(By.res("tab-bible")).click()
        device.wait(Until.hasObject(By.res("pager")), 5_000)
        val pager = device.findObject(By.res("pager"))
        repeat(3) { pager.fling(Direction.DOWN) }
        repeat(2) { pager.fling(Direction.UP) }
        pager.swipe(Direction.LEFT, 0.8f)
        device.waitForIdle()
        pager.swipe(Direction.RIGHT, 0.8f)
        device.waitForIdle()
        // Страница стиха: открыть, прокрутить, перелистнуть на соседний стих, вернуться к главе.
        device.findObject(By.res("verse-1"))?.click()
        if (device.wait(Until.hasObject(By.res(Pattern.compile("verse-list-\\d+"))), 5_000)) {
            val verse = device.findObject(By.res(Pattern.compile("verse-list-\\d+")))
            verse.fling(Direction.DOWN)
            verse.swipe(Direction.LEFT, 0.8f)
            device.waitForIdle()
            device.pressBack()
            device.wait(Until.hasObject(By.res("pager")), 5_000)
        }
        device.findObject(By.res("chapter-title"))?.click()
        device.wait(Until.hasObject(By.res("toc-sections")), 5_000)
        val toc = device.findObject(By.res("toc-sections"))
        repeat(2) { toc.fling(Direction.DOWN) }
        device.findObject(By.res("section-GOSPELS"))?.click()
        device.wait(Until.hasObject(By.res("section-page-GOSPELS")), 5_000)
        device.findObject(By.res("section-page-GOSPELS"))?.fling(Direction.DOWN)
        device.pressBack()
        device.waitForIdle()
        device.pressBack()
        device.waitForIdle()
    }
}
