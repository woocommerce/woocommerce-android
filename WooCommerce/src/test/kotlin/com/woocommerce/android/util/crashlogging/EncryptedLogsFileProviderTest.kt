package com.woocommerce.android.util.crashlogging

import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.logs.LogEntry
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class EncryptedLogsFileProviderTest : BaseUnitTest() {
    private lateinit var sut: EncryptedLogsFileProvider

    private val wooLog: WooLog = mock()

    @Before
    fun setUp() {
        sut = EncryptedLogsFileProvider(wooLog)
    }

    @Test
    fun `should provide a valid log file`() = testBlocking {
        val testLog = LogEntry(WooLog.T.WP, WooLog.LogLevel.i, "Test log entry")
        whenever(wooLog.getCurrentLogEntries()).thenReturn(listOf(testLog))

        val resultFile = sut.provide()

        assertThat(resultFile).exists()
            .canRead()
            .isFile
            .hasContent(testLog.toString())
    }
}
