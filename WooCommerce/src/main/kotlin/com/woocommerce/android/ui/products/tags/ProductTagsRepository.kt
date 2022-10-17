package com.woocommerce.android.ui.products.tags

import com.woocommerce.android.AppConstants
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.ProductTag
import com.woocommerce.android.model.toProductTag
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.ContinuationWrapper
import com.woocommerce.android.util.WooLog
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction.ADDED_PRODUCT_TAGS
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCT_TAGS
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

    private var loadContinuation = ContinuationWrapper<Boolean>(WooLog.T.PRODUCTS)
    private var addProductTagsContinuation = ContinuationWrapper<Boolean>(WooLog.T.PRODUCTS)
    private var offset = 0

    var canLoadMoreProductTags = true

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    /**
     * Submits a fetch request to get a list of products tags for the current site
     * and returns the full list of product tags from the database
     */
    suspend fun fetchProductTags(loadMore: Boolean = false, searchQuery: String? = null): List<ProductTag> {
        loadContinuation.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {
            offset = if (loadMore) offset + PRODUCT_TAGS_PAGE_SIZE else 0
            val payload = FetchProductTagsPayload(
                selectedSite.get(),
                pageSize = PRODUCT_TAGS_PAGE_SIZE,
                offset = offset,
                searchQuery = searchQuery
            )
            dispatcher.dispatch(WCProductActionBuilder.newFetchProductTagsAction(payload))
        }

        return getProductTags()
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
    suspend fun addProductTags(tagNames: List<String>): List<ProductTag> {
        addProductTagsContinuation.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {
            val payload = WCProductStore.AddProductTagsPayload(selectedSite.get(), tagNames)
            dispatcher.dispatch(WCProductActionBuilder.newAddProductTagsAction(payload))
        }
        return getProductTagsByNames(tagNames)
    }

    fun getProductTagsByNames(names: List<String>) =
        productStore.getProductTagsByNames(selectedSite.get(), names).map { it.toProductTag() }

    fun getProductTagByName(name: String) =
        productStore.getProductTagByName(selectedSite.get(), name)?.toProductTag()

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductTagsChanged(event: OnProductTagChanged) {
        when (event.causeOfChange) {
            FETCH_PRODUCT_TAGS -> {
                if (event.isError) {
                    loadContinuation.continueWith(false)
                    AnalyticsTracker.track(
                        AnalyticsEvent.PRODUCT_TAGS_LOAD_FAILED,
                        this.javaClass.simpleName,
                        event.error.type.toString(),
                        event.error.message
                    )
                } else {
                    canLoadMoreProductTags = event.canLoadMore
                    AnalyticsTracker.track(AnalyticsEvent.PRODUCT_TAGS_LOADED)
                    loadContinuation.continueWith(true)
                }
            }
            ADDED_PRODUCT_TAGS -> {
                // No need to handle errors because errors are currently handled by `OrderListViewModel`.
                addProductTagsContinuation.continueWith(false)
            }
            else -> {
            }
        }
    }
}
