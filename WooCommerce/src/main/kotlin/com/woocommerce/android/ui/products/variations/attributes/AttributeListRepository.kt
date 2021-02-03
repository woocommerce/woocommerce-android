package com.woocommerce.android.ui.products.variations.attributes

import com.woocommerce.android.model.ProductAttribute
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.product.attributes.WCProductAttributeModel
import org.wordpress.android.fluxc.store.WCProductAttributesStore
import javax.inject.Inject

class AttributeListRepository @Inject constructor(
    private val productAttributesStore: WCProductAttributesStore,
    private val selectedSite: SelectedSite
) {
    /**
     * Submits a fetch request to get a list of attributes for the current store and returns the fetched list
     */
    suspend fun fetchStoreAttributes(): List<ProductAttribute> {
        val result: List<WCProductAttributeModel>
        withContext(Dispatchers.Default) {
            productAttributesStore.fetchStoreAttributes(selectedSite.get()).also {
                result = it.model ?: emptyList()
            }
        }
        return result.map { it.toAppModel() }
    }

    fun onCleanup() {
        // TODO ??
    }
}
