package com.woocommerce.android.ui.appwidgets.stats.daily

import android.appwidget.AppWidgetManager
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.woocommerce.android.R
import com.woocommerce.android.extensions.getTitle
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.appwidgets.WidgetUtils
import com.woocommerce.android.ui.appwidgets.stats.today.GetWidgetStats
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WooCommerceStore

@HiltWorker
class UpdateTodayStatsWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val getWidgetStats: GetWidgetStats,
    private val selectedSite: SelectedSite,
    private val resourceProvider: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter,
    private val wooCommerceStore: WooCommerceStore,
    private val dateUtils: DateUtils
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WIDGET_ID = "widget_id"
    }

    override suspend fun doWork(): Result {
        val site = selectedSite.getIfExists()
        val widgetId = inputData.getInt(WIDGET_ID, -1)
        val remoteViews = RemoteViews(appContext.packageName, R.layout.stats_widget_daily)
        val widgetManager = AppWidgetManager.getInstance(appContext)

        displayTitle(site, remoteViews)

        // Show skeleton
        displaySkeleton(remoteViews)
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
                displayError(
                    remoteViews = remoteViews,
                    errorMessage = resourceProvider.getString(R.string.stats_widget_offline_error),
                    withRetryButton = true,
                    widgetId = widgetId
                )
                Result.retry()
            }
            GetWidgetStats.WidgetStatsResult.WidgetStatsAPINotSupportedFailure -> {
                displayError(
                    remoteViews = remoteViews,
                    errorMessage = resourceProvider.getString(R.string.stats_widget_availability_message),
                    withRetryButton = true,
                    widgetId = widgetId
                )
                Result.failure()
            }
            GetWidgetStats.WidgetStatsResult.WidgetStatsAuthFailure -> {
                displayError(
                    remoteViews = remoteViews,
                    errorMessage = resourceProvider.getString(R.string.stats_widget_log_in_message),
                    withRetryButton = false,
                    widgetId = widgetId
                )
                Result.failure()
            }
            is GetWidgetStats.WidgetStatsResult.WidgetStatsFailure -> {
                displayError(
                    remoteViews = remoteViews,
                    errorMessage = resourceProvider.getString(R.string.stats_widget_error_no_data),
                    withRetryButton = true,
                    widgetId = widgetId
                )
                Result.retry()
            }
            is GetWidgetStats.WidgetStatsResult.WidgetStats -> {
                if (site != null) {
                    displayInformation(
                        stats = todayStatsResult,
                        remoteViews = remoteViews,
                        site = site
                    )
                    Result.success()
                } else {
                    displayError(
                        remoteViews = remoteViews,
                        errorMessage = resourceProvider.getString(R.string.stats_widget_error_no_data),
                        withRetryButton = true,
                        widgetId = widgetId
                    )
                    Result.retry()
                }
            }
        }
    }

    private fun displayError(
        widgetId: Int,
        remoteViews: RemoteViews,
        errorMessage: String,
        withRetryButton: Boolean
    ) {
        val pendingIntent = WidgetUtils.getWidgetTapPendingIntent(appContext, DailyStatsWidgetProvider.WIDGET_NAME)
        remoteViews.setOnClickPendingIntent(
            R.id.widget_title_container,
            pendingIntent
        )

        remoteViews.setViewVisibility(R.id.widget_info, View.GONE)
        remoteViews.setViewVisibility(R.id.widget_error, View.VISIBLE)
        remoteViews.setTextViewText(R.id.widget_error_message, errorMessage)

        remoteViews.setViewVisibility(R.id.widget_retry_button, if (withRetryButton) View.VISIBLE else View.GONE)
        if (withRetryButton) {
            remoteViews.setOnClickPendingIntent(
                R.id.widget_error,
                WidgetUtils.getRetryIntent(appContext, DailyStatsWidgetProvider::class.java, widgetId)
            )
        } else {
            remoteViews.setOnClickPendingIntent(R.id.widget_error, pendingIntent)
        }
    }

    private fun displayInformation(
        stats: GetWidgetStats.WidgetStatsResult.WidgetStats,
        remoteViews: RemoteViews,
        site: SiteModel
    ) {
        val pendingIntent = WidgetUtils.getWidgetTapPendingIntent(appContext, DailyStatsWidgetProvider.WIDGET_NAME)
        remoteViews.setOnClickPendingIntent(R.id.widget_title_container, pendingIntent)
        remoteViews.setOnClickPendingIntent(R.id.widget_info, pendingIntent)

        val currencyCode = wooCommerceStore.getSiteSettings(site)?.currencyCode.orEmpty()
        val revenue = currencyFormatter.getFormattedAmountZeroRounded(
            stats.revenueGross,
            currencyCode
        )
        remoteViews.setViewVisibility(R.id.widget_info, View.VISIBLE)
        remoteViews.setViewVisibility(R.id.widget_error, View.GONE)
        remoteViews.setPendingIntentTemplate(
            R.id.widget_info,
            WidgetUtils.getWidgetTapPendingIntent(appContext, DailyStatsWidgetProvider.WIDGET_NAME)
        )

        remoteViews.setTextViewText(R.id.widget_revenue_value, revenue)
        remoteViews.setTextViewText(R.id.widget_orders_value, stats.ordersTotal.toString())
        remoteViews.setTextViewText(R.id.widget_visitors_value, stats.visitorsTotal.toString())

        remoteViews.setViewVisibility(R.id.widget_revenue_value, View.VISIBLE)
        remoteViews.setViewVisibility(R.id.widget_revenue_skeleton, View.INVISIBLE)

        remoteViews.setViewVisibility(R.id.widget_orders_value, View.VISIBLE)
        remoteViews.setViewVisibility(R.id.widget_orders_skeleton, View.INVISIBLE)

        remoteViews.setViewVisibility(R.id.widget_visitors_value, View.VISIBLE)
        remoteViews.setViewVisibility(R.id.widget_visitors_skeleton, View.INVISIBLE)

        remoteViews.setTextViewText(
            R.id.widget_update_time,
            String.format(
                resourceProvider.getString(R.string.stats_widget_last_updated_message),
                dateUtils.getCurrentTime()
            )
        )
    }

    private fun displaySkeleton(remoteViews: RemoteViews) {
        remoteViews.setViewVisibility(R.id.widget_revenue_value, View.INVISIBLE)
        remoteViews.setViewVisibility(R.id.widget_revenue_skeleton, View.VISIBLE)

        remoteViews.setViewVisibility(R.id.widget_orders_value, View.INVISIBLE)
        remoteViews.setViewVisibility(R.id.widget_orders_skeleton, View.VISIBLE)

        remoteViews.setViewVisibility(R.id.widget_visitors_value, View.INVISIBLE)
        remoteViews.setViewVisibility(R.id.widget_visitors_skeleton, View.VISIBLE)

        remoteViews.setViewVisibility(R.id.widget_info, View.VISIBLE)
        remoteViews.setViewVisibility(R.id.widget_error, View.GONE)
    }

    private fun displayTitle(site: SiteModel?, remoteViews: RemoteViews) {
        remoteViews.setTextViewText(R.id.widget_title, site.getTitle(resourceProvider.getString(R.string.my_store)))
    }
}


