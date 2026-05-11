package com.woocommerce.android.ui.login.qrlogin

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.UnifiedLoginTracker
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Owns the prologue's user-intent analytics and the camera-permission state machine.
 *
 * The host fragment supplies the OS-level inputs (whether camera permission is currently
 * granted, and whether the system would still re-prompt) via [onScanClicked] and
 * [onCameraPermissionResult]. The view model decides which dialog content to show next and
 * emits side-effect events ([Dispatch]) for the fragment to act on — launching the permission
 * request, opening app settings, or navigating to the scanner / fallback flow. The screen is
 * stateless and just renders [UiState.cameraPermissionDialog] when present.
 */
@HiltViewModel
class QrLoginPrologueViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val unifiedLoginTracker: UnifiedLoginTracker,
) : ScopedViewModel(savedState) {

    private val denialState = MutableStateFlow(CameraDenialState.Hidden)

    val uiState: StateFlow<UiState> = denialState
        .map { UiState(cameraPermissionDialog = it.toDialogState()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    fun onPrologueShown() {
        analyticsTracker.track(AnalyticsEvent.LOGIN_QR_PROLOGUE_SHOWN)
        unifiedLoginTracker.track(UnifiedLoginTracker.Flow.LOGIN_QR, UnifiedLoginTracker.Step.QR_PROLOGUE)
    }

    fun onScanClicked(isCameraPermissionGranted: Boolean) {
        analyticsTracker.track(AnalyticsEvent.LOGIN_QR_PROLOGUE_SCAN_TAPPED)
        unifiedLoginTracker.trackClick(UnifiedLoginTracker.Click.LOGIN_QR_SCAN)
        if (isCameraPermissionGranted) {
            triggerEvent(Dispatch.NavigateToScanner)
        } else {
            triggerEvent(Dispatch.LaunchCameraPermissionRequest)
        }
    }

    fun onFallbackClicked() {
        analyticsTracker.track(AnalyticsEvent.LOGIN_QR_PROLOGUE_FALLBACK_TAPPED)
        unifiedLoginTracker.trackClick(UnifiedLoginTracker.Click.LOGIN_QR_FALLBACK)
        triggerEvent(Dispatch.NavigateToFallback)
    }

    fun onCameraPermissionResult(granted: Boolean, shouldShowRationale: Boolean) {
        if (granted) {
            denialState.value = CameraDenialState.Hidden
            triggerEvent(Dispatch.NavigateToScanner)
            return
        }
        // Once the user has denied, shouldShowRationale tells us whether Android will keep
        // re-prompting (true → first denial) or has stopped (false → permanently denied /
        // "Don't ask again"). Before the very first request it would also be false, but we
        // only reach this branch after a denial.
        val next = if (shouldShowRationale) {
            CameraDenialState.FirstDenial
        } else {
            CameraDenialState.PermanentlyDenied
        }
        denialState.value = next
        analyticsTracker.track(
            AnalyticsEvent.LOGIN_QR_PROLOGUE_CAMERA_PERMISSION_DIALOG_SHOWN,
            mapOf(KEY_STATE to next.analyticsValue())
        )
    }

    fun onCameraDenialPrimaryClicked() {
        val current = denialState.value
        if (current == CameraDenialState.Hidden) return
        analyticsTracker.track(
            AnalyticsEvent.LOGIN_QR_PROLOGUE_CAMERA_PERMISSION_PRIMARY_TAPPED,
            mapOf(KEY_STATE to current.analyticsValue())
        )
        denialState.value = CameraDenialState.Hidden
        when (current) {
            CameraDenialState.FirstDenial -> triggerEvent(Dispatch.LaunchCameraPermissionRequest)
            CameraDenialState.PermanentlyDenied -> triggerEvent(Dispatch.OpenAppSettings)
            CameraDenialState.Hidden -> Unit
        }
    }

    fun onCameraDenialCancelled() {
        val current = denialState.value
        if (current == CameraDenialState.Hidden) return
        analyticsTracker.track(
            AnalyticsEvent.LOGIN_QR_PROLOGUE_CAMERA_PERMISSION_DISMISSED,
            mapOf(KEY_STATE to current.analyticsValue())
        )
        denialState.value = CameraDenialState.Hidden
    }

    data class UiState(val cameraPermissionDialog: CameraPermissionDialogState? = null)

    /**
     * Resource ids for the camera-permission dialog. The view model picks the appropriate set
     * based on whether the user can still be re-prompted or has to go through Settings; the
     * screen just renders these without re-deriving anything.
     */
    data class CameraPermissionDialogState(
        @StringRes val title: Int,
        @StringRes val body: Int,
        @StringRes val primaryLabel: Int,
    )

    private enum class CameraDenialState { Hidden, FirstDenial, PermanentlyDenied }

    sealed class Dispatch : Event() {
        object LaunchCameraPermissionRequest : Dispatch()
        object OpenAppSettings : Dispatch()
        object NavigateToScanner : Dispatch()
        object NavigateToFallback : Dispatch()
    }

    companion object {
        private const val KEY_STATE = "state"
        private const val VALUE_FIRST_DENIAL = "first_denial"
        private const val VALUE_PERMANENTLY_DENIED = "permanently_denied"

        private fun CameraDenialState.analyticsValue(): String = when (this) {
            CameraDenialState.FirstDenial -> VALUE_FIRST_DENIAL
            CameraDenialState.PermanentlyDenied -> VALUE_PERMANENTLY_DENIED
            CameraDenialState.Hidden -> error("Hidden state must not be reported to analytics")
        }

        private fun CameraDenialState.toDialogState(): CameraPermissionDialogState? = when (this) {
            CameraDenialState.Hidden -> null
            CameraDenialState.FirstDenial -> CameraPermissionDialogState(
                title = R.string.login_qr_prologue_camera_denied_title,
                body = R.string.login_qr_prologue_camera_denied_body,
                primaryLabel = R.string.login_qr_prologue_camera_denied_allow_button,
            )
            CameraDenialState.PermanentlyDenied -> CameraPermissionDialogState(
                title = R.string.login_qr_prologue_camera_blocked_title,
                body = R.string.login_qr_prologue_camera_blocked_body,
                primaryLabel = R.string.login_qr_prologue_camera_blocked_settings_button,
            )
        }
    }
}
