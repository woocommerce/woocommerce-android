package com.woocommerce.android.ui.troubleshooting.useCases

import com.woocommerce.android.WooException
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.dashboard.stats.GetStats
import com.woocommerce.android.ui.dashboard.stats.GetStats.LoadStatsResult
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.InProgress
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.Success
import com.woocommerce.android.ui.troubleshooting.FailureType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.time.measureTimedValue

class StoreAnalyticsCheckUseCase @Inject constructor(
    private val getStats: GetStats,
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore
) {
    operator fun invoke(): Flow<ConnectivityCheckStatus> = flow {
        emit(InProgress)

        val site = selectedSite.get()
        val analyticsSetting = wooCommerceStore.fetchAnalyticsEnabled(site)
        if (analyticsSetting.isError) {
            emit(
                Failure(
                    error = FailureType.GENERIC,
                    technicalDetails = formatErrorDetails(
                        operation = OPERATION_NAME,
                        errorType = analyticsSetting.error.type.name,
                        message = analyticsSetting.error.message
                    )
                )
            )
            return@flow
        }

        if (analyticsSetting.model == false) {
            emit(analyticsInactiveFailure())
            return@flow
        }

        val range = StatsTimeRangeSelection.build(
            selectionType = StatsTimeRangeSelection.SelectionType.TODAY,
            referenceDate = Date(),
            calendar = Calendar.getInstance(),
            locale = Locale.getDefault()
        )
        val (result, duration) = measureTimedValue {
            getStats(refresh = true, selectedRange = range)
                .filter { it.isRevenueCheckResult }
                .first()
        }

        emit(result.toConnectivityCheckStatus(duration.inWholeMilliseconds))
    }

    suspend fun enableAnalytics(): Result<Unit> = runCatching {
        val result = wooCommerceStore.enableAnalytics(selectedSite.get())
        when {
            result.isError -> throw WooException(result.error)
            result.model == true -> Unit
            else -> error("Failed to enable analytics setting")
        }
    }

    private val LoadStatsResult.isRevenueCheckResult: Boolean
        get() = when (this) {
            is LoadStatsResult.RevenueStatsSuccess -> isOutdated.not()
            is LoadStatsResult.RevenueStatsError,
            LoadStatsResult.PluginNotActive -> true
            LoadStatsResult.RevenueStatsLoading,
            LoadStatsResult.VisitorsStatsError,
            is LoadStatsResult.VisitorsStatsSuccess,
            LoadStatsResult.VisitorStatUnavailable,
            LoadStatsResult.VisitorStatsLoading -> false
        }

    private fun LoadStatsResult.toConnectivityCheckStatus(durationMs: Long): ConnectivityCheckStatus =
        when (this) {
            is LoadStatsResult.RevenueStatsSuccess -> Success(durationMs = durationMs)
            LoadStatsResult.PluginNotActive -> analyticsInactiveFailure(durationMs)
            is LoadStatsResult.RevenueStatsError -> Failure(
                error = FailureType.GENERIC,
                technicalDetails = formatErrorDetails(
                    operation = OPERATION_NAME,
                    errorType = FailureType.GENERIC.name,
                    message = message
                ),
                durationMs = durationMs
            )

            LoadStatsResult.RevenueStatsLoading,
            LoadStatsResult.VisitorsStatsError,
            is LoadStatsResult.VisitorsStatsSuccess,
            LoadStatsResult.VisitorStatUnavailable,
            LoadStatsResult.VisitorStatsLoading -> Failure(
                error = FailureType.GENERIC,
                technicalDetails = "Analytics check did not complete",
                durationMs = durationMs
            )
        }

    private fun analyticsInactiveFailure(durationMs: Long = 0L): Failure =
        Failure(
            error = FailureType.GENERIC,
            technicalDetails = formatErrorDetails(
                operation = OPERATION_NAME,
                errorType = PLUGIN_NOT_ACTIVE_ERROR_TYPE,
                message = "WooCommerce Analytics is inactive"
            ),
            durationMs = durationMs
        )

    companion object {
        const val OPERATION_NAME = "Checking analytics setting"
        const val PLUGIN_NOT_ACTIVE_ERROR_TYPE = "PLUGIN_NOT_ACTIVE"
    }
}
