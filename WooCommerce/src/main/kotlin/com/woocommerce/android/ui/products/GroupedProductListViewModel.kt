package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

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
        LiveDataDelegate(savedState, GroupedProductListViewState(originalProductIds, originalProductIds))
    private var productListViewState by productListViewStateData

    private val selectedProductIds
        get() = productListViewState.selectedProductIds

    // Used to differentiate whether ActionMode done button is clicked or the back button in ActionMode
    // Since onDestroyActionMode is called for both
    var isActionModeClicked: Boolean = false

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

    fun getSelectedProductIdsList() = productListViewState.selectedProductIds

    fun onProductsAdded(selectedProductIds: List<Long>) {
        // ignore already added products
        val uniqueSelectedProductIds = selectedProductIds.minus(this.selectedProductIds)
        // TODO handle linked products
        val totalProductsList = this.selectedProductIds + uniqueSelectedProductIds
        productListViewState = productListViewState.copy(
            selectedProductIds = totalProductsList,
            previouslySelectedProductIds = totalProductsList
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

    /**
     * Helps in restoring to previous state of product list if user
     * clicks on back button while in edit mode. All of the operations
     * done in edit mode will be discarded
     **/
    fun restorePreviousProductList() {
        productListViewState = productListViewState.copy(
            selectedProductIds = productListViewState.previouslySelectedProductIds
        )
        updateProductList()
    }

    /**
     * Helps in maintaining the position of the products in list
     * on orientation change, after drag-and-drop
     **/
    fun updateReOrderedProductList(reorderedProductList: List<Product>) {
        productListViewState = productListViewState.copy(
            selectedProductIds = reorderedProductList.map { it.remoteId }
        )
        _productList.value = if (reorderedProductList.isNotEmpty()) {
            reorderedProductList
        } else emptyList()
    }

    fun updatePreviouslySelectedProductIds() {
        productListViewState.previouslySelectedProductIds = selectedProductIds
    }

    fun setEditMode(mode: Boolean) {
        productListViewState.isEditMode = mode
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
                // sort the product list fetched from the database to match the selectedProductIds.
                // This is done to retain the drag&drop order.
                _productList.value = productsInDb.sortedBy {
                    selectedProductIds.indexOf(it.remoteId)
                }
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
            // sort the product list fetched from the network to match the selectedProductIds.
            // This is done to retain the drag&drop order.
            _productList.value = groupedProductListRepository.fetchProductList(
                groupedProductIds, loadMore
            ).sortedBy {
                selectedProductIds.indexOf(it.remoteId)
            }
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
        var previouslySelectedProductIds: List<Long>,
        var isEditMode: Boolean = false, // Indicates whether the screen is in edit mode or not
        val isSkeletonShown: Boolean? = null,
        val isLoadingMore: Boolean? = null
    ) : Parcelable {
        val isAddProductButtonVisible: Boolean
            get() = isSkeletonShown == false
    }
    @AssistedFactory
    interface Factory : ViewModelAssistedFactory<GroupedProductListViewModel>
}
