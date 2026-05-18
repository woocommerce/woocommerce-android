package com.woocommerce.android.ui.login.qrlogin

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.login.UnifiedLoginTracker
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Click
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Flow
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Step
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
    private val unifiedLoginTracker: UnifiedLoginTracker,
) : ScopedViewModel(savedState) {

    private val cameraPermissionDenial = MutableStateFlow(CameraDenialState.Hidden)

    val uiState: StateFlow<UiState> = cameraPermissionDenial
        .map { UiState(cameraPermissionDialog = it.toDialogState()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    fun onPrologueShown() {
        unifiedLoginTracker.track(Flow.LOGIN_QR, Step.QR_PROLOGUE)
    }

    fun onScanClicked(isCameraPermissionGranted: Boolean) {
        unifiedLoginTracker.trackClick(Click.LOGIN_QR_SCAN)
        if (isCameraPermissionGranted) {
            triggerEvent(Dispatch.NavigateToScanner)
        } else {
            requestCameraPermission()
        }
    }

    fun onSiteAddressLoginClicked() {
        unifiedLoginTracker.trackClick(Click.LOGIN_QR_FALLBACK)
        triggerEvent(Dispatch.NavigateToSiteAddressLogin)
    }

    fun onHelpClicked() {
        unifiedLoginTracker.trackClick(Click.SHOW_HELP)
        triggerEvent(Dispatch.NavigateToHelp)
    }

    fun onCameraPermissionResult(granted: Boolean, shouldShowRationale: Boolean) {
        if (granted) {
            unifiedLoginTracker.trackClick(Click.QR_CAMERA_PERMISSION_GRANTED)
            cameraPermissionDenial.value = CameraDenialState.Hidden
            triggerEvent(Dispatch.NavigateToScanner)
            return
        }
        unifiedLoginTracker.trackClick(Click.QR_CAMERA_PERMISSION_DENIED)
        cameraPermissionDenial.value = if (shouldShowRationale) {
            CameraDenialState.FirstDenial
        } else {
            CameraDenialState.PermanentlyDenied
        }
    }

    fun onCameraDenialPrimaryClicked() {
        val current = cameraPermissionDenial.value
        if (current == CameraDenialState.Hidden) return
        cameraPermissionDenial.value = CameraDenialState.Hidden
        when (current) {
            CameraDenialState.FirstDenial -> requestCameraPermission()
            CameraDenialState.PermanentlyDenied -> triggerEvent(Dispatch.OpenAppSettings)
            CameraDenialState.Hidden -> Unit
        }
    }

    fun onCameraDenialCancelled() {
        if (cameraPermissionDenial.value == CameraDenialState.Hidden) return
        cameraPermissionDenial.value = CameraDenialState.Hidden
    }

    private fun requestCameraPermission() {
        unifiedLoginTracker.trackClick(Click.QR_CAMERA_PERMISSION_DIALOG_SHOWN)
        triggerEvent(Dispatch.LaunchCameraPermissionRequest)
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
