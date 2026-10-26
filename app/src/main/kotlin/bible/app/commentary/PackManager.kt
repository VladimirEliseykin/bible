package bible.app.commentary

import bible.commentary.model.Json
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.URI
import java.net.UnknownHostException
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class Manifest(val version: Int, val url: String, val size: Long, val sha256: String, val bytes_unpacked: Long)

enum class Reason { NoNetwork, NoSpace, Corrupt, Other }

sealed interface PackState {
    data object Missing : PackState
    data class Downloading(val progress: Float) : PackState
    data class Ready(val version: Int) : PackState
    data class Failed(val reason: Reason) : PackState
}

/**
 * Пакет объяснений: скачивается одним файлом (gzip) по манифесту, проверяется sha256,
 * распаковывается во временный файл и только потом подменяет установленный. Недокачанное не остаётся.
 */
class PackManager(
    private val fetch: (String) -> InputStream,
    private val dir: File,
    private val manifestUrl: String,
    private val freeBytes: () -> Long = { dir.usableSpace },
) {
    private val db = File(dir, "commentary.db")
    private val marker = File(dir, "commentary.version")
    private val _state = MutableStateFlow(initial())
    val state: StateFlow<PackState> = _state

    private fun initial(): PackState =
        marker.takeIf { db.exists() && it.exists() }?.readText()?.toIntOrNull()?.let { PackState.Ready(it) } ?: PackState.Missing

    fun installed(): File? = db.takeIf { it.exists() && marker.exists() }

    suspend fun download() = withContext(Dispatchers.IO) {
        val gz = File(dir, "commentary.db.gz.tmp")
        val tmp = File(dir, "commentary.db.tmp")
        try {
            dir.mkdirs()
            _state.value = PackState.Downloading(0f)
            val manifest = fetch(manifestUrl).use { Json.decodeFromString(Manifest.serializer(), it.readBytes().decodeToString()) }
            if (freeBytes() < manifest.size + manifest.bytes_unpacked + SPARE_BYTES) throw NoSpace()
            val digest = MessageDigest.getInstance("SHA-256")
            DigestInputStream(fetch(URI(manifestUrl).resolve(manifest.url).toString()), digest).use { input ->
                gz.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        _state.value = PackState.Downloading((done.toFloat() / manifest.size).coerceIn(0f, 1f))
                    }
                }
            }
            val sha = digest.digest().joinToString("") { "%02x".format(it) }
            if (!sha.equals(manifest.sha256, ignoreCase = true)) throw Corrupt()
            GZIPInputStream(gz.inputStream()).use { input -> tmp.outputStream().use { input.copyTo(it, 64 * 1024) } }
            marker.delete()
            if (!tmp.renameTo(db) && !(db.delete() && tmp.renameTo(db))) throw IOException("rename failed")
            marker.writeText(manifest.version.toString())
            _state.value = PackState.Ready(manifest.version)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            _state.value = PackState.Failed(reasonOf(e))
            if (!marker.exists()) db.delete()
        } finally {
            gz.delete()
            tmp.delete()
        }
    }

    private class NoSpace : IOException("not enough space")
    private class Corrupt : IOException("checksum mismatch")

    private fun reasonOf(e: Throwable): Reason = when {
        e is NoSpace -> Reason.NoSpace
        e is Corrupt -> Reason.Corrupt
        generateSequence(e) { it.cause }.any { it.message.orEmpty().let { m -> "ENOSPC" in m || "No space left" in m } } -> Reason.NoSpace
        generateSequence(e) { it.cause }.any { it is UnknownHostException || it is ConnectException || it is SocketTimeoutException } -> Reason.NoNetwork
        else -> Reason.Other
    }

    companion object {
        /** Запас сверх размера пакета, чтобы не заполнить память телефона до конца. */
        const val SPARE_BYTES = 20L * 1024 * 1024
    }
}
