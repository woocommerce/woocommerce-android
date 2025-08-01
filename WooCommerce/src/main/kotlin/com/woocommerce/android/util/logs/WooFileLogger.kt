package com.woocommerce.android.util.logs

import android.content.Context
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooFileLogger(
    private val logsDirectory: File,
    private val appCoroutineScope: CoroutineScope,
    private val dispatchers: CoroutineDispatchers
) {
    @Inject
    constructor(
        context: Context,
        @AppCoroutineScope appCoroutineScope: CoroutineScope,
        dispatchers: CoroutineDispatchers
    ) : this(
        logsDirectory = File(context.filesDir, LOG_FILE_DIRECTORY),
        appCoroutineScope = appCoroutineScope,
        dispatchers = dispatchers
    )

    private val logFileWriter: LogFileWriter = LogFileWriter(
        logsDirectory = logsDirectory,
        maxLogFiles = MAX_LOG_FILES,
        dispatchers = dispatchers
    )

    private val internalLogsBuffer = Channel<LogEntry>(Channel.UNLIMITED)
    private val writeTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        appCoroutineScope.launch {
            internalLogsBuffer.bufferUntil(
                trigger = merge(
                    flow {
                        delay(FLUSH_PERIOD_MS)
                        emit(Unit)
                    }, // periodic flush
                    writeTrigger // flush on demand
                )
            )
                .collect { processLogs(it) }
        }

        // trigger writing logs to file when VM is closed
        Runtime.getRuntime().addShutdownHook(
            Thread {
                runBlocking { writeTrigger.emit(Unit) }
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
                logFileWriter.getLogFiles().find { it.name == fileName }?.readText()?.let { text ->
                    if (text.isBlank()) return@let emptyList<LogEntry>()
                    text.split("\n${LOG_ENTRY_PREFIX}").map {
                        LogEntry.fromString(it.removePrefix(LOG_ENTRY_PREFIX))
                    }
                }
            }.onFailure {
                it.printStackTrace()
            }.getOrNull()
        }
    }

    private suspend fun processLogs(logs: List<LogEntry>) {
        logFileWriter.writeLogs(
            logs.joinToString("\n") { "${LOG_ENTRY_PREFIX}${it}" }
        )
    }

    companion object Companion {
        private const val FLUSH_PERIOD_MS = 500L
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
