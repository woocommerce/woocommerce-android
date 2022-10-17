package com.woocommerce.android.ui.products.variations

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_VARIATION_CREATION_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_VARIATION_CREATION_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR_DESC
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_PRODUCT_ID
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.model.toDataModel
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.BatchUpdateVariationsPayload
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class VariationRepository @Inject constructor(
    private val productStore: WCProductStore,
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val PRODUCT_VARIATIONS_PAGE_SIZE = WCProductStore.DEFAULT_PRODUCT_VARIATIONS_PAGE_SIZE
    }

    private var offset = 0

    var canLoadMoreProductVariations = true
        private set

    /**
     * Submits a fetch request to get a list of products variations for the current site and productId
     * and returns the full list of product variations from the database
     */
    suspend fun fetchProductVariations(remoteProductId: Long, loadMore: Boolean = false): List<ProductVariation> {
        offset = if (loadMore) offset + PRODUCT_VARIATIONS_PAGE_SIZE else 0
        val payload = WCProductStore.FetchProductVariationsPayload(
            selectedSite.get(),
            remoteProductId,
            pageSize = PRODUCT_VARIATIONS_PAGE_SIZE,
            offset = offset
        )
        val result = productStore.fetchProductVariations(payload)

        if (result.isError) {
            AnalyticsTracker.track(
                AnalyticsEvent.PRODUCT_VARIANTS_LOAD_ERROR,
                this.javaClass.simpleName,
                result.error.type.toString(),
                result.error.message
            )
        } else {
            canLoadMoreProductVariations = result.canLoadMore
            AnalyticsTracker.track(AnalyticsEvent.PRODUCT_VARIANTS_LOADED)
        }

        return getProductVariationList(remoteProductId)
    }

    /**
     * Returns all product variations for a product and current site that are in the database
     */
    fun getProductVariationList(remoteProductId: Long): List<ProductVariation> {
        return productStore.getVariationsForProduct(selectedSite.get(), remoteProductId)
            .map { it.toAppModel() }
            .sorted()
    }

    /**
     * Returns the currency code for the site
     */
    fun getCurrencyCode() = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode

    /**
     * Fires the request to create a empty variation to a given product
     */
    suspend fun createEmptyVariation(product: Product): ProductVariation? =
        withContext(Dispatchers.IO) {
            productStore
                .generateEmptyVariation(selectedSite.get(), product.toDataModel())
                .handleVariationCreateResult(product)
        }

    /**
     * Bulk updates variations.
     */
    suspend fun bulkUpdateVariations(
        remoteProductId: Long,
        variationIds: Collection<Long>,
        newRegularPrice: String? = null,
        newSalePrice: String? = null,
    ): Boolean {
        val payloadBuilder = BatchUpdateVariationsPayload.Builder(selectedSite.get(), remoteProductId, variationIds)
        if (newRegularPrice != null) payloadBuilder.regularPrice(newRegularPrice)
        if (newSalePrice != null) payloadBuilder.salePrice(newSalePrice)
        val result = productStore.batchUpdateVariations(payloadBuilder.build())
        return !result.isError
    }

    private fun WooResult<WCProductVariationModel>.handleVariationCreateResult(
        product: Product
    ) = if (isError) {
        AnalyticsTracker.track(
            PRODUCT_VARIATION_CREATION_FAILED,
            mapOf(
                KEY_PRODUCT_ID to product.remoteId,
                KEY_ERROR_DESC to error.message
            )
        )
        null
    } else {
        AnalyticsTracker.track(
            PRODUCT_VARIATION_CREATION_SUCCESS,
            mapOf(KEY_PRODUCT_ID to product.remoteId)
        )
        model?.toAppModel()
    }
}
