package com.woocommerce.android.ui.products

import android.content.DialogInterface
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsTracker
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
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDiscardDialog
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

    private val _productList = MutableLiveData<List<Product>>()
    val productList: LiveData<List<Product>> = _productList

    final val productListViewStateData =
        LiveDataDelegate(savedState, GroupedProductListViewState(getOriginalGroupedProductIds()))
    private var productListViewState by productListViewStateData

    private val selectedGroupedProductIds
        get() = productListViewState.selectedGroupedProductIds

    val hasChanges: Boolean
        get() = selectedGroupedProductIds != getOriginalGroupedProductIds()

    override fun onCleared() {
        super.onCleared()
        groupedProductListRepository.onCleanup()
    }

    init {
        if (_productList.value == null) {
            loadGroupedProducts()
        }
    }

    fun onGroupedProductsAdded(selectedProductIds: List<Long>) {
        productListViewState = productListViewState.copy(
            selectedGroupedProductIds = selectedGroupedProductIds + selectedProductIds
        )
        AnalyticsTracker.track(Stat.GROUPED_PRODUCT_LINKED_PRODUCTS_ADDED)
        updateGroupedProductList()
    }

    fun onGroupedProductDeleted(product: Product) {
        productListViewState = productListViewState.copy(
            selectedGroupedProductIds = selectedGroupedProductIds - product.remoteId
        )
        AnalyticsTracker.track(Stat.GROUPED_PRODUCT_LINKED_PRODUCTS_DELETE_TAPPED)
        updateGroupedProductList()
    }

    private fun updateGroupedProductList() {
        _productList.value = groupedProductListRepository.getGroupedProductList(selectedGroupedProductIds)
        productListViewState = productListViewState.copy(isDoneButtonVisible = hasChanges)
    }

    fun onAddProductButtonClicked() {
        AnalyticsTracker.track(Stat.GROUPED_PRODUCT_LINKED_PRODUCTS_ADD_TAPPED)
        triggerEvent(ViewProductSelectionList(navArgs.remoteProductId))
    }

    fun onDoneButtonClicked() {
        AnalyticsTracker.track(Stat.GROUPED_PRODUCT_LINKED_PRODUCTS_DONE_BUTTON_TAPPED, mapOf(
            AnalyticsTracker.KEY_HAS_CHANGED_DATA to hasChanges
        ))
        triggerEvent(ExitWithResult(selectedGroupedProductIds))
    }

    fun onBackButtonClicked(): Boolean {
        return if (hasChanges) {
            triggerEvent(ShowDiscardDialog(
                negativeButtonId = string.keep_changes,
                positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                    triggerEvent(Exit)
                }
            ))
            false
        } else {
            true
        }
    }

    private fun loadGroupedProducts(loadMore: Boolean = false) {
        val productsInDb = groupedProductListRepository.getGroupedProductList(
            selectedGroupedProductIds
        )
        if (productsInDb.isNotEmpty()) {
            _productList.value = productsInDb
            productListViewState = productListViewState.copy(isSkeletonShown = false)
        } else {
            productListViewState = productListViewState.copy(isSkeletonShown = true)
        }

        launch { fetchGroupedProducts(selectedGroupedProductIds, loadMore = loadMore) }
    }

    private suspend fun fetchGroupedProducts(
        groupedProductIds: List<Long>,
        loadMore: Boolean
    ) {
        if (networkStatus.isConnected()) {
            _productList.value = groupedProductListRepository.fetchGroupedProductList(
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

    private fun getOriginalGroupedProductIds() = navArgs.groupedProductIds.split(",").map { it.toLong() }

    @Parcelize
    data class GroupedProductListViewState(
        val selectedGroupedProductIds: List<Long>,
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
