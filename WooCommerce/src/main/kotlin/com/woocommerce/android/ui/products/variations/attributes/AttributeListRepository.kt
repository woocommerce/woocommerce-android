package com.woocommerce.android.ui.products.variations.attributes

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.model.product.attributes.WCProductAttributeModel
import org.wordpress.android.fluxc.store.WCProductAttributesStore
import javax.inject.Inject

class AttributeListRepository @Inject constructor(
    private val productAttributesStore: WCProductAttributesStore,
    private val selectedSite: SelectedSite
) {
    /**
     * Submits a fetch request to get a list of attributes for the current store
     */
    suspend fun fetchStoreAttributes(remoteProductId: Long): List<WCProductAttributeModel> {
        // TODO
        return emptyList()
    }
}
