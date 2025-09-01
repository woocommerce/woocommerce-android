package com.woocommerce.android.ui.woopos.scanningsetup

import app.cash.turbine.test
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.woopos.common.composeui.modifier.BarcodeInputDetector
import com.woocommerce.android.ui.woopos.scanningsetup.WooPosScannerDetectionServiceForTracking
import com.woocommerce.android.ui.woopos.scanningsetup.WooPosScannerSetupNavigator
import com.woocommerce.android.ui.woopos.scanningsetup.WooPosScanningSetupAnalyticsTracker
import com.woocommerce.android.ui.woopos.scanningsetup.WooPosScanningSetupState.BarcodeReaderDevice
import com.woocommerce.android.ui.woopos.scanningsetup.WooPosScanningSetupState.Companion.TEST_BARCODE_EAN13
import com.woocommerce.android.ui.woopos.scanningsetup.WooPosScanningSetupState.ScanningSetupStep
import com.woocommerce.android.ui.woopos.scanningsetup.WooPosScanningSetupUiEvent
import com.woocommerce.android.ui.woopos.scanningsetup.WooPosScanningSetupViewModel
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class WooPosScanningSetupViewModelTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val navigator: WooPosScannerSetupNavigator = mock()
    private val analyticsTracker: WooPosScanningSetupAnalyticsTracker = mock()
    private val scannerDetectionService: WooPosScannerDetectionServiceForTracking = mock()

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
        val mockNextStep = ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = R.drawable.ic_barcode)
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
    fun `given ScannerSetupInfo step, when primary button clicked, then should navigate to next step`() = runTest {
        // GIVEN
        whenever(navigator.getNextStep(BarcodeReaderDevice.OTHER, ScanningSetupStep.DeviceSelection))
            .thenReturn(ScanningSetupStep.ScannerSetupInfo)
        whenever(navigator.getNextStep(BarcodeReaderDevice.OTHER, ScanningSetupStep.ScannerSetupInfo))
            .thenReturn(ScanningSetupStep.TestYourScanner)
        val viewModel = createViewModel()
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.OTHER))

        // WHEN
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

        // THEN
        assertThat(viewModel.state.value.currentStep).isEqualTo(ScanningSetupStep.TestYourScanner)
    }

    @Test
    fun `given ScannerSetupBarcodesOnProducts step, when primary button clicked, then should emit dismiss dialog event`() =
        runTest {
            // GIVEN
            val viewModel = createViewModel(initialStep = ScanningSetupStep.ScannerSetupBarcodesOnProducts)

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
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.DeviceSelection))
            .thenReturn(ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = R.drawable.ic_barcode))
        val viewModel = createViewModel()
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))

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
        val mockStep = ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = R.drawable.ic_barcode)
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.DeviceSelection))
            .thenReturn(mockStep)
        val viewModel = createViewModel()
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))
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
    fun `given DeviceSelection step, when barcode scanned, then scan ignored`() = runTest {
        // GIVEN
        val mockNextStep = ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = R.drawable.ic_barcode)
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.DeviceSelection))
            .thenReturn(mockNextStep)
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUiEvent(
            WooPosScanningSetupUiEvent.OnBarcodeScanned(
                BarcodeInputDetector.BarcodeResult.Success(barcode = TEST_BARCODE_EAN13, scanDurationMs = 1000L)
            )
        )

        // THEN
        assertThat(viewModel.state.value.selectedDevice).isNull()
        assertThat(viewModel.state.value.currentStep).isEqualTo(ScanningSetupStep.DeviceSelection)
    }

    @Test
    fun `given ScannerSetupInfo step, when barcode scanned, then scan ignored`() = runTest {
        // GIVEN
        whenever(navigator.getNextStep(BarcodeReaderDevice.OTHER, ScanningSetupStep.DeviceSelection))
            .thenReturn(ScanningSetupStep.ScannerSetupInfo)
        whenever(navigator.getNextStep(BarcodeReaderDevice.OTHER, ScanningSetupStep.ScannerSetupInfo))
            .thenReturn(ScanningSetupStep.TestYourScanner)
        val viewModel = createViewModel()
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.OTHER))

        // WHEN
        viewModel.onUiEvent(
            WooPosScanningSetupUiEvent.OnBarcodeScanned(
                BarcodeInputDetector.BarcodeResult.Success(barcode = TEST_BARCODE_EAN13, scanDurationMs = 1000L)
            )
        )

        // THEN
        assertThat(viewModel.state.value.currentStep).isEqualTo(ScanningSetupStep.ScannerSetupInfo)
    }

    @Test
    fun `given ScannerHIDModeSetup step, when barcode scanned, then scan ignored`() = runTest {
        // GIVEN
        val hidModeStep = ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = R.drawable.ic_barcode)
        val viewModel = createViewModel(
            initialStep = hidModeStep,
            selectedDevice = BarcodeReaderDevice.TERA_1200
        )

        // WHEN
        viewModel.onUiEvent(
            WooPosScanningSetupUiEvent.OnBarcodeScanned(
                BarcodeInputDetector.BarcodeResult.Success(barcode = TEST_BARCODE_EAN13, scanDurationMs = 1000L)
            )
        )

        // THEN
        assertThat(viewModel.state.value.currentStep).isEqualTo(hidModeStep)
    }

    @Test
    fun `given ScannerPairModeSetup step, when barcode scanned, then scan ignored`() = runTest {
        // GIVEN
        val pairModeStep = ScanningSetupStep.ScannerPairModeSetup(qrCodeImageRes = R.drawable.ic_barcode)
        val viewModel = createViewModel(
            initialStep = pairModeStep,
            selectedDevice = BarcodeReaderDevice.TERA_1200
        )

        // WHEN
        viewModel.onUiEvent(
            WooPosScanningSetupUiEvent.OnBarcodeScanned(
                BarcodeInputDetector.BarcodeResult.Success(barcode = TEST_BARCODE_EAN13, scanDurationMs = 1000L)
            )
        )

        // THEN
        assertThat(viewModel.state.value.currentStep).isEqualTo(pairModeStep)
    }

    @Test
    fun `given PairYourScanner step, when barcode scanned, then scan ignored`() = runTest {
        // GIVEN
        val pairYourScannerStep = ScanningSetupStep.PairYourScanner(R.string.woopos_scanning_setup_device_tera_1200)
        val viewModel = createViewModel(
            initialStep = pairYourScannerStep,
            selectedDevice = BarcodeReaderDevice.TERA_1200
        )

        // WHEN
        viewModel.onUiEvent(
            WooPosScanningSetupUiEvent.OnBarcodeScanned(
                BarcodeInputDetector.BarcodeResult.Success(barcode = TEST_BARCODE_EAN13, scanDurationMs = 1000L)
            )
        )

        // THEN
        assertThat(viewModel.state.value.currentStep).isEqualTo(pairYourScannerStep)
    }

    @Test
    fun `given ScannerSetupSuccess step, when barcode scanned, then scan ignored`() = runTest {
        // GIVEN
        val viewModel = createViewModel(
            initialStep = ScanningSetupStep.ScannerSetupSuccess,
            selectedDevice = BarcodeReaderDevice.TERA_1200
        )

        // WHEN
        viewModel.onUiEvent(
            WooPosScanningSetupUiEvent.OnBarcodeScanned(
                BarcodeInputDetector.BarcodeResult.Success(barcode = TEST_BARCODE_EAN13, scanDurationMs = 1000L)
            )
        )

        // THEN
        assertThat(viewModel.state.value.currentStep).isEqualTo(ScanningSetupStep.ScannerSetupSuccess)
    }

    @Test
    fun `given TestYourScannerScanFailed step, when barcode scanned, then scan ignored`() = runTest {
        // GIVEN
        val viewModel = createViewModel(
            initialStep = ScanningSetupStep.TestYourScannerScanFailed,
            selectedDevice = BarcodeReaderDevice.TERA_1200
        )

        // WHEN
        viewModel.onUiEvent(
            WooPosScanningSetupUiEvent.OnBarcodeScanned(
                BarcodeInputDetector.BarcodeResult.Success(barcode = TEST_BARCODE_EAN13, scanDurationMs = 1000L)
            )
        )

        // THEN
        assertThat(viewModel.state.value.currentStep).isEqualTo(ScanningSetupStep.TestYourScannerScanFailed)
    }

    @Test
    fun `given ScannerSetupBarcodesOnProducts step, when barcode scanned, then scan ignored`() = runTest {
        // GIVEN
        val viewModel = createViewModel(
            initialStep = ScanningSetupStep.ScannerSetupBarcodesOnProducts,
            selectedDevice = BarcodeReaderDevice.TERA_1200
        )

        // WHEN
        viewModel.onUiEvent(
            WooPosScanningSetupUiEvent.OnBarcodeScanned(
                BarcodeInputDetector.BarcodeResult.Success(barcode = TEST_BARCODE_EAN13, scanDurationMs = 1000L)
            )
        )

        // THEN
        assertThat(viewModel.state.value.currentStep).isEqualTo(ScanningSetupStep.ScannerSetupBarcodesOnProducts)
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
                navigator.getNextStepForValidBarcode(
                    BarcodeReaderDevice.TERA_1200,
                    ScanningSetupStep.TestYourScannerTimeout
                )
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
        val mockIntermediateStep = ScanningSetupStep.ScannerSetupInfo
        whenever(navigator.isStillOnTestBarcodeStep(ScanningSetupStep.TestYourScanner))
            .thenReturn(true)
        whenever(navigator.getTestBarcodeTimeoutStep(ScanningSetupStep.TestYourScanner))
            .thenReturn(ScanningSetupStep.TestYourScannerTimeout)
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.DeviceSelection))
            .thenReturn(mockIntermediateStep)
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, mockIntermediateStep))
            .thenReturn(ScanningSetupStep.TestYourScanner)
        val viewModel = createViewModel()
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))
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
                .thenReturn(ScanningSetupStep.TestYourScannerScanFailed)
            val viewModel = createViewModel()
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))

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
            whenever(
                navigator.getPreviousStep(
                    BarcodeReaderDevice.TERA_1200,
                    ScanningSetupStep.ScannerPairModeSetup(
                        qrCodeImageRes = R.drawable.ic_woopos_reader_setup_code_hid_tera_1200
                    )
                )
            )
                .thenReturn(mockPreviousStep)
            val viewModel = createViewModel(
                initialStep = ScanningSetupStep.ScannerPairModeSetup(
                    qrCodeImageRes = R.drawable.ic_woopos_reader_setup_code_hid_tera_1200
                ),
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
            val hidModeStep = ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = R.drawable.ic_barcode)
            val mockNextStep = ScanningSetupStep.ScannerPairModeSetup(qrCodeImageRes = R.drawable.ic_barcode)
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
        assertThat(step.titleRes).isEqualTo(R.string.woopos_scanning_setup_device_selection_title)
    }

    @Test
    fun `given ScannerHIDModeSetup state, when created, then should have correct string content`() = runTest {
        // GIVEN
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.DeviceSelection))
            .thenReturn(ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = R.drawable.ic_barcode))
        val viewModel = createViewModel()
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))

        // WHEN
        val step = viewModel.state.value.currentStep as ScanningSetupStep.ScannerHIDModeSetup

        // THEN
        assertThat(step.titleRes).isEqualTo(R.string.woopos_scanning_setup_hid_title)
        assertThat(step.messageRes).isEqualTo(R.string.woopos_scanning_setup_hid_message)
        assertThat(step.primaryButtonTextRes).isEqualTo(R.string.woopos_scanning_setup_button_next)
        assertThat(step.secondaryButtonTextRes).isEqualTo(R.string.woopos_scanning_setup_button_back)
    }

    @Test
    fun `given ScannerPairModeSetup state, when created, then should have correct string content`() = runTest {
        // GIVEN
        val hidModeStep = ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = R.drawable.ic_barcode)
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.DeviceSelection))
            .thenReturn(hidModeStep)
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, hidModeStep))
            .thenReturn(ScanningSetupStep.ScannerPairModeSetup(qrCodeImageRes = R.drawable.ic_barcode))
        val viewModel = createViewModel()
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

        // WHEN
        val step = viewModel.state.value.currentStep as ScanningSetupStep.ScannerPairModeSetup

        // THEN
        assertThat(step.titleRes).isEqualTo(R.string.woopos_scanning_setup_scanner_pair_mode_title)
        assertThat(step.messageRes).isEqualTo(R.string.woopos_scanning_setup_scanner_pair_mode_message)
        assertThat(step.primaryButtonTextRes).isEqualTo(R.string.woopos_scanning_setup_button_next)
        assertThat(step.secondaryButtonTextRes).isEqualTo(R.string.woopos_scanning_setup_button_back)
    }

    @Test
    fun `given PairYourScanner state, when created, then should have correct string content`() = runTest {
        // GIVEN
        val hidModeStep = ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = R.drawable.ic_barcode)
        val pairModeStep = ScanningSetupStep.ScannerPairModeSetup(qrCodeImageRes = R.drawable.ic_barcode)
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.DeviceSelection))
            .thenReturn(hidModeStep)
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, hidModeStep))
            .thenReturn(pairModeStep)
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, pairModeStep))
            .thenReturn(ScanningSetupStep.PairYourScanner(R.string.woopos_scanning_setup_device_tera_1200))
        val viewModel = createViewModel()
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

        // WHEN
        val step = viewModel.state.value.currentStep as ScanningSetupStep.PairYourScanner

        // THEN
        assertThat(step.titleRes).isEqualTo(R.string.woopos_scanning_setup_pair_your_scanner_title)
        assertThat(step.messageRes).isEqualTo(
            UiString.UiStringRes(
                R.string.woopos_scanning_setup_pair_your_scanner_message,
                listOf(UiString.UiStringRes(R.string.woopos_scanning_setup_device_tera_1200))
            )
        )
        assertThat(step.primaryButtonTextRes).isEqualTo(R.string.woopos_scanning_setup_button_next)
        assertThat(step.secondaryButtonTextRes).isEqualTo(R.string.woopos_scanning_setup_button_back)
        assertThat(step.bluetoothSettingsButtonTextRes).isEqualTo(R.string.woopos_scanning_setup_go_to_settings)
    }

    @Test
    fun `given TestYourScanner state, when created, then should have correct string content`() = runTest {
        // GIVEN
        val hidModeStep = ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = R.drawable.ic_barcode)
        val pairModeStep = ScanningSetupStep.ScannerPairModeSetup(qrCodeImageRes = R.drawable.ic_barcode)
        val pairYourScannerStep = ScanningSetupStep.PairYourScanner(R.string.woopos_scanning_setup_device_tera_1200)
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.DeviceSelection))
            .thenReturn(hidModeStep)
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, hidModeStep))
            .thenReturn(pairModeStep)
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, pairModeStep))
            .thenReturn(pairYourScannerStep)
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, pairYourScannerStep))
            .thenReturn(ScanningSetupStep.TestYourScanner)
        val viewModel = createViewModel()
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

        // WHEN
        val step = viewModel.state.value.currentStep as ScanningSetupStep.TestYourScanner

        // THEN
        assertThat(step.titleRes).isEqualTo(R.string.woopos_scanning_setup_test_scanner_title)
        assertThat(step.messageRes).isEqualTo(R.string.woopos_scanning_setup_test_scanner_message)
        assertThat(step.secondaryButtonTextRes).isEqualTo(R.string.woopos_scanning_setup_button_back)
    }

    @Test
    fun `given ScannerSetupSuccess state, when created, then should have correct string content`() = runTest {
        // GIVEN
        val hidModeStep = ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = R.drawable.ic_barcode)
        val pairModeStep = ScanningSetupStep.ScannerPairModeSetup(qrCodeImageRes = R.drawable.ic_barcode)
        val pairYourScannerStep = ScanningSetupStep.PairYourScanner(R.string.woopos_scanning_setup_device_tera_1200)
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.DeviceSelection))
            .thenReturn(hidModeStep)
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, hidModeStep))
            .thenReturn(pairModeStep)
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, pairModeStep))
            .thenReturn(pairYourScannerStep)
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, pairYourScannerStep))
            .thenReturn(ScanningSetupStep.TestYourScanner)
        whenever(navigator.getNextStepForValidBarcode(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.TestYourScanner))
            .thenReturn(ScanningSetupStep.ScannerSetupSuccess)
        val viewModel = createViewModel()
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

        // WHEN
        viewModel.onUiEvent(
            WooPosScanningSetupUiEvent.OnBarcodeScanned(
                BarcodeInputDetector.BarcodeResult.Success(
                    barcode = "1234567890128",
                    scanDurationMs = 100
                )
            )
        )
        val step = viewModel.state.value.currentStep as ScanningSetupStep.ScannerSetupSuccess

        // THEN
        assertThat(step.titleRes).isEqualTo(R.string.woopos_scanning_setup_success_title)
        assertThat(step.messageRes).isEqualTo(R.string.woopos_scanning_setup_success_message)
        assertThat(step.moreInfoButtonTextRes).isEqualTo(R.string.woopos_scanning_setup_more_information)
    }

    @Test
    fun `given ScannerSetupInfo state, when created, then should have correct string content`() = runTest {
        // GIVEN
        whenever(navigator.getNextStep(BarcodeReaderDevice.OTHER, ScanningSetupStep.DeviceSelection))
            .thenReturn(ScanningSetupStep.ScannerSetupInfo)
        val viewModel = createViewModel()
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.OTHER))

        // WHEN
        val step = viewModel.state.value.currentStep as ScanningSetupStep.ScannerSetupInfo

        // THEN
        assertThat(step.titleRes).isEqualTo(R.string.woopos_scanning_setup_info_title)
        assertThat(step.messageRes).isEqualTo(R.string.woopos_scanning_setup_info_message)
        assertThat(step.bulletPointsRes).containsExactly(
            R.string.woopos_scanning_setup_info_bullet_1,
            R.string.woopos_scanning_setup_info_bullet_2,
            R.string.woopos_scanning_setup_info_bullet_3
        )
        assertThat(step.infoTextRes).isEqualTo(R.string.woopos_scanning_setup_info_text)
        assertThat(step.backButtonTextRes).isEqualTo(R.string.woopos_scanning_setup_button_back)
        assertThat(step.nextButtonTextRes).isEqualTo(R.string.woopos_scanning_setup_button_next)
    }

    @Test
    fun `given ScannerSetupBarcodesOnProducts state, when created, then should have correct string content`() =
        runTest {
            // GIVEN
            val hidModeStep = ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = R.drawable.ic_barcode)
            val pairModeStep = ScanningSetupStep.ScannerPairModeSetup(qrCodeImageRes = R.drawable.ic_barcode)
            val pairYourScannerStep = ScanningSetupStep.PairYourScanner(R.string.woopos_scanning_setup_device_tera_1200)
            whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.DeviceSelection))
                .thenReturn(hidModeStep)
            whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, hidModeStep))
                .thenReturn(pairModeStep)
            whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, pairModeStep))
                .thenReturn(pairYourScannerStep)
            whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, pairYourScannerStep))
                .thenReturn(ScanningSetupStep.TestYourScanner)
            whenever(
                navigator.getNextStepForValidBarcode(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.TestYourScanner)
            )
                .thenReturn(ScanningSetupStep.ScannerSetupSuccess)
            whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.ScannerSetupSuccess))
                .thenReturn(ScanningSetupStep.ScannerSetupBarcodesOnProducts)
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
            assertThat(step.titleRes).isEqualTo(R.string.woopos_scanning_setup_barcodes_on_products_title)
            assertThat(step.messageRes).isEqualTo(R.string.woopos_scanning_setup_barcodes_on_products_message)
            assertThat(step.doneButtonTextRes).isEqualTo(R.string.woopos_scanning_setup_button_done)
            assertThat(step.backButtonTextRes).isEqualTo(R.string.woopos_scanning_setup_button_back)
        }

    @Test
    fun `when OnDialogShown event is sent, then should track barcode scanner setup flow shown event`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDialogShown)

        // THEN
        verify(analyticsTracker).trackDialogShown()
    }

    @Test
    fun `when TERA_1200 device is selected, then should track scanner selected event with tera_1200 scanner`() =
        runTest {
            // GIVEN
            val mockNextStep = ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = R.drawable.ic_barcode)
            whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.DeviceSelection))
                .thenReturn(mockNextStep)
            val viewModel = createViewModel()

            // WHEN
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))

            // THEN
            verify(analyticsTracker).trackScannerSelected(BarcodeReaderDevice.TERA_1200)
        }

    @Test
    fun `when STAR_BSH_20B device is selected, then should track scanner selected event with star_bsh_20b scanner`() =
        runTest {
            // GIVEN
            val mockNextStep = ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = R.drawable.ic_barcode)
            whenever(navigator.getNextStep(BarcodeReaderDevice.STAR_BSH_20B, ScanningSetupStep.DeviceSelection))
                .thenReturn(mockNextStep)
            val viewModel = createViewModel()

            // WHEN
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.STAR_BSH_20B))

            // THEN
            verify(analyticsTracker).trackScannerSelected(BarcodeReaderDevice.STAR_BSH_20B)
        }

    @Test
    fun `when OTHER device is selected, then should track scanner selected event with other scanner`() = runTest {
        // GIVEN
        val mockNextStep = ScanningSetupStep.ScannerSetupInfo
        whenever(navigator.getNextStep(BarcodeReaderDevice.OTHER, ScanningSetupStep.DeviceSelection))
            .thenReturn(mockNextStep)
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.OTHER))

        // THEN
        verify(analyticsTracker).trackScannerSelected(BarcodeReaderDevice.OTHER)
    }

    @Test
    fun `when OnDismissed event is sent, then should track dismissed event`() = runTest {
        // GIVEN
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, ScanningSetupStep.DeviceSelection))
            .thenReturn(ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = R.drawable.ic_barcode))
        val viewModel = createViewModel()
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))

        // WHEN
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDismissed)

        // THEN
        verify(analyticsTracker).trackDismissed(
            BarcodeReaderDevice.TERA_1200,
            ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = R.drawable.ic_barcode)
        )
    }

    @Test
    fun `when stopScannerDetection is called, then should stop periodic detection`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.stopScannerDetection()

        // THEN
        verify(scannerDetectionService).stopPeriodicDetection()
    }

    @Test
    fun `when OnDialogShown event is sent, then should start periodic detection`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDialogShown)

        // THEN
        verify(scannerDetectionService).startPeriodicDetection(any(), any())
    }

    private fun createViewModel(
        initialStep: ScanningSetupStep = ScanningSetupStep.DeviceSelection,
        selectedDevice: BarcodeReaderDevice? = null
    ): WooPosScanningSetupViewModel {
        whenever(navigator.getInitialStep()).thenReturn(initialStep)
        val viewModel = WooPosScanningSetupViewModel(navigator, analyticsTracker, scannerDetectionService)

        selectedDevice?.let {
            whenever(navigator.getNextStep(it, initialStep)).thenReturn(initialStep)
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(it))
        }

        return viewModel
    }
}
