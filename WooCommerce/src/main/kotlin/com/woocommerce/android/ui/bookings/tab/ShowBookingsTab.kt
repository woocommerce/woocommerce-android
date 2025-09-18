package com.woocommerce.android.ui.bookings.tab

import com.woocommerce.android.ciab.isCurrentSiteCIAB
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.FeatureFlag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ShowBookingsTab @Inject constructor(
    private val selectedSite: SelectedSite
) {

    suspend operator fun invoke(): Result<Boolean> =
        withContext(Dispatchers.IO) {
            Result.success(
                selectedSite.isCurrentSiteCIAB() &&
                    FeatureFlag.BOOKINGS_MVP.isEnabled()
            )
        }
}
