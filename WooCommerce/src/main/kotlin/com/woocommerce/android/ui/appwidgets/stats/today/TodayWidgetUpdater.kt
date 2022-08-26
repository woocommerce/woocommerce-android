package com.woocommerce.android.ui.appwidgets.stats.today

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.extensions.getTitle
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.appwidgets.WidgetColorMode
import com.woocommerce.android.ui.appwidgets.WidgetUpdater
import com.woocommerce.android.ui.appwidgets.WidgetUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

/**
 * Performs the actual update of the current day stats widget
 */
class TodayWidgetUpdater
@Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val siteStore: SiteStore,
    private val accountStore: AccountStore,
    private val networkStatus: NetworkStatus,
    private val resourceProvider: ResourceProvider,
    private val widgetUtils: WidgetUtils
) : WidgetUpdater {
    override fun updateAppWidget(
        context: Context,
        appWidgetId: Int,
        appWidgetManager: AppWidgetManager?
    ) {
        val widgetManager = appWidgetManager ?: AppWidgetManager.getInstance(context)

        val colorMode = appPrefsWrapper.getAppWidgetColor(appWidgetId) ?: WidgetColorMode.LIGHT
        val siteId = appPrefsWrapper.getAppWidgetSiteId(appWidgetId)
        val siteModel = siteStore.getSiteBySiteId(siteId)

        val networkAvailable = networkStatus.isConnected()
        val hasAccessToken = accountStore.hasAccessToken()
        val isUsingV4Api = appPrefsWrapper.isV4StatsSupported()

        val views = RemoteViews(context.packageName, widgetUtils.getLayout(colorMode))

        if (networkAvailable && hasAccessToken && siteModel != null) {
            views.setTextViewText(R.id.widget_title, siteModel.getTitle(context))
            views.setViewVisibility(R.id.widget_type, View.VISIBLE)

            siteModel.let {
                views.setOnClickPendingIntent(
                    R.id.widget_title_container,
                    widgetUtils.getPendingSelfIntent(context, siteModel.id)
                )
            }
            widgetUtils.showList(
                widgetManager,
                views,
                context,
                appWidgetId,
                colorMode,
                siteModel.id
            )
        } else {
            // Widget data will only be displayed if network is available,
            // user is logged in and if stats v4 is available
            val errorMessage = if (!networkAvailable) {
                R.string.stats_widget_offline_error
            } else if (!hasAccessToken) {
                R.string.stats_widget_log_in_message
            } else if (!isUsingV4Api) {
                R.string.stats_widget_availability_message
            } else R.string.stats_widget_error_no_data

            views.setTextViewText(R.id.widget_title, resourceProvider.getString(R.string.my_store))
            views.setViewVisibility(R.id.widget_type, View.GONE)
            widgetUtils.showError(
                widgetManager,
                views,
                appWidgetId,
                errorMessage,
                resourceProvider,
                context,
                TodayStatsWidget::class.java
            )
        }
    }

    override fun componentName(context: Context) = ComponentName(context, TodayStatsWidget::class.java)

    override fun delete(appWidgetId: Int) {
        // TODO: add tracking here when widget is deleted
        appPrefsWrapper.removeAppWidgetColorModeId(appWidgetId)
        appPrefsWrapper.removeAppWidgetSiteId(appWidgetId)
    }
}
