package com.woocommerce.android.util.crashlogging

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.util.WooLogWrapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class WooLogFileProviderTest {
    private lateinit var sut: WooLogFileProvider

    private val wooLogWrapper: WooLogWrapper = mock()

    @Before
    fun setUp() {
        sut = WooLogFileProvider(wooLogWrapper)
    }

    @Test
    fun `should provide a valid log file`() {
        val testLog = "testLog"
        whenever(wooLogWrapper.provideLogs()).thenReturn(testLog)

        val resultFile = sut.provide()

        assertThat(resultFile).exists()
            .canRead()
            .isFile
            .hasContent(testLog)
    }
}
