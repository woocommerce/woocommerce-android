package com.woocommerce.android.ui.barcodescanner

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
}
