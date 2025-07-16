package com.woocommerce.android.ui.woopos.home.scanningsetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.modifier.BarcodeInputDetector
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.BarcodeReaderDevice
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.ScanningSetupStep
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosScanningSetupViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _state = MutableStateFlow(
        WooPosScanningSetupState(
            currentStep = createDeviceSelectionStep(),
            selectedDevice = null
        )
    )
    val state: StateFlow<WooPosScanningSetupState> = _state.asStateFlow()

    private val _openBluetoothSettingsEvent = MutableSharedFlow<Unit>()
    val openBluetoothSettingsEvent: SharedFlow<Unit> = _openBluetoothSettingsEvent.asSharedFlow()

    private val _dismissDialogEvent = MutableSharedFlow<Unit>()
    val dismissDialogEvent: SharedFlow<Unit> = _dismissDialogEvent.asSharedFlow()

    private var autoNavigationJob: Job? = null

    fun onUiEvent(event: WooPosScanningSetupUiEvent) {
        when (event) {
            is WooPosScanningSetupUiEvent.OnDeviceSelected -> {
                _state.value = _state.value.copy(
                    selectedDevice = event.device
                )

                when (event.device) {
                    BarcodeReaderDevice.OTHER -> _state.value = _state.value.copy(
                        currentStep = createScannerSetupInfoStep(_state.value.currentStep)
                    )

                    else -> _state.value = _state.value.copy(
                        currentStep = createScannerHIDModeSetupStep(_state.value.currentStep)
                    )
                }
            }

            WooPosScanningSetupUiEvent.OnPrimaryButtonClicked -> {
                handlePrimaryButtonClick()
            }

            WooPosScanningSetupUiEvent.OnSecondaryButtonClicked -> {
                handleSecondaryButtonClick()
            }

            WooPosScanningSetupUiEvent.OnOpenBluetoothSettings -> {
                viewModelScope.launch {
                    _openBluetoothSettingsEvent.emit(Unit)
                }
            }

            is WooPosScanningSetupUiEvent.OnBarcodeScanned -> {
                handleBarcodeScanned(event.barcodeResult)
            }
        }
    }

    fun resetToInitialState() {
        _state.value = _state.value.copy(
            currentStep = createDeviceSelectionStep(),
            selectedDevice = null
        )
    }

    private fun handlePrimaryButtonClick() {
        when (_state.value.currentStep) {
            is ScanningSetupStep.DeviceSelection -> {
                error("Primary button should not be available on DeviceSelection step")
            }

            is ScanningSetupStep.ScannerHIDModeSetup -> {
                _state.value = _state.value.copy(
                    currentStep = createScannerPairModeSetupStep(_state.value.currentStep)
                )
            }

            is ScanningSetupStep.ScannerPairModeSetup -> {
                _state.value = _state.value.copy(
                    currentStep = createPairYourScannerStep(_state.value.currentStep)
                )
            }

            is ScanningSetupStep.PairYourScanner -> {
                _state.value = _state.value.copy(
                    currentStep = createTestYourScannerStep(_state.value.currentStep)
                ).also {
                    startAutoNavigationToTestYourScannerStep()
                }
            }

            is ScanningSetupStep.TestYourScanner -> {
                error("Primary button should not be available on TestYourScanner step")
            }

            is ScanningSetupStep.TestYourScannerTimeout -> {
                error("Primary button should not be available on TestYourScannerTimeout step")
            }

            is ScanningSetupStep.ScannerSetupSuccess -> {
                _state.value = _state.value.copy(
                    currentStep = createScannerSetupBarcodesOnProductsStep(_state.value.currentStep)
                )
            }

            is ScanningSetupStep.ScannerSetupInfo -> {
                viewModelScope.launch {
                    _dismissDialogEvent.emit(Unit)
                }
            }

            is ScanningSetupStep.TestYourScannerScanFailed -> {
                _state.value = _state.value.copy(
                    currentStep = createDeviceSelectionStep()
                )
            }

            is ScanningSetupStep.ScannerSetupBarcodesOnProducts -> {
                viewModelScope.launch {
                    _dismissDialogEvent.emit(Unit)
                }
            }
        }
    }

    private fun handleSecondaryButtonClick() {
        val previousStep = requireNotNull(
            _state.value.currentStep.previousStep
        ) { "Previous step cannot be null if secondary button present" }
        _state.value = _state.value.copy(currentStep = previousStep)
    }

    private fun createDeviceSelectionStep() = ScanningSetupStep.DeviceSelection(
        title = resourceProvider.getString(R.string.woopos_scanning_setup_device_selection_title),
        devices = listOf(
            BarcodeReaderDevice.TERA_1200,
            BarcodeReaderDevice.STAR_BSH_20B,
            BarcodeReaderDevice.INATECK_BLUETOOTH,
            BarcodeReaderDevice.OTHER
        ),
        previousStep = null
    )

    private fun createScannerHIDModeSetupStep(previousStep: ScanningSetupStep) = ScanningSetupStep.ScannerHIDModeSetup(
        title = resourceProvider.getString(R.string.woopos_scanning_setup_introduction_title),
        message = resourceProvider.getString(R.string.woopos_scanning_setup_introduction_message),
        qrCodeImageRes = R.drawable.ic_barcode,
        primaryButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_next),
        secondaryButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_back),
        previousStep = previousStep
    )

    private fun createScannerPairModeSetupStep(previousStep: ScanningSetupStep) =
        ScanningSetupStep.ScannerPairModeSetup(
            title = resourceProvider.getString(R.string.woopos_scanning_setup_scanner_pair_mode_title),
            message = resourceProvider.getString(R.string.woopos_scanning_setup_scanner_pair_mode_message),
            qrCodeImageRes = R.drawable.ic_barcode,
            primaryButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_next),
            secondaryButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_back),
            previousStep = previousStep
        )

    private fun createPairYourScannerStep(previousStep: ScanningSetupStep) = ScanningSetupStep.PairYourScanner(
        title = resourceProvider.getString(R.string.woopos_scanning_setup_pair_your_scanner_title),
        message = resourceProvider.getString(
            R.string.woopos_scanning_setup_pair_your_scanner_message,
            resourceProvider.getString(_state.value.selectedDevice!!.displayNameRes)
        ),
        iconRes = R.drawable.ic_woopos_bluetooth_settings,
        primaryButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_next),
        secondaryButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_back),
        bluetoothSettingsButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_go_to_settings),
        previousStep = previousStep
    )

    private fun createTestYourScannerStep(
        previousStep: ScanningSetupStep
    ) = ScanningSetupStep.TestYourScanner(
        title = resourceProvider.getString(R.string.woopos_scanning_setup_test_scanner_title),
        message = resourceProvider.getString(R.string.woopos_scanning_setup_test_scanner_message),
        barcodeValue = TEST_BARCODE_EAN13,
        secondaryButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_back),
        previousStep = previousStep,
    )

    private fun createTestYourScannerTimeoutStep(previousStep: ScanningSetupStep) =
        ScanningSetupStep.TestYourScannerTimeout(
            title = resourceProvider.getString(R.string.woopos_scanning_setup_timeout_title),
            message = resourceProvider.getString(R.string.woopos_scanning_setup_timeout_message),
            barcodeValue = TEST_BARCODE_EAN13,
            secondaryButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_back),
            previousStep = previousStep,
        )

    private fun createTestYourScannerScanFailedStep(previousStep: ScanningSetupStep) =
        ScanningSetupStep.TestYourScannerScanFailed(
            title = resourceProvider.getString(R.string.woopos_scanning_setup_scan_failed_title),
            message = resourceProvider.getString(R.string.woopos_scanning_setup_scan_failed_message),
            iconRes = R.drawable.ic_woo_pos_error_x,
            primaryButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_retry),
            secondaryButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_back),
            previousStep = previousStep,
        )

    private fun createScannerSetupSuccessStep() = ScanningSetupStep.ScannerSetupSuccess(
        title = resourceProvider.getString(R.string.woopos_scanning_setup_success_title),
        message = resourceProvider.getString(R.string.woopos_scanning_setup_success_message),
        moreInfoButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_more_information),
        previousStep = null,
    )

    private fun createScannerSetupBarcodesOnProductsStep(
        previousStep: ScanningSetupStep
    ) = ScanningSetupStep.ScannerSetupBarcodesOnProducts(
        title = resourceProvider.getString(R.string.woopos_scanning_setup_barcodes_on_products_title),
        message = resourceProvider.getString(R.string.woopos_scanning_setup_barcodes_on_products_message),
        doneButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_done),
        backButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_back),
        previousStep = previousStep
    )

    private fun createScannerSetupInfoStep(previousStep: ScanningSetupStep) = ScanningSetupStep.ScannerSetupInfo(
        title = resourceProvider.getString(R.string.woopos_scanning_setup_info_title),
        message = resourceProvider.getString(R.string.woopos_scanning_setup_info_message),
        bulletPoints = listOf(
            resourceProvider.getString(R.string.woopos_scanning_setup_info_bullet_1),
            resourceProvider.getString(R.string.woopos_scanning_setup_info_bullet_2),
            resourceProvider.getString(R.string.woopos_scanning_setup_info_bullet_3)
        ),
        infoText = resourceProvider.getString(R.string.woopos_scanning_setup_info_text),
        backButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_back),
        doneButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_done),
        previousStep = previousStep
    )

    private fun handleBarcodeScanned(barcodeResult: BarcodeInputDetector.BarcodeResult) {
        when (_state.value.currentStep) {
            is ScanningSetupStep.TestYourScanner,
            is ScanningSetupStep.TestYourScannerTimeout -> {
                if (barcodeResult.barcode == TEST_BARCODE_EAN13) {
                    _state.value = _state.value.copy(
                        currentStep = createScannerSetupSuccessStep()
                    )
                } else {
                    _state.value = _state.value.copy(
                        currentStep = createTestYourScannerScanFailedStep(_state.value.currentStep)
                    )
                }
            }

            else -> {
                error("Barcode scanning is not expected in the current step: ${_state.value.currentStep}")
            }
        }
    }

    private fun startAutoNavigationToTestYourScannerStep() {
        autoNavigationJob?.cancel()
        autoNavigationJob = viewModelScope.launch {
            delay(AUTO_NAVIGATION_DELAY_MS)
            if (_state.value.currentStep is ScanningSetupStep.TestYourScanner) {
                _state.value = _state.value.copy(
                    currentStep = createTestYourScannerTimeoutStep(_state.value.currentStep)
                )
            }
        }
    }

    companion object {
        private const val AUTO_NAVIGATION_DELAY_MS = 10000L
        private const val TEST_BARCODE_EAN13 = "1234567890128"
    }
}
