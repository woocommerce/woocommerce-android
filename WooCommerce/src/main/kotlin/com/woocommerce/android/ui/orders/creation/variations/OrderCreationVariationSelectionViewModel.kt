package com.woocommerce.android.ui.orders.creation.variations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.ui.products.variations.VariationRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class OrderCreationVariationSelectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val variationRepository: VariationRepository,
    private val productRepository: ProductDetailRepository,
    private val dispatchers: CoroutineDispatchers
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: OrderCreationVariationSelectionFragmentArgs by savedStateHandle.navArgs()

    private val loadMoreTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val parentProductLiveData = flow {
        val parentProduct = withContext(dispatchers.io) {
            productRepository.getProduct(navArgs.productId)
        }
        emit(parentProduct)
    }

    private val variationsList = flow {
        // Let's start with the cached variations
        emit(variationRepository.getProductVariationList(navArgs.productId))
        // Then fetch from network
        emit(variationRepository.fetchProductVariations(navArgs.productId))

        // Monitor loadMore requests
        loadMoreTrigger.collect {
            emit(variationRepository.fetchProductVariations(navArgs.productId, loadMore = true))
        }
    }

    val viewState = parentProductLiveData.combine(variationsList) { parentProduct, variationList ->
        ViewState(parentProduct, variationList)
    }.asLiveData()

    fun onLoadMore() {
        if (!variationRepository.canLoadMoreProductVariations) return
        loadMoreTrigger.tryEmit(Unit)
    }

    data class ViewState(
        val parentProduct: Product?,
        val variationsList: List<ProductVariation>
    )
}
