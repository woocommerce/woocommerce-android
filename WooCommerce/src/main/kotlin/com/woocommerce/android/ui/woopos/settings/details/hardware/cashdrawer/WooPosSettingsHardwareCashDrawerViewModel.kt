package com.woocommerce.android.ui.woopos.settings.details.hardware.cashdrawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.cashdrawer.WooPosCashDrawerController
import com.woocommerce.android.ui.woopos.cashdrawer.WooPosCashDrawerOpenResult
import com.woocommerce.android.ui.woopos.cashdrawer.WooPosCashDrawerReason
import com.woocommerce.android.ui.woopos.cashdrawer.WooPosReceiptPrinter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosSettingsHardwareCashDrawerViewModel @Inject constructor(
    private val drawer: WooPosCashDrawerController,
) : ViewModel() {
    val drawerName: StateFlow<String?> = drawer.drawerNameFlow
    val autoOpen: StateFlow<Boolean> = drawer.autoOpenFlow
    val isConnected: StateFlow<Boolean> = drawer.isConnected
    val selectedPrinter: StateFlow<WooPosReceiptPrinter?> = drawer.selectedPrinter

    private val _discoveredPrinters = MutableStateFlow<List<WooPosReceiptPrinter>>(emptyList())
    val discoveredPrinters: StateFlow<List<WooPosReceiptPrinter>> = _discoveredPrinters.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _connectionError = MutableStateFlow(false)
    val connectionError: StateFlow<Boolean> = _connectionError.asStateFlow()

    private val _isOpening = MutableStateFlow(false)
    val isOpening: StateFlow<Boolean> = _isOpening.asStateFlow()

    private val _lastResult = MutableStateFlow<WooPosCashDrawerOpenResult?>(null)
    val lastResult: StateFlow<WooPosCashDrawerOpenResult?> = _lastResult.asStateFlow()

    fun setDrawerName(value: String) = drawer.setDrawerName(value)
    fun setAutoOpen(value: Boolean) = drawer.setAutoOpen(value)

    fun discoverPrinters() {
        if (_isSearching.value) return
        viewModelScope.launch {
            _isSearching.value = true
            _connectionError.value = false
            try {
                drawer.discoverPrinters().fold(
                    onSuccess = { _discoveredPrinters.value = it },
                    onFailure = { _connectionError.value = true },
                )
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun connectPrinter(printer: WooPosReceiptPrinter) {
        viewModelScope.launch {
            _connectionError.value = drawer.connectPrinter(printer).isFailure
        }
    }

    fun disconnectPrinter() {
        viewModelScope.launch { drawer.disconnectPrinter() }
    }

    fun testOpen() {
        if (_isOpening.value || !isConnected.value) return
        viewModelScope.launch {
            _isOpening.value = true
            try {
                _lastResult.value = drawer.open(WooPosCashDrawerReason.TEST)
            } finally {
                _isOpening.value = false
            }
        }
    }
}
