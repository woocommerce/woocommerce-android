package com.woocommerce.android.ui.widgets.stats.today

import com.woocommerce.android.di.AppComponent
import com.woocommerce.android.ui.widgets.WidgetUpdater
import com.woocommerce.android.ui.widgets.stats.StatsWidget
import javax.inject.Inject

class TodayStatsWidget : StatsWidget() {
    @Inject lateinit var todayWidgetUpdater: TodayWidgetUpdater
    override val widgetUpdater: WidgetUpdater
        get() = todayWidgetUpdater

    override fun inject(appComponent: AppComponent) {
        appComponent.inject(this)
    }
}
