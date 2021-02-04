package com.woocommerce.android.ui.products.variations.attributes

import com.woocommerce.android.model.ProductGlobalAttribute
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.attribute.WCGlobalAttributeModel
import org.wordpress.android.fluxc.store.WCGlobalAttributeStore
import javax.inject.Inject

class ProductAttributeListRepository @Inject constructor(
    private val attributesStore: WCGlobalAttributeStore,
    private val selectedSite: SelectedSite
) {
    /**
     * Submits a fetch request to get a list of attributes for the current store and returns the fetched list
     */
    suspend fun fetchAttributesForStore(): List<ProductGlobalAttribute> {
        val result: List<WCGlobalAttributeModel>
        withContext(Dispatchers.Default) {
            attributesStore.fetchStoreAttributes(selectedSite.get()).also {
                result = it.model ?: emptyList()
            }
        }
        return result.map { it.toAppModel() }
    }

    // TODO
    /*fun getAttributesForStore(): List<ProductGlobalAttribute> {
        attributesStore.
    }*/

    fun onCleanup() {
        // TODO ??
    }
}
