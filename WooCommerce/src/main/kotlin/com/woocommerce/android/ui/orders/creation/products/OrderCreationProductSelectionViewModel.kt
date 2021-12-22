package com.woocommerce.android.ui.orders.creation.products

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductListRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderCreationProductSelectionViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
    private val productListRepository: ProductListRepository
) : ScopedViewModel(savedState) {
    private val productList = MutableLiveData<List<Product>>()
    val productListData: LiveData<List<Product>> = productList

    init {
        fetchProductList()
    }

    fun fetchProductList(
        loadMore: Boolean = false
    ) {
        launch {
            productList.value = productListRepository.fetchProductList(loadMore)
        }
    }
}
