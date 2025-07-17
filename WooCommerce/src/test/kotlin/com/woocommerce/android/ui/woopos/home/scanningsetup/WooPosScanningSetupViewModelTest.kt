package com.woocommerce.android.ui.woopos.home.scanningsetup

import app.cash.turbine.test
import com.woocommerce.android.ui.woopos.common.composeui.modifier.BarcodeInputDetector
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.BarcodeReaderDevice
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.Companion.TEST_BARCODE_EAN13
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.ScanningSetupStep
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class WooPosScanningSetupViewModelTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val navigator: WooPosScannerSetupNavigator = mock()

    @Test
    fun `given initialized, when no action taken, then should start with DeviceSelection step`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // THEN
        assertThat(viewModel.state.value.currentStep)
            .isInstanceOf(ScanningSetupStep.DeviceSelection::class.java)
        assertThat(viewModel.state.value.selectedDevice).isNull()
    }

    @Test
    fun `given DeviceSelection step, when device selected, then should update state with selected device`() = runTest {
        // GIVEN
        val mockNextStep = ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = android.R.drawable.ic_delete)
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.DeviceSelection))
            .thenReturn(mockNextStep)
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))

        // THEN
        assertThat(viewModel.state.value.selectedDevice).isEqualTo(BarcodeReaderDevice.TERA_1200)
        assertThat(viewModel.state.value.currentStep).isEqualTo(mockNextStep)
    }

    @Test
    fun `given ScannerSetupInfo step, when primary button clicked, then should emit dismiss dialog event`() = runTest {
        // GIVEN
        val viewModel = createViewModel(initialStep = ScanningSetupStep.ScannerSetupInfo)

        viewModel.dismissDialogEvent.test {
            // WHEN
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

            // THEN
            awaitItem()
        }
    }

    @Test
    fun `given OnOpenBluetoothSettings event, when triggered, then should emit openBluetoothSettingsEvent`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        viewModel.openBluetoothSettingsEvent.test {
            // WHEN
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnOpenBluetoothSettings)

            // THEN
            awaitItem()
        }
    }

    @Test
    fun `given resetToInitialState called, when invoked, then should reset to initial state`() = runTest {
        // GIVEN
        val mockStep = ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = android.R.drawable.ic_delete)
        val viewModel = createViewModel(
            initialStep = mockStep,
            selectedDevice = BarcodeReaderDevice.TERA_1200
        )
        assertThat(viewModel.state.value.currentStep).isEqualTo(mockStep)
        assertThat(viewModel.state.value.selectedDevice).isEqualTo(BarcodeReaderDevice.TERA_1200)

        // WHEN
        viewModel.resetToInitialState()

        // THEN
        assertThat(viewModel.state.value.currentStep)
            .isInstanceOf(ScanningSetupStep.DeviceSelection::class.java)
        assertThat(viewModel.state.value.selectedDevice).isNull()
    }

    @Test
    fun `given TestYourScanner step, when correct barcode scanned, then should navigate to next step`() = runTest {
        // GIVEN
        val mockNextStep = ScanningSetupStep.ScannerSetupSuccess
        whenever(navigator.getNextStepForValidBarcode(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.TestYourScanner))
            .thenReturn(mockNextStep)
        val viewModel = createViewModel(
            initialStep = ScanningSetupStep.TestYourScanner,
            selectedDevice = BarcodeReaderDevice.TERA_1200
        )

        // WHEN
        val correctBarcode =
            BarcodeInputDetector.BarcodeResult.Success(barcode = TEST_BARCODE_EAN13, scanDurationMs = 1000L)
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnBarcodeScanned(correctBarcode))

        // THEN
        assertThat(viewModel.state.value.currentStep).isEqualTo(mockNextStep)
    }

    @Test
    fun `given TestYourScanner step, when incorrect barcode scanned, then should navigate to scan failed step`() =
        runTest {
            // GIVEN
            whenever(navigator.getNextStepForInvalidBarcode(ScanningSetupStep.TestYourScanner))
                .thenReturn(ScanningSetupStep.TestYourScannerScanFailed)
            val viewModel = createViewModel(
                initialStep = ScanningSetupStep.TestYourScanner,
                selectedDevice = BarcodeReaderDevice.TERA_1200
            )

            // WHEN
            val incorrectBarcode =
                BarcodeInputDetector.BarcodeResult.Success(barcode = "incorrect", scanDurationMs = 1000L)
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnBarcodeScanned(incorrectBarcode))

            // THEN
            assertThat(viewModel.state.value.currentStep)
                .isInstanceOf(ScanningSetupStep.TestYourScannerScanFailed::class.java)
        }

    @Test
    fun `given TestYourScannerTimeout step, when correct barcode scanned, then should navigate to next step`() =
        runTest {
            // GIVEN
            val mockNextStep = ScanningSetupStep.ScannerSetupSuccess
            whenever(
                navigator.getNextStepForValidBarcode(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.TestYourScannerTimeout)
            ).thenReturn(mockNextStep)
            val viewModel = createViewModel(
                initialStep = ScanningSetupStep.TestYourScannerTimeout,
                selectedDevice = BarcodeReaderDevice.TERA_1200
            )

            // WHEN
            val correctBarcode =
                BarcodeInputDetector.BarcodeResult.Success(barcode = TEST_BARCODE_EAN13, scanDurationMs = 1000L)
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnBarcodeScanned(correctBarcode))

            // THEN
            assertThat(viewModel.state.value.currentStep).isEqualTo(mockNextStep)
        }

    @Test
    fun `given TestYourScanner step, when timeout reached, then should navigate to timeout step`() = runTest {
        // GIVEN
        whenever(navigator.isStillOnTestBarcodeStep(ScanningSetupStep.TestYourScanner))
            .thenReturn(true)
        whenever(navigator.getTestBarcodeTimeoutStep(ScanningSetupStep.TestYourScanner))
            .thenReturn(ScanningSetupStep.TestYourScannerTimeout)
        val viewModel = createViewModel(
            initialStep = ScanningSetupStep.TestYourScanner,
            selectedDevice = BarcodeReaderDevice.TERA_1200
        )

        // WHEN
        advanceTimeBy(11000L)

        // THEN
        assertThat(viewModel.state.value.currentStep)
            .isInstanceOf(ScanningSetupStep.TestYourScannerTimeout::class.java)
    }

    @Test
    fun `given TestYourScannerScanFailed step, when primary button clicked, then should reset to initial state`() =
        runTest {
            // GIVEN
            val viewModel = createViewModel(
                initialStep = ScanningSetupStep.TestYourScannerScanFailed,
                selectedDevice = BarcodeReaderDevice.TERA_1200
            )

            // WHEN
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

            // THEN
            assertThat(viewModel.state.value.currentStep)
                .isInstanceOf(ScanningSetupStep.DeviceSelection::class.java)
            assertThat(viewModel.state.value.selectedDevice).isNull()
        }

    @Test
    fun `given any step with secondary button, when secondary button clicked, then should delegate to navigator for previous step`() =
        runTest {
            // GIVEN
            val mockPreviousStep = ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = android.R.drawable.ic_delete)
            whenever(navigator.getPreviousStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.ScannerPairModeSetup()))
                .thenReturn(mockPreviousStep)
            val viewModel = createViewModel(
                initialStep = ScanningSetupStep.ScannerPairModeSetup(),
                selectedDevice = BarcodeReaderDevice.TERA_1200
            )

            // WHEN
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnSecondaryButtonClicked)

            // THEN
            assertThat(viewModel.state.value.currentStep).isEqualTo(mockPreviousStep)
        }

    @Test
    fun `given step with primary button, when primary button clicked, then should delegate to navigator for next step`() =
        runTest {
            // GIVEN
            val hidModeStep = ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = android.R.drawable.ic_delete)
            val mockNextStep = ScanningSetupStep.ScannerPairModeSetup()
            whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, hidModeStep))
                .thenReturn(mockNextStep)
            val viewModel = createViewModel(
                initialStep = hidModeStep,
                selectedDevice = BarcodeReaderDevice.TERA_1200
            )

            // WHEN
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

            // THEN
            assertThat(viewModel.state.value.currentStep).isEqualTo(mockNextStep)
        }

    private fun createViewModel(
        initialStep: ScanningSetupStep = ScanningSetupStep.DeviceSelection,
        selectedDevice: BarcodeReaderDevice? = null
    ): WooPosScanningSetupViewModel {
        whenever(navigator.getInitialStep()).thenReturn(initialStep)
        val viewModel = WooPosScanningSetupViewModel(navigator)

        selectedDevice?.let {
            whenever(navigator.getNextStep(it, initialStep)).thenReturn(initialStep)
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(it))
        }

        return viewModel
    }
}
