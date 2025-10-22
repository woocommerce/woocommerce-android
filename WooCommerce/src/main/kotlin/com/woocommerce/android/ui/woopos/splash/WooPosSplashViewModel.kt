package com.woocommerce.android.ui.woopos.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.common.data.WooPosPopularProductsProvider
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource.WooPosPrepopulatingDataStatus
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersInMemoryCache
import com.woocommerce.android.ui.woopos.tab.WooPosCanBeLaunchedInTab
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.Loaded
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.FlowCollector
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
    private val posCanBeLaunchedInTab: WooPosCanBeLaunchedInTab,
    private val ordersCache: WooPosOrdersInMemoryCache,
) : ViewModel() {
    private val _state = MutableStateFlow<WooPosSplashState>(WooPosSplashState.Loading)
    val state: StateFlow<WooPosSplashState> = _state

    init {
        val splashScreenStartTime = System.currentTimeMillis()

        viewModelScope.launch {
            val launchability = posCanBeLaunchedInTab()

            if (launchability is WooPosLaunchability.NotLaunchable) {
                _state.value = WooPosSplashState.NotEligible(launchability.reason)
                return@launch
            }

            joinAll(
                launch {
                    productsDataSource.prepopulateProductsCache()
                        .collect(syncStateCollector(splashScreenStartTime))
                },
                launch { popularProductsProvider.fetchAndCachePopularProducts() },
                launch { ordersCache.clear() }
            )
        }
    }

    fun onRetrySync() {
        viewModelScope.launch {
            val retryStartTime = System.currentTimeMillis()
            productsDataSource.prepopulateProductsCache().collect(syncStateCollector(retryStartTime))
        }
    }

    private fun syncStateCollector(
        startTime: Long
    ) = FlowCollector<WooPosPrepopulatingDataStatus> { state ->
        when (state) {
            WooPosPrepopulatingDataStatus.Syncing -> {
                _state.value = WooPosSplashState.Syncing
            }

            WooPosPrepopulatingDataStatus.Completed -> {
                _state.value = WooPosSplashState.Loaded
                trackPosLoaded(startTime)
            }

            is WooPosPrepopulatingDataStatus.Failed -> {
                _state.value = WooPosSplashState.SyncFailed(state.error)
            }
        }
    }

    private suspend fun trackPosLoaded(startTime: Long) {
        val event = Loaded.apply {
            val waitingTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(
                System.currentTimeMillis() - startTime
            ).toFloat()
            addProperties(mapOf("waiting_time" to waitingTimeSeconds.toString()))
        }
        analyticsTracker.track(event)
    }
}
