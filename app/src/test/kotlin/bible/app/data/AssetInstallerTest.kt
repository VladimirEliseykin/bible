package bible.app.data

import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class AssetInstallerTest {
    private val dir = Files.createTempDirectory("inst").toFile()
    private val target = File(dir, "bible.db")
    private val bytes = ByteArray(10_000) { it.toByte() }

    @Test fun copiesOnFirstRun() {
        AssetInstaller({ ByteArrayInputStream(bytes) }, target, "1").install()
        assertArrayEquals(bytes, target.readBytes())
    }

    @Test fun skipsCopyWhenSameVersionInstalled() {
        var opened = 0
        AssetInstaller({ opened++; ByteArrayInputStream(bytes) }, target, "1").install()
        AssetInstaller({ opened++; ByteArrayInputStream(bytes) }, target, "1").install()
        assertEquals(1, opened)
    }

    @Test fun recopiesWhenVersionChanges() {
        AssetInstaller({ ByteArrayInputStream(bytes) }, target, "1").install()
        val v2 = ByteArray(5) { 7 }
        AssetInstaller({ ByteArrayInputStream(v2) }, target, "2").install()
        assertArrayEquals(v2, target.readBytes())
    }

    @Test fun failedCopyLeavesNoFileAndIsRetried() {
        val broken = object : InputStream() {
            var n = 0
            override fun read(): Int = if (n++ < 100) 1 else throw IOException("No space left on device")
        }
        assertThrows(InstallFailed::class.java) { AssetInstaller({ broken }, target, "1").install() }
        assertFalse(target.exists())
        assertEquals(0, dir.listFiles()!!.count { it.name.endsWith(".tmp") })
        AssetInstaller({ ByteArrayInputStream(bytes) }, target, "1").install()
        assertArrayEquals(bytes, target.readBytes())
    }

    @Test fun failedUpgradeDoesNotKeepOldMarkedAsNew() {
        AssetInstaller({ ByteArrayInputStream(bytes) }, target, "1").install()
        val broken = object : InputStream() { override fun read(): Int = throw IOException("No space left on device") }
        assertThrows(InstallFailed::class.java) { AssetInstaller({ broken }, target, "2").install() }
        var opened = 0
        AssetInstaller({ opened++; ByteArrayInputStream(bytes) }, target, "2").install()
        assertEquals(1, opened)
    }

    @Test fun corruptInstalledFileIsReinstalledOnce() {
        AssetInstaller({ ByteArrayInputStream(bytes) }, target, "1").install()
        target.writeBytes(ByteArray(3))
        var verified = 0
        val file = AssetInstaller({ ByteArrayInputStream(bytes) }, target, "1").install { f ->
            verified++
            if (f.length() != bytes.size.toLong()) throw IllegalStateException("corrupt")
        }
        assertArrayEquals(bytes, file.readBytes())
        assertEquals(2, verified)
    }

    @Test fun verifyFailingAfterFreshCopyIsInstallFailed() {
        assertThrows(InstallFailed::class.java) {
            AssetInstaller({ ByteArrayInputStream(bytes) }, target, "1").install { throw IllegalStateException("bad asset") }
        }
        assertFalse(target.exists())
    }

    @Test fun installFailureKnowsWhenDiskIsFull() {
        val full = object : InputStream() { override fun read(): Int = throw IOException("write failed: ENOSPC (No space left on device)") }
        val e = assertThrows(InstallFailed::class.java) { AssetInstaller({ full }, target, "1").install() }
        assertEquals(true, e.noSpace)
    }

    @Test fun otherInstallFailureIsNotNoSpace() {
        val e = assertThrows(InstallFailed::class.java) { AssetInstaller({ throw IOException("asset missing") }, target, "1").install() }
        assertEquals(false, e.noSpace)
    }
}
