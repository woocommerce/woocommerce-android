package com.woocommerce.android.ui.appwidgets.stats.daily

import com.woocommerce.android.ui.appwidgets.WidgetUpdater
import com.woocommerce.android.ui.appwidgets.stats.StatsWidgetProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DailyStatsWidgetProvider : StatsWidgetProvider() {
    companion object {
        const val WIDGET_NAME = "daily-stats"
        const val WIDGET_UPDATE_INTERVAL = 15L
    }

    @Inject lateinit var dailyWidgetUpdater: DailyStatsWidgetUpdater

    override val widgetUpdater: WidgetUpdater
        get() = dailyWidgetUpdater
}
