package bible.app.data

import java.io.File
import java.io.IOException
import java.io.InputStream

class InstallFailed(cause: Throwable) : Exception(cause)

/** Копирует базу из assets во внутреннюю память один раз на версию; недокопированный файл не остаётся. */
class AssetInstaller(
    private val open: () -> InputStream,
    private val target: File,
    private val version: Int,
) {
    private val marker = File(target.parentFile, target.name + ".version")

    fun install(): File {
        if (target.exists() && marker.exists() && marker.readText() == version.toString()) return target
        val tmp = File(target.parentFile, target.name + ".tmp")
        try {
            target.parentFile?.mkdirs()
            open().use { input -> tmp.outputStream().use { input.copyTo(it, 64 * 1024) } }
            marker.delete()
            target.delete()
            if (!tmp.renameTo(target)) throw IOException("rename failed")
            marker.writeText(version.toString())
            return target
        } catch (e: Throwable) {
            tmp.delete()
            if (!marker.exists()) target.delete()
            throw InstallFailed(e)
        }
    }
}
