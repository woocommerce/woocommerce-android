package com.woocommerce.android.ui.woopos.lock

import android.content.Context
import android.util.AtomicFile
import com.woocommerce.android.util.CoroutineDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

interface WooPosLockedPOSStore {
    fun isMarked(localSiteId: Int): Result<Boolean>
    suspend fun mark(localSiteId: Int): Result<Unit>
    suspend fun clear(localSiteId: Int): Result<Unit>
    suspend fun clearAll(): Result<Unit>
}

@Singleton
class WooPosFileLockedPOSStore @Inject constructor(
    @ApplicationContext context: Context,
    private val dispatchers: CoroutineDispatchers,
) : WooPosLockedPOSStore {
    private val directory = File(context.noBackupFilesDir, "woopos-locked")

    // Startup probes only this bounded local record before MainActivity can resume its fragments.
    override fun isMarked(localSiteId: Int): Result<Boolean> = synchronized(this) {
        attempt { readMarker(localSiteId) }
    }

    private fun readMarker(localSiteId: Int): Boolean {
        checkDirectory()
        val file = file(localSiteId)
        return if (!exists(file)) {
            false
        } else {
            file.openRead().use { input ->
                require(input.read() == VERSION && input.read() == -1)
                true
            }
        }
    }

    override suspend fun mark(localSiteId: Int): Result<Unit> = operation {
        check(directory.isDirectory || directory.mkdirs())
        val file = file(localSiteId)
        val marked = exists(file) && file.openRead().use { it.read() == VERSION && it.read() == -1 }
        if (!marked) {
            val stream = file.startWrite()
            try {
                stream.write(VERSION)
                stream.fd.sync()
                file.finishWrite(stream)
            } catch (e: IOException) {
                file.failWrite(stream)
                throw e
            }
            file.openRead().use { require(it.read() == VERSION && it.read() == -1) }
        }
    }

    override suspend fun clear(localSiteId: Int): Result<Unit> = operation {
        checkDirectory()
        val file = file(localSiteId)
        file.delete()
        check(!exists(file))
    }

    override suspend fun clearAll(): Result<Unit> = operation {
        checkDirectory()
        check(!directory.exists() || directory.deleteRecursively())
    }

    private fun checkDirectory() {
        check(!directory.exists() || (directory.isDirectory && directory.canRead() && directory.canExecute()))
    }

    private fun file(localSiteId: Int): AtomicFile {
        require(localSiteId > 0)
        return AtomicFile(File(directory, "$localSiteId.locked"))
    }

    private fun exists(file: AtomicFile) = file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()

    private suspend fun <T> operation(block: () -> T): Result<T> = withContext(dispatchers.io + NonCancellable) {
        synchronized(this@WooPosFileLockedPOSStore) { attempt(block) }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun <T> attempt(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    companion object {
        private const val VERSION = 1
    }
}
