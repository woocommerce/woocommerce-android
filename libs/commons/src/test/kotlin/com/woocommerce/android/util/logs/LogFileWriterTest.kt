package com.woocommerce.android.util.logs

import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class LogFileWriterTest : BaseUnitTest() {
    // Place the logs in a temporary directory for testing
    private val logsDirectory = File(System.getProperty("java.io.tmpdir", "."), "logs_test")
    private val maxLogFiles = 3
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private var fakeDiskSpace = Long.MAX_VALUE

    private lateinit var logFileWriter: LogFileWriter

    @Before
    fun setup() {
        fakeDiskSpace = Long.MAX_VALUE
        logFileWriter = LogFileWriter(
            logsDirectory = logsDirectory,
            maxLogFiles = maxLogFiles,
            dispatchers = coroutinesTestRule.testDispatchers,
            availableDiskBytes = { fakeDiskSpace }
        )
    }

    @After
    fun tearDown() {
        logsDirectory.deleteRecursively()
    }

    @Test
    fun `when writing logs, then logs should be written to the current log file`() = testBlocking {
        // Write logs
        val logText = "Test log message"
        logFileWriter.writeLogs(logText)

        // Get current log file
        val currentLogFile = logFileWriter.getCurrentLogFile()

        // Verify the log file exists
        assertThat(currentLogFile.exists()).isTrue()

        // Verify the log file contains the log text
        val fileContent = currentLogFile.readText()
        assertThat(fileContent).contains(logText)
    }

    @Test
    fun `when getting current log file, then a file with today's date in the name should be returned`() = testBlocking {
        // Get current log file
        val currentLogFile = logFileWriter.getCurrentLogFile()

        // Verify the log file exists
        assertThat(currentLogFile.exists()).isTrue()

        // Verify the log file is in the logs directory
        assertThat(currentLogFile.parentFile).isEqualTo(logsDirectory)

        // Verify the log file name contains today's date
        val today = dateFormatter.format(Date())
        assertThat(currentLogFile.name).isEqualTo("log_$today.txt")
    }

    @Test
    fun `when getting log files, then a list of log files sorted by last modified date should be returned`() = testBlocking {
        // Create multiple log files with different dates
        val file1 = File(logsDirectory, "log_2025-01-01.txt")
        val file2 = File(logsDirectory, "log_2025-01-02.txt")
        val file3 = File(logsDirectory, "log_2025-01-03.txt")

        logsDirectory.mkdirs()
        file1.createNewFile()
        file2.createNewFile()
        file3.createNewFile()

        // Set different last modified times
        file1.setLastModified(System.currentTimeMillis() - 3000)
        file2.setLastModified(System.currentTimeMillis() - 2000)
        file3.setLastModified(System.currentTimeMillis() - 1000)

        // Get log files
        val logFiles = logFileWriter.getLogFiles()

        // Verify there are 3 log files
        assertThat(logFiles).hasSize(3)

        // Verify the log files are sorted by last modified date (most recent first)
        assertThat(logFiles[0]).isEqualTo(file3)
        assertThat(logFiles[1]).isEqualTo(file2)
        assertThat(logFiles[2]).isEqualTo(file1)
    }

    @Test
    fun `when getting log files, then only files with log_ prefix should be returned`() = testBlocking {
        // Create log files and non-log files
        val logFile = File(logsDirectory, "log_2025-01-01.txt")
        val nonLogFile = File(logsDirectory, "not_a_log.txt")

        logsDirectory.mkdirs()
        logFile.createNewFile()
        nonLogFile.createNewFile()

        // Get log files
        val logFiles = logFileWriter.getLogFiles()

        // Verify only log files are returned
        assertThat(logFiles).hasSize(1)
        assertThat(logFiles[0]).isEqualTo(logFile)
    }

    @Test
    fun `when log files exceed maxLogFiles, then files should be rotated`() = testBlocking {
        // Create more log files than maxLogFiles
        val file1 = File(logsDirectory, "log_2025-01-01.txt")
        val file2 = File(logsDirectory, "log_2025-01-02.txt")
        val file3 = File(logsDirectory, "log_2025-01-03.txt")
        val file4 = File(logsDirectory, "log_2025-01-04.txt")

        logsDirectory.mkdirs()
        file1.createNewFile()
        file2.createNewFile()
        file3.createNewFile()
        file4.createNewFile()

        // Set different last modified times - file1 is the oldest
        val currentTime = System.currentTimeMillis()
        file1.setLastModified(currentTime - 4000)
        file2.setLastModified(currentTime - 3000)
        file3.setLastModified(currentTime - 2000)
        file4.setLastModified(currentTime - 1000)

        // Get the log files before rotation
        val logFilesBefore = logFileWriter.getLogFiles()
        assertThat(logFilesBefore).hasSize(4)

        // Write to a new log file to trigger rotation
        // This will create file5 (today's log file) and should delete the oldest file
        logFileWriter.writeLogs("Test log message")

        // Wait for the rotation to complete
        advanceTimeBy(100)

        // Get the log files after rotation
        val logFilesAfter = logFileWriter.getLogFiles()

        // We should still have maxLogFiles (3) files
        assertThat(logFilesAfter).hasSize(maxLogFiles)

        // The oldest file (file1) should not be in the list
        assertThat(logFilesAfter.map { it.name }).doesNotContain(file1.name)

        // The newest files should be in the list
        assertThat(logFilesAfter.map { it.name }).contains(file4.name)

        // Today's log file should be in the list
        val todayFileName = "log_${dateFormatter.format(Date())}.txt"
        assertThat(logFilesAfter.map { it.name }).contains(todayFileName)
    }

    @Test
    fun `when directory does not exist, then it should be created`() = testBlocking {
        // Delete the logs directory if it exists
        logsDirectory.deleteRecursively()

        // Verify the logs directory does not exist
        assertThat(logsDirectory.exists()).isFalse()

        // Write logs to trigger directory creation
        logFileWriter.writeLogs("Test log message")

        // Verify the logs directory has been created
        assertThat(logsDirectory.exists()).isTrue()
        assertThat(logsDirectory.isDirectory).isTrue()
    }

    @Test
    fun `when log file does not exist, then it should be created with today's date`() = testBlocking {
        // Delete the logs directory if it exists
        logsDirectory.deleteRecursively()

        // Get current log file
        val currentLogFile = logFileWriter.getCurrentLogFile()

        // Verify the log file has been created with today's date
        val today = dateFormatter.format(Date())
        assertThat(currentLogFile.name).isEqualTo("log_$today.txt")
        assertThat(currentLogFile.exists()).isTrue()
    }

    @Test
    fun `when calling getCurrentLogFile multiple times on the same day, then the same file should be returned`() = testBlocking {
        // Get current log file
        val currentLogFile1 = logFileWriter.getCurrentLogFile()

        // Get current log file again
        val currentLogFile2 = logFileWriter.getCurrentLogFile()

        // Verify both calls return the same file
        assertThat(currentLogFile2).isEqualTo(currentLogFile1)
    }

    @Test
    fun `when calling writeLogs multiple times, then logs should be appended to the same file`() = testBlocking {
        // Write logs
        val logText1 = "Test log message 1"
        logFileWriter.writeLogs(logText1)

        // Write more logs
        val logText2 = "Test log message 2"
        logFileWriter.writeLogs(logText2)

        // Get current log file
        val currentLogFile = logFileWriter.getCurrentLogFile()

        // Verify the log file contains both log texts
        val fileContent = currentLogFile.readText()
        assertThat(fileContent).contains(logText1)
        assertThat(fileContent).contains(logText2)
    }

    @Test
    fun `when log file exceeds max size, then file is reset and new logs are written`() = testBlocking {
        val currentLogFile = logFileWriter.getCurrentLogFile()

        val largeContent = "x".repeat(2 * 1024 * 1024)
        currentLogFile.writeText(largeContent)

        logFileWriter.writeLogs("New log entry after reset")

        val fileContent = currentLogFile.readText()
        assertThat(fileContent).doesNotContain(largeContent)
        assertThat(fileContent).contains("New log entry after reset")
    }

    @Test
    fun `when log file is below max size, then writes should succeed`() = testBlocking {
        val currentLogFile = logFileWriter.getCurrentLogFile()

        val smallContent = "x".repeat(1024)
        currentLogFile.writeText(smallContent)

        logFileWriter.writeLogs("New log entry")

        val fileContent = currentLogFile.readText()
        assertThat(fileContent).contains("New log entry")
    }

    @Test
    fun `when disk space is low, then writes should be skipped and oldest logs deleted`() = testBlocking {
        val today = dateFormatter.format(Date())
        val file1 = File(logsDirectory, "log_2025-01-01.txt")
        val file2 = File(logsDirectory, "log_2025-01-02.txt")
        val file3 = File(logsDirectory, "log_2025-01-03.txt")
        val todayFile = File(logsDirectory, "log_$today.txt")

        logsDirectory.mkdirs()
        file1.writeText("oldest log")
        file2.writeText("older log")
        file3.writeText("recent log")
        file1.setLastModified(System.currentTimeMillis() - 4000)
        file2.setLastModified(System.currentTimeMillis() - 3000)
        file3.setLastModified(System.currentTimeMillis() - 2000)

        fakeDiskSpace = 0L

        logFileWriter.writeLogs("This should not be written")

        assertThat(file1.exists()).isFalse()
        assertThat(file2.exists()).isFalse()
        assertThat(file3.exists()).isTrue()
        assertThat(todayFile.exists()).isTrue()
        assertThat(todayFile.readText()).doesNotContain("This should not be written")
    }
}
