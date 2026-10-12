package com.woocommerce.android.ui.products.variations

import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_VARIATION_CREATION_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_VARIATION_CREATION_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR_DESC
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_PRODUCT_ID
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.model.SubscriptionProductVariation
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.model.toDataModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.variations.domain.VariationCandidate
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.BatchUpdateVariationsPayload
import org.wordpress.android.fluxc.store.WCProductStore.VariationSorting
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class VariationRepository @Inject constructor(
    private val productStore: WCProductStore,
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
    private val dispatchers: CoroutineDispatchers,
) {
    companion object {
        private const val PRODUCT_VARIATIONS_PAGE_SIZE = WCProductStore.DEFAULT_PRODUCT_VARIATIONS_PAGE_SIZE
    }

    private var offset = 0

    var canLoadMoreProductVariations = true
        private set

    /**
     * Submits a fetch request to get a list of products variations for the current site and productId
     * and returns the full list of product variations from the database, or a failure if the request failed
     */
    suspend fun fetchProductVariations(
        remoteProductId: Long,
        loadMore: Boolean = false
    ): Result<List<ProductVariation>> {
        val requestedOffset = if (loadMore) offset + PRODUCT_VARIATIONS_PAGE_SIZE else 0
        return fetchVariationsPage(remoteProductId, requestedOffset, VariationSorting.MENU_ORDER).map { canLoadMore ->
            offset = requestedOffset
            canLoadMoreProductVariations = canLoadMore
            getProductVariationList(remoteProductId)
        }
    }

    /**
     * Fetches every page of variations for the given product, starting from the first page,
     * and returns the full list from the database, or a failure if any page request failed.
     * Pages are requested by id because variations sharing the same menu_order can repeat across offset pages.
     */
    suspend fun getAllVariations(remoteProductId: Long): Result<List<ProductVariation>> =
        withContext(dispatchers.io) {
            var pageOffset = 0
            do {
                val canLoadMore = fetchVariationsPage(remoteProductId, pageOffset, VariationSorting.ID)
                    .getOrElse { return@withContext Result.failure(it) }
                pageOffset += PRODUCT_VARIATIONS_PAGE_SIZE
            } while (canLoadMore)
            Result.success(getProductVariationList(remoteProductId))
        }

    private suspend fun fetchVariationsPage(
        remoteProductId: Long,
        pageOffset: Int,
        sorting: VariationSorting
    ): Result<Boolean> {
        val payload = WCProductStore.FetchProductVariationsPayload(
            selectedSite.get(),
            remoteProductId,
            pageSize = PRODUCT_VARIATIONS_PAGE_SIZE,
            offset = pageOffset,
            sorting = sorting
        )
        val result = productStore.fetchProductVariations(payload)

        if (result.isError) {
            AnalyticsTracker.track(
                AnalyticsEvent.PRODUCT_VARIANTS_LOAD_ERROR,
                this.javaClass.simpleName,
                result.error.type.toString(),
                result.error.message
            )
            return Result.failure(WooException(result.error))
        }

        AnalyticsTracker.track(AnalyticsEvent.PRODUCT_VARIANTS_LOADED)
        return Result.success(requireNotNull(result.model).canLoadMore)
    }

    /**
     * Returns all product variations for a product and current site that are in the database
     */
    suspend fun getProductVariationList(remoteProductId: Long): List<ProductVariation> {
        val product = productStore.getProductByRemoteId(selectedSite.get(), remoteProductId)
        return productStore.getVariationsForProduct(selectedSite.get(), remoteProductId)
            .map {
                if (product?.type == ProductType.VARIABLE_SUBSCRIPTION.value) {
                    SubscriptionProductVariation(it)
                } else {
                    it.toAppModel()
                }
            }
            .sorted()
    }

    /**
     * Returns the currency code for the site
     */
    suspend fun getCurrencyCode() = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode

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
        stockQuantity: Double? = null
    ): Boolean {
        val payloadBuilder = BatchUpdateVariationsPayload.Builder(selectedSite.get(), remoteProductId, variationIds)
        if (newRegularPrice != null) payloadBuilder.regularPrice(newRegularPrice)
        if (newSalePrice != null) payloadBuilder.salePrice(newSalePrice)
        if (stockQuantity != null) payloadBuilder.stockQuantity(stockQuantity.toInt())
        val result = productStore.batchUpdateVariations(payloadBuilder.build())
        return !result.isError
    }

    suspend fun bulkCreateVariations(
        remoteProductId: Long,
        variationCandidates: List<VariationCandidate>
    ): RequestResult = withContext(dispatchers.io) {
        productStore.batchGenerateVariations(
            WCProductStore.BatchGenerateVariationsPayload(
                selectedSite.get(),
                remoteProductId,
                variations = variationCandidates.map { candidate ->
                    candidate.map { variantOption ->
                        variantOption.asSourceModel()
                    }
                }
            )
        ).let {
            if (it.isError) {
                RequestResult.ERROR
            } else {
                RequestResult.SUCCESS
            }
        }
    }

    suspend fun createVariations(
        remoteProductId: Long,
        variations: List<ProductVariation>
    ): Result<Unit> = withContext(dispatchers.io) {
        productStore.createVariations(
            selectedSite.get(),
            productId = LocalOrRemoteId.RemoteId(remoteProductId),
            variations = variations.map {
                it.toDataModel()
            }
        ).let {
            if (it.isError) {
                Result.failure(WooException(it.error))
            } else {
                Result.success(Unit)
            }
        }
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
