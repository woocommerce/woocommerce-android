package com.woocommerce.android.ui.woopos.settings.details.hardware.barcodescanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.common.data.WOO_POS_BARCODE_DOC_URL
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosSettingsHardwareBarcodeScannerViewModel @Inject constructor() : ViewModel() {

    private val _showScanningSetupDialog = MutableSharedFlow<Unit>()
    val showScanningSetupDialog = _showScanningSetupDialog.asSharedFlow()

    private val _openUrl = MutableSharedFlow<String>()
    val openUrl = _openUrl.asSharedFlow()

    fun onSetupScannerClicked() {
        viewModelScope.launch {
            _showScanningSetupDialog.emit(Unit)
        }
    }

    fun onDocumentationClicked() {
        viewModelScope.launch {
            _openUrl.emit(WOO_POS_BARCODE_DOC_URL)
        }
    }
}
