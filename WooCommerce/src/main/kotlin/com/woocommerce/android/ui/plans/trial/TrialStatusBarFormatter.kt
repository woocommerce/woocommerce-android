package com.woocommerce.android.ui.plans.trial

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import com.woocommerce.android.R
import com.woocommerce.android.util.StringUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

class TrialStatusBarFormatter @AssistedInject constructor(
    @Assisted private val context: Context
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

        return SpannableString(statusMessage)
    }
}
