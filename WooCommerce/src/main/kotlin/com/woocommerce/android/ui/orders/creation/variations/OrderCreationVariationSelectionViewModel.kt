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
import kotlinx.coroutines.flow.*
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

    private val parentProductFlow = flow {
        val productId = navArgs.productId
        val parentProduct = withContext(dispatchers.io) {
            productRepository.getProduct(productId)
        }
        emit(parentProduct)
    }

    private val variationsListFlow = flow {
        // Let's start with the cached variations
        val cachedVariations = withContext(dispatchers.io) {
            variationRepository.getProductVariationList(navArgs.productId).takeIf { it.isNotEmpty() }
        }
        emit(cachedVariations)
        // Then fetch from network
        emit(variationRepository.fetchProductVariations(navArgs.productId))

        // Monitor loadMore requests
        loadMoreTrigger.collect {
            emit(variationRepository.fetchProductVariations(navArgs.productId, loadMore = true))
        }
    }.map { variations ->
        variations?.filter { it.price != null }
    }

    val viewState = parentProductFlow
        .combine(variationsListFlow) { parentProduct, variationList ->
            ViewState(parentProduct, variationList)
        }
        .asLiveData()

    fun onLoadMore() {
        if (!variationRepository.canLoadMoreProductVariations) return
        loadMoreTrigger.tryEmit(Unit)
    }

    data class ViewState(
        val parentProduct: Product?,
        val variationsList: List<ProductVariation>?
    ) {
        val isSkeletonShown: Boolean = variationsList == null
    }
}
