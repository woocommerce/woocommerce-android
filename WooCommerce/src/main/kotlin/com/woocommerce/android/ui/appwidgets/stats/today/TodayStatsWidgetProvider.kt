package com.woocommerce.android.ui.appwidgets.stats.today

import com.woocommerce.android.ui.appwidgets.WidgetUpdater
import com.woocommerce.android.ui.appwidgets.stats.StatsWidgetProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TodayStatsWidgetProvider : StatsWidgetProvider() {
    companion object {
        const val WIDGET_NAME = "today-stats"
        const val WIDGET_UPDATE_INTERVAL = 15L
    }

    @Inject lateinit var todayWidgetUpdater: TodayStatsWidgetUpdater

    override val widgetUpdater: WidgetUpdater
        get() = todayWidgetUpdater
}
