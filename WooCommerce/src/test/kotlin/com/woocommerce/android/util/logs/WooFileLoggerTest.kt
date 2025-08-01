package com.woocommerce.android.util.logs

import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class WooFileLoggerTest : BaseUnitTest() {
    private val testScope: TestScope = TestScope(coroutinesTestRule.testDispatcher)

    // Place the logs in a temporary directory for testing
    private val logsDirectory = File(System.getProperty("java.io.tmpdir", "."), "logs")

    private lateinit var wooFileLogger: WooFileLogger

    @Before
    fun setup() {
        wooFileLogger = WooFileLogger(
            logsDirectory = logsDirectory,
            appCoroutineScope = testScope,
            dispatchers = coroutinesTestRule.testDispatchers
        )
    }

    @After
    fun tearDown() {
        logsDirectory.deleteRecursively()
    }

    @Test
    fun `when adding log entries, they should be buffered until flushed`() = testBlocking {
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
    fun `when adding log entries, they should be automatically flushed after delay`() = testBlocking {
        // Add a few log entries (less than maxChunkSize)
        for (i in 1..3) {
            val logEntry = LogEntry(WooLog.T.UTILS, WooLog.LogLevel.i, "Test log message $i")
            wooFileLogger.addEntry(logEntry)
        }

        // Verify the log entries are not immediately processed
        assertThat(wooFileLogger.getCurrentLogFileContent()).isEmpty()

        // Wait for the automatic flush to complete (after the flush delay)
        advanceTimeBy(WooFileLogger.FLUSH_PERIOD_MS + 100) // Wait for the flush delay plus a bit more
        runCurrent()

        // Verify the log entries are now processed
        assertThat(wooFileLogger.getCurrentLogFileContent()).hasSize(3)

        // Verify all log entries are in the processed batch
        for (i in 1..3) {
            assertThat(wooFileLogger.getCurrentLogFileContent().any { it.text == "Test log message $i" }).isTrue()
        }
    }

    @Test
    fun `when force flushing, log entries should be immediately written to file`() = testBlocking {
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
}
