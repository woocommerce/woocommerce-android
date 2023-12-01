package com.woocommerce.android.ui.barcodescanner

import androidx.camera.core.ImageProxy
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel.PermissionState
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel.PermissionState.Granted
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel.PermissionState.PermanentlyDenied
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel.PermissionState.ShouldShowRationale
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel.ScanningEvents.LaunchCameraPermission
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel.ScanningEvents.OpenAppSettings
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.ui.orders.creation.CodeScanningErrorType
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper
import com.woocommerce.android.ui.orders.creation.GoogleMLKitCodeScanner
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class BarcodeScanningViewModelTest : BaseUnitTest() {
    private lateinit var barcodeScanningViewModel: BarcodeScanningViewModel
    private lateinit var savedStateHandle: SavedStateHandle
    private val codeScanner: GoogleMLKitCodeScanner = mock()

    @Before
    fun setup() {
        savedStateHandle = mock()
        barcodeScanningViewModel = BarcodeScanningViewModel(codeScanner, savedStateHandle)
    }

    @Test
    fun `when view model init, then permission state is unknown`() {
        assertThat(barcodeScanningViewModel.permissionState.value).isEqualTo(PermissionState.Unknown)
    }

    @Test
    fun `given camera permission granted, then update the PermissionState accordingly`() {
        barcodeScanningViewModel.updatePermissionState(
            isPermissionGranted = true,
            shouldShowRequestPermissionRationale = false
        )

        assertThat(barcodeScanningViewModel.permissionState.value).isEqualTo(Granted)
    }

    @Test
    fun `given camera permission not granted and should show rationale, then update the PermissionState accordingly`() {
        barcodeScanningViewModel.updatePermissionState(
            isPermissionGranted = false,
            shouldShowRequestPermissionRationale = true
        )

        assertThat(barcodeScanningViewModel.permissionState.value).isInstanceOf(ShouldShowRationale::class.java)
    }

    @Test
    fun `given camera permission not granted and should show rationale, then dialog title is correct`() {
        barcodeScanningViewModel.updatePermissionState(
            isPermissionGranted = false,
            shouldShowRequestPermissionRationale = true
        )

        assertThat(
            (barcodeScanningViewModel.permissionState.value as ShouldShowRationale).title
        ).isEqualTo(R.string.barcode_scanning_alert_dialog_title)
    }

    @Test
    fun `given camera permission not granted and should show rationale, then dialog message is correct`() {
        barcodeScanningViewModel.updatePermissionState(
            isPermissionGranted = false,
            shouldShowRequestPermissionRationale = true
        )

        assertThat(
            (barcodeScanningViewModel.permissionState.value as ShouldShowRationale).message
        ).isEqualTo(R.string.barcode_scanning_alert_dialog_rationale_message)
    }

    @Test
    fun `given camera permission not granted and should show rationale, then cta label is correct`() {
        barcodeScanningViewModel.updatePermissionState(
            isPermissionGranted = false,
            shouldShowRequestPermissionRationale = true
        )

        assertThat(
            (barcodeScanningViewModel.permissionState.value as ShouldShowRationale).ctaLabel
        ).isEqualTo(R.string.barcode_scanning_alert_dialog_rationale_cta_label)
    }

    @Test
    fun `given camera permission not granted and should show rationale, when the CTA is clicked, then trigger proper event`() {
        barcodeScanningViewModel.updatePermissionState(
            isPermissionGranted = false,
            shouldShowRequestPermissionRationale = true
        )

        (barcodeScanningViewModel.permissionState.value as ShouldShowRationale).ctaAction.invoke(mock())

        assertThat(barcodeScanningViewModel.event.value).isInstanceOf(LaunchCameraPermission::class.java)
    }

    @Test
    fun `given camera permission not granted and should not show rationale, then update the PermissionState accordingly`() {
        barcodeScanningViewModel.updatePermissionState(
            isPermissionGranted = false,
            shouldShowRequestPermissionRationale = false
        )

        assertThat(barcodeScanningViewModel.permissionState.value).isInstanceOf(PermanentlyDenied::class.java)
    }

    @Test
    fun `given camera permission not granted and should not show rationale, then dialog title is correct`() {
        barcodeScanningViewModel.updatePermissionState(
            isPermissionGranted = false,
            shouldShowRequestPermissionRationale = false
        )

        assertThat(
            (barcodeScanningViewModel.permissionState.value as PermanentlyDenied).title
        ).isEqualTo(R.string.barcode_scanning_alert_dialog_title)
    }

    @Test
    fun `given camera permission not granted and should not show rationale, then dialog message is correct`() {
        barcodeScanningViewModel.updatePermissionState(
            isPermissionGranted = false,
            shouldShowRequestPermissionRationale = false
        )

        assertThat(
            (barcodeScanningViewModel.permissionState.value as PermanentlyDenied).message
        ).isEqualTo(R.string.barcode_scanning_alert_dialog_permanently_denied_message)
    }

    @Test
    fun `given camera permission not granted and should not show rationale, then dialog cta label is correct`() {
        barcodeScanningViewModel.updatePermissionState(
            isPermissionGranted = false,
            shouldShowRequestPermissionRationale = false
        )

        assertThat(
            (barcodeScanningViewModel.permissionState.value as PermanentlyDenied).ctaLabel
        ).isEqualTo(R.string.barcode_scanning_alert_dialog_permanently_denied_cta_label)
    }

    @Test
    fun `given camera permission not granted and should show not rationale, when the CTA is clicked, then trigger proper event`() {
        barcodeScanningViewModel.updatePermissionState(
            isPermissionGranted = false,
            shouldShowRequestPermissionRationale = false
        )

        (barcodeScanningViewModel.permissionState.value as PermanentlyDenied).ctaAction.invoke(mock())

        assertThat(barcodeScanningViewModel.event.value).isInstanceOf(OpenAppSettings::class.java)
    }

    @Test
    fun `given camera permission permanently denied, when the cancel is clicked, then trigger proper event`() {
        barcodeScanningViewModel.updatePermissionState(
            isPermissionGranted = false,
            shouldShowRequestPermissionRationale = false
        )

        (barcodeScanningViewModel.permissionState.value as PermanentlyDenied).dismissCtaAction.invoke()

        assertThat(barcodeScanningViewModel.event.value).isInstanceOf(Exit::class.java)
    }

    @Test
    fun `given camera permission is denied once, when the cancel is clicked, then trigger proper event`() {
        barcodeScanningViewModel.updatePermissionState(
            isPermissionGranted = false,
            shouldShowRequestPermissionRationale = true
        )

        (barcodeScanningViewModel.permissionState.value as ShouldShowRationale).dismissCtaAction.invoke()

        assertThat(barcodeScanningViewModel.event.value).isInstanceOf(Exit::class.java)
    }

    @Test
    fun `given camera permission is permanently denied, when alert dialog shown, then dismiss CTA label is correct`() {
        barcodeScanningViewModel.updatePermissionState(
            isPermissionGranted = false,
            shouldShowRequestPermissionRationale = false
        )

        assertThat(
            (barcodeScanningViewModel.permissionState.value as PermanentlyDenied).dismissCtaLabel
        ).isEqualTo(R.string.barcode_scanning_alert_dialog_dismiss_label)
    }

    @Test
    fun `given camera permission is denied once, when alert dialog shown, then dismiss CTA label is correct`() {
        barcodeScanningViewModel.updatePermissionState(
            isPermissionGranted = false,
            shouldShowRequestPermissionRationale = true
        )

        assertThat(
            (barcodeScanningViewModel.permissionState.value as ShouldShowRationale).dismissCtaLabel
        ).isEqualTo(R.string.barcode_scanning_alert_dialog_dismiss_label)
    }

    @Test
    fun `when onBindingException is called, then trigger proper event`() {
        val exception = IllegalStateException("Test exception")

        barcodeScanningViewModel.onBindingException(exception)

        assertThat(barcodeScanningViewModel.event.value)
            .isEqualTo(
                BarcodeScanningViewModel.ScanningEvents.OnScanningResult(
                    CodeScannerStatus.Failure(
                        error = exception.message,
                        type = CodeScanningErrorType.Other(exception)
                    )
                )
            )
    }

    @Test
    fun `given recognition started and frame with code, when onNewFrame is called, then scanning result success emitted`() =
        runTest {
            val imageProxy: ImageProxy = mock()
            val codeScannerStatus = CodeScannerStatus.Success(
                "123",
                GoogleBarcodeFormatMapper.BarcodeFormat.FormatCode128
            )
            whenever(codeScanner.recogniseCode(imageProxy)).thenReturn(codeScannerStatus)
            barcodeScanningViewModel.startCodesRecognition()

            barcodeScanningViewModel.onNewFrame(imageProxy)

            assertThat(barcodeScanningViewModel.event.value)
                .isEqualTo(
                    BarcodeScanningViewModel.ScanningEvents.OnScanningResult(
                        codeScannerStatus
                    )
                )
        }

    @Test
    fun `given recognition not started and frame, when onNewFrame is called, then nothing is emitted`() =
        runTest {
            val imageProxy: ImageProxy = mock()

            barcodeScanningViewModel.onNewFrame(imageProxy)

            assertThat(barcodeScanningViewModel.event.value).isNull()
        }

    @Test
    fun `given recognition started and stopped, when onNewFrame is called, then nothing is emitted`() =
        runTest {
            val imageProxy: ImageProxy = mock()
            barcodeScanningViewModel.startCodesRecognition()
            barcodeScanningViewModel.stopCodesRecognition()

            barcodeScanningViewModel.onNewFrame(imageProxy)

            assertThat(barcodeScanningViewModel.event.value).isNull()
        }

    @Test
    fun `given recognition started and frame without code, when onNewFrame is called, then nothing is emitted`() =
        runTest {
            val imageProxy: ImageProxy = mock()
            val codeScannerStatus = CodeScannerStatus.NotFound
            whenever(codeScanner.recogniseCode(imageProxy)).thenReturn(codeScannerStatus)
            barcodeScanningViewModel.startCodesRecognition()

            barcodeScanningViewModel.onNewFrame(imageProxy)

            assertThat(barcodeScanningViewModel.event.value).isNull()
        }

    @Test
    fun `given recognition started and frame recognised with error, when onNewFrame is called, then error is emitted`() =
        runTest {
            val imageProxy: ImageProxy = mock()
            val codeScannerStatus = CodeScannerStatus.Failure(
                error = "Test error",
                type = CodeScanningErrorType.Other(Throwable("Test error"))
            )
            whenever(codeScanner.recogniseCode(imageProxy)).thenReturn(codeScannerStatus)
            barcodeScanningViewModel.startCodesRecognition()

            barcodeScanningViewModel.onNewFrame(imageProxy)

            assertThat(barcodeScanningViewModel.event.value)
                .isEqualTo(
                    BarcodeScanningViewModel.ScanningEvents.OnScanningResult(
                        codeScannerStatus
                    )
                )
        }

    @Test
    fun `given recognition started and stopped and started, when onNewFrame is called, then success emitted`() =
        runTest {
            val imageProxy: ImageProxy = mock()
            val codeScannerStatus = CodeScannerStatus.Success(
                "123",
                GoogleBarcodeFormatMapper.BarcodeFormat.FormatCode128
            )
            whenever(codeScanner.recogniseCode(imageProxy)).thenReturn(codeScannerStatus)
            barcodeScanningViewModel.startCodesRecognition()
            barcodeScanningViewModel.stopCodesRecognition()
            barcodeScanningViewModel.startCodesRecognition()

            barcodeScanningViewModel.onNewFrame(imageProxy)

            assertThat(barcodeScanningViewModel.event.value)
                .isEqualTo(
                    BarcodeScanningViewModel.ScanningEvents.OnScanningResult(
                        codeScannerStatus
                    )
                )
        }

    @Test
    fun `given recognition started, when onNewFrame is called multiple times, then success emitted multiple times`() =
        runTest {
            val imageProxy: ImageProxy = mock()
            val codeScannerStatus = CodeScannerStatus.Success(
                "123",
                GoogleBarcodeFormatMapper.BarcodeFormat.FormatCode128
            )
            whenever(codeScanner.recogniseCode(imageProxy)).thenReturn(codeScannerStatus)
            barcodeScanningViewModel.startCodesRecognition()

            val events = barcodeScanningViewModel.event.captureValues()

            repeat(200) {
                barcodeScanningViewModel.onNewFrame(imageProxy)
            }

            assertThat(events).hasSize(200)
        }

    @Test
    fun `given recognition started and slow, when onNewFrame is called multiple times, then success emitted buffer size times`() =
        runTest {
            val imageProxy: ImageProxy = mock()
            val codeScannerStatus = CodeScannerStatus.Success(
                "123",
                GoogleBarcodeFormatMapper.BarcodeFormat.FormatCode128
            )
            whenever(codeScanner.recogniseCode(imageProxy)).doSuspendableAnswer {
                delay(1)
                codeScannerStatus
            }
            barcodeScanningViewModel.startCodesRecognition()

            val events = mutableListOf<MultiLiveEvent.Event>()
            barcodeScanningViewModel.event.observeForever {
                events.add(it)
            }

            repeat(200) {
                barcodeScanningViewModel.onNewFrame(imageProxy)
            }
            advanceUntilIdle()

            assertThat(events).hasSize(31)
        }
}
