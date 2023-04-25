package com.woocommerce.android.ui.plans.trial

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.UnderlineSpan
import androidx.core.text.inSpans
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.FREE_TRIAL_UPGRADE_NOW_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_SOURCE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_BANNER
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.storecreation.dispatcher.PlanUpgradeStartFragment
import com.woocommerce.android.ui.plans.domain.StartUpgradeFlow
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.widgets.WooClickableSpan
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

class TrialStatusBarFormatter @AssistedInject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    @Assisted private val context: Context,
    @Assisted private val startUpgradeFlow: StartUpgradeFlow,
) {

    fun format(daysLeftInTrial: Int): Spannable {
        val statusMessage = if (daysLeftInTrial > 0) {
            StringUtils.getQuantityString(
                context = context,
                quantity = daysLeftInTrial,
                default = R.string.free_trial_days_left_plural,
                one = R.string.free_trial_one_day_left
            ).let { context.getString(R.string.free_trial_days_left, it) }
        } else {
            context.getString(R.string.free_trial_your_trial_ended)
        }

        return SpannableStringBuilder()
            .append(statusMessage)
            .append(" ")
            .inSpans(
                WooClickableSpan(customLinkColor = context.getColor(R.color.free_trial_component_text)) {
                    analyticsTrackerWrapper.track(FREE_TRIAL_UPGRADE_NOW_TAPPED, mapOf(KEY_SOURCE to VALUE_BANNER))
                    startUpgradeFlow.invoke(PlanUpgradeStartFragment.PlanUpgradeStartSource.BANNER)
                },
                UnderlineSpan(),
            ) {
                append(context.getString(R.string.free_trial_upgrade_now))
            }
    }
}
