package com.woocommerce.android.ui.orders.creation.products

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductListRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class OrderCreationProductSelectionViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
    private val productListRepository: ProductListRepository
) : ScopedViewModel(savedState) {
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    private val productList = MutableLiveData<List<Product>>()
    val productListData: LiveData<List<Product>> = productList

    init {
        fetchProductList()
    }

    fun fetchProductList(
        loadMore: Boolean = false
    ) {
        viewState = viewState.copy(isSkeletonShown = true)
        /**
         * We will probably want to improve this call to check if the product list
         * is already available on database before relying directly on this call
         */
        launch {
            productList.value = productListRepository.fetchProductList(loadMore)
            viewState = viewState.copy(isSkeletonShown = false)
        }
    }

    @Parcelize
    data class ViewState(
        val isSkeletonShown: Boolean? = null
    ) : Parcelable
}
