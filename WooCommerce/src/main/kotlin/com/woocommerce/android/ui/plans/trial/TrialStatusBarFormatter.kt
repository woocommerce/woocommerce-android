package com.woocommerce.android.ui.plans.trial

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import androidx.core.text.inSpans
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.widgets.WooClickableSpan
import dagger.hilt.android.qualifiers.ActivityContext
import javax.inject.Inject

class TrialStatusBarFormatter @Inject constructor(
    @ActivityContext private val context: Context,
    private val resourceProvider: ResourceProvider,
    private val selectedSite: SelectedSite
) {

    fun format(daysLeftInTrial: Int): Spannable {

        val statusMessage = if (daysLeftInTrial > 0) {
            resourceProvider.getString(R.string.free_trial_days_left, daysLeftInTrial)
        } else {
            resourceProvider.getString(R.string.free_trial_trial_ended)
        }

        return SpannableStringBuilder()
            .append(statusMessage)
            .append(" ")
            .inSpans(
                WooClickableSpan {
                    ChromeCustomTabUtils.launchUrl(context, "https://wordpress.com/plans/${selectedSite.get().siteId}")
                }
            ) {
                append(resourceProvider.getString(R.string.free_trial_upgrade_now))
            }
    }
}
