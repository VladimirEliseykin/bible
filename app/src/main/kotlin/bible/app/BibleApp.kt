package bible.app

import android.app.Application
import android.content.Context
import bible.app.commentary.CommentaryDatabase
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

    private val commentaryLock = Mutex()
    private var commentary: CommentaryDatabase? = null

    /** Объяснения стихов: лежат в APK рядом с bible.db и ставятся так же, один раз на сборку. */
    suspend fun commentary(): CommentaryDatabase = commentaryLock.withLock {
        commentary ?: withContext(Dispatchers.IO + NonCancellable) {
            var opened: CommentaryDatabase? = null
            AssetInstaller(
                open = { context.assets.open("commentary.db") },
                target = File(context.noBackupFilesDir, "commentary.db"),
                version = installedBuild(),
            ).install { file ->
                opened?.close()
                opened = CommentaryDatabase(file).also { check(it.version > 0) { "commentary.db: нет версии" } }
            }
            opened!!
        }.also { commentary = it }
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
