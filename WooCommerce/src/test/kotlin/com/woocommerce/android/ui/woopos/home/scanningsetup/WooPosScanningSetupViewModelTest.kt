package com.woocommerce.android.ui.woopos.home.scanningsetup

import app.cash.turbine.test
import com.woocommerce.android.R
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
        whenever(navigator.getNextStep(BarcodeReaderDevice.OTHER, ScanningSetupStep.DeviceSelection))
            .thenReturn(ScanningSetupStep.ScannerSetupInfo)
        val viewModel = createViewModel()
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.OTHER))

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
        val mockNextStep = ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = android.R.drawable.ic_delete)
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.DeviceSelection))
            .thenReturn(mockNextStep)
        val viewModel = createViewModel()

        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))
        assertThat(viewModel.state.value.currentStep).isEqualTo(mockNextStep)
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
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.DeviceSelection))
            .thenReturn(ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = android.R.drawable.ic_delete))
        whenever(
            navigator.getNextStep(
                BarcodeReaderDevice.TERA_1200,
                ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = android.R.drawable.ic_delete)
            )
        )
            .thenReturn(ScanningSetupStep.ScannerPairModeSetup())
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.ScannerPairModeSetup()))
            .thenReturn(ScanningSetupStep.PairYourScanner(R.string.woopos_scanning_setup_device_tera_1200))
        whenever(
            navigator.getNextStep(
                BarcodeReaderDevice.TERA_1200,
                ScanningSetupStep.PairYourScanner(R.string.woopos_scanning_setup_device_tera_1200)
            )
        )
            .thenReturn(ScanningSetupStep.TestYourScanner)
        whenever(navigator.getNextStepForValidBarcode(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.TestYourScanner))
            .thenReturn(mockNextStep)
        val viewModel = createViewModel()

        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

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
            whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.DeviceSelection))
                .thenReturn(ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = android.R.drawable.ic_delete))
            whenever(
                navigator.getNextStep(
                    BarcodeReaderDevice.TERA_1200,
                    ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = android.R.drawable.ic_delete)
                )
            )
                .thenReturn(ScanningSetupStep.ScannerPairModeSetup())
            whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.ScannerPairModeSetup()))
                .thenReturn(ScanningSetupStep.PairYourScanner(R.string.woopos_scanning_setup_device_tera_1200))
            whenever(
                navigator.getNextStep(
                    BarcodeReaderDevice.TERA_1200,
                    ScanningSetupStep.PairYourScanner(R.string.woopos_scanning_setup_device_tera_1200)
                )
            ).thenReturn(ScanningSetupStep.TestYourScanner)
            whenever(navigator.getNextStepForInvalidBarcode(ScanningSetupStep.TestYourScanner))
                .thenReturn(ScanningSetupStep.TestYourScannerScanFailed)
            val viewModel = createViewModel()

            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

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
            whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.DeviceSelection))
                .thenReturn(ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = android.R.drawable.ic_delete))
            whenever(
                navigator.getNextStep(
                    BarcodeReaderDevice.TERA_1200,
                    ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = android.R.drawable.ic_delete)
                )
            )
                .thenReturn(ScanningSetupStep.ScannerPairModeSetup())
            whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.ScannerPairModeSetup()))
                .thenReturn(ScanningSetupStep.PairYourScanner(R.string.woopos_scanning_setup_device_tera_1200))
            whenever(
                navigator.getNextStep(
                    BarcodeReaderDevice.TERA_1200,
                    ScanningSetupStep.PairYourScanner(R.string.woopos_scanning_setup_device_tera_1200)
                )
            )
                .thenReturn(ScanningSetupStep.TestYourScanner)
            whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.TestYourScanner))
                .thenReturn(mockNextStep)
            whenever(
                navigator.getNextStepForValidBarcode(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.TestYourScanner)
            ).thenReturn(mockNextStep)
            val viewModel = createViewModel()

            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

            advanceTimeBy(11000L)

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
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.DeviceSelection))
            .thenReturn(ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = android.R.drawable.ic_delete))
        whenever(
            navigator.getNextStep(
                BarcodeReaderDevice.TERA_1200,
                ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = android.R.drawable.ic_delete)
            )
        )
            .thenReturn(ScanningSetupStep.ScannerPairModeSetup())
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.ScannerPairModeSetup()))
            .thenReturn(ScanningSetupStep.PairYourScanner(R.string.woopos_scanning_setup_device_tera_1200))
        whenever(
            navigator.getNextStep(
                BarcodeReaderDevice.TERA_1200,
                ScanningSetupStep.PairYourScanner(R.string.woopos_scanning_setup_device_tera_1200)
            )
        )
            .thenReturn(ScanningSetupStep.TestYourScanner)
        whenever(navigator.isStillOnTestBarcodeStep(ScanningSetupStep.TestYourScanner))
            .thenReturn(true)
        whenever(navigator.getTestBarcodeTimeoutStep(ScanningSetupStep.TestYourScanner))
            .thenReturn(ScanningSetupStep.TestYourScannerTimeout)
        val viewModel = createViewModel()

        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

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
            whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.DeviceSelection))
                .thenReturn(ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = android.R.drawable.ic_delete))
            whenever(
                navigator.getNextStep(
                    BarcodeReaderDevice.TERA_1200,
                    ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = android.R.drawable.ic_delete)
                )
            )
                .thenReturn(ScanningSetupStep.ScannerPairModeSetup())
            whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.ScannerPairModeSetup()))
                .thenReturn(ScanningSetupStep.PairYourScanner(R.string.woopos_scanning_setup_device_tera_1200))
            whenever(
                navigator.getNextStep(
                    BarcodeReaderDevice.TERA_1200,
                    ScanningSetupStep.PairYourScanner(R.string.woopos_scanning_setup_device_tera_1200)
                )
            )
                .thenReturn(ScanningSetupStep.TestYourScanner)
            whenever(navigator.getNextStepForInvalidBarcode(ScanningSetupStep.TestYourScanner))
                .thenReturn(ScanningSetupStep.TestYourScannerScanFailed)
            val viewModel = createViewModel()

            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

            val incorrectBarcode =
                BarcodeInputDetector.BarcodeResult.Success(barcode = "incorrect", scanDurationMs = 1000L)
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnBarcodeScanned(incorrectBarcode))

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
            whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.DeviceSelection))
                .thenReturn(mockPreviousStep)
            whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, mockPreviousStep))
                .thenReturn(ScanningSetupStep.ScannerPairModeSetup())
            whenever(navigator.getPreviousStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.ScannerPairModeSetup()))
                .thenReturn(mockPreviousStep)
            val viewModel = createViewModel()

            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

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
            whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.DeviceSelection))
                .thenReturn(hidModeStep)
            whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, hidModeStep))
                .thenReturn(mockNextStep)
            val viewModel = createViewModel()

            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))

            // WHEN
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

            // THEN
            assertThat(viewModel.state.value.currentStep).isEqualTo(mockNextStep)
        }

    @Test
    fun `given DeviceSelection state, when created, then should have correct string content`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        val step = viewModel.state.value.currentStep as ScanningSetupStep.DeviceSelection

        // THEN
        assertThat(step.title).isEqualTo("Set up a barcode scanner")
    }

    @Test
    fun `given ScannerHIDModeSetup state, when created, then should have correct string content`() = runTest {
        // GIVEN
        val viewModel = createViewModel()
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))

        // WHEN
        val step = viewModel.state.value.currentStep as ScanningSetupStep.ScannerHIDModeSetup

        // THEN
        assertThat(step.title).isEqualTo("Introduction")
        assertThat(step.message).isEqualTo("Introduction message")
        assertThat(step.primaryButtonText).isEqualTo("Next")
        assertThat(step.secondaryButtonText).isEqualTo("Back")
    }

    @Test
    fun `given ScannerPairModeSetup state, when created, then should have correct string content`() = runTest {
        // GIVEN
        val viewModel = createViewModel()
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

        // WHEN
        val step = viewModel.state.value.currentStep as ScanningSetupStep.ScannerPairModeSetup

        // THEN
        assertThat(step.title).isEqualTo("Pair mode")
        assertThat(step.message).isEqualTo("Pair mode message")
        assertThat(step.primaryButtonText).isEqualTo("Next")
        assertThat(step.secondaryButtonText).isEqualTo("Back")
    }

    @Test
    fun `given PairYourScanner state, when created, then should have correct string content`() = runTest {
        // GIVEN
        val viewModel = createViewModel()
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

        // WHEN
        val step = viewModel.state.value.currentStep as ScanningSetupStep.PairYourScanner

        // THEN
        assertThat(step.title).isEqualTo("Pair your scanner")
        assertThat(step.message).isEqualTo("Pair your scanner message TERA 1200")
        assertThat(step.primaryButtonText).isEqualTo("Next")
        assertThat(step.secondaryButtonText).isEqualTo("Back")
        assertThat(step.bluetoothSettingsButtonText).isEqualTo("Go to settings")
    }

    @Test
    fun `given TestYourScanner state, when created, then should have correct string content`() = runTest {
        // GIVEN
        val viewModel = createViewModel()
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

        // WHEN
        val step = viewModel.state.value.currentStep as ScanningSetupStep.TestYourScanner

        // THEN
        assertThat(step.title).isEqualTo("Test scanner")
        assertThat(step.message).isEqualTo("Test scanner message")
        assertThat(step.secondaryButtonText).isEqualTo("Back")
    }

    @Test
    fun `given ScannerSetupSuccess state, when created, then should have correct string content`() = runTest {
        // GIVEN
        val viewModel = createViewModel()
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

        // WHEN
        viewModel.onUiEvent(
            WooPosScanningSetupUiEvent.OnBarcodeScanned(
                com.woocommerce.android.ui.woopos.common.composeui.modifier.BarcodeInputDetector.BarcodeResult.Success(
                    barcode = "1234567890128",
                    scanDurationMs = 100
                )
            )
        )
        val step = viewModel.state.value.currentStep as ScanningSetupStep.ScannerSetupSuccess

        // THEN
        assertThat(step.title).isEqualTo("Success")
        assertThat(step.message).isEqualTo("Success message")
        assertThat(step.moreInfoButtonText).isEqualTo("More information")
    }

    @Test
    fun `given ScannerSetupInfo state, when created, then should have correct string content`() = runTest {
        // GIVEN
        val viewModel = createViewModel()
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.OTHER))

        // WHEN
        val step = viewModel.state.value.currentStep as ScanningSetupStep.ScannerSetupInfo

        // THEN
        assertThat(step.title).isEqualTo("Info")
        assertThat(step.message).isEqualTo("Info message")
        assertThat(step.bulletPoints).containsExactly("Bullet 1", "Bullet 2", "Bullet 3")
        assertThat(step.infoText).isEqualTo("Info text")
        assertThat(step.backButtonText).isEqualTo("Back")
        assertThat(step.doneButtonText).isEqualTo("Done")
    }

    @Test
    fun `given ScannerSetupBarcodesOnProducts state, when created, then should have correct string content`() =
        runTest {
            // GIVEN
            val viewModel = createViewModel()
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

            viewModel.onUiEvent(
                WooPosScanningSetupUiEvent.OnBarcodeScanned(
                    BarcodeInputDetector.BarcodeResult.Success(
                        barcode = "1234567890128",
                        scanDurationMs = 100
                    )
                )
            )

            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

            // WHEN
            val step = viewModel.state.value.currentStep as ScanningSetupStep.ScannerSetupBarcodesOnProducts

            // THEN
            assertThat(step.title).isEqualTo("How to set up barcodes on products")
            assertThat(step.message).isEqualTo(
                "You can set up barcodes in the GTIN, UPC, EAN, ISBN field in the product's inventory tab. " +
                    "For more details"
            )
            assertThat(step.doneButtonText).isEqualTo("Done")
            assertThat(step.backButtonText).isEqualTo("Back")
        }

    private fun createViewModel(): WooPosScanningSetupViewModel {
        whenever(navigator.getInitialStep()).thenReturn(ScanningSetupStep.DeviceSelection)
        return WooPosScanningSetupViewModel(navigator)
    }
}
