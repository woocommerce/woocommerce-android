package com.woocommerce.android.ui.products

import android.content.DialogInterface
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R.string
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.extensions.getList
import com.woocommerce.android.extensions.removeItem
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
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

    private val _productList = MutableLiveData<MutableList<Product>>()
    val productList: LiveData<MutableList<Product>> = _productList

    final val productListViewStateData = LiveDataDelegate(savedState, GroupedProductListViewState())
    private var productListViewState by productListViewStateData

    override fun onCleared() {
        super.onCleared()
        groupedProductListRepository.onCleanup()
    }

    init {
        if (_productList.value == null) {
            loadGroupedProducts()
        }
    }

    fun onGroupedProductDeleted(product: Product) {
        val oldProductListSize = _productList.getList().size
        _productList.getList()
            .firstOrNull { it.remoteId == product.remoteId }
            ?.let { _productList.removeItem(it) }

        productListViewState = productListViewState.copy(
            hasChanges = oldProductListSize != _productList.getList().size
        )
    }

    fun onDoneButtonClicked() {
        triggerEvent(ExitWithResult(_productList.getList().map { it.remoteId }))
    }

    fun onBackButtonClicked(): Boolean {
        return if (productListViewState.hasChanges == true) {
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
        val groupedProductIds = getGroupedProductIds()
        val productsInDb = groupedProductListRepository.getGroupedProductList(groupedProductIds)
        if (productsInDb.isNotEmpty()) {
            _productList.value = productsInDb.toMutableList()
            productListViewState = productListViewState.copy(isSkeletonShown = false)
        } else {
            productListViewState = productListViewState.copy(isSkeletonShown = true)
        }

        launch { fetchGroupedProducts(groupedProductIds, loadMore = loadMore) }
    }

    private suspend fun fetchGroupedProducts(
        groupedProductIds: List<Long>,
        loadMore: Boolean
    ) {
        if (networkStatus.isConnected()) {
            _productList.value = groupedProductListRepository.fetchGroupedProductList(
                groupedProductIds, loadMore
            ).toMutableList()
        } else {
            // Network is not connected
            triggerEvent(ShowSnackbar(string.offline_error))
        }

        productListViewState = productListViewState.copy(
            isSkeletonShown = false,
            isLoadingMore = false
        )
    }

    private fun getGroupedProductIds() = navArgs.groupedProductIds.split(",").map { it.toLong() }

    @Parcelize
    data class GroupedProductListViewState(
        val isSkeletonShown: Boolean? = null,
        val isLoadingMore: Boolean? = null,
        val hasChanges: Boolean? = null
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<GroupedProductListViewModel>
}
