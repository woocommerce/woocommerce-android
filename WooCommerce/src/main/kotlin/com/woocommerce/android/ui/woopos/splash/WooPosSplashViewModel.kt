package com.woocommerce.android.ui.woopos.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.home.products.WooPosProductsDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosHomeViewModel @Inject constructor(
    private val productsDataSource: WooPosProductsDataSource,
) : ViewModel() {
    private val _state = MutableStateFlow<WooPosSplashState>(WooPosSplashState.Loading)
    val state: StateFlow<WooPosSplashState> = _state

    init {
        viewModelScope.launch {
            val result = productsDataSource.loadSimpleProducts(forceRefreshProducts = true)
            when {
                result.isSuccess -> _state.value = WooPosSplashState.Loaded
                result.isFailure -> _state.value = WooPosSplashState.Error
            }
        }
    }
}
