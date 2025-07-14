package com.woocommerce.android.ui.products.grouped

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.products.ProductNavigationTarget
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class GroupedProductListViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val networkStatus: NetworkStatus,
    private val groupedProductListRepository: GroupedProductListRepository
) : ScopedViewModel(savedState) {
    private val navArgs: GroupedProductListFragmentArgs by savedState.navArgs()

    private val originalProductIds = navArgs.productIds.toList()

    private val _productList = MutableLiveData<List<Product>>()
    val productList: LiveData<List<Product>> = _productList

    /**
     * Saving more data than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can replace @Suppress("OPT_IN_USAGE")
     * with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val productListViewStateData = LiveDataDelegate(savedState, GroupedProductListViewState(originalProductIds))
    private var productListViewState by productListViewStateData

    private val selectedProductIds
        get() = productListViewState.selectedProductIds

    val groupedProductListType
        get() = navArgs.groupedProductListType

    val hasChanges: Boolean
        get() = selectedProductIds != originalProductIds

    override fun onCleared() {
        super.onCleared()
        groupedProductListRepository.onCleanup()
    }

    init {
        if (_productList.value == null) {
            loadProducts()
        }
        productListViewState = productListViewState.copy(
            isEmptyViewShown = navArgs.productIds.isEmpty() &&
                selectedProductIds.isEmpty()
        )
    }

    fun getKeyForGroupedProductListType() = groupedProductListType.resultKey

    fun onProductsAdded(selectedProductIds: List<Long>) {
        // ignore already added products
        val uniqueSelectedProductIds = selectedProductIds.minus(this.selectedProductIds)
        productListViewState = productListViewState.copy(
            selectedProductIds = this.selectedProductIds + uniqueSelectedProductIds
        )
        track(AnalyticsTracker.Companion.ConnectedProductsListAction.ADDED)
        updateProductList()
    }

    fun onProductDeleted(product: Product) {
        productListViewState = productListViewState.copy(
            selectedProductIds = selectedProductIds - product.remoteId
        )
        track(AnalyticsTracker.Companion.ConnectedProductsListAction.DELETE_TAPPED)
        updateProductList()
    }

    private fun updateProductList() {
        _productList.value = if (selectedProductIds.isNotEmpty()) {
            groupedProductListRepository.getProductList(selectedProductIds)
        } else {
            emptyList()
        }

        productListViewState = productListViewState.copy(
            isEmptyViewShown = _productList.value?.isEmpty() ?: true
        )
    }

    fun onAddProductButtonClicked() {
        track(AnalyticsTracker.Companion.ConnectedProductsListAction.ADD_TAPPED)
        triggerEvent(
            ProductNavigationTarget.ViewProductSelectionList(
                navArgs.remoteProductId,
                navArgs.groupedProductListType,
                excludedProductIds = selectedProductIds
            )
        )
    }

    fun onBackButtonClicked(): Boolean {
        if (hasChanges) {
            triggerEvent(MultiLiveEvent.Event.ExitWithResult(selectedProductIds))
            return false
        }
        return true
    }

    private fun loadProducts(loadMore: Boolean = false) {
        if (selectedProductIds.isEmpty()) {
            _productList.value = emptyList()
            productListViewState = productListViewState.copy(isSkeletonShown = false)
        } else {
            val productsInDb = groupedProductListRepository.getProductList(
                selectedProductIds
            )
            if (productsInDb.isNotEmpty()) {
                _productList.value = productsInDb
                productListViewState = productListViewState.copy(isSkeletonShown = false)
            } else {
                productListViewState = productListViewState.copy(isSkeletonShown = true)
            }

            launch { fetchProducts(selectedProductIds, loadMore = loadMore) }
        }
    }

    private suspend fun fetchProducts(
        groupedProductIds: List<Long>,
        loadMore: Boolean
    ) {
        if (networkStatus.isConnected()) {
            _productList.value = groupedProductListRepository.fetchProductList(
                groupedProductIds, loadMore
            )
        } else {
            // Network is not connected
            triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.offline_error))
        }

        productListViewState = productListViewState.copy(
            isSkeletonShown = false,
            isLoadingMore = false,
            isEmptyViewShown = _productList.value?.isEmpty() ?: true
        )
    }

    private fun track(action: AnalyticsTracker.Companion.ConnectedProductsListAction) {
        AnalyticsTracker.track(
            AnalyticsEvent.CONNECTED_PRODUCTS_LIST,
            mapOf(
                AnalyticsTracker.KEY_CONNECTED_PRODUCTS_LIST_CONTEXT to groupedProductListType.statContext.value,
                AnalyticsTracker.KEY_CONNECTED_PRODUCTS_LIST_ACTION to action.value
            )
        )
    }

    @Parcelize
    data class GroupedProductListViewState(
        val selectedProductIds: List<Long>,
        val isSkeletonShown: Boolean? = null,
        val isLoadingMore: Boolean? = null,
        val isEmptyViewShown: Boolean? = null
    ) : Parcelable {
        val isAddProductButtonVisible: Boolean
            get() = isSkeletonShown == false
    }
}
