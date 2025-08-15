package com.woocommerce.android.ui.woopos.settings.details.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosSettingsStoreViewModel @Inject constructor(
    private val storeRepository: WooPosStoreRepository,
    private val receiptRepository: WooPosReceiptRepository
) : ViewModel() {
    private val _state = MutableStateFlow<WooPosSettingsStoreState>(WooPosSettingsStoreState.Loading)
    val state: StateFlow<WooPosSettingsStoreState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.value = WooPosSettingsStoreState.Loading

            val storeInfo = storeRepository.getStoreInfo()
            _state.value = WooPosSettingsStoreState.Loaded(
                storeInfo = storeInfo,
                receiptState = WooPosSettingsStoreState.ReceiptState.Loading
            )

            loadReceiptData()
        }
    }

    private fun loadReceiptData() {
        viewModelScope.launch {
            val currentState = _state.value
            if (currentState is WooPosSettingsStoreState.Loaded) {
                val receiptResult = receiptRepository.getReceiptInfo()
                val newReceiptState = when (receiptResult) {
                    is WooPosReceiptDataResult.Success -> {
                        WooPosSettingsStoreState.ReceiptState.Success(receiptResult.receiptInfo)
                    }
                    is WooPosReceiptDataResult.NotAvailable -> {
                        WooPosSettingsStoreState.ReceiptState.NotSupported
                    }
                    is WooPosReceiptDataResult.Error -> {
                        WooPosSettingsStoreState.ReceiptState.Error
                    }
                }

                _state.value = currentState.copy(receiptState = newReceiptState)
            }
        }
    }
}