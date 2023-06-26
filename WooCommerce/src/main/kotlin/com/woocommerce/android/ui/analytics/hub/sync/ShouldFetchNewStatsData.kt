package com.woocommerce.android.ui.analytics.hub.sync

import javax.inject.Inject

class ShouldFetchNewStatsData @Inject constructor() {
    operator fun invoke(): Boolean {
        return true
    }
}
