package com.woocommerce.android.ui.widgets.stats.today

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.woocommerce.android.ui.widgets.WidgetUpdater
import javax.inject.Inject

/**
 * Performs the actual update of the current day stats widget
 */
class TodayWidgetUpdater
@Inject constructor() : WidgetUpdater {
    override fun updateAppWidget(context: Context, appWidgetId: Int, appWidgetManager: AppWidgetManager?) {
        // TODO
    }

    override fun componentName(context: Context): ComponentName {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun delete(appWidgetId: Int) {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}
