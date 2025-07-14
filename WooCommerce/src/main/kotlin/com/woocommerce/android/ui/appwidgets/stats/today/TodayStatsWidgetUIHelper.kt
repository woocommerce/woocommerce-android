package com.woocommerce.android.ui.appwidgets.stats.today

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.woocommerce.android.R
import com.woocommerce.android.extensions.getTitle
import com.woocommerce.android.ui.appwidgets.WidgetUtils
import com.woocommerce.android.ui.appwidgets.stats.GetWidgetStats
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

class TodayStatsWidgetUIHelper @Inject constructor(
    private val appContext: Context,
    private val resourceProvider: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter,
    private val dateUtils: DateUtils
) {
    fun displayError(
        widgetId: Int,
        remoteViews: RemoteViews,
        errorMessageRes: Int,
        withRetryButton: Boolean
    ) {
        val pendingIntent = WidgetUtils.getWidgetTapPendingIntent(appContext, TodayStatsWidgetProvider.WIDGET_NAME)
        remoteViews.setOnClickPendingIntent(
            R.id.widget_title_container,
            pendingIntent
        )

        remoteViews.setViewVisibility(R.id.widget_info, View.GONE)
        remoteViews.setViewVisibility(R.id.widget_error, View.VISIBLE)
        remoteViews.setTextViewText(R.id.widget_error_message, resourceProvider.getString(errorMessageRes))

        remoteViews.setViewVisibility(R.id.widget_retry_button, if (withRetryButton) View.VISIBLE else View.GONE)
        if (withRetryButton) {
            remoteViews.setOnClickPendingIntent(
                R.id.widget_error,
                WidgetUtils.getRetryIntent(appContext, TodayStatsWidgetProvider::class.java, widgetId)
            )
        } else {
            remoteViews.setOnClickPendingIntent(R.id.widget_error, pendingIntent)
        }
    }

    fun displayInformation(
        stats: GetWidgetStats.WidgetStatsResult.WidgetStats,
        remoteViews: RemoteViews
    ) {
        val pendingIntent = WidgetUtils.getWidgetTapPendingIntent(appContext, TodayStatsWidgetProvider.WIDGET_NAME)
        remoteViews.setOnClickPendingIntent(R.id.widget_title_container, pendingIntent)
        remoteViews.setOnClickPendingIntent(R.id.widget_info, pendingIntent)

        val revenue = currencyFormatter.getFormattedAmountZeroRounded(
            stats.revenueGross,
            stats.currencyCode
        )
        remoteViews.setViewVisibility(R.id.widget_info, View.VISIBLE)
        remoteViews.setViewVisibility(R.id.widget_error, View.GONE)
        remoteViews.setPendingIntentTemplate(
            R.id.widget_info,
            WidgetUtils.getWidgetTapPendingIntent(appContext, TodayStatsWidgetProvider.WIDGET_NAME)
        )

        remoteViews.setTextViewText(R.id.widget_revenue_value, revenue)
        remoteViews.setTextViewText(R.id.widget_orders_value, stats.ordersTotal.toString())
        stats.visitorsTotal?.let {
            remoteViews.setTextViewText(R.id.widget_visitors_value, it.toString())
        }
        remoteViews.setViewVisibility(R.id.widget_revenue_value, View.VISIBLE)
        remoteViews.setViewVisibility(R.id.widget_revenue_skeleton, View.INVISIBLE)

        remoteViews.setViewVisibility(R.id.widget_orders_value, View.VISIBLE)
        remoteViews.setViewVisibility(R.id.widget_orders_skeleton, View.INVISIBLE)

        remoteViews.setViewVisibility(R.id.widget_visitors_value, View.VISIBLE)
        remoteViews.setViewVisibility(R.id.widget_visitors_skeleton, View.INVISIBLE)

        remoteViews.setViewVisibility(
            R.id.widget_visitors_title,
            if (stats.visitorsTotal != null) View.VISIBLE else View.GONE
        )
        remoteViews.setViewVisibility(
            R.id.widget_visitors_value,
            if (stats.visitorsTotal != null) View.VISIBLE else View.GONE
        )

        remoteViews.setTextViewText(
            R.id.widget_update_time,
            String.format(
                resourceProvider.getString(R.string.stats_widget_last_updated_message),
                dateUtils.getCurrentTime()
            )
        )
    }

    fun displaySkeleton(remoteViews: RemoteViews) {
        remoteViews.setViewVisibility(R.id.widget_revenue_value, View.INVISIBLE)
        remoteViews.setViewVisibility(R.id.widget_revenue_skeleton, View.VISIBLE)

        remoteViews.setViewVisibility(R.id.widget_orders_value, View.INVISIBLE)
        remoteViews.setViewVisibility(R.id.widget_orders_skeleton, View.VISIBLE)

        remoteViews.setViewVisibility(R.id.widget_visitors_value, View.INVISIBLE)
        remoteViews.setViewVisibility(R.id.widget_visitors_skeleton, View.VISIBLE)

        remoteViews.setViewVisibility(R.id.widget_info, View.VISIBLE)
        remoteViews.setViewVisibility(R.id.widget_error, View.GONE)
    }

    fun displayTitle(site: SiteModel?, remoteViews: RemoteViews) {
        remoteViews.setTextViewText(R.id.widget_title, site.getTitle(resourceProvider.getString(R.string.my_store)))
    }
}
