package com.woocommerce.android.ui.woopos.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.ui.woopos.common.data.WooPosPopularProductsProvider
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource.WooPosPrepopulatingDataStatus
import com.woocommerce.android.ui.woopos.localcatalog.WooPosIsWooBelowCatalogFixVersion
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersDataSource
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersInMemoryCache
import com.woocommerce.android.ui.woopos.tab.WooPosCanBeLaunchedInTab
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.CatalogBlockedContinueWithBasicSyncTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.Loaded
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.LocalCatalogBlockedFellBackToRemote
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.LocalCatalogDownloadingScreenExitPosTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.LocalCatalogDownloadingScreenShown
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.SplashScreenErrorShown
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.SplashScreenRetryTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
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
    private val ordersDataSource: WooPosOrdersDataSource,
    private val preferencesRepository: WooPosPreferencesRepository,
    private val getWooCoreVersion: GetWooCorePluginCachedVersion,
    private val isWooBelowCatalogFixVersion: WooPosIsWooBelowCatalogFixVersion,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
) : ViewModel() {
    private val _state = MutableStateFlow<WooPosSplashState>(WooPosSplashState.Loading)
    val state: StateFlow<WooPosSplashState> = _state

    init {
        val splashScreenStartTime = System.currentTimeMillis()
        viewModelScope.launch {
            preferencesRepository.setLastUsedTimestamp()

            val launchability = posCanBeLaunchedInTab()

            if (launchability is WooPosLaunchability.NotLaunchable) {
                _state.value = WooPosSplashState.NotEligible(launchability.reason)
                return@launch
            }

            joinAll(
                launch {
                    productsDataSource.prepopulateCache()
                        .collect(syncStateCollector(splashScreenStartTime))
                },
                launch { popularProductsProvider.fetchAndCachePopularProducts() },
                launch { ordersCache.clear() }
            )
            prepopulateOrdersCache()
        }
    }

    fun onRetrySync() {
        viewModelScope.launch {
            analyticsTracker.track(SplashScreenRetryTapped)
            val retryStartTime = System.currentTimeMillis()
            productsDataSource.prepopulateCache().collect(syncStateCollector(retryStartTime))
        }
    }

    fun onContinueWithBasicSyncClicked() {
        viewModelScope.launch {
            analyticsTracker.track(CatalogBlockedContinueWithBasicSyncTapped)
            // The catalog already loaded via the remote fallback; "Got it" just dismisses the notice.
            _state.value = WooPosSplashState.Loaded
        }
    }

    fun onExitPosClicked() {
        viewModelScope.launch {
            val isSyncing = _state.value is WooPosSplashState.Syncing ||
                _state.value is WooPosSplashState.SyncPreparing ||
                _state.value is WooPosSplashState.SyncProgress
            if (isSyncing) {
                analyticsTracker.track(LocalCatalogDownloadingScreenExitPosTapped)
            }
        }
    }

    private fun syncStateCollector(
        startTime: Long
    ) = FlowCollector<WooPosPrepopulatingDataStatus> { state ->
        when (state) {
            WooPosPrepopulatingDataStatus.Syncing -> {
                _state.value = WooPosSplashState.Syncing
                analyticsTracker.track(LocalCatalogDownloadingScreenShown)
            }

            WooPosPrepopulatingDataStatus.SyncPreparing -> {
                _state.value = WooPosSplashState.SyncPreparing
            }

            is WooPosPrepopulatingDataStatus.SyncProgress -> {
                _state.value = WooPosSplashState.SyncProgress(
                    processed = state.processed,
                    total = state.total,
                )
            }

            WooPosPrepopulatingDataStatus.Completed -> {
                _state.value = WooPosSplashState.Loaded
                trackPosLoaded(startTime)
            }

            is WooPosPrepopulatingDataStatus.Failed -> {
                if (state.isServerPermissionsError) {
                    // A blocked catalog file always falls back to basic (remote) sync so POS keeps
                    // working. On Woo < 11 we just enter POS; on Woo >= 11 we first surface the host
                    // issue, which the user dismisses with "Got it".
                    fallBackToRemoteForBlockedCatalog(startTime, blockedError = state.error)
                } else {
                    analyticsTracker.track(SplashScreenErrorShown)
                    _state.value = WooPosSplashState.SyncFailed(state.error)
                }
            }
        }
    }

    private suspend fun fallBackToRemoteForBlockedCatalog(startTime: Long, blockedError: String) {
        analyticsTracker.track(LocalCatalogBlockedFellBackToRemote(getWooCoreVersion()))
        val mustAcknowledgeHostIssue = !isWooBelowCatalogFixVersion()
        productsDataSource.fallBackToRemoteDueToCatalogBlock().collect { status ->
            when (status) {
                WooPosPrepopulatingDataStatus.Completed -> {
                    trackPosLoaded(startTime)
                    if (mustAcknowledgeHostIssue) {
                        analyticsTracker.track(SplashScreenErrorShown)
                        _state.value = WooPosSplashState.SyncFailed(blockedError, isServerPermissionsError = true)
                    } else {
                        _state.value = WooPosSplashState.Loaded
                    }
                }

                is WooPosPrepopulatingDataStatus.Failed -> {
                    analyticsTracker.track(SplashScreenErrorShown)
                    _state.value = WooPosSplashState.SyncFailed(status.error)
                }

                else -> Unit
            }
        }
    }

    private suspend fun trackPosLoaded(startTime: Long) {
        val syncStrategy = productsDataSource.getCurrentSyncStrategy()

        val event = Loaded(syncStrategy).apply {
            val waitingTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(
                System.currentTimeMillis() - startTime
            ).toFloat()
            addProperties(mapOf("waiting_time" to waitingTimeSeconds.toString()))
        }
        analyticsTracker.track(event)
    }

    private fun prepopulateOrdersCache() {
        appCoroutineScope.launch {
            ordersDataSource.loadOrders().collect {}
        }
    }
}
