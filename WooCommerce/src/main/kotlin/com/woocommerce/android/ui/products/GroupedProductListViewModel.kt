package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.assisted.AssistedFactory
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.ConnectedProductsListAction
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_CONNECTED_PRODUCTS_LIST_ACTION
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_CONNECTED_PRODUCTS_LIST_CONTEXT
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductSelectionList
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch

@OpenClassOnDebug
class GroupedProductListViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val networkStatus: NetworkStatus,
    private val groupedProductListRepository: GroupedProductListRepository
) : ScopedViewModel(savedState, dispatchers) {
    private val navArgs: GroupedProductListFragmentArgs by savedState.navArgs()

    private val originalProductIds = navArgs.productIds.toList()

    private val _productList = MutableLiveData<List<Product>>()
    val productList: LiveData<List<Product>> = _productList

    final val productListViewStateData =
        LiveDataDelegate(savedState, GroupedProductListViewState(originalProductIds))
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
    }

    fun getKeyForGroupedProductListType() = groupedProductListType.resultKey

    fun onProductsAdded(selectedProductIds: List<Long>) {
        // ignore already added products
        val uniqueSelectedProductIds = selectedProductIds.minus(this.selectedProductIds)
        // TODO handle linked products
        productListViewState = productListViewState.copy(
            selectedProductIds = this.selectedProductIds + uniqueSelectedProductIds
        )
        track(ConnectedProductsListAction.ADDED)
        updateProductList()
    }

    fun onProductDeleted(product: Product) {
        // TODO handle linked products
        productListViewState = productListViewState.copy(
            selectedProductIds = selectedProductIds - product.remoteId
        )
        track(ConnectedProductsListAction.DELETE_TAPPED)
        updateProductList()
    }

    private fun updateProductList() {
        _productList.value = if (selectedProductIds.isNotEmpty()) {
            groupedProductListRepository.getProductList(selectedProductIds)
        } else emptyList()
    }

    fun onAddProductButtonClicked() {
        track(ConnectedProductsListAction.ADD_TAPPED)
        triggerEvent(
            ViewProductSelectionList(
                navArgs.remoteProductId,
                navArgs.groupedProductListType,
                excludedProductIds = selectedProductIds
            )
        )
    }

    fun onBackButtonClicked(): Boolean {
        if (hasChanges) {
            triggerEvent(ExitWithResult(selectedProductIds))
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
            triggerEvent(ShowSnackbar(string.offline_error))
        }

        productListViewState = productListViewState.copy(
            isSkeletonShown = false,
            isLoadingMore = false
        )
    }

    private fun track(action: ConnectedProductsListAction) {
        AnalyticsTracker.track(
            Stat.CONNECTED_PRODUCTS_LIST,
            mapOf(
                KEY_CONNECTED_PRODUCTS_LIST_CONTEXT to groupedProductListType.statContext.value,
                KEY_CONNECTED_PRODUCTS_LIST_ACTION to action.value
            ))
    }

    @Parcelize
    data class GroupedProductListViewState(
        val selectedProductIds: List<Long>,
        val isSkeletonShown: Boolean? = null,
        val isLoadingMore: Boolean? = null
    ) : Parcelable {
        val isAddProductButtonVisible: Boolean
            get() = isSkeletonShown == false
    }
    @AssistedFactory
    interface Factory : ViewModelAssistedFactory<GroupedProductListViewModel>
}
