package com.woocommerce.android.ui.appwidgets.stats.today

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.appwidgets.stats.GetWidgetStats
import com.woocommerce.android.ui.appwidgets.stats.GetWidgetStats.WidgetStatsResult
import com.woocommerce.android.ui.appwidgets.stats.GetWidgetStats.WidgetStatsResult.WidgetStats
import com.woocommerce.android.ui.appwidgets.stats.GetWidgetStats.WidgetStatsResult.WidgetStatsAPINotSupportedFailure
import com.woocommerce.android.ui.appwidgets.stats.GetWidgetStats.WidgetStatsResult.WidgetStatsAuthFailure
import com.woocommerce.android.ui.appwidgets.stats.GetWidgetStats.WidgetStatsResult.WidgetStatsBatterySaverActive
import com.woocommerce.android.ui.appwidgets.stats.GetWidgetStats.WidgetStatsResult.WidgetStatsFailure
import com.woocommerce.android.ui.appwidgets.stats.GetWidgetStats.WidgetStatsResult.WidgetStatsNetworkFailure
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.locale.LocaleProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.wordpress.android.fluxc.model.SiteModel
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Suppress("LongParameterList")
@HiltWorker
class UpdateTodayStatsWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val getWidgetStats: GetWidgetStats,
    private val selectedSite: SelectedSite,
    private val todayStatsWidgetUIHelper: TodayStatsWidgetUIHelper,
    private val dateUtils: DateUtils,
    private val localeProvider: LocaleProvider
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
        val todayRange = StatsTimeRangeSelection.build(
            selectionType = SelectionType.TODAY,
            referenceDate = dateUtils.getCurrentDateInSiteTimeZone() ?: Date(),
            calendar = Calendar.getInstance(),
            locale = localeProvider.provideLocale() ?: Locale.getDefault()
        )
        val todayStatsResult = getWidgetStats(todayRange, site)

        // Display results
        val result = getTaskResult(widgetId, remoteViews, site, todayStatsResult)
        widgetManager.partiallyUpdateAppWidget(widgetId, remoteViews)

        return result
    }

    private fun getTaskResult(
        widgetId: Int,
        remoteViews: RemoteViews,
        site: SiteModel?,
        todayStatsResult: WidgetStatsResult
    ): Result {
        return when (todayStatsResult) {
            WidgetStatsBatterySaverActive -> {
                todayStatsWidgetUIHelper.displayError(
                    remoteViews = remoteViews,
                    errorMessageRes = R.string.stats_widget_battery_saver_error,
                    withRetryButton = false,
                    widgetId = widgetId
                )
                Result.failure()
            }
            WidgetStatsNetworkFailure -> {
                todayStatsWidgetUIHelper.displayError(
                    remoteViews = remoteViews,
                    errorMessageRes = R.string.stats_widget_offline_error,
                    withRetryButton = true,
                    widgetId = widgetId
                )
                Result.retry()
            }
            WidgetStatsAPINotSupportedFailure -> {
                todayStatsWidgetUIHelper.displayError(
                    remoteViews = remoteViews,
                    errorMessageRes = R.string.stats_widget_availability_message,
                    withRetryButton = true,
                    widgetId = widgetId
                )
                Result.failure()
            }
            WidgetStatsAuthFailure -> {
                todayStatsWidgetUIHelper.displayError(
                    remoteViews = remoteViews,
                    errorMessageRes = R.string.stats_widget_log_in_message,
                    withRetryButton = false,
                    widgetId = widgetId
                )
                Result.failure()
            }
            is WidgetStatsFailure -> {
                todayStatsWidgetUIHelper.displayError(
                    remoteViews = remoteViews,
                    errorMessageRes = R.string.stats_widget_error_no_data,
                    withRetryButton = true,
                    widgetId = widgetId
                )
                Result.retry()
            }
            is WidgetStats -> {
                handleStatsAvailable(todayStatsResult, site, remoteViews, widgetId)
            }
        }
    }

    private fun handleStatsAvailable(
        todayStatsResult: WidgetStats,
        site: SiteModel?,
        remoteViews: RemoteViews,
        widgetId: Int
    ): Result {
        if (site != null) {
            todayStatsWidgetUIHelper.displayInformation(
                stats = todayStatsResult,
                remoteViews = remoteViews
            )
            return Result.success()
        } else {
            todayStatsWidgetUIHelper.displayError(
                remoteViews = remoteViews,
                errorMessageRes = R.string.stats_widget_error_no_data,
                withRetryButton = true,
                widgetId = widgetId
            )
            return Result.retry()
        }
    }
}
