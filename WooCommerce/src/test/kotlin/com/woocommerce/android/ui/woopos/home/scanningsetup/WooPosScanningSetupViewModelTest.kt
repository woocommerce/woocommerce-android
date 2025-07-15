package com.woocommerce.android.ui.woopos.home.scanningsetup

import app.cash.turbine.test
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.BarcodeReaderDevice
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.ScanningSetupStep
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.viewmodel.ResourceProvider
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

    private val resourceProvider: ResourceProvider = mock()

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
    fun `given ScannerSetupInfo step, when verifying previousStep is tracked, then should maintain proper navigation history`() =
        runTest {
            // GIVEN
            val viewModel = createViewModel()

            // WHEN
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.OTHER))

            // THEN
            val currentStep = viewModel.state.value.currentStep as ScanningSetupStep.ScannerSetupInfo
            assertThat(currentStep.previousStep)
                .isInstanceOf(ScanningSetupStep.DeviceSelection::class.java)
        }

    @Test
    fun `given multi-step navigation, when checking previousStep chain, then should maintain proper navigation stack`() =
        runTest {
            // GIVEN
            val viewModel = createViewModel()

            // WHEN
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))
            viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

            // THEN
            val currentStep = viewModel.state.value.currentStep as ScanningSetupStep.ScannerPairModeSetup
            assertThat(currentStep.previousStep)
                .isInstanceOf(ScanningSetupStep.ScannerHIDModeSetup::class.java)

            val previousStep = currentStep.previousStep as ScanningSetupStep.ScannerHIDModeSetup
            assertThat(previousStep.previousStep)
                .isInstanceOf(ScanningSetupStep.DeviceSelection::class.java)
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
        advanceTimeBy(3100)
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
    fun `given ScannerSetupBarcodesOnProducts state, when created, then should have correct string content`() = runTest {
        // GIVEN
        val viewModel = createViewModel()
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnDeviceSelected(BarcodeReaderDevice.TERA_1200))
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)
        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

        advanceTimeBy(3100)

        viewModel.onUiEvent(WooPosScanningSetupUiEvent.OnPrimaryButtonClicked)

        // WHEN
        val step = viewModel.state.value.currentStep as ScanningSetupStep.ScannerSetupBarcodesOnProducts

        // THEN
        assertThat(step.title).isEqualTo("How to set up barcodes on products")
        assertThat(step.message).isEqualTo("You can set up barcodes in the GTIN, UPC, EAN, ISBN field in the product's inventory tab. For more details")
        assertThat(step.doneButtonText).isEqualTo("Done")
        assertThat(step.backButtonText).isEqualTo("Back")
    }

    private fun createViewModel(): WooPosScanningSetupViewModel {
        setupMockResourceProvider()
        return WooPosScanningSetupViewModel(resourceProvider)
    }

    private fun setupMockResourceProvider() {
        whenever(resourceProvider.getString(R.string.woopos_scanning_setup_device_selection_title))
            .thenReturn("Set up a barcode scanner")
        whenever(resourceProvider.getString(R.string.woopos_scanning_setup_introduction_title))
            .thenReturn("Introduction")
        whenever(resourceProvider.getString(R.string.woopos_scanning_setup_introduction_message))
            .thenReturn("Introduction message")
        whenever(resourceProvider.getString(R.string.woopos_scanning_setup_scanner_pair_mode_title))
            .thenReturn("Pair mode")
        whenever(resourceProvider.getString(R.string.woopos_scanning_setup_scanner_pair_mode_message))
            .thenReturn("Pair mode message")
        whenever(resourceProvider.getString(R.string.woopos_scanning_setup_pair_your_scanner_title))
            .thenReturn("Pair your scanner")
        whenever(resourceProvider.getString(R.string.woopos_scanning_setup_pair_your_scanner_message, "TERA 1200"))
            .thenReturn("Pair your scanner message TERA 1200")
        whenever(resourceProvider.getString(R.string.woopos_scanning_setup_test_scanner_title))
            .thenReturn("Test scanner")
        whenever(resourceProvider.getString(R.string.woopos_scanning_setup_test_scanner_message))
            .thenReturn("Test scanner message")
        whenever(resourceProvider.getString(R.string.woopos_scanning_setup_success_title))
            .thenReturn("Success")
        whenever(resourceProvider.getString(R.string.woopos_scanning_setup_success_message))
            .thenReturn("Success message")
        whenever(resourceProvider.getString(R.string.woopos_scanning_setup_info_title))
            .thenReturn("Info")
        whenever(resourceProvider.getString(R.string.woopos_scanning_setup_info_message))
            .thenReturn("Info message")
        whenever(resourceProvider.getString(R.string.woopos_scanning_setup_info_bullet_1))
            .thenReturn("Bullet 1")
        whenever(resourceProvider.getString(R.string.woopos_scanning_setup_info_bullet_2))
            .thenReturn("Bullet 2")
        whenever(resourceProvider.getString(R.string.woopos_scanning_setup_info_bullet_3))
            .thenReturn("Bullet 3")
        whenever(resourceProvider.getString(R.string.woopos_scanning_setup_info_text))
            .thenReturn("Info text")
        whenever(resourceProvider.getString(R.string.woopos_scanning_setup_button_next))
            .thenReturn("Next")
        whenever(resourceProvider.getString(R.string.woopos_scanning_setup_button_back))
            .thenReturn("Back")
        whenever(resourceProvider.getString(R.string.woopos_scanning_setup_button_done))
            .thenReturn("Done")
        whenever(resourceProvider.getString(R.string.woopos_scanning_setup_go_to_settings))
            .thenReturn("Go to settings")
        whenever(resourceProvider.getString(R.string.woopos_scanning_setup_more_information))
            .thenReturn("More information")
        whenever(resourceProvider.getString(R.string.woopos_scanning_setup_device_tera_1200))
            .thenReturn("TERA 1200")
        whenever(resourceProvider.getString(R.string.woopos_scanning_setup_device_star_bsh_20b))
            .thenReturn("STAR BSH 20B")
        whenever(resourceProvider.getString(R.string.woopos_scanning_setup_device_inateck_bluetooth))
            .thenReturn("INATECK Bluetooth")
        whenever(resourceProvider.getString(R.string.woopos_scanning_setup_device_other))
            .thenReturn("Other")
        whenever(resourceProvider.getString(R.string.woopos_scanning_setup_barcodes_on_products_title))
            .thenReturn("How to set up barcodes on products")
        whenever(resourceProvider.getString(R.string.woopos_scanning_setup_barcodes_on_products_message))
            .thenReturn("You can set up barcodes in the GTIN, UPC, EAN, ISBN field in the product's inventory tab. For more details")
    }
}
