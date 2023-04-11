package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Product
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ProductBundleViewModel @Inject constructor(
    savedState: SavedStateHandle,
    getProductByIds: GetProductByIds
) : ScopedViewModel(savedState) {
    private val navArgs: ProductBundleFragmentArgs by savedState.navArgs()

    private val productIds = navArgs.bundledProductsIds.toList()

    private val _productList = MutableLiveData<List<Product>>()
    val productList: LiveData<List<Product>> = _productList

    val productListViewStateData = LiveDataDelegate(savedState, BundledProductListViewState(isSkeletonShown = true))
    private var productListViewState by productListViewStateData

    init {
        launch {
            getProductByIds(productIds)
                .distinctUntilChanged()
                .collect { status ->
                if (status is GetProductByIds.GetProductsStatus.Succeeded) {
                    _productList.value = status.products
                }
                productListViewState =
                    productListViewState.copy(isSkeletonShown = status is GetProductByIds.GetProductsStatus.Loading)
            }
        }
    }

    @Parcelize
    data class BundledProductListViewState(val isSkeletonShown: Boolean? = null) : Parcelable
}
