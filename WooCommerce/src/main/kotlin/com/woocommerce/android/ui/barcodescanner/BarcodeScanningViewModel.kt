package com.woocommerce.android.ui.barcodescanner

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import javax.inject.Inject

class BarcodeScanningViewModel @Inject constructor(
    savedState: SavedStateHandle,
) : ScopedViewModel(savedState) {

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
