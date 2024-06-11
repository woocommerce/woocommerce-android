package com.woocommerce.android.ui.orders.creation.customerlist

import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CustomerListIsAdvancedSearchSupported @Inject constructor(
    private val dispatchers: CoroutineDispatchers,
    private val getWooVersion: GetWooCorePluginCachedVersion,
) {
    suspend operator fun invoke() = withContext(dispatchers.io) {
        val version = getWooVersion() ?: return@withContext false
        version.semverCompareTo(WOO_CORE_SUPPORTS_SEARCH_BY_ALL_CUSTOMERS_VERSION) >= 0
    }

    private companion object {
        private const val WOO_CORE_SUPPORTS_SEARCH_BY_ALL_CUSTOMERS_VERSION = "8.0.0"
    }
}
