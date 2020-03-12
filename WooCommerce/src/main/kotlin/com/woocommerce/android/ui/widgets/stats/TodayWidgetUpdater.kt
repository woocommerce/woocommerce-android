package com.woocommerce.android.ui.widgets.stats

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.extensions.getTitle
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.widgets.WidgetUpdater
import com.woocommerce.android.util.WidgetUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import org.wordpress.android.fluxc.store.AccountStore
import javax.inject.Inject

/**
 * Performs the actual update of the current day stats widget
 */
class TodayWidgetUpdater
@Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSite: SelectedSite,
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
        val views = RemoteViews(context.packageName, R.layout.stats_widget_list)
        val widgetManager = appWidgetManager ?: AppWidgetManager.getInstance(context)

        val networkAvailable = networkStatus.isConnected()
        val hasAccessToken = accountStore.hasAccessToken()
        val isUsingV4Api = appPrefsWrapper.isUsingV4Api

        // Widget data will only be displayed if network is available,
        // user is logged in and if stats v4 is available
        val errorMessage = if (!networkAvailable) {
            R.string.offline_error
        } else if (!hasAccessToken) {
            R.string.stats_widget_log_in_message
        } else if (!isUsingV4Api) {
            R.string.stats_widget_availability_message
        } else null

        errorMessage?.let {
            views.setTextViewText(R.id.widget_title, resourceProvider.getString(R.string.my_store))
            views.setViewVisibility(R.id.widget_type, View.GONE)
            widgetUtils.showError(
                    widgetManager,
                    views,
                    appWidgetId,
                    it,
                    resourceProvider,
                    context,
                    StatsTodayWidget::class.java
            )
        } ?: run {
            val siteModel = selectedSite.get()
            views.setTextViewText(R.id.widget_title, siteModel.getTitle(context))
            views.setViewVisibility(R.id.widget_type, View.VISIBLE)
            views.setOnClickPendingIntent(
                    R.id.widget_title_container,
                    widgetUtils.getPendingSelfIntent(context)
            )
            widgetUtils.showList(
                    widgetManager,
                    views,
                    context,
                    appWidgetId
            )
        }
    }

    override fun componentName(context: Context) = ComponentName(context, StatsTodayWidget::class.java)

    override fun delete(appWidgetId: Int) {
        // TODO: add tracking here when widget is deleted
    }
}
