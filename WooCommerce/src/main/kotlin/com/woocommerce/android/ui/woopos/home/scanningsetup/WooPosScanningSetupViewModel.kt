package com.woocommerce.android.ui.woopos.home.scanningsetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.common.composeui.modifier.BarcodeInputDetector
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.Companion.TEST_BARCODE_EAN13
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.ScanningSetupStep
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
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
    private val navigator: WooPosScannerSetupNavigator,
    private val analyticsTracker: WooPosAnalyticsTracker
) : ViewModel() {

    private val _state = MutableStateFlow(
        WooPosScanningSetupState(
            currentStep = navigator.getInitialStep(),
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
            WooPosScanningSetupUiEvent.OnDialogShown -> {
                viewModelScope.launch {
                    analyticsTracker.track(WooPosAnalyticsEvent.Event.BarcodeScannerSetupFlowShown)
                }
            }

            is WooPosScanningSetupUiEvent.OnDeviceSelected -> {
                _state.value = _state.value.copy(
                    selectedDevice = event.device
                )

                viewModelScope.launch {
                    analyticsTracker.track(
                        WooPosAnalyticsEvent.Event.BarcodeScannerSetupScannerSelected(
                            scanner = event.device.name
                        )
                    )
                }

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

    private fun handlePrimaryButtonClick() {
        when (_state.value.currentStep) {
            is ScanningSetupStep.ScannerHIDModeSetup,
            is ScanningSetupStep.ScannerPairModeSetup,
            is ScanningSetupStep.PairYourScanner,
            is ScanningSetupStep.ScannerSetupInfo,
            is ScanningSetupStep.ScannerSetupSuccess -> {
                navigateToNextStep()
            }

            is ScanningSetupStep.ScannerSetupBarcodesOnProducts -> {
                viewModelScope.launch {
                    _dismissDialogEvent.emit(Unit)
                }
            }

            is ScanningSetupStep.TestYourScannerScanFailed -> resetToInitialState()

            is ScanningSetupStep.DeviceSelection,
            is ScanningSetupStep.TestYourScanner,
            is ScanningSetupStep.TestYourScannerTimeout ->
                error(
                    "Primary button should not be available on ${_state.value.currentStep::class.simpleName} step"
                )
        }
    }

    private fun handleSecondaryButtonClick() {
        val selectedDevice = _state.value.selectedDevice
        val previousStep = if (selectedDevice != null) {
            navigator.getPreviousStep(selectedDevice, _state.value.currentStep)
        } else {
            navigator.getInitialStep()
        }

        requireNotNull(previousStep) { "Previous step cannot be null if secondary button present" }
        _state.value = _state.value.copy(currentStep = previousStep)
    }

    fun resetToInitialState() {
        _state.value = _state.value.copy(
            currentStep = navigator.getInitialStep(),
            selectedDevice = null
        )
    }

    private fun navigateToNextStep() {
        val selectedDevice = requireNotNull(_state.value.selectedDevice) { "Selected device cannot be null" }
        val nextStep = navigator.getNextStep(selectedDevice, _state.value.currentStep)
        _state.value = _state.value.copy(currentStep = nextStep)

        if (nextStep is ScanningSetupStep.TestYourScanner) {
            startAutoNavigationToTestYourScannerFailedStep()
        }
    }

    private fun handleBarcodeScanned(barcodeResult: BarcodeInputDetector.BarcodeResult) {
        val selectedDevice = requireNotNull(_state.value.selectedDevice) { "Selected device cannot be null" }
        val nextStep = if (barcodeResult.barcode == TEST_BARCODE_EAN13) {
            navigator.getNextStepForValidBarcode(selectedDevice, _state.value.currentStep)
        } else {
            navigator.getNextStepForInvalidBarcode(_state.value.currentStep)
        }
        _state.value = _state.value.copy(currentStep = nextStep)
    }

    private fun startAutoNavigationToTestYourScannerFailedStep() {
        autoNavigationJob?.cancel()
        autoNavigationJob = viewModelScope.launch {
            delay(AUTO_NAVIGATION_DELAY_MS)
            if (navigator.isStillOnTestBarcodeStep(_state.value.currentStep)) {
                val timeoutStep = navigator.getTestBarcodeTimeoutStep(_state.value.currentStep)
                _state.value = _state.value.copy(currentStep = timeoutStep)
            }
        }
    }

    companion object {
        private const val AUTO_NAVIGATION_DELAY_MS = 10000L
    }
}
