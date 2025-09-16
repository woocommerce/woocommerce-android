package com.woocommerce.android.ui.bookings.tab

import com.woocommerce.android.util.FeatureFlag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ShowBookingsTab @Inject constructor() {
    suspend operator fun invoke(): Result<Boolean> = withContext(Dispatchers.IO) {
        // TODO: Fetch if site has any published bookable product AND if site is CIAB
        return@withContext Result.success(FeatureFlag.BOOKINGS_MVP.isEnabled())
    }
}
