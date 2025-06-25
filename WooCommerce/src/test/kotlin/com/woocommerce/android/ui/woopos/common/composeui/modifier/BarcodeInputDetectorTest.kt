package com.woocommerce.android.ui.woopos.common.composeui.modifier

import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import java.util.Date

@ExperimentalCoroutinesApi
class BarcodeInputDetectorTest {
    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private var onBarcodeScanned: (String) -> Unit = mock()
    private var timeProvider: CurrentTimeProvider = mock()
    private var currentTime = 10000L

    private fun setupDetector(testScope: TestScope): BarcodeInputDetector {
        currentTime = 10000L
        whenever(timeProvider.currentDate()).thenReturn(Date(currentTime))
        return BarcodeInputDetector(onBarcodeScanned, testScope, timeProvider)
    }

    private fun TestScope.advanceTestTimeBy(milliseconds: Long) {
        currentTime += milliseconds
        whenever(timeProvider.currentDate()).thenReturn(Date(currentTime))
        advanceTimeBy(milliseconds)
    }

    @Test
    fun `given fast keyboard input, when enter key pressed, then barcode scan is triggered`() = runTest {
        // GIVEN
        val detector = setupDetector(this)
        val barcode = "12345678"

        // WHEN
        for (char in barcode) {
            detector.handleKeyInput(char)
            advanceTestTimeBy(10)
        }
        detector.handleKeyInput('\n')

        // THEN
        verify(onBarcodeScanned).invoke(barcode)
    }

    @Test
    fun `given valid barcode input, when timeout elapses, then barcode scan is triggered`() = runTest {
        // GIVEN
        val detector = setupDetector(this)
        val barcode = "1234567"

        // WHEN
        for (char in barcode) {
            detector.handleKeyInput(char)
            advanceTestTimeBy(10)
        }
        advanceTestTimeBy(BarcodeInputDetector.MAX_SCANNER_TOTAL_SCAN_TIMEOUT_MS)

        // THEN
        verify(onBarcodeScanned).invoke(barcode)
    }

    @Test
    fun `given slow keyboard input, when enter key pressed, then barcode scan is not triggered`() = runTest {
        // GIVEN
        val detector = setupDetector(this)
        val barcode = "12345678"

        // WHEN
        for (char in barcode) {
            detector.handleKeyInput(char)
            advanceTestTimeBy(200)
        }
        detector.handleKeyInput('\n')

        // THEN
        verify(onBarcodeScanned, never()).invoke(barcode)
    }

    @Test
    fun `given no input provided, when enter pressed, then barcode scan is not triggered`() = runTest {
        // GIVEN
        val detector = setupDetector(this)

        // WHEN
        detector.handleKeyInput('\n')

        // THEN
        verify(onBarcodeScanned, never()).invoke("123")
    }

    @Test
    fun `given started scan, when total timeout exceeded, then new scan is started`() = runTest {
        // GIVEN
        val detector = setupDetector(this)
        detector.handleKeyInput('1')
        advanceTestTimeBy(10)
        detector.handleKeyInput('2')
        advanceTestTimeBy(10)

        // WHEN
        currentTime += BarcodeInputDetector.MAX_SCANNER_TOTAL_SCAN_TIMEOUT_MS + 1
        whenever(timeProvider.currentDate()).thenReturn(Date(currentTime))
        detector.handleKeyInput('3')
        detector.handleKeyInput('\n')

        // THEN
        verify(onBarcodeScanned).invoke("3")
    }

    @Test
    fun `given two consecutive valid barcodes, when both scanned, then both are detected`() = runTest {
        // GIVEN
        val detector = setupDetector(this)
        val barcode1 = "12345678"
        val barcode2 = "87654321"

        // WHEN
        for (char in barcode1) {
            detector.handleKeyInput(char)
            advanceTestTimeBy(10)
        }
        detector.handleKeyInput('\n')

        for (char in barcode2) {
            detector.handleKeyInput(char)
            advanceTestTimeBy(10)
        }
        detector.handleKeyInput('\n')

        // THEN
        verify(onBarcodeScanned).invoke(barcode1)
        verify(onBarcodeScanned).invoke(barcode2)
    }
}
