package com.woocommerce.android.util.logs

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class WooFileLogger(
    private val appCoroutineScope: CoroutineScope,
    private val dispatchers: CoroutineDispatchers,
    private val processLifecycleOwner: LifecycleOwner,
    private val crashLogging: Provider<CrashLogging>? = null,
    private val logFileWriter: LogFileWriter,
) {
    @Inject
    constructor(
        context: Context,
        @AppCoroutineScope appCoroutineScope: CoroutineScope,
        dispatchers: CoroutineDispatchers,
        crashLogging: Provider<CrashLogging>
    ) : this(
        appCoroutineScope = appCoroutineScope,
        dispatchers = dispatchers,
        processLifecycleOwner = ProcessLifecycleOwner.get(),
        crashLogging = crashLogging,
        logFileWriter = LogFileWriter(
            logsDirectory = File(context.filesDir, LOG_FILE_DIRECTORY),
            maxLogFiles = MAX_LOG_FILES,
            dispatchers = dispatchers,
        ),
    )

    private val internalLogsBuffer = Channel<LogEntry>(Channel.UNLIMITED)
    private val writeTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val processLifecycle
        get() = processLifecycleOwner.lifecycle

    init {
        appCoroutineScope.launch {
            internalLogsBuffer.bufferUntil(
                trigger = merge(
                    flow {
                        val flushPeriod = if (processLifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                            FOREGROUND_FLUSH_PERIOD_MS
                        } else {
                            BACKGROUND_FLUSH_PERIOD_MS
                        }
                        delay(flushPeriod)
                        emit(Unit)
                    }, // periodic flush
                    writeTrigger // flush on demand
                )
            )
                .collect { processLogs(it) }
        }

        // Flush logs when app goes to the background
        processLifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) {
                    appCoroutineScope.launch {
                        forceFlush()
                    }
                }
            }
        )
    }

    fun addEntry(logEntry: LogEntry) {
        internalLogsBuffer.trySend(logEntry)
    }

    suspend fun forceFlush() {
        writeTrigger.emit(Unit)
    }

    suspend fun getLogFiles(): List<File> = logFileWriter.getLogFiles()

    suspend fun getCurrentLogFile(): File = logFileWriter.getCurrentLogFile()

    suspend fun getCurrentLogFileContent(): List<LogEntry> {
        return getLogFileContent(getCurrentLogFile().name).orEmpty()
    }

    suspend fun getLogFileContent(fileName: String): List<LogEntry>? {
        return withContext(dispatchers.io) {
            runCatching {
                logFileWriter.readFileContent(fileName)?.let { text ->
                    if (text.isBlank()) return@let emptyList<LogEntry>()
                    text.split("\n${LOG_ENTRY_PREFIX}").mapNotNull {
                        LogEntry.fromString(it.removePrefix(LOG_ENTRY_PREFIX))
                    }
                }
            }.onFailure {
                it.printStackTrace()
                if (!it.isDiskSpaceError()) {
                    // Report any error that is not due to disk space issues
                    crashLogging?.get()?.sendReport(it, message = "Error writing logs to file")
                }
            }.getOrNull()
        }
    }

    private suspend fun processLogs(logs: List<LogEntry>) {
        runCatching {
            logFileWriter.writeLogs(
                logs.joinToString("\n") { "$LOG_ENTRY_PREFIX$it" }
            )
        }.onFailure {
            it.printStackTrace()
            if (!it.isDiskSpaceError()) {
                // Report any error that is not due to disk space issues
                crashLogging?.get()?.sendReport(it, message = "Error writing logs to file")
            }
        }
    }

    private fun Throwable.isDiskSpaceError() = message.orEmpty().let { msg ->
        msg.contains("ENOSPC") || msg.contains("No space left on device", ignoreCase = true)
    }

    companion object {
        // Flush logs every 5 seconds when the app is in the foreground
        @VisibleForTesting
        const val FOREGROUND_FLUSH_PERIOD_MS = 5000L

        // Flush logs every 500 milliseconds when the app is in the background
        // We use a shorter period to ensure logs are flushed more frequently and to avoid losing logs as the chances
        // of the app being killed in the background are higher.
        @VisibleForTesting
        const val BACKGROUND_FLUSH_PERIOD_MS = 500L

        private const val LOG_FILE_DIRECTORY = "logs"
        private const val MAX_LOG_FILES = 7
        private const val LOG_ENTRY_PREFIX = "--"
    }
}

/**
 * Buffers elements from a ReceiveChannel until a trigger Flow emits, then emits the buffered elements as a List.
 */
private fun <T> ReceiveChannel<T>.bufferUntil(trigger: Flow<Unit>): Flow<List<T>> {
    return flow {
        while (true) {
            val bufferChunks = ArrayList<T>()

            trigger.first()

            // receive the first element (suspend until it is there)
            val first = receiveCatching().getOrNull() ?: break
            bufferChunks.add(first)

            // drain the channel until it is empty
            while (true) {
                val element = tryReceive().getOrNull() ?: break
                bufferChunks.add(element)
            }

            emit(bufferChunks)
        }
    }
}
