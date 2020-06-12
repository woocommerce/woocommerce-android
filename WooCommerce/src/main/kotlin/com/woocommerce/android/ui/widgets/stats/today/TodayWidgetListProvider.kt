package com.woocommerce.android.ui.widgets.stats.today

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory

/**
 * Class extends [RemoteViewsFactory] and acts as an interface for the current day stats widget ListView adapter
 */
class TodayWidgetListProvider(val context: Context, intent: Intent) : RemoteViewsFactory {
    override fun onCreate() {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun getLoadingView(): RemoteViews {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun getItemId(p0: Int): Long {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun onDataSetChanged() {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun hasStableIds(): Boolean {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun getViewAt(p0: Int): RemoteViews {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun getCount(): Int {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun getViewTypeCount(): Int {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun onDestroy() {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}
