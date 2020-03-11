package com.woocommerce.android.ui.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ui.widgets.stats.TodayWidgetUpdater
import javax.inject.Inject

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

    class StatsWidgetUpdaters
    @Inject constructor(
        private val todayWidgetUpdater: TodayWidgetUpdater,
        private val appPrefsWrapper: AppPrefsWrapper,
        private val context: Context
    ) {
        private val widgetUpdaters = listOf(todayWidgetUpdater)

        fun update(context: Context) {
            widgetUpdaters.forEach {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val allWidgetIds = appWidgetManager.getAppWidgetIds(it.componentName(context))
                for (appWidgetId in allWidgetIds) {
                    it.updateAppWidget(context, appWidgetId, appWidgetManager)
                }
            }
        }

        fun updateTodayWidget() {
            todayWidgetUpdater.update()
        }

        private fun WidgetUpdater.update() {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(this.componentName(context))
            for (appWidgetId in allWidgetIds) {
                this.updateAppWidget(context, appWidgetId, appWidgetManager)
            }
        }
    }
}
