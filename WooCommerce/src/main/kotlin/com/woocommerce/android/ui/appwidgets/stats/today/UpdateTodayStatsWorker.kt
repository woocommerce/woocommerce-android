package com.woocommerce.android.ui.appwidgets.stats.today

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import com.woocommerce.android.R
import com.woocommerce.android.background.BatteryAwareCoroutineWorker
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.appwidgets.stats.GetWidgetStats
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
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val getWidgetStats: GetWidgetStats,
    private val selectedSite: SelectedSite,
    private val todayStatsWidgetUIHelper: TodayStatsWidgetUIHelper,
    private val dateUtils: DateUtils,
    private val localeProvider: LocaleProvider
) : BatteryAwareCoroutineWorker(appContext, workerParams) {

    companion object {
        const val DATA_WIDGET_ID = "widget_id"
    }

    override val notificationId = 3045
    override val titleResId = R.string.notification_channel_background_works_title
    override val messageResId = R.string.stats_widget_notification_message

    override suspend fun executeWork(): Result {
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
