package com.woocommerce.android.ui.woopos.home.scanningsetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.BarcodeReaderDevice
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.ScanningSetupStep
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
class WooPosScanningSetupViewModel @Inject constructor() : ViewModel() {

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

    companion object {
        private const val WOO_POS_BARCODE_DOC_URL = "https://woocommerce.com/document/barcode-and-qr-code-scanner/"
    }

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
        title = "Start using a barcode scanner",
        message = "Choose an option:",
        setupButtonText = "Set up a barcode scanner",
        documentationButtonText = "View barcode scanner documentation"
    )

    private fun createDeviceSelectionStep() = ScanningSetupStep.DeviceSelection(
        title = "Set up a barcode scanner",
        devices = listOf(
            BarcodeReaderDevice.TERA_1200,
            BarcodeReaderDevice.STAR_BSH_20B,
            BarcodeReaderDevice.INATECK_BLUETOOTH,
            BarcodeReaderDevice.OTHER
        )
    )

    private fun createBluetoothIntroductionStep() = ScanningSetupStep.Introduction(
        title = "Set up your ${_state.value.selectedDevice!!.displayName}",
        message = "Follow these steps to connect your ${_state.value.selectedDevice!!.displayName} barcode scanner.",
        primaryButtonText = "Next",
        secondaryButtonText = "Back"
    )

    private fun createBluetoothWarningStep() = ScanningSetupStep.BluetoothWarning(
        title = "Bluetooth pairing",
        message = "Make sure your scanner is in pairing mode before proceeding.",
        primaryButtonText = "Next",
        secondaryButtonText = "Back"
    )

    private fun createBluetoothPairingStep() = ScanningSetupStep.BluetoothPairing(
        title = "Scan this barcode",
        message = "Use your scanner to scan this barcode to configure it for pairing.",
        barcodeImageRes = R.drawable.ic_barcode,
        instructionText = "Scan the barcode above with your scanner",
        primaryButtonText = "Next",
        secondaryButtonText = "Back"
    )

    private fun createPairOnYourDeviceStep() = ScanningSetupStep.PairOnYourDevice(
        title = "Pair on your device",
        message = "Now scan this barcode to complete the pairing process.",
        barcodeImageRes = R.drawable.ic_barcode,
        instructionText = "Scan the barcode above to complete pairing",
        primaryButtonText = "Next",
        secondaryButtonText = "Back"
    )

    private fun createTestYourScannerStep() = ScanningSetupStep.TestYourScanner(
        title = "Test your scanner",
        message = "Scan this test barcode to verify your scanner is working correctly.",
        barcodeImageRes = R.drawable.ic_barcode,
        instructionText = "Scan the barcode above to test your scanner",
        primaryButtonText = "Done",
        secondaryButtonText = "Back"
    )

    private fun createScannerSetupCompleteStep() = ScanningSetupStep.ScannerSetupComplete(
        title = "Scanner setup complete!",
        message = "Your barcode scanner is now ready to use. You can start scanning products to add them to orders.",
        primaryButtonText = "Done"
    )
}

sealed class WooPosScanningSetupUiEvent {
    data object OnBluetoothScannerSelected : WooPosScanningSetupUiEvent()
    data object OnPrimaryButtonClicked : WooPosScanningSetupUiEvent()
    data object OnSecondaryButtonClicked : WooPosScanningSetupUiEvent()
    data object OnViewDocumentation : WooPosScanningSetupUiEvent()
    data class OnDeviceSelected(val device: BarcodeReaderDevice) : WooPosScanningSetupUiEvent()
}
