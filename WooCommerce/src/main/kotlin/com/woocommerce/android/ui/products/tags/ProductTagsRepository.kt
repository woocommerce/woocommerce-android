package com.woocommerce.android.ui.products.tags

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.ProductTag
import com.woocommerce.android.model.toProductTag
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.dispatchAndAwait
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductTagsPayload
import org.wordpress.android.fluxc.store.WCProductStore.OnProductTagChanged
import javax.inject.Inject

class ProductTagsRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val PRODUCT_TAGS_PAGE_SIZE = WCProductStore.DEFAULT_PRODUCT_TAGS_PAGE_SIZE
    }

    private var offset = 0

    var canLoadMoreProductTags = true

    /**
     * Submits a fetch request to get a list of products tags for the current site
     * and returns the full list of product tags from the database
     */
    suspend fun fetchProductTags(loadMore: Boolean = false, searchQuery: String? = null): Result<List<ProductTag>> {
        offset = if (loadMore) offset + PRODUCT_TAGS_PAGE_SIZE else 0
        val payload = FetchProductTagsPayload(
            selectedSite.get(),
            pageSize = PRODUCT_TAGS_PAGE_SIZE,
            offset = offset,
            searchQuery = searchQuery
        )
        val action = WCProductActionBuilder.newFetchProductTagsAction(payload)
        val result: OnProductTagChanged = dispatcher.dispatchAndAwait(action)

        return if (result.isError) {
            AnalyticsTracker.track(
                AnalyticsEvent.PRODUCT_TAGS_LOAD_FAILED,
                this.javaClass.simpleName,
                result.error.type.toString(),
                result.error.message
            )
            Result.failure(OnChangedException(result.error, result.error.message))
        } else {
            AnalyticsTracker.track(AnalyticsEvent.PRODUCT_TAGS_LOADED)
            Result.success(getProductTags())
        }
    }

    /**
     * Returns all product tags for the current site that are in the database
     */
    fun getProductTags(): List<ProductTag> {
        return productStore.getTagsForSite(selectedSite.get())
            .map { it.toProductTag() }
    }

    /**
     * Fires the request to add a new product tags
     *
     * @return the result of the action as a [Boolean]
     */
    suspend fun addProductTags(tagNames: List<String>): Result<List<ProductTag>> {
        val payload = WCProductStore.AddProductTagsPayload(selectedSite.get(), tagNames)
        val action = WCProductActionBuilder.newAddProductTagsAction(payload)
        val result: OnProductTagChanged = dispatcher.dispatchAndAwait(action)

        return when {
            result.isError -> Result.failure(OnChangedException(result.error, result.error.message))
            else -> Result.success(getProductTagsByNames(tagNames))
        }
    }

    fun getProductTagsByNames(names: List<String>) =
        productStore.getProductTagsByNames(selectedSite.get(), names).map { it.toProductTag() }

    fun getProductTagByName(name: String) =
        productStore.getProductTagByName(selectedSite.get(), name)?.toProductTag()
}
