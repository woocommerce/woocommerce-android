package com.woocommerce.android.ui.mystore.domain

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.mystore.data.StatsRepository
import com.woocommerce.android.ui.mystore.data.StatsRepository.StatsException
import com.woocommerce.android.ui.mystore.domain.GetStats.LoadStatsResult.*
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.FeatureFlag
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsErrorType
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import javax.inject.Inject

class GetStats @Inject constructor(
    private val selectedSite: SelectedSite,
    private val statsRepository: StatsRepository,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val coroutineDispatchers: CoroutineDispatchers
) {

    @ExperimentalCoroutinesApi
    suspend operator fun invoke(refresh: Boolean, granularity: StatsGranularity): Flow<LoadStatsResult> =
        merge(
            hasOrders(),
            revenueStats(refresh, granularity),
            visitorStats(refresh, granularity)
        ).flowOn(coroutineDispatchers.computation)

    private suspend fun hasOrders(): Flow<HasOrders> =
        statsRepository.checkIfStoreHasNoOrdersFlow()
            .transform {
                if (it.getOrNull() == true) {
                    emit(HasOrders(false))
                } else {
                    emit(HasOrders(true))
                }
            }

    private fun revenueStats(forceRefresh: Boolean, granularity: StatsGranularity): Flow<LoadStatsResult> = flow {
        val revenueStatsResult = statsRepository.fetchRevenueStats(granularity, forceRefresh)
        revenueStatsResult.fold(
            onSuccess = { stats ->
                appPrefsWrapper.setV4StatsSupported(true)
                emit(RevenueStatsSuccess(stats))
            },
            onFailure = {
                if (isPluginNotActiveError(it)) {
                    appPrefsWrapper.setV4StatsSupported(false)
                    emit(PluginNotActive)
                } else {
                    emit(GenericError)
                }
            }
        )
    }

    private fun visitorStats(forceRefresh: Boolean, granularity: StatsGranularity): Flow<LoadStatsResult> = flow {
        val visitorStatsResult = if (selectedSite.getIfExists()?.isJetpackCPConnected != true) {
            statsRepository.fetchVisitorStats(granularity, forceRefresh)
        } else {
            null
        }
        visitorStatsResult?.let {
            it.fold(
                onSuccess = { stats -> emit(VisitorsStatsSuccess(stats)) },
                onFailure = { emit(VisitorsStatsError) }
            )
        } ?: run {
            // Which means the site is using Jetpack Connection package
            if (FeatureFlag.JETPACK_CP.isEnabled()) {
                emit(IsJetPackCPEnabled)
            }
        }
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

        object GenericError : LoadStatsResult()
        object VisitorsStatsError : LoadStatsResult()
        object PluginNotActive : LoadStatsResult()
        object IsJetPackCPEnabled : LoadStatsResult()
    }
}
