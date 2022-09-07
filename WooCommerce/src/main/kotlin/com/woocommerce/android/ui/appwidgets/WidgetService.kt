package com.woocommerce.android.ui.appwidgets

import android.content.Intent
import android.widget.RemoteViewsService
import android.widget.RemoteViewsService.RemoteViewsFactory
import com.woocommerce.android.ui.appwidgets.stats.today.TodayWidgetListRemoteViewsFactory
import dagger.hilt.android.AndroidEntryPoint

/**
 * The service to be connected to a remote adapter to request RemoteViews.
 *
 * Modify the [onGetViewFactory] method when you want to add multiple widgets to the app.
 * Note that a new [RemoteViewsFactory] must be added first.
 *
 * Currently only used by [TodayWidgetListRemoteViewsFactory] for displaying current day stats
 */
@AndroidEntryPoint
class WidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TodayWidgetListRemoteViewsFactory(
            this.applicationContext,
            intent
        )
    }
}
