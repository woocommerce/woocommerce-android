package com.woocommerce.android.ui.woopos.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.common.data.WooPosPopularProductsProvider
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.Loaded
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class WooPosSplashViewModel @Inject constructor(
    private val productsDataSource: WooPosProductsDataSource,
    private val popularProductsProvider: WooPosPopularProductsProvider,
    private val analyticsTracker: WooPosAnalyticsTracker,
) : ViewModel() {
    private val _state = MutableStateFlow<WooPosSplashState>(WooPosSplashState.Loading)
    val state: StateFlow<WooPosSplashState> = _state

    init {
        val splashScreenStartTime = System.currentTimeMillis()
        viewModelScope.launch {
            joinAll(
                launch { productsDataSource.prepopulateProductsCache() },
                launch { popularProductsProvider.fetchAndCachePopularProducts() }
            )
            _state.value = WooPosSplashState.Loaded
            trackPosLoaded(splashScreenStartTime)
        }
    }

    private suspend fun trackPosLoaded(splashScreenStartTime: Long) {
        val event = Loaded.apply {
            val waitingTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(
                System.currentTimeMillis() - splashScreenStartTime
            ).toFloat()
            addProperties(mapOf("waiting_time" to waitingTimeSeconds.toString()))
        }
        analyticsTracker.track(event)
    }
}
