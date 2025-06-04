package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.AppConstants
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.ContinuationWrapper
import com.woocommerce.android.util.WooLog
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

internal class SearchException(val error: WooPosSearchByIdentifierResult.Error) : Exception()

class WooPosSearchByIdentifierRemote @Inject constructor(
    private val dispatcher: Dispatcher,
    private val selectedSite: SelectedSite
) {
    private var searchBySKUContinuation = ContinuationWrapper<List<Product>>(WooLog.T.PRODUCTS)
    private var searchByGlobalUniqueIdContinuation = ContinuationWrapper<List<Product>>(WooLog.T.PRODUCTS)

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    suspend fun searchProductsBySku(sku: String): Result<List<Product>> {
        val result = searchBySKUContinuation.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {
            val payload = WCProductStore.SearchProductsPayload(
                site = selectedSite.get(),
                searchQuery = sku,
                skuSearchOptions = WCProductStore.SkuSearchOptions.ExactSearch,
                pageSize = WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE,
                offset = 0,
                sorting = WCProductStore.ProductSorting.TITLE_ASC,
                excludedProductIds = null,
                filterOptions = emptyMap()
            )
            dispatcher.dispatch(WCProductActionBuilder.newSearchProductsAction(payload))
        }

        return when (result) {
            is ContinuationWrapper.ContinuationResult.Cancellation ->
                Result.failure(SearchException(WooPosSearchByIdentifierResult.Error.RequestCancelled))
            is ContinuationWrapper.ContinuationResult.Success ->
                Result.success(result.value)
        }
    }

    suspend fun searchProductsByGlobalUniqueId(globalUniqueId: String): Result<List<Product>> {
        val result = searchByGlobalUniqueIdContinuation.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {
            val payload = WCProductStore.SearchProductsByGlobalUniqueIdPayload(
                site = selectedSite.get(),
                globalUniqueId = globalUniqueId,
                pageSize = WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE,
                offset = 0,
                sorting = WCProductStore.ProductSorting.TITLE_ASC,
                excludedProductIds = null,
                filterOptions = emptyMap()
            )
            dispatcher.dispatch(WCProductActionBuilder.newSearchProductsByGlobalUniqueIdAction(payload))
        }

        return when (result) {
            is ContinuationWrapper.ContinuationResult.Cancellation ->
                Result.failure(SearchException(WooPosSearchByIdentifierResult.Error.RequestCancelled))
            is ContinuationWrapper.ContinuationResult.Success ->
                Result.success(result.value)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductsSearched(event: WCProductStore.OnProductsSearched) {
        val continuation = if (event.globalUniqueIdSearchQuery != null) {
            searchByGlobalUniqueIdContinuation
        } else {
            searchBySKUContinuation
        }

        if (event.isError) {
            continuation.continueWith(emptyList())
        } else {
            val products = event.searchResults.map { it.toAppModel() }
            continuation.continueWith(products)
        }
    }
}
