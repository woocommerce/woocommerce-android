package com.woocommerce.android.ui.products.images.ai

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Product
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProductImageRemoveBackgroundViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {

    private val navArgs: ProductImageRemoveBackgroundFragmentArgs by savedState.navArgs()

    val productImage: Product.Image = navArgs.image

    object ExitScreen : MultiLiveEvent.Event()
}
