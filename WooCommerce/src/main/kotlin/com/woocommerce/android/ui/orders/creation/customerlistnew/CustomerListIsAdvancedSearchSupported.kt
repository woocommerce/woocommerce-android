package com.woocommerce.android.ui.orders.creation.customerlistnew

import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class CustomerListIsAdvancedSearchSupported @Inject constructor(
    private val wooCommerceStore: WooCommerceStore,
    private val dispatchers: CoroutineDispatchers,
    private val selectedSite: SelectedSite,
) {
    suspend operator fun invoke() = withContext(dispatchers.io) {
        val version = wooCommerceStore.getSitePlugin(selectedSite.get(), WooCommerceStore.WooPlugin.WOO_CORE)?.version
            ?: return@withContext false
        version.semverCompareTo(WOO_CORE_SUPPORTS_SEARCH_BY_ALL_CUSTOMERS_VERSION) >= 0
    }

    private companion object {
        private const val WOO_CORE_SUPPORTS_SEARCH_BY_ALL_CUSTOMERS_VERSION = "8.0.0"
    }
}
