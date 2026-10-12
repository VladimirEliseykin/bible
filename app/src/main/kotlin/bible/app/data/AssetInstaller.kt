package bible.app.data

import java.io.File
import java.io.IOException
import java.io.InputStream

class InstallFailed(cause: Throwable) : Exception(cause)

/**
 * Копирует базу из assets во внутреннюю память один раз на [version] (версию сборки приложения).
 * Недокопированный файл не остаётся; установленный файл проверяется [verify] и при порче ставится заново.
 */
class AssetInstaller(
    private val open: () -> InputStream,
    private val target: File,
    private val version: String,
) {
    private val marker = File(target.parentFile, target.name + ".version")

    fun install(verify: (File) -> Unit = {}): File {
        if (target.exists() && marker.exists() && marker.readText() == version) {
            try {
                verify(target)
                return target
            } catch (e: Exception) {
                marker.delete()
                target.delete()
            }
        }
        copy()
        try {
            verify(target)
        } catch (e: Exception) {
            marker.delete()
            target.delete()
            throw InstallFailed(e)
        }
        return target
    }

    private fun copy() {
        val tmp = File(target.parentFile, target.name + ".tmp")
        try {
            target.parentFile?.mkdirs()
            open().use { input ->
                tmp.outputStream().use { out ->
                    input.copyTo(out, 64 * 1024)
                    out.fd.sync()
                }
            }
            marker.delete()
            target.delete()
            if (!tmp.renameTo(target)) throw IOException("rename failed")
            marker.writeText(version)
        } catch (e: Throwable) {
            tmp.delete()
            if (!marker.exists()) target.delete()
            throw InstallFailed(e)
        }
    }
}
