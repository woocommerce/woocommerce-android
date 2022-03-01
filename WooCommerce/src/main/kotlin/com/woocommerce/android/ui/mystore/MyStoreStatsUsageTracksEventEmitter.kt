package com.woocommerce.android.ui.mystore

import com.woocommerce.android.analytics.AnalyticsTracker.Stat.USED_ANALYTICS
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import dagger.hilt.android.scopes.ActivityRetainedScoped
import org.wordpress.android.util.DateTimeUtils
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
    private companion object {
        const val MINIMUM_INTERACTION_TIME = 10
        const val INTERACTIONS_THRESHOLD = 5
        const val IDLE_TIME_THRESHOLD = 20
    }

    private var interactions = 0
    private var firstInteractionTime: Date? = null
    private var lastInteractionTime: Date? = null

    fun interacted(interactionTime: Date = Date()) {
        println("🦀 $this interacted at $interactionTime")

        // Check if they were idle for some time.
        lastInteractionTime?.let {
            if (DateTimeUtils.secondsBetween(interactionTime, it) >= IDLE_TIME_THRESHOLD) {
                reset()
            }
        }

        val firstInteractionTime = firstInteractionTime ?: run {
            interactions = 1
            firstInteractionTime = interactionTime
            lastInteractionTime = interactionTime

            return
        }

        interactions++
        lastInteractionTime = interactionTime

        if (DateTimeUtils.secondsBetween(interactionTime, firstInteractionTime) >= MINIMUM_INTERACTION_TIME &&
            interactions >= INTERACTIONS_THRESHOLD) {

            reset()
            analyticsTrackerWrapper.track(USED_ANALYTICS)
        }
    }

    private fun reset() {
        interactions = 0
        firstInteractionTime = null
        lastInteractionTime = null
    }
}
