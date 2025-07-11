package com.woocommerce.android.ui.woopos.home.scanningsetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.BarcodeReaderDevice
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.ScanningSetupStep
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
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
            isVisible = false,
            currentStep = createDeviceSelectionStep(),
            selectedDevice = null
        )
    )
    val state: StateFlow<WooPosScanningSetupState> = _state.asStateFlow()

    private val _showBarcodeInfoDialogEvent = MutableSharedFlow<Unit>()
    val showBarcodeInfoDialogEvent: SharedFlow<Unit> = _showBarcodeInfoDialogEvent.asSharedFlow()

    private val _openBluetoothSettingsEvent = MutableSharedFlow<Unit>()
    val openBluetoothSettingsEvent: SharedFlow<Unit> = _openBluetoothSettingsEvent.asSharedFlow()

    fun onUiEvent(event: WooPosScanningSetupUiEvent) {
        when (event) {
            is WooPosScanningSetupUiEvent.OnDeviceSelected -> {
                _state.value = _state.value.copy(
                    selectedDevice = event.device
                )

                when (event.device) {
                    BarcodeReaderDevice.OTHER -> viewModelScope.launch {
                        _showBarcodeInfoDialogEvent.emit(Unit)
                    }

                    else -> _state.value = _state.value.copy(
                        currentStep = createScannerHIDModeSetupStep()
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
                    currentStep = createScannerPairModeSetupStep()
                )
            }

            is ScanningSetupStep.ScannerPairModeSetup -> {
                _state.value = _state.value.copy(
                    currentStep = createPairYourScannerStep()
                )
            }
            is ScanningSetupStep.PairYourScanner -> {
                _state.value = _state.value.copy(
                    currentStep = createTestYourScannerStep()
                )
            }

            is ScanningSetupStep.TestYourScanner -> {
                error("Primary button should not be available on TestYourScanner step")
            }
        }
    }

    private fun handleSecondaryButtonClick() {
        when (_state.value.currentStep) {
            is ScanningSetupStep.DeviceSelection -> error(
                "Secondary button should not be available on DeviceSelection step"
            )

            is ScanningSetupStep.ScannerHIDModeSetup -> {
                _state.value = _state.value.copy(
                    currentStep = createDeviceSelectionStep()
                )
            }

            is ScanningSetupStep.ScannerPairModeSetup -> {
                _state.value = _state.value.copy(
                    currentStep = createScannerHIDModeSetupStep()
                )
            }
            is ScanningSetupStep.PairYourScanner -> {
                _state.value = _state.value.copy(
                    currentStep = createScannerPairModeSetupStep()
                )
            }

            is ScanningSetupStep.TestYourScanner -> {
                _state.value = _state.value.copy(
                    currentStep = createPairYourScannerStep()
                )
            }
        }
    }

    private fun createDeviceSelectionStep() = ScanningSetupStep.DeviceSelection(
        title = resourceProvider.getString(R.string.woopos_scanning_setup_device_selection_title),
        devices = listOf(
            BarcodeReaderDevice.TERA_1200,
            BarcodeReaderDevice.STAR_BSH_20B,
            BarcodeReaderDevice.INATECK_BLUETOOTH,
            BarcodeReaderDevice.OTHER
        )
    )

    private fun createScannerHIDModeSetupStep() = ScanningSetupStep.ScannerHIDModeSetup(
        title = resourceProvider.getString(R.string.woopos_scanning_setup_introduction_title),
        message = resourceProvider.getString(R.string.woopos_scanning_setup_introduction_message),
        qrCodeImageRes = R.drawable.ic_barcode,
        primaryButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_next),
        secondaryButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_back)
    )

    private fun createScannerPairModeSetupStep() = ScanningSetupStep.ScannerPairModeSetup(
        title = resourceProvider.getString(R.string.woopos_scanning_setup_scanner_pair_mode_title),
        message = resourceProvider.getString(R.string.woopos_scanning_setup_scanner_pair_mode_message),
        qrCodeImageRes = R.drawable.ic_barcode,
        primaryButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_next),
        secondaryButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_back)
    )
    private fun createPairYourScannerStep() = ScanningSetupStep.PairYourScanner(
        title = resourceProvider.getString(R.string.woopos_scanning_setup_pair_your_scanner_title),
        message = resourceProvider.getString(
            R.string.woopos_scanning_setup_pair_your_scanner_message,
            resourceProvider.getString(_state.value.selectedDevice!!.displayNameRes)
        ),
        iconRes = R.drawable.ic_woopos_bluetooth_settings,
        primaryButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_next),
        secondaryButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_back),
        bluetoothSettingsButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_go_to_settings)
    )

    private fun createTestYourScannerStep() = ScanningSetupStep.TestYourScanner(
        title = resourceProvider.getString(R.string.woopos_scanning_setup_test_scanner_title),
        message = resourceProvider.getString(R.string.woopos_scanning_setup_test_scanner_message),
        barcodeImageRes = R.drawable.ic_barcode,
        secondaryButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_back)
    )
}

sealed class WooPosScanningSetupUiEvent {
    data object OnPrimaryButtonClicked : WooPosScanningSetupUiEvent()
    data object OnSecondaryButtonClicked : WooPosScanningSetupUiEvent()
    data object OnOpenBluetoothSettings : WooPosScanningSetupUiEvent()
    data class OnDeviceSelected(val device: BarcodeReaderDevice) : WooPosScanningSetupUiEvent()
}
