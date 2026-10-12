package bible.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Проходит главные сценарии: запуск, чтение с прокруткой и листанием глав, оглавление. */
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
        device.findObject(By.res("chapter-title"))?.click()
        device.wait(Until.hasObject(By.res("toc-list")), 5_000)
        val toc = device.findObject(By.res("toc-list"))
        repeat(2) { toc.fling(Direction.DOWN) }
        device.findObject(By.res("shelf-GOSPELS"))?.swipe(Direction.LEFT, 0.8f)
        device.pressBack()
        device.waitForIdle()
    }
}
