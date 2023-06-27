package com.woocommerce.android.ui.orders.creation.barcodescanner

import androidx.camera.core.ImageProxy
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.orders.creation.CodeScanner
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BarcodeScanningViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val codeScanner: CodeScanner,
) : ScopedViewModel(savedState) {
    fun startScan(imageProxy: ImageProxy) {
        viewModelScope.launch {
            startCodeScan(imageProxy).collect { codeScannerStatus ->
                when (codeScannerStatus) {
                    is CodeScannerStatus.Failure -> {
                        triggerEvent(MultiLiveEvent.Event.ExitWithResult(codeScannerStatus))
                    }
                    is CodeScannerStatus.Success -> {
                        triggerEvent(MultiLiveEvent.Event.ExitWithResult(codeScannerStatus))
                    }
                }
            }
        }
    }
    private fun startCodeScan(imageProxy: ImageProxy): Flow<CodeScannerStatus> {
        return codeScanner.startScan(imageProxy)
    }
}
