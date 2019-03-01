package com.woocommerce.android.ui.orders

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.store.WCProductStore

class ProductHelper(
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite,
    private val dispatcher: Dispatcher
) {
    fun getProductImage(remoteProductId: Long, fetchProductIfNotExists: Boolean = false): String? {
        selectedSite.getIfExists()?.let { site ->
            val image = productStore.getProductImageByRemoteId(site, remoteProductId)
            image?.let {
                return it
            }

            if (fetchProductIfNotExists && !productStore.getProductExistsByRemoteId(site, remoteProductId)) {
                val payload = WCProductStore.FetchSingleProductPayload(site, remoteProductId)
                dispatcher.dispatch(WCProductActionBuilder.newFetchSingleProductAction(payload))
            }
        }

        return null
    }
}
