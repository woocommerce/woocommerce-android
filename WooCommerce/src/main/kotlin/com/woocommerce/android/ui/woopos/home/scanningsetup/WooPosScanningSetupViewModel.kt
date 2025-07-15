package com.woocommerce.android.ui.woopos.home.scanningsetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.common.composeui.modifier.BarcodeInputDetector
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.ScanningSetupStep
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
    private val navigator: ScannerSetupNavigator
) : ViewModel() {

    private val _state = MutableStateFlow(
        WooPosScanningSetupState(
            isVisible = false,
            currentStep = navigator.createDeviceSelectionStep(),
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

                val nextStep = navigator.getNextStep(event.device, _state.value.currentStep)
                _state.value = _state.value.copy(currentStep = nextStep)
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
            currentStep = navigator.restartFlow(),
            selectedDevice = null
        )
    }

    private fun handlePrimaryButtonClick() {
        when (_state.value.currentStep) {
            is ScanningSetupStep.ScannerHIDModeSetup,
            is ScanningSetupStep.ScannerPairModeSetup,
            is ScanningSetupStep.PairYourScanner -> {
                navigateToNextStep()
            }

            is ScanningSetupStep.ScannerSetupInfo -> {
                viewModelScope.launch {
                    _dismissDialogEvent.emit(Unit)
                }
            }

            is ScanningSetupStep.TestYourScannerScanFailed -> resetToInitialState()

            is ScanningSetupStep.ScannerSetupSuccess -> {
                error("Not implemented yet")
            }

            is ScanningSetupStep.DeviceSelection,
            is ScanningSetupStep.TestYourScanner,
            is ScanningSetupStep.TestYourScannerTimeout ->
                error("Primary button should not be available on ${_state.value.currentStep::class.simpleName} step")
        }
    }

    private fun navigateToNextStep() {
        val selectedDevice = requireNotNull(_state.value.selectedDevice) { "Selected device cannot be null" }
        val nextStep = navigator.getNextStep(selectedDevice, _state.value.currentStep)
        _state.value = _state.value.copy(currentStep = nextStep)

        if (nextStep is ScanningSetupStep.TestYourScanner) {
            startAutoNavigationToTestYourScannerFailedStep()
        }
    }

    private fun handleSecondaryButtonClick() {
        val selectedDevice = _state.value.selectedDevice
        val previousStep = if (selectedDevice != null) {
            navigator.getPreviousStep(selectedDevice, _state.value.currentStep)
        } else {
            navigator.createDeviceSelectionStep()
        }

        requireNotNull(previousStep) { "Previous step cannot be null if secondary button present" }
        _state.value = _state.value.copy(currentStep = previousStep)
    }

    private fun handleBarcodeScanned(barcodeResult: BarcodeInputDetector.BarcodeResult) {
        when (_state.value.currentStep) {
            is ScanningSetupStep.TestYourScanner,
            is ScanningSetupStep.TestYourScannerTimeout -> {
                val selectedDevice = requireNotNull(_state.value.selectedDevice) { "Selected device cannot be null" }
                if (barcodeResult.barcode == TEST_BARCODE_EAN13) {
                    val nextStep = navigator.getNextStep(selectedDevice, _state.value.currentStep)
                    _state.value = _state.value.copy(currentStep = nextStep)
                } else {
                    _state.value = _state.value.copy(
                        currentStep = navigator.createTestYourScannerScanFailedStep()
                    )
                }
            }

            else -> {
                error("Barcode scanning is not expected in the current step: ${_state.value.currentStep}")
            }
        }
    }

    private fun startAutoNavigationToTestYourScannerFailedStep() {
        autoNavigationJob?.cancel()
        autoNavigationJob = viewModelScope.launch {
            delay(AUTO_NAVIGATION_DELAY_MS)
            if (_state.value.currentStep is ScanningSetupStep.TestYourScanner) {
                _state.value = _state.value.copy(
                    currentStep = navigator.createTestYourScannerTimeoutStep()
                )
            }
        }
    }

    companion object {
        private const val AUTO_NAVIGATION_DELAY_MS = 10000L
        private const val TEST_BARCODE_EAN13 = "1234567890128"
    }
}
