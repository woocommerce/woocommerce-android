package com.woocommerce.android.ui.pospromo

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class PosPromoAnalyticsTracker @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
) {
    fun trackModalViewed() {
        analyticsTrackerWrapper.track(AnalyticsEvent.POS_PROMO_MODAL_VIEWED)
    }

    fun trackSlideViewed(slideIndex: Int) {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.POS_PROMO_MODAL_SLIDE_VIEWED,
            mapOf(KEY_SLIDE_INDEX to slideIndex)
        )
    }

    fun trackModalDismissed() {
        analyticsTrackerWrapper.track(AnalyticsEvent.POS_PROMO_MODAL_DISMISSED)
    }

    fun trackExploreClicked() {
        analyticsTrackerWrapper.track(AnalyticsEvent.POS_PROMO_MODAL_EXPLORE_CLICKED)
    }

    companion object {
        private const val KEY_SLIDE_INDEX = "slide_index"
    }
}
