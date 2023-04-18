package com.woocommerce.android.ui.orders.creation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductListRepository
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BarcodeScanningViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val productRepository: ProductListRepository,
) : ScopedViewModel(savedState) {

    private val scannedProductItems: MutableList<ProductSelectorViewModel.SelectedItem> = mutableListOf()
    private val scannedProductSKU: MutableList<String> = mutableListOf()
    private val _viewState: MutableLiveData<ViewState> = MutableLiveData<ViewState>()
    val viewState: LiveData<ViewState> = _viewState

    init {
        _viewState.value = ViewState(
            scannedProductItems = mutableListOf(),
            currentScannedProduct = null
        )
    }

    fun fetchProductBySKU(sku: String) {
        if (sku !in scannedProductSKU && _viewState.value?.isScanningInProgress == false) {
            _viewState.value = _viewState.value!!.copy(
                isScanningInProgress = true
            )
            Log.d("ABCD", "SKU not already scanned: $sku")
            viewModelScope.launch {
                productRepository.searchProductList(
                    searchQuery = sku,
                    isSkuSearch = true,
                )?.let { products ->
                    _viewState.value = _viewState.value!!.copy(
                        isScanningInProgress = false
                    )
                    scannedProductSKU.clear()
                    scannedProductSKU.add(sku)
                    products.firstOrNull()?.let { product ->
                        Log.d("ABCD", "Scanned Product: ${product.name}")
                        _viewState.value = ViewState(
                            scannedProductItems = scannedProductItems,
                            currentScannedProduct = product
                        )
                    }
                }
            }
        }
    }

    fun addToCartClick() {
        if (_viewState.value?.currentScannedProduct?.type?.equals("variation", ignoreCase = true) == true) {
            scannedProductItems.add(
                ProductSelectorViewModel.SelectedItem.ProductVariation(
                    productId = _viewState.value?.currentScannedProduct!!.parentId,
                    variationId = _viewState.value?.currentScannedProduct!!.remoteId
                )
            )
        } else {
            scannedProductItems.add(
                ProductSelectorViewModel.SelectedItem.Product(productId = _viewState.value?.currentScannedProduct!!.remoteId)
            )
        }
    }

    fun onDoneClick() {
        triggerEvent(ScannedItems(scannedProductItems))
    }

    data class ScannedItems(
        val selectedItems: List<ProductSelectorViewModel.SelectedItem>,
    ) : MultiLiveEvent.Event()
}

data class ViewState(
    val scannedProductItems: MutableList<ProductSelectorViewModel.SelectedItem>,
    val currentScannedProduct: Product?,
    var isScanningInProgress: Boolean = false
)
