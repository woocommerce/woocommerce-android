package com.woocommerce.android.ui.blaze.creation.ad

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ProductImagePickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productRepository: ProductDetailRepository,
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: ProductImagePickerFragmentArgs by savedStateHandle.navArgs()

    private val _viewState = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = ViewState(emptyList())
    )
    val viewState = _viewState.asLiveData()

    init {
        launch {
            val product = productRepository.getProduct(navArgs.productId)
            product?.let {
                _viewState.update { it.copy(productImages = product.images) }
            }
        }
    }

    fun onImageSelected(productImage: Product.Image) {
        triggerEvent(
            ExitWithResult(
                ImageSelectedResult(productImage = productImage)
            )
        )
    }

    fun onBackButtonTapped() {
        triggerEvent(Exit)
    }

    @Parcelize
    data class ViewState(
        val productImages: List<Product.Image>
    ) : Parcelable

    @Parcelize
    data class ImageSelectedResult(val productImage: Product.Image) : Parcelable
}
