package com.woocommerce.android.ui.products.details

import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class DetermineProductPasswordApi @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore
) {
    suspend operator fun invoke(): ProductPasswordApi {
        val wcVersion = withContext(Dispatchers.IO) {
            wooCommerceStore.getSitePlugin(
                selectedSite.get(),
                WooCommerceStore.WooPlugin.WOO_CORE
            )?.version
        }

        return when {
            wcVersion == null ||
                wcVersion.semverCompareTo("8.1.0") < 0 -> {
                if (selectedSite.connectionType != SiteConnectionType.ApplicationPasswords) {
                    ProductPasswordApi.WPCOM
                } else {
                    ProductPasswordApi.UNSUPPORTED
                }
            }

            else -> ProductPasswordApi.CORE
        }
    }
}

enum class ProductPasswordApi {
    WPCOM,
    CORE,
    UNSUPPORTED
}
