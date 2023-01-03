package com.woocommerce.android.ui.appwidgets.stats

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.woocommerce.android.ui.appwidgets.WidgetUpdater

/**
 * Abstract class that defines the basic methods to programmatically interface with the App Widget,
 * based on broadcast events.
 *
 * Through this class, broadcasts will be received when the App Widget is updated, enabled, disabled and deleted.
 */
abstract class StatsWidgetProvider : AppWidgetProvider() {
    abstract val widgetUpdater: WidgetUpdater

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)
        val appWidgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (appWidgetId > -1) {
            widgetUpdater.updateAppWidget(
                context,
                appWidgetId
            )
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            widgetUpdater.updateAppWidget(
                context,
                appWidgetId,
                appWidgetManager
            )
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            widgetUpdater.delete(context, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        if (context != null) {
            widgetUpdater.updateAppWidget(
                context,
                appWidgetId,
                appWidgetManager
            )
        }
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }
}
