package com.woocommerce.android.ui.woopos.lock

import android.content.Context
import android.util.AtomicFile
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
    private val directory = File(context.noBackupFilesDir, "woopos-pin-attempts")

    @Synchronized
    override fun read(localSiteId: Int): WooPosPINAttemptState {
        checkDirectory()
        val file = file(localSiteId)
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return WooPosPINAttemptState()
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
        val file = file(localSiteId)
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
        checkDirectory()
        check(!directory.exists() || directory.deleteRecursively())
    }

    private fun checkDirectory() {
        check(!directory.exists() || (directory.isDirectory && directory.canRead() && directory.canExecute()))
    }

    private fun file(localSiteId: Int): AtomicFile {
        require(localSiteId > 0)
        return AtomicFile(File(directory, "$localSiteId.attempts"))
    }

    companion object {
        private const val VERSION = 1
    }
}
