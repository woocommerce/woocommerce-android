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
    private val storeRepository: WooPosSettingsStoreRepository,
    private val receiptRepository: WooPosReceiptRepository
) : ViewModel() {
    private val _state = MutableStateFlow(WooPosSettingsStoreState())
    val state: StateFlow<WooPosSettingsStoreState> = _state.asStateFlow()

    init {
        loadStoreData()
        loadReceiptData()
    }

    private fun loadStoreData() {
        viewModelScope.launch {
            val storeInfo = storeRepository.getStoreInfo()
            _state.value = _state.value.copy(
                storeInfoState = WooPosSettingsStoreState.StoreState.Loaded(storeInfo)
            )
        }
    }

    private fun loadReceiptData() {
        viewModelScope.launch {
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

            _state.value = _state.value.copy(receiptState = newReceiptState)
        }
    }
}
