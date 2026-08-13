package com.woocommerce.android.util.logs

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class WooFileLoggerTest : BaseUnitTest() {
    private val testScope: TestScope = TestScope(coroutinesTestRule.testDispatcher)

    // Place the logs in a temporary directory for testing
    private val logsDirectory = File(System.getProperty("java.io.tmpdir", "."), "logs")
    private val archiveDirectory = File(System.getProperty("java.io.tmpdir", "."), "logs_archive")

    private lateinit var testLifecycleOwner: TestLifecycleOwner

    private lateinit var wooFileLogger: WooFileLogger

    fun setup(initialLifecycleState: Lifecycle.State = Lifecycle.State.INITIALIZED) {
        testLifecycleOwner = TestLifecycleOwner(initialState = initialLifecycleState)

        wooFileLogger = WooFileLogger(
            appCoroutineScope = testScope,
            processLifecycleOwner = testLifecycleOwner,
            dispatchers = coroutinesTestRule.testDispatchers,
            logFileWriter = LogFileWriter(
                logsDirectory = logsDirectory,
                maxLogFiles = 7,
                dispatchers = coroutinesTestRule.testDispatchers,
                availableDiskBytes = { Long.MAX_VALUE }
            ),
            logFilesArchiver = LogFilesArchiver(
                archiveDirectory = archiveDirectory,
                dispatchers = coroutinesTestRule.testDispatchers
            ),
        )
    }

    @After
    fun tearDown() {
        logsDirectory.deleteRecursively()
        archiveDirectory.deleteRecursively()
    }

    @Test
    fun `when adding log entries, then they should be buffered until flushed`() = testBlocking {
        setup()

        // Create a test log entry
        val logEntry = LogEntry(WooLog.T.UTILS, WooLog.LogLevel.i, "Test log message")

        // Add the log entry
        wooFileLogger.addEntry(logEntry)

        // Verify the log entry is not immediately processed
        assertThat(wooFileLogger.getCurrentLogFileContent()).isEmpty()

        // Force flush the log entries
        wooFileLogger.forceFlush()

        // Wait for the flush to complete
        advanceTimeBy(100)

        // Verify the log entry is now processed
        assertThat(wooFileLogger.getCurrentLogFileContent()).hasSize(1)
        assertThat(wooFileLogger.getCurrentLogFileContent().single()).isEqualTo(logEntry)
    }

    @Test
    fun `given app is in background, when adding log entries, then they should be automatically flushed after delay`() =
        testBlocking {
            setup()

            // Add a few log entries (less than maxChunkSize)
            for (i in 1..3) {
                val logEntry = LogEntry(WooLog.T.UTILS, WooLog.LogLevel.i, "Test log message $i")
                wooFileLogger.addEntry(logEntry)
            }

            // Verify the log entries are not immediately processed
            assertThat(wooFileLogger.getCurrentLogFileContent()).isEmpty()

            // Wait for the automatic flush to complete (after the flush delay)
            advanceTimeBy(WooFileLogger.BACKGROUND_FLUSH_PERIOD_MS + 100) // Wait for the flush delay plus a bit more
            runCurrent()

            // Verify the log entries are now processed
            assertThat(wooFileLogger.getCurrentLogFileContent()).hasSize(3)

            // Verify all log entries are in the processed batch
            for (i in 1..3) {
                assertThat(wooFileLogger.getCurrentLogFileContent().any { it.text == "Test log message $i" }).isTrue()
            }
        }

    @Test
    fun `given app is in foreground, when adding log entries, then they should be automatically flushed after delay`() =
        testBlocking {
            // Simulate app being in foreground
            setup(Lifecycle.State.STARTED)

            // Add a few log entries
            for (i in 1..3) {
                val logEntry = LogEntry(WooLog.T.UTILS, WooLog.LogLevel.i, "Test log message $i")
                wooFileLogger.addEntry(logEntry)
            }

            // Verify the log entries are not immediately processed
            assertThat(wooFileLogger.getCurrentLogFileContent()).isEmpty()

            // Confirm the logs are not flushed when the background flush delay is used
            advanceTimeBy(WooFileLogger.BACKGROUND_FLUSH_PERIOD_MS + 100)
            runCurrent()

            // Verify the log entries are still not processed
            assertThat(wooFileLogger.getCurrentLogFileContent()).isEmpty()

            // Wait for the automatic flush to complete (after the foreground flush delay)
            advanceTimeBy(WooFileLogger.FOREGROUND_FLUSH_PERIOD_MS + 100) // Wait for the flush delay plus a bit more
            runCurrent()

            // Verify the log entries are now processed
            assertThat(wooFileLogger.getCurrentLogFileContent()).hasSize(3)

            // Verify all log entries are in the processed batch
            for (i in 1..3) {
                assertThat(wooFileLogger.getCurrentLogFileContent().any { it.text == "Test log message $i" }).isTrue()
            }
        }

    @Test
    fun `when force flushing, then log entries should be immediately written to file`() = testBlocking {
        setup()

        // Add a few log entries
        for (i in 1..3) {
            val logEntry = LogEntry(WooLog.T.UTILS, WooLog.LogLevel.i, "Test log message $i")
            wooFileLogger.addEntry(logEntry)
        }

        // Verify the log entries are not immediately processed
        assertThat(wooFileLogger.getCurrentLogFileContent()).isEmpty()

        // Force flush the log entries
        wooFileLogger.forceFlush()

        // Wait for the flush to complete
        advanceTimeBy(100)

        // Verify the log entries are now processed
        assertThat(wooFileLogger.getCurrentLogFileContent()).hasSize(3)

        // Verify all log entries are in the processed batch
        for (i in 1..3) {
            assertThat(wooFileLogger.getCurrentLogFileContent().any { it.text == "Test log message $i" }).isTrue()
        }
    }

    @Test
    fun `when getting log files, then a list of log files should be returned`() = testBlocking {
        setup()

        // Add and flush some log entries to create a log file
        val logEntry = LogEntry(WooLog.T.UTILS, WooLog.LogLevel.i, "Test log message")
        wooFileLogger.addEntry(logEntry)
        wooFileLogger.forceFlush()
        advanceTimeBy(100)

        // Get log files
        val logFiles = wooFileLogger.getLogFiles()

        // Verify there is at least one log file
        assertThat(logFiles).isNotEmpty()

        // Verify the log file is in the logs directory
        assertThat(logFiles.first().parentFile).isEqualTo(logsDirectory)

        // Verify the log file name starts with "log_"
        assertThat(logFiles.first().name).startsWith("log_")
    }

    @Test
    fun `when getting current log file, then the current log file should be returned`() = testBlocking {
        setup()

        // Add and flush some log entries to create a log file
        val logEntry = LogEntry(WooLog.T.UTILS, WooLog.LogLevel.i, "Test log message")
        wooFileLogger.addEntry(logEntry)
        wooFileLogger.forceFlush()
        advanceTimeBy(100)

        // Get current log file
        val currentLogFile = wooFileLogger.getCurrentLogFile()

        // Verify the current log file exists
        assertThat(currentLogFile.exists()).isTrue()

        // Verify the current log file is in the logs directory
        assertThat(currentLogFile.parentFile).isEqualTo(logsDirectory)

        // Verify the current log file name starts with "log_"
        assertThat(currentLogFile.name).startsWith("log_")
    }

    @Test
    fun `when getting log file content for a specific file, then log entries from that file should be returned`() =
        testBlocking {
            setup()

            // Add and flush some log entries to create a log file
            val logEntry = LogEntry(WooLog.T.UTILS, WooLog.LogLevel.i, "Test log message")
            wooFileLogger.addEntry(logEntry)
            wooFileLogger.forceFlush()
            advanceTimeBy(100)

            // Get current log file
            val currentLogFile = wooFileLogger.getCurrentLogFile()

            // Get log file content
            val logFileContent = wooFileLogger.getLogFileContent(currentLogFile.name)

            // Verify the log file content contains the log entry
            assertThat(logFileContent).isNotNull()
            assertThat(logFileContent).hasSize(1)
            assertThat(logFileContent!!.first()).isEqualTo(logEntry)
        }

    @Test
    fun `when getting log file content for a non-existent file, then null should be returned`() = testBlocking {
        setup()

        // Get log file content for a non-existent file
        val logFileContent = wooFileLogger.getLogFileContent("non_existent_file.txt")

        // Verify the log file content is null
        assertThat(logFileContent).isNull()
    }

    @Test
    fun `when getting log file content for an empty file, then an empty list should be returned`() = testBlocking {
        setup()

        // Create an empty log file
        val emptyLogFile = File(logsDirectory, "log_empty.txt")
        logsDirectory.mkdirs()
        emptyLogFile.createNewFile()

        // Get log file content for the empty file
        val logFileContent = wooFileLogger.getLogFileContent(emptyLogFile.name)

        // Verify the log file content is an empty list
        assertThat(logFileContent).isNotNull()
        assertThat(logFileContent).isEmpty()
    }

    @Test
    fun `given file has corrupted entries, when reading content, then only valid entries are returned`() =
        testBlocking {
            // GIVEN
            setup()
            val validEntry = LogEntry(WooLog.T.UTILS, WooLog.LogLevel.i, "Valid message")
            wooFileLogger.addEntry(validEntry)
            wooFileLogger.forceFlush()
            advanceTimeBy(100)

            val logFile = wooFileLogger.getCurrentLogFile()
            logFile.appendText("\n--garbled content that is not a valid log entry")
            logFile.appendText("\n--[Mar-02 14:00:00:000 UNKNOWN_TAG d] unknown tag entry")

            // WHEN
            val logFileContent = wooFileLogger.getLogFileContent(logFile.name)

            // THEN
            val entries = requireNotNull(logFileContent)
            assertThat(entries).hasSize(1)
            assertThat(entries.first()).isEqualTo(validEntry)
        }
}
