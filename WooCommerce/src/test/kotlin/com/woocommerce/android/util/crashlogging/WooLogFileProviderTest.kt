package com.woocommerce.android.util.crashlogging

import com.woocommerce.android.util.WooLog
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class WooLogFileProviderTest {
    private lateinit var sut: WooLogFileProvider

    private val wooLog: WooLog = mock()

    @Before
    fun setUp() {
        sut = WooLogFileProvider(wooLog)
    }

    @Test
    fun `should provide a valid log file`() {
        val testLog = "testLog"
        whenever(wooLog.provideLogs()).thenReturn(testLog)

        val resultFile = sut.provide()

        assertThat(resultFile).exists()
            .canRead()
            .isFile
            .hasContent(testLog)
    }
}
