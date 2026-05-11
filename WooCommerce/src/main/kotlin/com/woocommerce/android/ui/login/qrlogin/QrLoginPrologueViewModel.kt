package com.woocommerce.android.ui.login.qrlogin

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Owns the camera-permission state machine for the QR login prologue.
 *
 * The host fragment supplies the OS-level inputs (whether camera permission is currently
 * granted, and whether the system would still re-prompt) via [onScanClicked] and
 * [onCameraPermissionResult]. The view model decides which dialog state to show next and
 * emits side-effect events ([Dispatch]) for the fragment to act on — launching the permission
 * request, opening app settings, or navigating to the scanner. The screen itself is stateless
 * and just renders [UiState.cameraDenial].
 */
@HiltViewModel
class QrLoginPrologueViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ScopedViewModel(savedState) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onScanClicked(isCameraPermissionGranted: Boolean) {
        if (isCameraPermissionGranted) {
            triggerEvent(Dispatch.NavigateToScanner)
        } else {
            triggerEvent(Dispatch.LaunchCameraPermissionRequest)
        }
    }

    fun onCameraPermissionResult(granted: Boolean, shouldShowRationale: Boolean) {
        if (granted) {
            _uiState.update { it.copy(cameraDenial = CameraDenialState.Hidden) }
            triggerEvent(Dispatch.NavigateToScanner)
            return
        }
        // Once the user has denied, shouldShowRationale tells us whether Android will keep
        // re-prompting (true → first denial) or has stopped (false → permanently denied /
        // "Don't ask again"). Before the very first request it would also be false, but we
        // only reach this branch after a denial.
        val nextState = if (shouldShowRationale) {
            CameraDenialState.FirstDenial
        } else {
            CameraDenialState.PermanentlyDenied
        }
        _uiState.update { it.copy(cameraDenial = nextState) }
        analyticsTracker.track(
            AnalyticsEvent.LOGIN_QR_PROLOGUE_CAMERA_PERMISSION_DIALOG_SHOWN,
            mapOf(KEY_STATE to nextState.analyticsValue())
        )
    }

    fun onCameraDenialPrimaryClicked() {
        val current = _uiState.value.cameraDenial
        if (current == CameraDenialState.Hidden) return
        analyticsTracker.track(
            AnalyticsEvent.LOGIN_QR_PROLOGUE_CAMERA_PERMISSION_PRIMARY_TAPPED,
            mapOf(KEY_STATE to current.analyticsValue())
        )
        _uiState.update { it.copy(cameraDenial = CameraDenialState.Hidden) }
        when (current) {
            CameraDenialState.FirstDenial -> triggerEvent(Dispatch.LaunchCameraPermissionRequest)
            CameraDenialState.PermanentlyDenied -> triggerEvent(Dispatch.OpenAppSettings)
            CameraDenialState.Hidden -> Unit
        }
    }

    fun onCameraDenialCancelled() {
        val current = _uiState.value.cameraDenial
        if (current == CameraDenialState.Hidden) return
        analyticsTracker.track(
            AnalyticsEvent.LOGIN_QR_PROLOGUE_CAMERA_PERMISSION_DISMISSED,
            mapOf(KEY_STATE to current.analyticsValue())
        )
        _uiState.update { it.copy(cameraDenial = CameraDenialState.Hidden) }
    }

    data class UiState(val cameraDenial: CameraDenialState = CameraDenialState.Hidden)

    /**
     * Dialog state shown after a camera-permission denial. [FirstDenial] is recoverable in-app;
     * [PermanentlyDenied] requires the user to enable the permission via Settings.
     */
    enum class CameraDenialState { Hidden, FirstDenial, PermanentlyDenied }

    sealed class Dispatch : Event() {
        object LaunchCameraPermissionRequest : Dispatch()
        object OpenAppSettings : Dispatch()
        object NavigateToScanner : Dispatch()
    }

    companion object {
        private const val KEY_STATE = "state"
        private const val VALUE_FIRST_DENIAL = "first_denial"
        private const val VALUE_PERMANENTLY_DENIED = "permanently_denied"
        private const val VALUE_HIDDEN = "hidden"

        private fun CameraDenialState.analyticsValue(): String = when (this) {
            CameraDenialState.FirstDenial -> VALUE_FIRST_DENIAL
            CameraDenialState.PermanentlyDenied -> VALUE_PERMANENTLY_DENIED
            CameraDenialState.Hidden -> VALUE_HIDDEN
        }
    }
}
