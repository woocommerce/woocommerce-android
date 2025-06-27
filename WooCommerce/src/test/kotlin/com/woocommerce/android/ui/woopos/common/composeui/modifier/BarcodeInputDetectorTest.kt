package com.woocommerce.android.ui.woopos.common.composeui.modifier

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import java.util.Date

class BarcodeInputDetectorTest {

    private var onBarcodeScanned: (String, BarcodeInputDetector.ScanMetadata) -> Unit = mock()
    private var onBarcodeScanningFailed: ((BarcodeInputDetector.FailureMetadata) -> Unit)? = mock()
    private var timeProvider: CurrentTimeProvider = mock()
    private var currentTime = 10000L

    private fun setupDetector(): BarcodeInputDetector {
        currentTime = 10000L
        whenever(timeProvider.currentDate()).thenReturn(Date(currentTime))
        return BarcodeInputDetector(onBarcodeScanned, onBarcodeScanningFailed, timeProvider)
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
        verify(onBarcodeScanned).invoke(eq(barcode), any())
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
        verify(onBarcodeScanned, never()).invoke(eq(barcode), any())
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
        verify(onBarcodeScanned, never()).invoke(eq("123"), any())
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
        verify(onBarcodeScanned).invoke(eq(barcode1), any())
        verify(onBarcodeScanned).invoke(eq(barcode2), any())
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
        verify(onBarcodeScanned).invoke(eq(barcode), any())
        verify(onBarcodeScanned, never()).invoke(eq("12"), any())
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
        verify(onBarcodeScanned).invoke(eq(barcode), any())
    }

    @Test
    fun `given empty buffer, when terminator pressed, then no barcode is scanned`() = runTest {
        // GIVEN
        val detector = setupDetector()

        // WHEN
        detector.handleKeyInput('\n')

        // THEN
        verify(onBarcodeScanned, never()).invoke(eq(""), any())
    }

    @Test
    fun `given input is too short (1 char), when enter pressed, then failure event is triggered with too_short reason`() = runTest {
        // GIVEN
        val detector = setupDetector()
        
        // WHEN
        detector.handleKeyInput('1')
        detector.handleKeyInput('\n')
        
        // THEN
        verify(onBarcodeScanningFailed)?.invoke(any())
        verify(onBarcodeScanned, never()).invoke(any(), any())
    }

    @Test
    fun `given input is too short (3 chars), when enter pressed, then failure event is triggered with too_short reason`() = runTest {
        // GIVEN
        val detector = setupDetector()
        
        // WHEN
        detector.handleKeyInput('1')
        detector.handleKeyInput('2')
        detector.handleKeyInput('3')
        detector.handleKeyInput('\n')
        
        // THEN
        verify(onBarcodeScanningFailed)?.invoke(any())
        verify(onBarcodeScanned, never()).invoke(any(), any())
    }

    @Test
    fun `given slow input causing timeout, when new input starts, then failure event is triggered with no_terminator reason`() = runTest {
        // GIVEN
        val detector = setupDetector()
        
        // WHEN
        detector.handleKeyInput('1')
        detector.handleKeyInput('2')
        detector.handleKeyInput('3')
        detector.handleKeyInput('4')
        detector.handleKeyInput('5')
        advanceTestTimeBy(10)
        
        // Wait more than the inter-char delay to trigger timeout
        advanceTestTimeBy(250)
        
        // New input starts (which triggers the failure for previous input)
        detector.handleKeyInput('8')
        
        // THEN
        verify(onBarcodeScanningFailed)?.invoke(any())
    }

    @Test
    fun `given input is exactly minimum length (4 chars), when enter pressed, then success event is triggered`() = runTest {
        // GIVEN
        val detector = setupDetector()
        
        // WHEN
        detector.handleKeyInput('1')
        detector.handleKeyInput('2')
        detector.handleKeyInput('3')
        detector.handleKeyInput('4')
        detector.handleKeyInput('\n')
        
        // THEN
        verify(onBarcodeScanned).invoke(eq("1234"), any())
        verify(onBarcodeScanningFailed, never())?.invoke(any())
    }

    @Test
    fun `given input is 5 chars, when enter pressed, then success event is triggered`() = runTest {
        // GIVEN
        val detector = setupDetector()
        
        // WHEN
        detector.handleKeyInput('1')
        detector.handleKeyInput('2')
        detector.handleKeyInput('3')
        detector.handleKeyInput('4')
        detector.handleKeyInput('5')
        detector.handleKeyInput('\n')
        
        // THEN
        verify(onBarcodeScanned).invoke(eq("12345"), any())
        verify(onBarcodeScanningFailed, never())?.invoke(any())
    }

    @Test
    fun `given input is 6 chars, when enter pressed, then success event is triggered`() = runTest {
        // GIVEN
        val detector = setupDetector()
        
        // WHEN
        detector.handleKeyInput('1')
        detector.handleKeyInput('2')
        detector.handleKeyInput('3')
        detector.handleKeyInput('4')
        detector.handleKeyInput('5')
        detector.handleKeyInput('6')
        detector.handleKeyInput('\n')
        
        // THEN
        verify(onBarcodeScanned).invoke(eq("123456"), any())
        verify(onBarcodeScanningFailed, never())?.invoke(any())
    }
}
