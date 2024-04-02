package com.woocommerce.android.ui.dashboard

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.util.DateTimeUtils
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Accepts interaction events from the Analytics / My Store UI and decides whether the group of interactions can be
 * considered as a _usage_ of the UI.
 *
 * See p91TBi-6Cl-p2 for more information about the algorithm.
 *
 * The UI should call [interacted] when these events happen:
 *
 * - Scrolling
 * - Pull-to-refresh
 * - Tapping on the bars in the chart
 * - Changing the tab
 * - Navigating to the My Store tab
 * - Tapping on a product in the Top Performers list
 *
 * If we ever change the algorithm in the future, we should probably consider renaming the Tracks event to avoid
 * incorrect comparisons with old events. We should also make sure to change the iOS code if we're changing anything
 * in here. Both platforms should have the same algorithm so we are able to compare both.
 */
@Singleton
class DashboardStatsUsageTracksEventEmitter @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
    private val selectedSite: SelectedSite
) {
    private companion object {
        /**
         * The minimum amount of time (seconds) that the merchant have interacted with the
         * Analytics UI before an event is triggered.
         */
        const val MINIMUM_INTERACTION_TIME = 10

        /**
         * The minimum number of Analytics UI interactions before an event is triggered.
         */
        const val INTERACTIONS_THRESHOLD = 5

        /**
         * The maximum number of seconds in between interactions before we will consider the
         * merchant to have been idle. If they were idle, the time and interactions counting
         * will be reset.
         */
        const val IDLE_TIME_THRESHOLD = 20
    }

    private var interactions = 0
    private var firstInteractionTime: Date? = null
    private var lastInteractionTime: Date? = null

    init {
        // Reset if the user changed to a different site.
        selectedSite.observe()
            .onEach { reset() }
            .launchIn(appCoroutineScope)
    }

    fun interacted(interactionTime: Date = Date()) {
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
            interactions >= INTERACTIONS_THRESHOLD
        ) {
            reset()
            analyticsTrackerWrapper.track(AnalyticsEvent.USED_ANALYTICS)
        }
    }

    private fun reset() {
        interactions = 0
        firstInteractionTime = null
        lastInteractionTime = null
    }

    fun interactedWithCustomRange() {
        analyticsTrackerWrapper.track(AnalyticsEvent.DASHBOARD_STATS_CUSTOM_RANGE_INTERACTED)
    }
}
