package com.woocommerce.android.util.logs

import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

@OptIn(ExperimentalCoroutinesApi::class)
class LogFilesArchiverTest : BaseUnitTest() {
    private val archiveDirectory = File(System.getProperty("java.io.tmpdir", "."), "logs_archive_test")
    private val logsDirectory = File(System.getProperty("java.io.tmpdir", "."), "logs_archive_source_test")

    private lateinit var archiver: LogFilesArchiver

    @Before
    fun setup() {
        logsDirectory.mkdirs()
        archiver = LogFilesArchiver(
            archiveDirectory = archiveDirectory,
            dispatchers = coroutinesTestRule.testDispatchers
        )
    }

    @After
    fun tearDown() {
        archiveDirectory.deleteRecursively()
        logsDirectory.deleteRecursively()
    }

    private fun createLogFile(name: String, content: String) =
        File(logsDirectory, name).apply { writeText(content) }

    @Test
    fun `when archiving log files, then every file is included as a zip entry`() = testBlocking {
        val logFiles = listOf(
            createLogFile("log_2026-07-27.txt", "first day"),
            createLogFile("log_2026-07-28.txt", "second day"),
            createLogFile("log_2026-07-29.txt", "third day")
        )

        val archive = archiver.archive(logFiles = logFiles, deviceInfo = "device info")

        assertThat(archive).isNotNull
        ZipFile(archive).use { zip ->
            val entryNames = zip.entries().toList().map { it.name }
            assertThat(entryNames).contains("log_2026-07-27.txt", "log_2026-07-28.txt", "log_2026-07-29.txt")
        }
    }

    @Test
    fun `when archiving log files, then entries keep the original file content`() = testBlocking {
        val logFile = createLogFile("log_2026-07-29.txt", "the log content")

        val archive = archiver.archive(logFiles = listOf(logFile), deviceInfo = "device info")

        ZipFile(archive).use { zip ->
            val entry = zip.getEntry("log_2026-07-29.txt")
            assertThat(zip.getInputStream(entry).reader().readText()).isEqualTo("the log content")
        }
    }

    @Test
    fun `when archiving log files, then device info is added as a separate entry`() = testBlocking {
        val logFile = createLogFile("log_2026-07-29.txt", "the log content")

        val archive = archiver.archive(logFiles = listOf(logFile), deviceInfo = "OS: 15, DeviceName: Pixel")

        ZipFile(archive).use { zip ->
            val entry = zip.getEntry(LogFilesArchiver.DEVICE_INFO_FILE_NAME)
            assertThat(entry).isNotNull
            assertThat(zip.getInputStream(entry).reader().readText()).isEqualTo("OS: 15, DeviceName: Pixel")
        }
    }

    @Test
    fun `given no log files, when archiving, then null is returned`() = testBlocking {
        val archive = archiver.archive(logFiles = emptyList(), deviceInfo = "device info")

        assertThat(archive).isNull()
    }

    @Test
    fun `given a previous archive exists, when archiving again, then the archive is rebuilt`() = testBlocking {
        val firstArchive = archiver.archive(
            logFiles = listOf(createLogFile("log_2026-07-28.txt", "old")),
            deviceInfo = "device info"
        )
        assertThat(firstArchive).isNotNull

        val secondArchive = archiver.archive(
            logFiles = listOf(createLogFile("log_2026-07-29.txt", "new")),
            deviceInfo = "device info"
        )

        ZipFile(secondArchive).use { zip ->
            val entryNames = zip.entries().toList().map { it.name }
            assertThat(entryNames).contains("log_2026-07-29.txt")
            assertThat(entryNames).doesNotContain("log_2026-07-28.txt")
        }
    }

    @Test
    fun `when archiving log files, then the archive is created in the archive directory with the expected name`() =
        testBlocking {
            val logFile = createLogFile("log_2026-07-29.txt", "the log content")

            val archive = archiver.archive(logFiles = listOf(logFile), deviceInfo = "device info")

            assertThat(archive?.parentFile).isEqualTo(archiveDirectory)
            assertThat(archive?.name).isEqualTo(LogFilesArchiver.ARCHIVE_FILE_NAME)
        }
}
