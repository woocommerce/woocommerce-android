package com.woocommerce.android.ui.appwidgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.woocommerce.android.R
import com.woocommerce.android.ui.appwidgets.stats.StatsWidget.Companion.SITE_ID_KEY
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
    @Suppress("LongParameterList")
    fun showList(
        appWidgetManager: AppWidgetManager,
        views: RemoteViews,
        context: Context,
        appWidgetId: Int,
        siteId: Int
    ) {
        views.setPendingIntentTemplate(R.id.widget_content, getPendingTemplate(context))
        views.setViewVisibility(R.id.widget_content, View.VISIBLE)
        views.setViewVisibility(R.id.widget_error, View.GONE)

        val listIntent = Intent(context, WidgetService::class.java)
        listIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        listIntent.putExtra(SITE_ID_KEY, siteId)
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
        widgetType: Class<*>
    ) {
        views.setOnClickPendingIntent(
            R.id.widget_title_container,
            PendingIntent.getActivity(
                context,
                0,
                Intent(),
                PENDING_INTENT_FLAGS
            )
        )
        views.setViewVisibility(R.id.widget_content, View.GONE)
        views.setViewVisibility(R.id.widget_error, View.VISIBLE)
        views.setTextViewText(
            R.id.widget_error_message,
            resourceProvider.getString(errorMessage)
        )
        val pendingSync = getRetryIntent(context, widgetType, appWidgetId)
        views.setOnClickPendingIntent(R.id.widget_error, pendingSync)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    @Suppress("UnusedPrivateMember")
    fun getPendingSelfIntent(
        context: Context,
        localSiteId: Int
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        // TODO pass the localSiteId to the activity in order to auto login the
        // user to the widget's site id
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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

    private fun getPendingTemplate(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            getRandomId(),
            intent,
            PENDING_INTENT_FLAGS
        )
    }

    private fun getRandomId(): Int {
        return Random(Date().time).nextInt()
    }

    companion object {
        private const val PENDING_INTENT_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }
}
