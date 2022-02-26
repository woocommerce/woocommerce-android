package com.woocommerce.android.ui.mystore

import com.woocommerce.android.analytics.AnalyticsTracker.Stat.USED_ANALYTICS
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import dagger.hilt.android.scopes.ActivityRetainedScoped
import java.util.*
import javax.inject.Inject

/**
 * This is scoped as [ActivityRetainedScoped] so that a new instance will only be created when the user
 * switches to a different store or logs back in.
 */
@ActivityRetainedScoped
class MyStoreStatsUsageTracksEventEmitter @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    fun interacted(at: Date = Date()) {
        println("🦀 $this interacted at $at")

        analyticsTrackerWrapper.track(USED_ANALYTICS)
    }
}
