package com.woocommerce.android.ui.widgets.stats

import com.woocommerce.android.di.AppComponent
import com.woocommerce.android.ui.widgets.WidgetUpdater
import javax.inject.Inject

class StatsTodayWidget : StatsWidget() {
    @Inject lateinit var todayWidgetUpdater: TodayWidgetUpdater
    override val widgetUpdater: WidgetUpdater
        get() = todayWidgetUpdater

    override fun inject(appComponent: AppComponent) {
        appComponent.inject(this)
    }
}
