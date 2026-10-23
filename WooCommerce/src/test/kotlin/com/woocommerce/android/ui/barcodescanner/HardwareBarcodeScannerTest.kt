package com.woocommerce.android.ui.barcodescanner

import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class HardwareBarcodeScannerTest : BaseUnitTest() {
    private val currentTimeProvider: CurrentTimeProvider = mock {
        on { currentDate() } doReturn Date(0)
    }
    private val scans = mutableListOf<CodeScannerStatus.Success>()
    private val onBarcodeScanned: (CodeScannerStatus.Success) -> Unit = { scans.add(it) }

    private fun TestScope.createScanner() = HardwareBarcodeScanner(
        appCoroutineScope = backgroundScope,
        dispatchers = coroutinesTestRule.testDispatchers,
        currentTimeProvider = currentTimeProvider,
    )

    @Test
    fun `given listening, when an EAN-13 code is typed with enter, then it is scanned as EAN-13`() = testBlocking {
        // GIVEN
        val scanner = createScanner()
        scanner.startListening(onBarcodeScanned)

        // WHEN
        val consumed = "4006381333931\n".map { scanner.onKeyDown(it.code, isTextInputFocused = false) }

        // THEN
        assertThat(consumed).containsOnly(true)
        assertThat(scans).containsExactly(
            CodeScannerStatus.Success(code = "4006381333931", format = BarcodeFormat.FormatEAN13)
        )
    }

    @Test
    fun `given listening, when a non numeric code is typed with enter, then it is scanned with unknown format`() =
        testBlocking {
            // GIVEN
            val scanner = createScanner()
            scanner.startListening(onBarcodeScanned)

            // WHEN
            "SKU-RED-01\n".forEach { scanner.onKeyDown(it.code, isTextInputFocused = false) }

            // THEN
            assertThat(scans).containsExactly(
                CodeScannerStatus.Success(code = "SKU-RED-01", format = BarcodeFormat.FormatUnknown)
            )
        }

    @Test
    fun `given listening, when a numeric code with an invalid check digit is typed, then format is unknown`() =
        testBlocking {
            // GIVEN
            val scanner = createScanner()
            scanner.startListening(onBarcodeScanned)

            // WHEN
            "12345678\n".forEach { scanner.onKeyDown(it.code, isTextInputFocused = false) }

            // THEN
            assertThat(scans).containsExactly(
                CodeScannerStatus.Success(code = "12345678", format = BarcodeFormat.FormatUnknown)
            )
        }

    @Test
    fun `given a text input is focused, when a code is typed, then input is not consumed`() = testBlocking {
        // GIVEN
        val scanner = createScanner()
        scanner.startListening(onBarcodeScanned)

        // WHEN
        val consumed = "4006381333931\n".map { scanner.onKeyDown(it.code, isTextInputFocused = true) }

        // THEN
        assertThat(consumed).containsOnly(false)
        assertThat(scans).isEmpty()
    }

    @Test
    fun `given listening stopped, when a code is typed, then input is not consumed`() = testBlocking {
        // GIVEN
        val scanner = createScanner()
        scanner.startListening(onBarcodeScanned)
        scanner.stopListening(onBarcodeScanned)

        // WHEN
        val consumed = "4006381333931\n".map { scanner.onKeyDown(it.code, isTextInputFocused = false) }

        // THEN
        assertThat(consumed).containsOnly(false)
        assertThat(scans).isEmpty()
    }

    @Test
    fun `given listening and nothing typed, when enter or space is pressed, then input is not consumed`() =
        testBlocking {
            // GIVEN
            val scanner = createScanner()
            scanner.startListening(onBarcodeScanned)

            // WHEN
            val consumed = listOf('\n', ' ').map { scanner.onKeyDown(it.code, isTextInputFocused = false) }

            // THEN
            assertThat(consumed).containsOnly(false)
        }
}
