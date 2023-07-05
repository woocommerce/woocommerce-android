package com.woocommerce.android.ui.barcodescanner

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import javax.inject.Inject

class BarcodeScanningViewModel @Inject constructor(
    savedState: SavedStateHandle,
) : ScopedViewModel(savedState) {
    private val _permissionState = MutableLiveData<PermissionState>()
    val permissionState: LiveData<PermissionState> = _permissionState

    fun updatePermissionState(
        isPermissionGranted: Boolean,
        shouldShowRequestPermissionRationale: Boolean,
    ) {
        when {
            isPermissionGranted -> {
                // display scanning screen
                _permissionState.value = PermissionState.Granted
            }

            !isPermissionGranted && shouldShowRequestPermissionRationale -> {
                // Denied once, ask for camera permission again
                _permissionState.value = PermissionState.ShouldShowRationale(
                    title = R.string.barcode_scanning_alert_dialog_title,
                    message = R.string.barcode_scanning_alert_dialog_rationale_message,
                    ctaLabel = R.string.barcode_scanning_alert_dialog_rationale_cta_label,
                    ctaAction = { triggerEvent(ScanningEvents.LaunchCameraPermission) }
                )
            }

            !isPermissionGranted && !shouldShowRequestPermissionRationale -> {
                // Permanently denied, ask to enable permission from the app settings
                _permissionState.value = PermissionState.PermanentlyDenied(
                    title = R.string.barcode_scanning_alert_dialog_title,
                    message = R.string.barcode_scanning_alert_dialog_permanently_denied_message,
                    ctaLabel = R.string.barcode_scanning_alert_dialog_permanently_denied_cta_label,
                    ctaAction = {
                        // Open settings
                        triggerEvent(ScanningEvents.OpenAppSettings)
                    }
                )
            }

            else -> {
                _permissionState.value = PermissionState.Unknown
            }
        }
    }

    fun onResume() {
        if (event.value is ScanningEvents.OpenAppSettings) {
            triggerEvent(ScanningEvents.LaunchCameraPermission)
        }
    }

    sealed class ScanningEvents : Event() {
        object LaunchCameraPermission : ScanningEvents()
        object OpenAppSettings : ScanningEvents()
    }

    sealed class PermissionState {
        object Granted : PermissionState()

        data class ShouldShowRationale(
            @StringRes val title: Int,
            @StringRes val message: Int,
            @StringRes val ctaLabel: Int,
            val ctaAction: () -> Unit
        ) : PermissionState()

        data class PermanentlyDenied(
            @StringRes val title: Int,
            @StringRes val message: Int,
            @StringRes val ctaLabel: Int,
            val ctaAction: () -> Unit
        ) : PermissionState()

        object Unknown : PermissionState()
    }
}
