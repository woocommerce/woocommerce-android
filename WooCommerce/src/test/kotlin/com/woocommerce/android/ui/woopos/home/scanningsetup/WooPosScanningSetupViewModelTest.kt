package com.woocommerce.android.ui.woopos.home.scanningsetup

import app.cash.turbine.test
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.BarcodeReaderDevice
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.ScanningSetupStep
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private val navigator: ScannerSetupNavigator = mock()

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
    fun `given DeviceSelection step, when OTHER device selected, then should navigate to ScannerSetupInfo`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.OTHER))

        // THEN
        assertThat(viewModel.state.value.currentStep)
            .isInstanceOf(ScanningSetupStep.ScannerSetupInfo::class.java)
        assertThat(viewModel.state.value.selectedDevice).isEqualTo(BarcodeReaderDevice.OTHER)
    }

    @Test
    fun `given DeviceSelection step, when non-OTHER device selected, then should navigate to ScannerHIDModeSetup`() =
        runTest {
            // GIVEN
            val viewModel = createViewModel()

            // WHEN
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))

            // THEN
            assertThat(viewModel.state.value.currentStep)
                .isInstanceOf(ScanningSetupStep.ScannerHIDModeSetup::class.java)
            assertThat(viewModel.state.value.selectedDevice).isEqualTo(BarcodeReaderDevice.TERA_1200)
        }

    @Test
    fun `given ScannerHIDModeSetup step, when primary button clicked, then should navigate to ScannerPairModeSetup`() =
        runTest {
            // GIVEN
            val viewModel = createViewModel()
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))

            // WHEN
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

            // THEN
            assertThat(viewModel.state.value.currentStep)
                .isInstanceOf(ScanningSetupStep.ScannerPairModeSetup::class.java)
        }

    @Test
    fun `given ScannerPairModeSetup step, when primary button clicked, then should navigate to PairYourScanner`() =
        runTest {
            // GIVEN
            val viewModel = createViewModel()
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

            // WHEN
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

            // THEN
            assertThat(viewModel.state.value.currentStep)
                .isInstanceOf(ScanningSetupStep.PairYourScanner::class.java)
        }

    @Test
    fun `given PairYourScanner step, when primary button clicked, then should navigate to TestYourScanner`() = runTest {
        // GIVEN
        val viewModel = createViewModel()
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

        // WHEN
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

        // THEN
        assertThat(viewModel.state.value.currentStep)
            .isInstanceOf(ScanningSetupStep.TestYourScanner::class.java)
    }

    @Test
    fun `given ScannerSetupInfo step from DeviceSelection, when secondary button clicked, then should navigate back to DeviceSelection`() =
        runTest {
            // GIVEN
            val viewModel = createViewModel()
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.OTHER))

            // WHEN
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnSecondaryButtonClicked)

            // THEN
            assertThat(viewModel.state.value.currentStep)
                .isInstanceOf(ScanningSetupStep.DeviceSelection::class.java)
        }

    @Test
    fun `given ScannerHIDModeSetup step, when secondary button clicked, then should navigate back to DeviceSelection`() =
        runTest {
            // GIVEN
            val viewModel = createViewModel()
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))

            // WHEN
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnSecondaryButtonClicked)

            // THEN
            assertThat(viewModel.state.value.currentStep)
                .isInstanceOf(ScanningSetupStep.DeviceSelection::class.java)
        }

    @Test
    fun `given ScannerPairModeSetup step, when secondary button clicked, then should navigate back to ScannerHIDModeSetup`() =
        runTest {
            // GIVEN
            val viewModel = createViewModel()
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

            // WHEN
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnSecondaryButtonClicked)

            // THEN
            assertThat(viewModel.state.value.currentStep)
                .isInstanceOf(ScanningSetupStep.ScannerHIDModeSetup::class.java)
        }

    @Test
    fun `given PairYourScanner step, when secondary button clicked, then should navigate back to ScannerPairModeSetup`() =
        runTest {
            // GIVEN
            val viewModel = createViewModel()
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

            // WHEN
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnSecondaryButtonClicked)

            // THEN
            assertThat(viewModel.state.value.currentStep)
                .isInstanceOf(ScanningSetupStep.ScannerPairModeSetup::class.java)
        }

    @Test
    fun `given TestYourScanner step, when secondary button clicked, then should navigate back to PairYourScanner`() =
        runTest {
            // GIVEN
            val viewModel = createViewModel()
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

            // WHEN
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnSecondaryButtonClicked)

            // THEN
            assertThat(viewModel.state.value.currentStep)
                .isInstanceOf(ScanningSetupStep.PairYourScanner::class.java)
        }

    @Test
    fun `given ScannerSetupInfo step, when primary button clicked, then should emit dismiss dialog event`() = runTest {
        // GIVEN
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
    fun `given resetToInitialState called, when invoked, then should reset to DeviceSelection step`() = runTest {
        // GIVEN
        val viewModel = createViewModel()
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

        // WHEN
        viewModel.resetToInitialState()

        // THEN
        assertThat(viewModel.state.value.currentStep)
            .isInstanceOf(ScanningSetupStep.DeviceSelection::class.java)
        assertThat(viewModel.state.value.selectedDevice).isNull()
    }

    @Test
    fun `given STAR_BSH_20B device selected, when primary button clicked from HID mode setup, then should navigate directly to PairYourScanner`() = runTest {
        // GIVEN
        val viewModel = createViewModel()
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.STAR_BSH_20B))

        // WHEN
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

        // THEN
        assertThat(viewModel.state.value.currentStep)
            .isInstanceOf(ScanningSetupStep.PairYourScanner::class.java)
    }


    private fun createViewModel(): WooPosScanningSetupViewModel {
        setupMockNavigator()
        return WooPosScanningSetupViewModel(navigator)
    }

    private fun setupMockNavigator() {
        val deviceSelectionStep = ScanningSetupStep.DeviceSelection(
            title = "Set up a barcode scanner",
            devices = listOf(
                BarcodeReaderDevice.TERA_1200,
                BarcodeReaderDevice.STAR_BSH_20B,
                BarcodeReaderDevice.INATECK_BLUETOOTH,
                BarcodeReaderDevice.OTHER
            )
        )

        val hidModeStep = ScanningSetupStep.ScannerHIDModeSetup(
            title = "Introduction",
            message = "Introduction message",
            qrCodeImageRes = R.drawable.ic_barcode,
            primaryButtonText = "Next",
            secondaryButtonText = "Back"
        )

        val pairModeStep = ScanningSetupStep.ScannerPairModeSetup(
            title = "Pair mode",
            message = "Pair mode message",
            qrCodeImageRes = R.drawable.ic_barcode,
            primaryButtonText = "Next",
            secondaryButtonText = "Back"
        )

        val pairYourScannerStep = ScanningSetupStep.PairYourScanner(
            title = "Pair your scanner",
            message = "Pair your scanner message TERA 1200",
            iconRes = R.drawable.ic_woopos_bluetooth_settings,
            primaryButtonText = "Next",
            secondaryButtonText = "Back",
            bluetoothSettingsButtonText = "Go to settings"
        )

        val testScannerStep = ScanningSetupStep.TestYourScanner(
            title = "Test scanner",
            message = "Test scanner message",
            barcodeValue = "1234567890128",
            secondaryButtonText = "Back"
        )

        val setupInfoStep = ScanningSetupStep.ScannerSetupInfo(
            title = "Info",
            message = "Info message",
            bulletPoints = listOf("Bullet 1", "Bullet 2", "Bullet 3"),
            infoText = "Info text",
            backButtonText = "Back",
            doneButtonText = "Done"
        )

        val testScannerTimeoutStep = ScanningSetupStep.TestYourScannerTimeout(
            title = "Test scanner timeout",
            message = "Test scanner timeout message",
            barcodeValue = "1234567890128",
            secondaryButtonText = "Back"
        )

        setupNavigatorMocks(
            deviceSelectionStep,
            hidModeStep,
            pairModeStep,
            pairYourScannerStep,
            testScannerStep,
            setupInfoStep,
            testScannerTimeoutStep
        )
    }

    private fun setupNavigatorMocks(
        deviceSelectionStep: ScanningSetupStep.DeviceSelection,
        hidModeStep: ScanningSetupStep.ScannerHIDModeSetup,
        pairModeStep: ScanningSetupStep.ScannerPairModeSetup,
        pairYourScannerStep: ScanningSetupStep.PairYourScanner,
        testScannerStep: ScanningSetupStep.TestYourScanner,
        setupInfoStep: ScanningSetupStep.ScannerSetupInfo,
        testScannerTimeoutStep: ScanningSetupStep.TestYourScannerTimeout
    ) {
        whenever(navigator.createDeviceSelectionStep()).thenReturn(deviceSelectionStep)
        whenever(navigator.createTestYourScannerTimeoutStep()).thenReturn(testScannerTimeoutStep)
        whenever(navigator.restartFlow()).thenReturn(deviceSelectionStep)
        whenever(navigator.getNextStep(BarcodeReaderDevice.OTHER, deviceSelectionStep)).thenReturn(setupInfoStep)
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, deviceSelectionStep)).thenReturn(hidModeStep)
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, hidModeStep)).thenReturn(pairModeStep)
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, pairModeStep)).thenReturn(pairYourScannerStep)
        whenever(navigator.getNextStep(BarcodeReaderDevice.TERA_1200, pairYourScannerStep)).thenReturn(testScannerStep)
        whenever(navigator.getPreviousStep(BarcodeReaderDevice.OTHER, setupInfoStep)).thenReturn(deviceSelectionStep)
        whenever(navigator.getPreviousStep(BarcodeReaderDevice.TERA_1200, hidModeStep)).thenReturn(deviceSelectionStep)
        whenever(navigator.getPreviousStep(BarcodeReaderDevice.TERA_1200, pairModeStep)).thenReturn(hidModeStep)
        whenever(navigator.getPreviousStep(BarcodeReaderDevice.TERA_1200, pairYourScannerStep)).thenReturn(pairModeStep)
        whenever(navigator.getPreviousStep(BarcodeReaderDevice.TERA_1200, testScannerStep)).thenReturn(pairYourScannerStep)

        // Add STAR_BSH_20B support
        whenever(navigator.getNextStep(BarcodeReaderDevice.STAR_BSH_20B, deviceSelectionStep)).thenReturn(hidModeStep)
        whenever(navigator.getNextStep(BarcodeReaderDevice.STAR_BSH_20B, hidModeStep)).thenReturn(pairYourScannerStep)
        whenever(navigator.getNextStep(BarcodeReaderDevice.STAR_BSH_20B, pairYourScannerStep)).thenReturn(testScannerStep)
        whenever(navigator.getPreviousStep(BarcodeReaderDevice.STAR_BSH_20B, hidModeStep)).thenReturn(deviceSelectionStep)
        whenever(navigator.getPreviousStep(BarcodeReaderDevice.STAR_BSH_20B, pairYourScannerStep)).thenReturn(hidModeStep)
        whenever(navigator.getPreviousStep(BarcodeReaderDevice.STAR_BSH_20B, testScannerStep)).thenReturn(pairYourScannerStep)
    }
}
