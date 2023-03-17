package com.woocommerce.android.ui.plans.trial

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.UnderlineSpan
import androidx.core.text.inSpans
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.FREE_TRIAL_UPGRADE_NOW
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_SOURCE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_BANNER
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.plans.domain.StartUpgradeFlow
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.widgets.WooClickableSpan
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

class TrialStatusBarFormatter @AssistedInject constructor(
    private val resourceProvider: ResourceProvider,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    @Assisted private val startUpgradeFlow: StartUpgradeFlow,
) {

    fun format(daysLeftInTrial: Int): Spannable {
        val statusMessage = if (daysLeftInTrial > 0) {
            resourceProvider.getString(R.string.free_trial_days_left, daysLeftInTrial)
        } else {
            resourceProvider.getString(R.string.free_trial_your_trial_ended)
        }

        return SpannableStringBuilder()
            .append(statusMessage)
            .append(" ")
            .inSpans(
                WooClickableSpan(customLinkColor = resourceProvider.getColor(R.color.woo_purple_60)) {
                    analyticsTrackerWrapper.track(FREE_TRIAL_UPGRADE_NOW, mapOf(KEY_SOURCE to VALUE_BANNER))
                    startUpgradeFlow.invoke()
                },
                UnderlineSpan(),
            ) {
                append(resourceProvider.getString(R.string.free_trial_upgrade_now))
            }
    }
}
