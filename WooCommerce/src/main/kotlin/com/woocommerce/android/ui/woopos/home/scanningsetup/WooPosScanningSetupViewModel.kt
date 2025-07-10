package com.woocommerce.android.ui.woopos.home.scanningsetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.data.WOO_POS_BARCODE_DOC_URL
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
            currentStep = createWelcomeStep(),
            selectedDevice = null
        )
    )
    val state: StateFlow<WooPosScanningSetupState> = _state.asStateFlow()

    private val _openUrlEvent = MutableSharedFlow<String>()
    val openUrlEvent: SharedFlow<String> = _openUrlEvent.asSharedFlow()

    private val _showBarcodeInfoDialogEvent = MutableSharedFlow<Unit>()
    val showBarcodeInfoDialogEvent: SharedFlow<Unit> = _showBarcodeInfoDialogEvent.asSharedFlow()

    private val _openBluetoothSettingsEvent = MutableSharedFlow<Unit>()
    val openBluetoothSettingsEvent: SharedFlow<Unit> = _openBluetoothSettingsEvent.asSharedFlow()

    fun onUiEvent(event: WooPosScanningSetupUiEvent) {
        when (event) {
            WooPosScanningSetupUiEvent.OnBluetoothScannerSelected -> {
                _state.value = _state.value.copy(
                    currentStep = createDeviceSelectionStep()
                )
            }

            is WooPosScanningSetupUiEvent.OnDeviceSelected -> {
                _state.value = _state.value.copy(
                    selectedDevice = event.device
                )

                when (event.device) {
                    BarcodeReaderDevice.OTHER -> viewModelScope.launch {
                        _showBarcodeInfoDialogEvent.emit(Unit)
                    }

                    else -> _state.value = _state.value.copy(
                        currentStep = createBluetoothIntroductionStep()
                    )
                }
            }

            WooPosScanningSetupUiEvent.OnPrimaryButtonClicked -> {
                handlePrimaryButtonClick()
            }

            WooPosScanningSetupUiEvent.OnSecondaryButtonClicked -> {
                handleSecondaryButtonClick()
            }

            WooPosScanningSetupUiEvent.OnViewDocumentation -> {
                viewModelScope.launch {
                    _openUrlEvent.emit(WOO_POS_BARCODE_DOC_URL)
                }
            }

            WooPosScanningSetupUiEvent.OnOpenBluetoothSettings -> {
                viewModelScope.launch {
                    _openBluetoothSettingsEvent.emit(Unit)
                }
            }
        }
    }

    fun resetToWelcomeState() {
        _state.value = _state.value.copy(
            currentStep = createWelcomeStep(),
            selectedDevice = null
        )
    }

    private fun handlePrimaryButtonClick() {
        when (_state.value.currentStep) {
            is ScanningSetupStep.Welcome -> {
                error("Primary button should not be available on Welcome step")
            }

            is ScanningSetupStep.DeviceSelection -> {
                error("Primary button should not be available on DeviceSelection step")
            }

            is ScanningSetupStep.Introduction -> {
                _state.value = _state.value.copy(
                    currentStep = createBluetoothWarningStep()
                )
            }

            is ScanningSetupStep.BluetoothWarning -> {
                _state.value = _state.value.copy(
                    currentStep = createBluetoothPairingStep()
                )
            }

            is ScanningSetupStep.BluetoothPairing -> {
                _state.value = _state.value.copy(
                    currentStep = createPairOnYourDeviceStep()
                )
            }

            is ScanningSetupStep.PairOnYourDevice -> {
                _state.value = _state.value.copy(
                    currentStep = createTestYourScannerStep()
                )
            }

            is ScanningSetupStep.TestYourScanner -> {
                _state.value = _state.value.copy(
                    currentStep = createScannerSetupCompleteStep()
                )
            }

            is ScanningSetupStep.ScannerSetupComplete -> {
                // Handled by parent through onDismissRequest
            }
        }
    }

    private fun handleSecondaryButtonClick() {
        when (_state.value.currentStep) {
            is ScanningSetupStep.Welcome -> error("Secondary button should not be available on Welcome step")
            is ScanningSetupStep.DeviceSelection -> error(
                "Secondary button should not be available on DeviceSelection step"
            )

            is ScanningSetupStep.Introduction -> {
                _state.value = _state.value.copy(
                    currentStep = createDeviceSelectionStep()
                )
            }

            is ScanningSetupStep.BluetoothWarning -> {
                _state.value = _state.value.copy(
                    currentStep = createBluetoothIntroductionStep()
                )
            }

            is ScanningSetupStep.BluetoothPairing -> {
                _state.value = _state.value.copy(
                    currentStep = createBluetoothWarningStep()
                )
            }

            is ScanningSetupStep.PairOnYourDevice -> {
                _state.value = _state.value.copy(
                    currentStep = createBluetoothPairingStep()
                )
            }

            is ScanningSetupStep.TestYourScanner -> {
                _state.value = _state.value.copy(
                    currentStep = createPairOnYourDeviceStep()
                )
            }

            is ScanningSetupStep.ScannerSetupComplete -> {
                error("Secondary button should not be available on ScannerSetupComplete step")
            }
        }
    }

    private fun createWelcomeStep() = ScanningSetupStep.Welcome(
        title = resourceProvider.getString(R.string.woopos_scanning_setup_welcome_title),
        message = resourceProvider.getString(R.string.woopos_scanning_setup_welcome_message),
        setupButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_welcome_setup_button),
        documentationButtonText = resourceProvider.getString(
            R.string.woopos_scanning_setup_welcome_documentation_button
        )
    )

    private fun createDeviceSelectionStep() = ScanningSetupStep.DeviceSelection(
        title = resourceProvider.getString(R.string.woopos_scanning_setup_device_selection_title),
        devices = listOf(
            BarcodeReaderDevice.TERA_1200,
            BarcodeReaderDevice.STAR_BSH_20B,
            BarcodeReaderDevice.INATECK_BLUETOOTH,
            BarcodeReaderDevice.OTHER
        )
    )

    private fun createBluetoothIntroductionStep() = ScanningSetupStep.Introduction(
        title = resourceProvider.getString(
            R.string.woopos_scanning_setup_introduction_title,
            resourceProvider.getString(_state.value.selectedDevice!!.displayNameRes)
        ),
        message = resourceProvider.getString(
            R.string.woopos_scanning_setup_introduction_message,
            resourceProvider.getString(_state.value.selectedDevice!!.displayNameRes)
        ),
        primaryButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_next),
        secondaryButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_back)
    )

    private fun createBluetoothWarningStep() = ScanningSetupStep.BluetoothWarning(
        title = resourceProvider.getString(R.string.woopos_scanning_setup_bluetooth_warning_title),
        message = resourceProvider.getString(R.string.woopos_scanning_setup_bluetooth_warning_message),
        primaryButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_next),
        secondaryButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_back)
    )

    private fun createBluetoothPairingStep() = ScanningSetupStep.BluetoothPairing(
        title = resourceProvider.getString(R.string.woopos_scanning_setup_bluetooth_pairing_title),
        message = resourceProvider.getString(R.string.woopos_scanning_setup_bluetooth_pairing_message),
        barcodeImageRes = R.drawable.ic_barcode,
        instructionText = resourceProvider.getString(R.string.woopos_scanning_setup_bluetooth_pairing_instruction),
        primaryButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_next),
        secondaryButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_back)
    )

    private fun createPairOnYourDeviceStep() = ScanningSetupStep.PairOnYourDevice(
        title = resourceProvider.getString(R.string.woopos_scanning_setup_pair_device_title),
        message = resourceProvider.getString(
            R.string.woopos_scanning_setup_pair_device_message,
            resourceProvider.getString(_state.value.selectedDevice!!.displayNameRes),
        ),
        barcodeImageRes = R.drawable.ic_barcode,
        instructionText = resourceProvider.getString(R.string.woopos_scanning_setup_pair_device_instruction),
        primaryButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_next),
        secondaryButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_back),
        bluetoothSettingsButtonText = resourceProvider.getString(
            R.string.woopos_scanning_setup_pair_device_bluetooth_button
        )
    )

    private fun createTestYourScannerStep() = ScanningSetupStep.TestYourScanner(
        title = resourceProvider.getString(R.string.woopos_scanning_setup_test_scanner_title),
        message = resourceProvider.getString(R.string.woopos_scanning_setup_test_scanner_message),
        barcodeImageRes = R.drawable.ic_barcode,
        instructionText = resourceProvider.getString(R.string.woopos_scanning_setup_test_scanner_instruction),
        primaryButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_done),
        secondaryButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_back)
    )

    private fun createScannerSetupCompleteStep() = ScanningSetupStep.ScannerSetupComplete(
        title = resourceProvider.getString(R.string.woopos_scanning_setup_complete_title),
        message = resourceProvider.getString(R.string.woopos_scanning_setup_complete_message),
        primaryButtonText = resourceProvider.getString(R.string.woopos_scanning_setup_button_done)
    )
}

sealed class WooPosScanningSetupUiEvent {
    data object OnBluetoothScannerSelected : WooPosScanningSetupUiEvent()
    data object OnPrimaryButtonClicked : WooPosScanningSetupUiEvent()
    data object OnSecondaryButtonClicked : WooPosScanningSetupUiEvent()
    data object OnViewDocumentation : WooPosScanningSetupUiEvent()
    data object OnOpenBluetoothSettings : WooPosScanningSetupUiEvent()
    data class OnDeviceSelected(val device: BarcodeReaderDevice) : WooPosScanningSetupUiEvent()
}
