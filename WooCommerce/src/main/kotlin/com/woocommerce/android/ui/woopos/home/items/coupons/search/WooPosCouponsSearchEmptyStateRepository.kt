package com.woocommerce.android.ui.woopos.home.items.coupons.search

import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class WooPosCouponsSearchEmptyStateRepository @Inject constructor(
    private val preferencesRepository: WooPosPreferencesRepository,
) {
    suspend fun getLastSearches(): List<String> = preferencesRepository.recentCouponSearches.first()

    suspend fun addRecentSearch(search: String) {
        preferencesRepository.addRecentCouponSearch(search)
    }
}
