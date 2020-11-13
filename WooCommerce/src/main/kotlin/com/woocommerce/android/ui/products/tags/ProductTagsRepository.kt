package com.woocommerce.android.ui.products.tags

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.ProductTag
import com.woocommerce.android.model.toProductTag
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.suspendCancellableCoroutineWithTimeout
import com.woocommerce.android.util.suspendCoroutineWithTimeout
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCT_TAGS
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductTagsPayload
import org.wordpress.android.fluxc.store.WCProductStore.OnProductTagChanged
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

@OpenClassOnDebug
class ProductTagsRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val ACTION_TIMEOUT = 10L * 1000
        private const val PRODUCT_TAGS_PAGE_SIZE = WCProductStore.DEFAULT_PRODUCT_TAGS_PAGE_SIZE
    }

    private var loadContinuation: CancellableContinuation<Boolean>? = null
    private var addProductTagsContinuation: Continuation<Boolean>? = null
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
        try {
            loadContinuation?.cancel()
            suspendCancellableCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                offset = if (loadMore) offset + PRODUCT_TAGS_PAGE_SIZE else 0
                loadContinuation = it
                val payload = FetchProductTagsPayload(
                    selectedSite.get(),
                    pageSize = PRODUCT_TAGS_PAGE_SIZE,
                    offset = offset,
                    searchQuery = searchQuery
                )
                dispatcher.dispatch(WCProductActionBuilder.newFetchProductTagsAction(payload))
            }
        } catch (e: CancellationException) {
            WooLog.e(WooLog.T.PRODUCTS, "CancellationException while fetching product tags", e)
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
        try {
            suspendCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                addProductTagsContinuation = it

                val payload = WCProductStore.AddProductTagsPayload(selectedSite.get(), tagNames)
                dispatcher.dispatch(WCProductActionBuilder.newAddProductTagsAction(payload))
            }
        } catch (e: CancellationException) {
            WooLog.e(
                WooLog.T.PRODUCTS,
                "CancellationException while adding product tags: $tagNames", e
            )
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
                    loadContinuation?.resume(false)
                    AnalyticsTracker.track(
                        Stat.PRODUCT_TAGS_LOAD_FAILED,
                        this.javaClass.simpleName,
                        event.error.type.toString(),
                        event.error.message
                    )
                } else {
                    canLoadMoreProductTags = event.canLoadMore
                    AnalyticsTracker.track(Stat.PRODUCT_TAGS_LOADED)
                    loadContinuation?.resume(true)
                }
                loadContinuation = null
            }
            else -> { }
        }
    }
}
