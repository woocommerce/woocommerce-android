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
    private val storeRepository: WooPosStoreReceiptRepository
) : ViewModel() {
    private val _state = MutableStateFlow<WooPosSettingsStoreState>(WooPosSettingsStoreState.Loading)
    val state: StateFlow<WooPosSettingsStoreState> = _state.asStateFlow()

    init {
        loadStoreData()
    }

    private fun loadStoreData() {
        viewModelScope.launch {
            _state.value = WooPosSettingsStoreState.Loading

            when (val result = storeRepository.getStoreData()) {
                is WooPosStoreDataResult.Success -> {
                    _state.value = WooPosSettingsStoreState.Loaded(
                        storeInfo = result.storeInfo,
                        receiptInfo = result.receiptInfo
                    )
                }
                is WooPosStoreDataResult.NotAvailable,
                is WooPosStoreDataResult.Error -> {
                    val emptyStoreInfo = WooPosSettingsStoreState.StoreInfo("", "", "", "")
                    _state.value = WooPosSettingsStoreState.Loaded(
                        storeInfo = emptyStoreInfo,
                        receiptInfo = null
                    )
                }
            }
        }
    }
}
