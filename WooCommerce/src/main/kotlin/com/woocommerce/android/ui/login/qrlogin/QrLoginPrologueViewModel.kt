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

@HiltViewModel
class QrLoginPrologueViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val unifiedLoginTracker: UnifiedLoginTracker,
) : ScopedViewModel(savedState) {

    private val cameraPermissionDenial = MutableStateFlow(CameraDenialState.Hidden)

    val uiState: StateFlow<UiState> = cameraPermissionDenial
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

    fun onSiteAddressLoginClicked() {
        analyticsTracker.track(AnalyticsEvent.LOGIN_QR_PROLOGUE_FALLBACK_TAPPED)
        unifiedLoginTracker.trackClick(UnifiedLoginTracker.Click.LOGIN_QR_FALLBACK)
        triggerEvent(Dispatch.NavigateToSiteAddressLogin)
    }

    fun onHelpClicked() {
        analyticsTracker.track(AnalyticsEvent.LOGIN_QR_HELP_TAPPED)
        unifiedLoginTracker.trackClick(UnifiedLoginTracker.Click.SHOW_HELP)
        triggerEvent(Dispatch.NavigateToHelp)
    }

    fun onCameraPermissionResult(granted: Boolean, shouldShowRationale: Boolean) {
        if (granted) {
            cameraPermissionDenial.value = CameraDenialState.Hidden
            triggerEvent(Dispatch.NavigateToScanner)
            return
        }
        val next = if (shouldShowRationale) {
            CameraDenialState.FirstDenial
        } else {
            CameraDenialState.PermanentlyDenied
        }
        cameraPermissionDenial.value = next
        analyticsTracker.track(
            AnalyticsEvent.LOGIN_QR_PROLOGUE_CAMERA_PERMISSION_DIALOG_SHOWN,
            mapOf(KEY_STATE to next.analyticsValue())
        )
    }

    fun onCameraDenialPrimaryClicked() {
        val current = cameraPermissionDenial.value
        if (current == CameraDenialState.Hidden) return
        analyticsTracker.track(
            AnalyticsEvent.LOGIN_QR_PROLOGUE_CAMERA_PERMISSION_PRIMARY_TAPPED,
            mapOf(KEY_STATE to current.analyticsValue())
        )
        cameraPermissionDenial.value = CameraDenialState.Hidden
        when (current) {
            CameraDenialState.FirstDenial -> triggerEvent(Dispatch.LaunchCameraPermissionRequest)
            CameraDenialState.PermanentlyDenied -> triggerEvent(Dispatch.OpenAppSettings)
            CameraDenialState.Hidden -> Unit
        }
    }

    fun onCameraDenialCancelled() {
        val current = cameraPermissionDenial.value
        if (current == CameraDenialState.Hidden) return
        analyticsTracker.track(
            AnalyticsEvent.LOGIN_QR_PROLOGUE_CAMERA_PERMISSION_DISMISSED,
            mapOf(KEY_STATE to current.analyticsValue())
        )
        cameraPermissionDenial.value = CameraDenialState.Hidden
    }

    data class UiState(val cameraPermissionDialog: CameraPermissionDialogState? = null)

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
        object NavigateToSiteAddressLogin : Dispatch()
        object NavigateToHelp : Dispatch()
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
