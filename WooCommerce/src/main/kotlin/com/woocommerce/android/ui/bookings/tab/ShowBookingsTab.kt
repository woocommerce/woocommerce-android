package com.woocommerce.android.ui.bookings.tab

import com.woocommerce.android.util.FeatureFlag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ShowBookingsTab @Inject constructor() {
    suspend operator fun invoke(): Result<Boolean> = withContext(Dispatchers.IO) {
        // TODO CHECK IF THERE ARE ANY BOOKABLE PRODUCTS PUBLISHED FOR THE SITE
        return@withContext Result.success(FeatureFlag.BOOKINGS_MVP.isEnabled())
    }
}
