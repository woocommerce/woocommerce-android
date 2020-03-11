package com.woocommerce.android.ui.widgets

import android.content.Intent
import android.widget.RemoteViewsService
import android.widget.RemoteViewsService.RemoteViewsFactory
import com.woocommerce.android.ui.widgets.stats.TodayWidgetListProvider

/**
 * The service to be connected to for a remote adapter to request RemoteViews.
 * Modify the [onGetViewFactory] method when you want to add more widgets to the app.
 * Note that a new [RemoteViewsFactory] must be added first.
 *
 * Currently only used by [TodayWidgetListProvider] for displaying current day stats
 */
class WidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TodayWidgetListProvider(this.applicationContext, intent)
    }
}
