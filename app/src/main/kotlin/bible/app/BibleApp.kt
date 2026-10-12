package bible.app

import android.app.Application
import android.content.Context
import bible.app.data.AssetInstaller
import bible.app.data.BibleDatabase
import bible.app.data.DB_VERSION
import bible.app.data.ReadingStore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AppContainer(private val context: Context) {
    val store = ReadingStore(context)
    private val lock = Mutex()
    private var db: BibleDatabase? = null

    suspend fun database(): BibleDatabase = lock.withLock {
        db ?: withContext(Dispatchers.IO) {
            val file = AssetInstaller(
                open = { context.assets.open("bible.db") },
                target = File(context.noBackupFilesDir, "bible.db"),
                version = DB_VERSION,
            ).install()
            BibleDatabase(file)
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
