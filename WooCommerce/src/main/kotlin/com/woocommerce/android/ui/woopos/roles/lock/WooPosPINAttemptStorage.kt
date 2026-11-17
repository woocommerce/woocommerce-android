package com.woocommerce.android.ui.woopos.roles.lock

import android.content.Context
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINStorageFiles
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

internal interface WooPosPINAttemptStorage {
    fun read(localSiteId: Int): WooPosPINAttemptState
    fun write(localSiteId: Int, state: WooPosPINAttemptState)
    fun clearAll()
}

@Singleton
internal class WooPosFilePINAttemptStorage @Inject constructor(
    @ApplicationContext context: Context,
) : WooPosPINAttemptStorage {
    private val files by lazy {
        WooPosPINStorageFiles(File(context.noBackupFilesDir, "woopos-pin-attempts"), "attempts")
    }
    private val directory get() = files.directory

    @Synchronized
    override fun read(localSiteId: Int): WooPosPINAttemptState {
        files.checkDirectoryAccess()
        val file = files.file(localSiteId)
        if (!files.exists(file)) {
            return WooPosPINAttemptState.NO_FAILURES
        }
        return DataInputStream(file.openRead()).use { input ->
            require(input.readInt() == VERSION)
            WooPosPINAttemptState(input.readLong(), input.readInt(), input.readLong(), input.readLong()).also {
                require(input.read() == -1)
            }
        }
    }

    @Synchronized
    override fun write(localSiteId: Int, state: WooPosPINAttemptState) {
        check(directory.isDirectory || directory.mkdirs())
        val file = files.file(localSiteId)
        val stream = file.startWrite()
        try {
            DataOutputStream(stream).apply {
                writeInt(VERSION)
                writeLong(state.failures)
                writeInt(state.bootCount)
                writeLong(state.lastFailureMillis)
                writeLong(state.waitMillis)
                flush()
            }
            stream.fd.sync()
            file.finishWrite(stream)
        } catch (e: IOException) {
            file.failWrite(stream)
            throw e
        }
        check(read(localSiteId) == state)
    }

    @Synchronized
    override fun clearAll() {
        check(!directory.exists() || directory.deleteRecursively())
    }

    companion object {
        private const val VERSION = 1
    }
}
