package com.woocommerce.android.ui.appwidgets.stats.today

import com.woocommerce.android.ui.appwidgets.WidgetUpdater
import com.woocommerce.android.ui.appwidgets.stats.StatsWidget
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TodayStatsWidget @Inject constructor(private val todayWidgetUpdater: TodayWidgetUpdater) : StatsWidget() {
    override val widgetUpdater: WidgetUpdater
        get() = todayWidgetUpdater
}
