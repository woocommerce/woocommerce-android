package com.woocommerce.android.util.crashlogging

import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.logs.LogEntry
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class EncryptedLogsFileProviderTest : BaseUnitTest() {
    private lateinit var sut: EncryptedLogsFileProvider

    private val wooLog: WooLog = mock()

    @Before
    fun setUp() {
        sut = EncryptedLogsFileProvider(wooLog, coroutinesTestRule.testDispatchers)
    }

    @Test
    fun `when requesting log files, then it should provide a valid file`() = testBlocking {
        val testLog = LogEntry(WooLog.T.WP, WooLog.LogLevel.i, "Test log entry")
        whenever(wooLog.getCurrentLogEntries()).thenReturn(listOf(testLog))

        val resultFile = sut.provide()

        assertThat(resultFile).exists()
            .canRead()
            .isFile
            .hasContent(testLog.toString())
    }

    @Test
    fun `when requesting log files, then it should limit the number of log entries`() = testBlocking {
        val logEntries = (1..1500).map {
            LogEntry(WooLog.T.WP, WooLog.LogLevel.i, "Log entry $it")
        }
        whenever(wooLog.getCurrentLogEntries()).thenReturn(logEntries)

        val resultFile = sut.provide()

        val fileContent = resultFile.readLines()
        assertThat(fileContent).hasSize(1000)
        assertThat(fileContent.first()).contains("Log entry 501")
        assertThat(fileContent.last()).contains("Log entry 1500")
    }
}
