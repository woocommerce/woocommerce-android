package com.woocommerce.android.ui.woopos.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.Loaded
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosSplashViewModel @Inject constructor(
    private val productsDataSource: WooPosProductsDataSource,
    private val analyticsTracker: WooPosAnalyticsTracker,
) : ViewModel() {
    private val _state = MutableStateFlow<WooPosSplashState>(WooPosSplashState.Loading)
    val state: StateFlow<WooPosSplashState> = _state

    private val splashScreenStartTime = System.currentTimeMillis()

    init {
        viewModelScope.launch {
            productsDataSource.loadSimpleProducts(forceRefreshProducts = true)
                .collect { result ->
                    when (result) {
                        is WooPosProductsDataSource.ProductsResult.Cached -> {}
                        is WooPosProductsDataSource.ProductsResult.Remote -> {
                            _state.value = WooPosSplashState.Loaded
                            trackPosLoaded()
                        }
                    }
                }
        }
    }

    private suspend fun trackPosLoaded() {
        val event = Loaded.apply {
            @Suppress("MagicNumber")
            val waitingTimeSeconds = (System.currentTimeMillis() - splashScreenStartTime) / 1000f
            addProperties(mapOf("waiting_time" to waitingTimeSeconds.toString()))
        }
        analyticsTracker.track(event)
    }
}
