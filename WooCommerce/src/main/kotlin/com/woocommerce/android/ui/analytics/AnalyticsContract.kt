package com.woocommerce.android.ui.analytics

import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRangeSelectorContract.AnalyticsDateRangeSelectorViewState


class AnalyticsContract {

    data class AnalyticsState(
        val analyticsDateRangeSelectorState: AnalyticsDateRangeSelectorViewState,
    )

    sealed class AnalyticsEvent

    sealed class AnalyticsEffect
}


