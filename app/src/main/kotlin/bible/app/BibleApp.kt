package bible.app

import android.app.Application
import android.content.Context
import bible.app.commentary.CommentaryDatabase
import bible.app.commentary.PackManager
import bible.app.commentary.PackState
import bible.app.data.AssetInstaller
import bible.app.data.BibleDatabase
import bible.app.data.ReadingStore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AppContainer(private val context: Context) {
    val store = ReadingStore(context)

    /** Пакет объяснений стихов: скачивается с GitHub Releases, дальше работает без сети. */
    val pack = PackManager(
        fetch = { url -> openUrl(url) },
        dir = File(context.noBackupFilesDir, "commentary"),
        manifestUrl = COMMENTARY_MANIFEST_URL,
    )
    private var commentary: CommentaryDatabase? = null
    private var commentaryVersion = -1

    /** Открытый пакет объяснений или null, если он ещё не скачан. Переоткрывается после обновления. */
    @Synchronized
    fun commentary(): CommentaryDatabase? {
        val state = pack.state.value
        val file = pack.installed() ?: return null
        val version = (state as? PackState.Ready)?.version ?: commentaryVersion
        if (commentary == null || version != commentaryVersion) {
            commentary?.close()
            commentary = runCatching { CommentaryDatabase(file) }.getOrNull()
            commentaryVersion = version
        }
        return commentary
    }
    private val lock = Mutex()
    private var db: BibleDatabase? = null

    /** Версия базы = момент установки/обновления APK: новая сборка всегда переустанавливает базу из assets. */
    private fun installedBuild(): String =
        context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime.toString()

    suspend fun database(): BibleDatabase = lock.withLock {
        db ?: withContext(Dispatchers.IO + NonCancellable) {
            var opened: BibleDatabase? = null
            AssetInstaller(
                open = { context.assets.open("bible.db") },
                target = File(context.noBackupFilesDir, "bible.db"),
                version = installedBuild(),
            ).install { file ->
                opened?.close()
                opened = BibleDatabase(file).also { check(it.books.size == 77) { "bible.db: ${it.books.size} книг" } }
            }
            opened!!
        }.also { db = it }
    }
}

class BibleApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Манифест последней версии пакета объяснений (GitHub Releases репозитория приложения). */
const val COMMENTARY_MANIFEST_URL = "https://github.com/VladimirEliseykin/bible/releases/latest/download/commentary.json"

private fun openUrl(url: String): java.io.InputStream {
    val c = java.net.URL(url).openConnection() as java.net.HttpURLConnection
    c.connectTimeout = 15_000
    c.readTimeout = 30_000
    c.instanceFollowRedirects = true
    val code = c.responseCode
    if (code !in 200..299) throw java.io.IOException("HTTP $code for $url")
    return c.inputStream
}
