package com.woocommerce.android.extensions

import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.ResourceProvider
import java.time.Period

fun Period.pluralizedDays(resourceProvider: ResourceProvider): String {
    return when (days) {
        1 -> resourceProvider.getString(R.string.free_trial_one_day_left)
        else -> resourceProvider.getString(R.string.free_trial_days_left_plural, days)
    }
}
