package com.woocommerce.android.ui.appwidgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import com.woocommerce.android.R
import com.woocommerce.android.ui.appwidgets.WidgetUtils.Companion.WIDGET_NO_REGISTERED
import com.woocommerce.android.ui.appwidgets.stats.today.TodayStatsWidgetProvider
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.viewmodel.ResourceProvider
import java.util.Date
import javax.inject.Inject
import kotlin.random.Random

/**
 * Utils class for for displaying data and handling click events in widgets.
 * Currently used to display stats widget list OR error message if stats is unavailable.
 */
class WidgetUtils
@Inject constructor() {
    fun showList(
        appWidgetManager: AppWidgetManager,
        views: RemoteViews,
        context: Context,
        appWidgetId: Int,
        widgetName: String
    ) {
        views.setPendingIntentTemplate(
            R.id.widget_content,
            getWidgetTapPendingIntent(context, widgetName)
        )
        views.setViewVisibility(R.id.widget_content, View.VISIBLE)
        views.setViewVisibility(R.id.widget_error, View.GONE)

        val listIntent = Intent(context, WidgetService::class.java)
        listIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        listIntent.data = Uri.parse(listIntent.toUri(Intent.URI_INTENT_SCHEME))

        views.setRemoteAdapter(R.id.widget_content, listIntent)
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_content)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    @Suppress("LongParameterList")
    fun showError(
        appWidgetManager: AppWidgetManager,
        views: RemoteViews,
        appWidgetId: Int,
        errorMessage: Int,
        resourceProvider: ResourceProvider,
        context: Context,
        hasAccessToken: Boolean,
        widgetType: Class<*>,
        widgetName: String
    ) {
        val pendingIntent = getWidgetTapPendingIntent(context, widgetName)
        views.setOnClickPendingIntent(
            R.id.widget_title_container,
            pendingIntent
        )

        views.setViewVisibility(R.id.widget_content, View.GONE)
        views.setViewVisibility(R.id.widget_error, View.VISIBLE)
        views.setTextViewText(
            R.id.widget_error_message,
            resourceProvider.getString(errorMessage)
        )

        // if the access token exists it means the user is logged in and is experiencing an error, in which
        // case we show a Retry button that attempts to fetch stats again. if the user isn't logged in, we
        // hide the Retry button and launch the app when the widget is clicked.
        views.setViewVisibility(R.id.widget_retry_button, if (hasAccessToken) View.VISIBLE else View.GONE)
        if (hasAccessToken) {
            views.setOnClickPendingIntent(R.id.widget_error, getRetryIntent(context, widgetType, appWidgetId))
        } else {
            views.setOnClickPendingIntent(R.id.widget_error, pendingIntent)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    fun getWidgetTapPendingIntent(
        context: Context,
        widgetName: String
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MainActivity.FIELD_OPENED_FROM_WIDGET, true)
            putExtra(MainActivity.FIELD_WIDGET_NAME, widgetName)
        }
        return PendingIntent.getActivity(
            context,
            getRandomId(),
            intent,
            PENDING_INTENT_FLAGS
        )
    }

    private fun getRetryIntent(
        context: Context,
        widgetType: Class<*>,
        appWidgetId: Int
    ): PendingIntent? {
        val intentSync = Intent(context, widgetType)
        intentSync.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE

        intentSync.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        return PendingIntent.getBroadcast(
            context,
            Random(appWidgetId).nextInt(),
            intentSync,
            PENDING_INTENT_FLAGS
        )
    }

    private fun getRandomId(): Int {
        return Random(Date().time).nextInt()
    }

    companion object {
        private val PENDING_INTENT_FLAGS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        const val WIDGET_NO_REGISTERED = "no-registered"
    }
}

fun AppWidgetProviderInfo.getWidgetName(): String {
    return when (this.provider.className) {
        TodayStatsWidgetProvider::class.java.name -> TodayStatsWidgetProvider.WIDGET_NAME
        else -> WIDGET_NO_REGISTERED
    }
}
