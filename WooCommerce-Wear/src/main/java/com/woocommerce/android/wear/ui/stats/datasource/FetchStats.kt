package com.woocommerce.android.wear.ui.stats.datasource

import com.woocommerce.android.wear.analytics.AnalyticsTracker
import com.woocommerce.android.wear.extensions.combineWithTimeout
import com.woocommerce.android.wear.phone.PhoneConnectionRepository
import com.woocommerce.android.wear.system.NetworkStatus
import com.woocommerce.android.wear.ui.stats.datasource.FetchStats.StoreStatsRequest.Error
import com.woocommerce.android.wear.ui.stats.datasource.FetchStats.StoreStatsRequest.Finished
import com.woocommerce.android.wear.ui.stats.datasource.FetchStats.StoreStatsRequest.Waiting
import com.woocommerce.android.wear.ui.stats.datasource.StoreStatsData.RevenueData
import com.woocommerce.commons.MessagePath
import com.woocommerce.commons.WearAnalyticsEvent.WATCH_DATA_REQUESTED_FROM_PHONE
import com.woocommerce.commons.WearAnalyticsEvent.WATCH_DATA_REQUESTED_FROM_STORE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class FetchStats @Inject constructor(
    private val statsRepository: StatsRepository,
    private val phoneRepository: PhoneConnectionRepository,
    private val wooCommerceStore: WooCommerceStore,
    private val networkStatus: NetworkStatus,
    private val analyticsTracker: AnalyticsTracker
) {
    private val revenueStats = MutableStateFlow<RevenueData?>(null)
    private val visitorStats = MutableStateFlow<Int?>(null)

    suspend operator fun invoke(
        selectedSite: SiteModel
    ) = when {
        networkStatus.isConnected() -> fetchStatsFromStore(selectedSite)
        phoneRepository.isPhoneConnectionAvailable() -> fetchStatsFromPhone(selectedSite)
        else -> flowOf(Error)
    }.distinctUntilChanged()

    private suspend fun fetchStatsFromPhone(
        selectedSite: SiteModel
    ): Flow<StoreStatsRequest> {
        analyticsTracker.track(WATCH_DATA_REQUESTED_FROM_PHONE)
        phoneRepository.sendMessage(MessagePath.REQUEST_STATS)
        return statsRepository.observeStatsDataChanges(selectedSite)
            .combineWithTimeout { statsData, isTimeout ->
                when {
                    statsData?.isComplete == true -> Finished(statsData)
                    isTimeout.not() -> Waiting
                    else -> Error
                }
            }.filterNotNull()
    }

    private suspend fun fetchStatsFromStore(
        selectedSite: SiteModel
    ): Flow<StoreStatsRequest> {
        analyticsTracker.track(WATCH_DATA_REQUESTED_FROM_STORE)
        fetchRevenueStats(selectedSite)
        fetchVisitorsStats(selectedSite)

        return combine(revenueStats, visitorStats) { revenue, visitors ->
            StoreStatsData(revenue, visitors)
        }.combineWithTimeout { data, isTimeout ->
            when {
                data.isComplete -> Finished(data)
                isTimeout.not() -> Waiting
                else -> Error
            }
        }.filterNotNull()
    }

    private suspend fun fetchRevenueStats(selectedSite: SiteModel) {
        statsRepository.fetchRevenueStats(selectedSite)
            .fold(
                onSuccess = { revenue ->
                    val totals = revenue?.parseTotal()

                    val formattedRevenue = wooCommerceStore.formatCurrencyForDisplay(
                        amount = totals?.totalSales ?: 0.0,
                        site = selectedSite,
                        currencyCode = null,
                        applyDecimalFormatting = true
                    )

                    val revenueData = RevenueData(
                        totalRevenue = formattedRevenue,
                        orderCount = totals?.ordersCount ?: 0
                    )

                    revenueStats.value = revenueData
                },
                onFailure = {
                    revenueStats.value = null
                }
            )
    }

    private suspend fun fetchVisitorsStats(selectedSite: SiteModel) {
        statsRepository.fetchVisitorStats(selectedSite)
            .fold(
                onSuccess = { visitors ->
                    visitorStats.value = visitors ?: 0
                },
                onFailure = {
                    visitorStats.value = null
                }
            )
    }

    sealed class StoreStatsRequest {
        data object Error : StoreStatsRequest()
        data object Waiting : StoreStatsRequest()
        data class Finished(val data: StoreStatsData) : StoreStatsRequest()
    }
}
