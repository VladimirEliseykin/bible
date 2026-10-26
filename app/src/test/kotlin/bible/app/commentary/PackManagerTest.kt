package bible.app.commentary

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.UnknownHostException
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.GZIPOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PackManagerTest {
    private val dir = Files.createTempDirectory("pack").toFile()
    private val db = ByteArray(50_000) { (it % 251).toByte() }
    private val gz = ByteArrayOutputStream().also { o -> GZIPOutputStream(o).use { it.write(db) } }.toByteArray()
    private val sha = MessageDigest.getInstance("SHA-256").digest(gz).joinToString("") { "%02x".format(it) }
    private val base = "https://example.org/rel/"
    private fun manifest(s: String = sha) = """{"version":2,"url":"commentary.db.gz","size":${gz.size},"sha256":"$s","bytes_unpacked":${db.size}}"""

    private fun manager(free: Long = 100_000_000, files: Map<String, () -> InputStream>) =
        PackManager(fetch = { url -> (files[url] ?: error("unexpected $url")).invoke() }, dir = dir, manifestUrl = base + "commentary.json", freeBytes = { free })

    private fun ok() = mapOf<String, () -> InputStream>(
        base + "commentary.json" to { ByteArrayInputStream(manifest().toByteArray()) },
        base + "commentary.db.gz" to { ByteArrayInputStream(gz) },
    )

    @Test fun installsAndReportsReady() = runTest {
        val m = manager(files = ok())
        m.download()
        assertEquals(PackState.Ready(2), m.state.value)
        assertArrayEquals(db, m.installed()!!.readBytes())
    }

    @Test fun wrongChecksumIsCorruptAndLeavesNothing() = runTest {
        val m = manager(files = ok() + (base + "commentary.json" to { ByteArrayInputStream(manifest("0".repeat(64)).toByteArray()) }))
        m.download()
        assertEquals(PackState.Failed(Reason.Corrupt), m.state.value)
        assertNull(m.installed())
        assertEquals(emptyList<String>(), dir.list()!!.toList())
    }

    @Test fun notEnoughFreeSpaceBeforeDownload() = runTest {
        val m = manager(free = 1_000, files = ok())
        m.download()
        assertEquals(PackState.Failed(Reason.NoSpace), m.state.value)
    }

    @Test fun diskFullDuringDownload() = runTest {
        val broken = object : InputStream() { var n = 0; override fun read(): Int = if (n++ < 10) 1 else throw IOException("write failed: ENOSPC (No space left on device)") }
        val m = manager(files = ok() + (base + "commentary.db.gz" to { broken }))
        m.download()
        assertEquals(PackState.Failed(Reason.NoSpace), m.state.value)
        assertEquals(emptyList<String>(), dir.list()!!.toList())
    }

    @Test fun noNetwork() = runTest {
        val m = manager(files = mapOf(base + "commentary.json" to { throw UnknownHostException("github.com") }))
        m.download()
        assertEquals(PackState.Failed(Reason.NoNetwork), m.state.value)
    }

    @Test fun installedPackSurvivesNewManagerAndFailedUpdateKeepsOld() = runTest {
        manager(files = ok()).download()
        val again = manager(files = mapOf(base + "commentary.json" to { throw UnknownHostException("x") }))
        assertEquals(PackState.Ready(2), again.state.value)
        again.download()
        assertArrayEquals(db, again.installed()!!.readBytes())
    }
}
