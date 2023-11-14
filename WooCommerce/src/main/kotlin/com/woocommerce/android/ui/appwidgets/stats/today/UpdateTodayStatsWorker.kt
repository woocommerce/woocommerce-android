package com.woocommerce.android.ui.appwidgets.stats.today

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.appwidgets.stats.GetWidgetStats
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCStatsStore

@HiltWorker
class UpdateTodayStatsWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val getWidgetStats: GetWidgetStats,
    private val selectedSite: SelectedSite,
    private val todayStatsWidgetUIHelper: TodayStatsWidgetUIHelper
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val DATA_WIDGET_ID = "widget_id"
    }

    override suspend fun doWork(): Result {
        val site = selectedSite.getIfExists()
        val widgetId = inputData.getInt(DATA_WIDGET_ID, -1)
        val remoteViews = RemoteViews(appContext.packageName, R.layout.stats_widget_daily)
        val widgetManager = AppWidgetManager.getInstance(appContext)

        todayStatsWidgetUIHelper.displayTitle(site, remoteViews)

        // Show skeleton
        todayStatsWidgetUIHelper.displaySkeleton(remoteViews)
        widgetManager.updateAppWidget(widgetId, remoteViews)

        // Fetch data
        val todayStatsResult = getWidgetStats(WCStatsStore.StatsGranularity.DAYS, site)

        // Display results
        val result = getTaskResult(widgetId, remoteViews, site, todayStatsResult)
        widgetManager.partiallyUpdateAppWidget(widgetId, remoteViews)

        return result
    }

    private fun getTaskResult(
        widgetId: Int,
        remoteViews: RemoteViews,
        site: SiteModel?,
        todayStatsResult: GetWidgetStats.WidgetStatsResult
    ): Result {
        return when (todayStatsResult) {
            GetWidgetStats.WidgetStatsResult.WidgetStatsNetworkFailure -> {
                todayStatsWidgetUIHelper.displayError(
                    remoteViews = remoteViews,
                    errorMessageRes = R.string.stats_widget_offline_error,
                    withRetryButton = true,
                    widgetId = widgetId
                )
                Result.retry()
            }
            GetWidgetStats.WidgetStatsResult.WidgetStatsAPINotSupportedFailure -> {
                todayStatsWidgetUIHelper.displayError(
                    remoteViews = remoteViews,
                    errorMessageRes = R.string.stats_widget_availability_message,
                    withRetryButton = true,
                    widgetId = widgetId
                )
                Result.failure()
            }
            GetWidgetStats.WidgetStatsResult.WidgetStatsAuthFailure -> {
                todayStatsWidgetUIHelper.displayError(
                    remoteViews = remoteViews,
                    errorMessageRes = R.string.stats_widget_log_in_message,
                    withRetryButton = false,
                    widgetId = widgetId
                )
                Result.failure()
            }
            is GetWidgetStats.WidgetStatsResult.WidgetStatsFailure -> {
                todayStatsWidgetUIHelper.displayError(
                    remoteViews = remoteViews,
                    errorMessageRes = R.string.stats_widget_error_no_data,
                    withRetryButton = true,
                    widgetId = widgetId
                )
                Result.retry()
            }
            is GetWidgetStats.WidgetStatsResult.WidgetStats -> {
                if (site != null) {
                    todayStatsWidgetUIHelper.displayInformation(
                        stats = todayStatsResult,
                        remoteViews = remoteViews
                    )
                    Result.success()
                } else {
                    todayStatsWidgetUIHelper.displayError(
                        remoteViews = remoteViews,
                        errorMessageRes = R.string.stats_widget_error_no_data,
                        withRetryButton = true,
                        widgetId = widgetId
                    )
                    Result.retry()
                }
            }
        }
    }
}
