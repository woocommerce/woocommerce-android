package com.woocommerce.android.ui.woopos.settings.details.hardware.barcodescanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.common.data.WOO_POS_BARCODE_DOC_URL
import com.woocommerce.android.ui.woopos.common.util.ScannerInfo
import com.woocommerce.android.ui.woopos.common.util.WooPosScannerDetectionUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosSettingsHardwareBarcodeScannerViewModel @Inject constructor(
    private val scannerDetectionUtil: WooPosScannerDetectionUtil
) : ViewModel() {

    private val _state = MutableStateFlow(WooPosSettingsHardwareBarcodeScannerState())
    val state: StateFlow<WooPosSettingsHardwareBarcodeScannerState> = _state.asStateFlow()

    private val _showScanningSetupDialog = MutableSharedFlow<Unit>()
    val showScanningSetupDialog = _showScanningSetupDialog.asSharedFlow()

    private val _openUrl = MutableSharedFlow<String>()
    val openUrl = _openUrl.asSharedFlow()

    init {
        startScannerDetection()
    }

    private fun startScannerDetection() {
        viewModelScope.launch {
            while (isActive) {
                val scannerInfo = scannerDetectionUtil.detectConnectedScanner()
                _state.update { currentState ->
                    currentState.copy(scannerInfo = scannerInfo)
                }
                delay(SCANNER_DETECTION_INTERVAL_MS)
            }
        }
    }

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

    companion object {
        private const val SCANNER_DETECTION_INTERVAL_MS = 2000L
    }
}

data class WooPosSettingsHardwareBarcodeScannerState(
    val scannerInfo: ScannerInfo = ScannerInfo.NoScannerDetected
)