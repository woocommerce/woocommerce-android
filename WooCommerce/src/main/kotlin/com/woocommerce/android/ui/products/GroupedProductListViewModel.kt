package com.woocommerce.android.ui.products

import android.content.DialogInterface
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
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
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
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

    private val originalProductIds =
        navArgs.productIds
            .takeIf { it.isNotEmpty() }
            ?.split(",")
            ?.mapNotNull { it.toLongOrNull() }
            .orEmpty()

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
        AnalyticsTracker.track(
            Stat.CONNECTED_PRODUCTS_LIST,
            mapOf(
                KEY_CONNECTED_PRODUCTS_LIST_CONTEXT to groupedProductListType.statContext.value,
                KEY_CONNECTED_PRODUCTS_LIST_ACTION to ConnectedProductsListAction.ADDED.value
            ))
        updateProductList()
    }

    fun onProductDeleted(product: Product) {
        // TODO handle linked products
        productListViewState = productListViewState.copy(
            selectedProductIds = selectedProductIds - product.remoteId
        )
        AnalyticsTracker.track(Stat.GROUPED_PRODUCT_LINKED_PRODUCTS_DELETE_TAPPED)
        updateProductList()
    }

    private fun updateProductList() {
        _productList.value = if (selectedProductIds.isNotEmpty()) {
            groupedProductListRepository.getProductList(selectedProductIds)
        } else emptyList()
        productListViewState = productListViewState.copy(isDoneButtonVisible = hasChanges)
    }

    fun onAddProductButtonClicked() {
        AnalyticsTracker.track(Stat.GROUPED_PRODUCT_LINKED_PRODUCTS_ADD_TAPPED)
        triggerEvent(ViewProductSelectionList(
            navArgs.remoteProductId,
            navArgs.groupedProductListType,
            excludedProductIds = selectedProductIds)
        )
    }

    fun onDoneButtonClicked() {
        AnalyticsTracker.track(Stat.GROUPED_PRODUCT_LINKED_PRODUCTS_DONE_BUTTON_TAPPED, mapOf(
            AnalyticsTracker.KEY_HAS_CHANGED_DATA to hasChanges
        ))
        triggerEvent(ExitWithResult(selectedProductIds))
    }

    fun onBackButtonClicked(): Boolean {
        return if (hasChanges) {
            triggerEvent(ShowDialog.buildDiscardDialogEvent(
                positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                    triggerEvent(Exit)
                },
                negativeButtonId = string.keep_changes
            ))
            false
        } else {
            true
        }
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

    @Parcelize
    data class GroupedProductListViewState(
        val selectedProductIds: List<Long>,
        val isSkeletonShown: Boolean? = null,
        val isLoadingMore: Boolean? = null,
        val isDoneButtonVisible: Boolean? = null
    ) : Parcelable {
        val isAddProductButtonVisible: Boolean
            get() = isSkeletonShown == false
    }
    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<GroupedProductListViewModel>
}
