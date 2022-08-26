package com.woocommerce.android.ui.appwidgets.stats.today

import com.woocommerce.android.ui.appwidgets.WidgetUpdater
import com.woocommerce.android.ui.appwidgets.stats.StatsWidget
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TodayStatsWidget() : StatsWidget() {
    @Inject lateinit var todayWidgetUpdater: TodayWidgetUpdater
    override val widgetUpdater: WidgetUpdater
        get() = todayWidgetUpdater
}
