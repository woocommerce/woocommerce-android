package com.woocommerce.android.ui.mystore.domain

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.di.DefaultDispatcher
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.mystore.data.StatsRepository
import com.woocommerce.android.ui.mystore.data.StatsRepository.StatsException
import com.woocommerce.android.ui.mystore.domain.GetStats.LoadStatsResult.*
import com.woocommerce.android.util.FeatureFlag
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsErrorType
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import javax.inject.Inject

class GetStats @Inject constructor(
    private val selectedSite: SelectedSite,
    private val statsRepository: StatsRepository,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) {
    operator fun invoke(
        forceRefresh: Boolean,
        granularity: StatsGranularity
    ): Flow<LoadStatsResult> =
        flow {
            coroutineScope {
                if (forceRefresh) {
                    emit(IsLoadingStats(true))
                }

                val hasNoOrdersTask = async {
                    statsRepository.checkIfStoreHasNoOrders()
                }
                val revenueStatsTask = async {
                    statsRepository.fetchRevenueStats(granularity, forceRefresh)
                }
                val visitorStatsTask = async {
                    if (selectedSite.getIfExists()?.isJetpackCPConnected != true) {
                        statsRepository.fetchVisitorStats(granularity, forceRefresh)
                    } else {
                        null
                    }
                }

                val storeHasNoOrders = hasNoOrdersTask.await().getOrNull()
                if (storeHasNoOrders == true) {
                    emit(HasOrders(false))
                } else {
                    emit(HasOrders(true))
                    val revenueStatsResult = revenueStatsTask.await()
                    val visitorStatsResult = visitorStatsTask.await()

                    emit(IsLoadingStats(false))
                    handle(revenueStatsResult, granularity)
                    visitorStatsResult?.let {
                        it.fold(
                            onSuccess = { visitorStats ->
                                emit(VisitorsStatsSuccess(visitorStats))
                            },
                            onFailure = {
                                emit(VisitorsStatsError)
                            }
                        )
                    } ?: run {
                        // Which means the site is using Jetpack Connection package
                        if (FeatureFlag.JETPACK_CP.isEnabled()) {
                            emit(IsJetPackCPEnabled)
                        }
                    }
                }
            }
        }.flowOn(dispatcher)

    private suspend fun FlowCollector<LoadStatsResult>.handle(
        revenueStatsResult: Result<WCRevenueStatsModel?>,
        granularity: StatsGranularity
    ) {
        revenueStatsResult.fold(
            onSuccess = { stats ->
                AnalyticsTracker.track(
                    AnalyticsTracker.Stat.DASHBOARD_MAIN_STATS_LOADED,
                    mapOf(AnalyticsTracker.KEY_RANGE to granularity.name.lowercase())
                )
                AppPrefs.setV4StatsSupported(true)
                emit(RevenueStatsSuccess(stats))
            },
            onFailure = {
                if (isPluginNotActiveError(it)) {
                    AppPrefs.setV4StatsSupported(false)
                    emit(PluginNotActive)
                } else {
                    emit(GenericError)
                }
            }
        )
    }

    private fun isPluginNotActiveError(error: Throwable): Boolean =
        (error as? StatsException)?.error?.type == OrderStatsErrorType.PLUGIN_NOT_ACTIVE

    sealed class LoadStatsResult {

        data class RevenueStatsSuccess(
            val stats: WCRevenueStatsModel?
        ) : LoadStatsResult()

        data class VisitorsStatsSuccess(
            val stats: Map<String, Int>
        ) : LoadStatsResult()

        data class HasOrders(
            val hasOrder: Boolean
        ) : LoadStatsResult()

        data class IsLoadingStats(
            val isLoading: Boolean
        ) : LoadStatsResult()

        object GenericError : LoadStatsResult()
        object VisitorsStatsError : LoadStatsResult()
        object PluginNotActive : LoadStatsResult()
        object IsJetPackCPEnabled : LoadStatsResult()
    }
}
