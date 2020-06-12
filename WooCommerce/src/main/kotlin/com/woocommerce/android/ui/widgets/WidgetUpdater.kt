package com.woocommerce.android.ui.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

/**
 * Light weight Interface that can be injected into activities/fragments/services
 * in order to update/add/delete a widget.
 */
interface WidgetUpdater {
    /**
     * Called when a widget needs to be refreshed/updated
     */
    fun updateAppWidget(
        context: Context,
        appWidgetId: Int,
        appWidgetManager: AppWidgetManager? = null
    )

    fun componentName(context: Context): ComponentName

    /**
     * Called when a widget is deleted
     */
    fun delete(appWidgetId: Int)
}
