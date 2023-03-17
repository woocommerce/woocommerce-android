package com.woocommerce.android.ui.plans.domain

import com.woocommerce.android.extensions.clock
import com.woocommerce.android.tools.SelectedSite
import java.time.Period
import java.time.ZonedDateTime
import javax.inject.Inject

class CalculateRemainingTrialPeriod @Inject constructor(
    private val selectedSite: SelectedSite
) {

    operator fun invoke(expirationDate: ZonedDateTime): Period {
        val currentDateInSiteTimezone = ZonedDateTime.now(selectedSite.get().clock).toLocalDate()

        return Period.between(
            currentDateInSiteTimezone,
            expirationDate.toLocalDate().minusDays(1)
        )
    }
}
