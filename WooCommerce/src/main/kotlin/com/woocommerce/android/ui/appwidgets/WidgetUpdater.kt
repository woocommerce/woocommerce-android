package com.woocommerce.android.ui.appwidgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.woocommerce.android.ui.appwidgets.stats.today.TodayStatsWidgetUpdater
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
    fun delete(context: Context, appWidgetId: Int)

    class StatsWidgetUpdaters
    @Inject constructor(
        private val todayStatsWidgetUpdater: TodayStatsWidgetUpdater,
        private val context: Context
    ) {
        private val widgetUpdaters = listOf(todayStatsWidgetUpdater)

        /**
         * Update method is called when we need to update all the widgets that are active.
         * For instance, we would need to update all the active widgets when user logs out of the app
         * since most of the data needed for the widget, requires authentication.
         */
        fun update(context: Context) {
            widgetUpdaters.forEach {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val allWidgetIds = appWidgetManager.getAppWidgetIds(it.componentName(context))
                for (appWidgetId in allWidgetIds) {
                    it.updateAppWidget(context, appWidgetId, appWidgetManager)
                }
            }
        }

        /**
         * Method used to update the today widget when stats is refreshed OR
         * another store is selected
         */
        fun updateTodayWidget() {
            todayStatsWidgetUpdater.update()
        }

        private fun WidgetUpdater.update() {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(this.componentName(context))
            allWidgetIds.forEach { appWidgetId ->
                this.updateAppWidget(context, appWidgetId, appWidgetManager)
            }
        }
    }
}
