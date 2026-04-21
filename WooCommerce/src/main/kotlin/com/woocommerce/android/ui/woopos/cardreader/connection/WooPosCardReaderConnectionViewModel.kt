package com.woocommerce.android.ui.woopos.cardreader.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState
import com.woocommerce.android.ui.woopos.util.WooPosPermissionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosCardReaderConnectionViewModel @Inject constructor(
    private val controllerFactory: WooPosCardReaderConnectionControllerFactory,
    private val permissionUtils: WooPosPermissionUtils,
) : ViewModel() {
    private val _isRemoteTapToPayExplainerVisible = MutableStateFlow(false)
    val isRemoteTapToPayExplainerVisible: StateFlow<Boolean> = _isRemoteTapToPayExplainerVisible.asStateFlow()

    fun onRemoteTapToPayTipClicked() {
        _isRemoteTapToPayExplainerVisible.value = true
    }

    fun onRemoteTapToPayExplainerDismissed() {
        _isRemoteTapToPayExplainerVisible.value = false
    }

    private val controller: WooPosCardReaderConnectionController by lazy {
        controllerFactory.create(viewModelScope)
    }

    val state: StateFlow<WooPosCardReaderConnectionState> = controller.state

    private val _event = MutableSharedFlow<Event>()
    val event = _event.asSharedFlow()

    init {
        viewModelScope.launch {
            controller.event.collect { controllerEvent ->
                when (controllerEvent) {
                    WooPosCardReaderConnectionController.ControllerEvent.RequestBluetoothPermission -> {
                        _event.emit(Event.RequestBluetoothPermission)
                    }
                    WooPosCardReaderConnectionController.ControllerEvent.RequestEnableBluetooth -> {
                        _event.emit(Event.RequestEnableBluetooth)
                    }
                    WooPosCardReaderConnectionController.ControllerEvent.RequestLocationPermission -> {
                        _event.emit(Event.RequestLocationPermission)
                    }
                    WooPosCardReaderConnectionController.ControllerEvent.RequestEnableLocation -> {
                        _event.emit(Event.RequestEnableLocation)
                    }
                    WooPosCardReaderConnectionController.ControllerEvent.OpenAppSettings -> {
                        permissionUtils.showAppSettings()
                    }
                    WooPosCardReaderConnectionController.ControllerEvent.Cancelled -> {
                        _event.emit(Event.Dismissed)
                    }
                    is WooPosCardReaderConnectionController.ControllerEvent.OnboardingRequired -> {
                        _event.emit(Event.NavigateToOnboarding(controllerEvent.onboardingState))
                    }
                }
            }
        }
    }

    fun startConnectionFlow() {
        controller.startConnectionFlow()
    }

    fun startUpdateFlow() {
        controller.startUpdateFlow()
    }

    fun dismissDialog() {
        controller.cancel()
    }

    fun onBackPressed() {
        when (val currentState = state.value) {
            is WooPosCardReaderConnectionState.UpdateRequired -> {
                if (currentState.showCancelWarning) {
                    dismissDialog()
                } else {
                    currentState.onBackClicked()
                }
            }
            is WooPosCardReaderConnectionState.UpdateAvailable -> {
                if (currentState.showCancelWarning) {
                    dismissDialog()
                } else {
                    currentState.onBackClicked()
                }
            }
            is WooPosCardReaderConnectionState.OnboardingError,
            is WooPosCardReaderConnectionState.BluetoothDisabled,
            is WooPosCardReaderConnectionState.Connected,
            WooPosCardReaderConnectionState.Connecting,
            is WooPosCardReaderConnectionState.ConnectingFailed,
            is WooPosCardReaderConnectionState.ConnectingFailedBatteryLow,
            is WooPosCardReaderConnectionState.InvalidMerchantAddress,
            is WooPosCardReaderConnectionState.InvalidPostalCode,
            is WooPosCardReaderConnectionState.LocationDisabled,
            is WooPosCardReaderConnectionState.MissingBluetoothPermission,
            is WooPosCardReaderConnectionState.MissingLocationPermission,
            is WooPosCardReaderConnectionState.MultipleReadersFound,
            is WooPosCardReaderConnectionState.ReaderFound,
            is WooPosCardReaderConnectionState.Scanning,
            is WooPosCardReaderConnectionState.ScanningFailed,
            WooPosCardReaderConnectionState.UpdateCompleted,
            is WooPosCardReaderConnectionState.UpdateFailed,
            is WooPosCardReaderConnectionState.UpdateFailedBatteryLow -> {
                dismissDialog()
            }
        }
    }

    fun onBluetoothPermissionResult(granted: Boolean, shouldShowRationale: Boolean) {
        controller.onBluetoothPermissionResult(granted, shouldShowRationale)
    }

    fun onLocationPermissionResult(granted: Boolean, shouldShowRationale: Boolean) {
        controller.onLocationPermissionResult(granted, shouldShowRationale)
    }

    fun onLocationEnabled() {
        controller.onLocationEnabled()
    }

    fun onBluetoothEnabled() {
        controller.onBluetoothEnabled()
    }

    fun onOnboardingCompleted() {
        controller.onOnboardingCompleted()
    }

    fun onResume() {
        controller.recheckPermissions()
    }

    sealed interface Event {
        data object RequestBluetoothPermission : Event
        data object RequestEnableBluetooth : Event
        data object RequestLocationPermission : Event
        data object RequestEnableLocation : Event
        data object Dismissed : Event
        data class NavigateToOnboarding(val onboardingState: CardReaderOnboardingState) : Event
    }
}
