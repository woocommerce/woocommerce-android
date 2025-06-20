package com.woocommerce.android.ui.woopos.common.composeui.modifier

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import java.util.Date

class BarcodeInputDetectorTest {

    private var onBarcodeScanned: (String) -> Unit = mock()
    private var timeProvider: CurrentTimeProvider = mock()
    private var currentTime = 10000L

    private fun setupDetector(): BarcodeInputDetector {
        currentTime = 10000L
        whenever(timeProvider.currentDate()).thenReturn(Date(currentTime))
        return BarcodeInputDetector(onBarcodeScanned, timeProvider)
    }

    private fun advanceTestTimeBy(milliseconds: Long) {
        currentTime += milliseconds
        whenever(timeProvider.currentDate()).thenReturn(Date(currentTime))
    }

    @Test
    fun `given fast keyboard input, when enter key pressed, then barcode scan is triggered`() = runTest {
        // GIVEN
        val detector = setupDetector()
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
    fun `given slow keyboard input, when enter key pressed, then barcode scan is not triggered`() = runTest {
        // GIVEN
        val detector = setupDetector()
        val barcode = "12345678"

        // WHEN
        for (char in barcode) {
            detector.handleKeyInput(char)
            advanceTestTimeBy(250) // More than MAX_SCANNER_INTER_CHAR_DELAY_MS (200ms)
        }
        detector.handleKeyInput('\n')

        // THEN
        verify(onBarcodeScanned, never()).invoke(barcode)
    }

    @Test
    fun `given input shorter than minimum length, when enter pressed, then barcode scan is not triggered`() = runTest {
        // GIVEN
        val detector = setupDetector()
        detector.handleKeyInput('1')
        detector.handleKeyInput('2')
        detector.handleKeyInput('3')

        // WHEN
        detector.handleKeyInput('\n')

        // THEN
        verify(onBarcodeScanned, never()).invoke("123")
    }

    @Test
    fun `given two consecutive valid barcodes, when both scanned, then both are detected`() = runTest {
        // GIVEN
        val detector = setupDetector()
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

    @Test
    fun `given partial input with timeout, when new input starts, then buffer is cleared and new scan detected`() = runTest {
        // GIVEN
        val detector = setupDetector()

        // WHEN - start typing a barcode but don't finish
        detector.handleKeyInput('1')
        detector.handleKeyInput('2')
        advanceTestTimeBy(10)

        // Wait more than the inter-char delay
        advanceTestTimeBy(250)

        // Then start a new barcode
        val barcode = "87654321"
        for (char in barcode) {
            detector.handleKeyInput(char)
            advanceTestTimeBy(10)
        }
        detector.handleKeyInput('\n')

        // THEN - only the complete barcode should be detected
        verify(onBarcodeScanned).invoke(barcode)
        verify(onBarcodeScanned, never()).invoke("12")
    }

    @Test
    fun `given carriage return terminator, when valid barcode scanned, then barcode is detected`() = runTest {
        // GIVEN
        val detector = setupDetector()
        val barcode = "12345678"

        // WHEN - use \r instead of \n as terminator
        for (char in barcode) {
            detector.handleKeyInput(char)
            advanceTestTimeBy(10)
        }
        detector.handleKeyInput('\r')

        // THEN
        verify(onBarcodeScanned).invoke(barcode)
    }

    @Test
    fun `given empty buffer, when terminator pressed, then no barcode is scanned`() = runTest {
        // GIVEN
        val detector = setupDetector()

        // WHEN
        detector.handleKeyInput('\n')

        // THEN
        verify(onBarcodeScanned, never()).invoke("")
    }
}
