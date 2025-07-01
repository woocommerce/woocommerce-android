package com.woocommerce.android.ui.woopos.common.composeui.modifier

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class BarcodeInputDetectorTest {
    private var onBarcodeEvent: (BarcodeInputDetector.BarcodeResult) -> Unit = mock()
    private var timeProvider: CurrentTimeProvider = mock()
    private var currentTime = 10000L

    private fun setupDetector(testScope: TestScope): BarcodeInputDetector {
        currentTime = 10000L
        whenever(timeProvider.currentDate()).thenReturn(Date(currentTime))
        return BarcodeInputDetector(onBarcodeEvent, timeProvider, testScope)
    }

    private fun advanceTestTimeBy(milliseconds: Long) {
        currentTime += milliseconds
        whenever(timeProvider.currentDate()).thenReturn(Date(currentTime))
    }

    @Test
    fun `given fast keyboard input, when enter key pressed, then success event is triggered with correct data`() = runTest {
        // GIVEN
        val detector = setupDetector(this)
        val barcode = "12345678"
        val resultCaptor = argumentCaptor<BarcodeInputDetector.BarcodeResult>()

        // WHEN
        for (char in barcode) {
            detector.handleKeyInput(char)
            advanceTestTimeBy(10)
        }
        detector.handleKeyInput('\n')

        // THEN
        verify(onBarcodeEvent).invoke(resultCaptor.capture())
        val result = resultCaptor.firstValue
        assertThat(result).isInstanceOf(BarcodeInputDetector.BarcodeResult.Success::class.java)
        val successResult = result as BarcodeInputDetector.BarcodeResult.Success
        assertThat(successResult.barcode).isEqualTo(barcode)
        assertThat(successResult.scanDurationMs).isGreaterThan(0L)
    }

    @Test
    fun `given slow keyboard input, when timeout occurs, then error event triggered with no_terminator reason`() = runTest {
        // GIVEN
        val detector = setupDetector(this)
        val barcode = "12345678"
        val resultCaptor = argumentCaptor<BarcodeInputDetector.BarcodeResult>()

        // WHEN
        for ((index, char) in barcode.withIndex()) {
            detector.handleKeyInput(char)
            if (index == 3) {
                advanceTestTimeBy(250)
                advanceTimeBy(250)
                detector.handleKeyInput('9')
                break
            } else {
                advanceTestTimeBy(10)
            }
        }

        // THEN
        verify(onBarcodeEvent).invoke(resultCaptor.capture())
        val result = resultCaptor.firstValue
        assertThat(result).isInstanceOf(BarcodeInputDetector.BarcodeResult.Error::class.java)
        val errorResult = result as BarcodeInputDetector.BarcodeResult.Error
        assertThat(errorResult.barcode).isEqualTo("1234")
        assertThat(errorResult.failureReason).isEqualTo(BarcodeInputDetector.FailureReason.NO_TERMINATOR)
        assertThat(errorResult.scanDurationMs).isGreaterThan(0L)
    }

    @Test
    fun `given input shorter than minimum length, when enter pressed, then error event triggered with too_short reason`() = runTest {
        // GIVEN
        val detector = setupDetector(this)
        val shortBarcode = "12345"
        val resultCaptor = argumentCaptor<BarcodeInputDetector.BarcodeResult>()

        // WHEN
        for (char in shortBarcode) {
            detector.handleKeyInput(char)
            advanceTestTimeBy(10)
        }
        detector.handleKeyInput('\n')

        // THEN
        verify(onBarcodeEvent).invoke(resultCaptor.capture())
        val result = resultCaptor.firstValue
        assertThat(result).isInstanceOf(BarcodeInputDetector.BarcodeResult.Error::class.java)
        val errorResult = result as BarcodeInputDetector.BarcodeResult.Error
        assertThat(errorResult.barcode).isEqualTo(shortBarcode)
        assertThat(errorResult.failureReason).isEqualTo(BarcodeInputDetector.FailureReason.TOO_SHORT)
        assertThat(errorResult.scanDurationMs).isGreaterThan(0L)
    }

    @Test
    fun `given two consecutive valid barcodes, when both scanned, then both success events are detected`() = runTest {
        // GIVEN
        val detector = setupDetector(this)
        val barcode1 = "12345678"
        val barcode2 = "87654321"
        val resultCaptor = argumentCaptor<BarcodeInputDetector.BarcodeResult>()

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
        verify(onBarcodeEvent, times(2)).invoke(resultCaptor.capture())
        val results = resultCaptor.allValues

        val firstResult = results[0] as BarcodeInputDetector.BarcodeResult.Success
        assertThat(firstResult.barcode).isEqualTo(barcode1)

        val secondResult = results[1] as BarcodeInputDetector.BarcodeResult.Success
        assertThat(secondResult.barcode).isEqualTo(barcode2)
    }

    @Test
    fun `given partial input with timeout, when new input starts, then error event followed by success event`() = runTest {
        // GIVEN
        val detector = setupDetector(this)
        val resultCaptor = argumentCaptor<BarcodeInputDetector.BarcodeResult>()

        // WHEN
        detector.handleKeyInput('1')
        detector.handleKeyInput('2')
        advanceTestTimeBy(10)

        advanceTestTimeBy(250)
        advanceTimeBy(250)

        val barcode = "87654321"
        for (char in barcode) {
            detector.handleKeyInput(char)
            advanceTestTimeBy(10)
        }
        detector.handleKeyInput('\n')

        // THEN
        verify(onBarcodeEvent, times(2)).invoke(resultCaptor.capture())
        val results = resultCaptor.allValues

        val errorResult = results[0] as BarcodeInputDetector.BarcodeResult.Error
        assertThat(errorResult.barcode).isEqualTo("12")
        assertThat(errorResult.failureReason).isEqualTo(BarcodeInputDetector.FailureReason.NO_TERMINATOR)

        val successResult = results[1] as BarcodeInputDetector.BarcodeResult.Success
        assertThat(successResult.barcode).isEqualTo(barcode)
    }

    @Test
    fun `given carriage return terminator, when valid barcode scanned, then success event is detected`() = runTest {
        // GIVEN
        val detector = setupDetector(this)
        val barcode = "12345678"
        val resultCaptor = argumentCaptor<BarcodeInputDetector.BarcodeResult>()

        // WHEN
        for (char in barcode) {
            detector.handleKeyInput(char)
            advanceTestTimeBy(10)
        }
        detector.handleKeyInput('\r')

        // THEN
        verify(onBarcodeEvent).invoke(resultCaptor.capture())
        val result = resultCaptor.firstValue
        assertThat(result).isInstanceOf(BarcodeInputDetector.BarcodeResult.Success::class.java)
        val successResult = result as BarcodeInputDetector.BarcodeResult.Success
        assertThat(successResult.barcode).isEqualTo(barcode)
    }

    @Test
    fun `given empty buffer, when terminator pressed, then no barcode is scanned`() = runTest {
        // GIVEN
        val detector = setupDetector(this)

        // WHEN
        detector.handleKeyInput('\n')

        // THEN
        verify(onBarcodeEvent, never()).invoke(any())
    }

    @Test
    fun `given minimum length barcode, when scanned, then success event triggered`() = runTest {
        // GIVEN
        val detector = setupDetector(this)
        val minLengthBarcode = "123456"
        val resultCaptor = argumentCaptor<BarcodeInputDetector.BarcodeResult>()

        // WHEN
        for (char in minLengthBarcode) {
            detector.handleKeyInput(char)
            advanceTestTimeBy(10)
        }
        detector.handleKeyInput('\n')

        // THEN
        verify(onBarcodeEvent).invoke(resultCaptor.capture())
        val result = resultCaptor.firstValue
        assertThat(result).isInstanceOf(BarcodeInputDetector.BarcodeResult.Success::class.java)
        val successResult = result as BarcodeInputDetector.BarcodeResult.Success
        assertThat(successResult.barcode).isEqualTo(minLengthBarcode)
    }

    @Test
    fun `given very short barcode, when scanned, then error event with too_short reason`() = runTest {
        // GIVEN
        val detector = setupDetector(this)
        val veryShortBarcode = "1" // Much shorter than minimum
        val resultCaptor = argumentCaptor<BarcodeInputDetector.BarcodeResult>()

        // WHEN
        detector.handleKeyInput('1')
        advanceTestTimeBy(10)
        detector.handleKeyInput('\n')

        // THEN
        verify(onBarcodeEvent).invoke(resultCaptor.capture())
        val result = resultCaptor.firstValue
        assertThat(result).isInstanceOf(BarcodeInputDetector.BarcodeResult.Error::class.java)
        val errorResult = result as BarcodeInputDetector.BarcodeResult.Error
        assertThat(errorResult.barcode).isEqualTo(veryShortBarcode)
        assertThat(errorResult.failureReason).isEqualTo(BarcodeInputDetector.FailureReason.TOO_SHORT)
    }

    @Test
    fun `given long valid barcode, when scanned successfully, then success event contains full barcode`() = runTest {
        // GIVEN
        val detector = setupDetector(this)
        val longBarcode = "1234567890123456789"
        val resultCaptor = argumentCaptor<BarcodeInputDetector.BarcodeResult>()

        // WHEN
        for (char in longBarcode) {
            detector.handleKeyInput(char)
            advanceTestTimeBy(10)
        }
        detector.handleKeyInput('\n')

        // THEN
        verify(onBarcodeEvent).invoke(resultCaptor.capture())
        val result = resultCaptor.firstValue
        assertThat(result).isInstanceOf(BarcodeInputDetector.BarcodeResult.Success::class.java)
        val successResult = result as BarcodeInputDetector.BarcodeResult.Success
        assertThat(successResult.barcode).isEqualTo(longBarcode)
        assertThat(successResult.scanDurationMs).isGreaterThan(0L)
    }

    @Test
    fun `given detector clear called, when next input processed, then scan duration starts fresh`() = runTest {
        // GIVEN
        val detector = setupDetector(this)
        val barcode = "12345678"

        for (char in barcode) {
            detector.handleKeyInput(char)
            advanceTestTimeBy(10)
        }
        detector.handleKeyInput('\n')

        // WHEN
        detector.clear()
        advanceTestTimeBy(100)
        val resultCaptor = argumentCaptor<BarcodeInputDetector.BarcodeResult>()

        for (char in barcode) {
            detector.handleKeyInput(char)
            advanceTestTimeBy(10)
        }
        detector.handleKeyInput('\n')

        // THEN
        verify(onBarcodeEvent, times(2)).invoke(resultCaptor.capture())
        val secondResult = resultCaptor.allValues[1] as BarcodeInputDetector.BarcodeResult.Success
        assertThat(secondResult.scanDurationMs).isLessThan(100L)
    }
}
