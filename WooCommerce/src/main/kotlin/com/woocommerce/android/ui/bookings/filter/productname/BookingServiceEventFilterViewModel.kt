package com.woocommerce.android.ui.bookings.filter.productname

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.list.ProductListRepository
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import org.wordpress.android.fluxc.store.WCProductStore

@HiltViewModel(assistedFactory = BookingServiceEventFilterViewModel.Factory::class)
class BookingServiceEventFilterViewModel @AssistedInject constructor(
    @Assisted private val initialServiceEvents: BookingsFilterOption.ServiceEvents?,
    @Assisted private val onFilterChanged: (BookingsFilterOption.ServiceEvents) -> Unit,
    savedStateHandle: SavedStateHandle,
    private val productListRepository: ProductListRepository,
) : ScopedViewModel(savedStateHandle) {

    private val _uiState = MutableStateFlow(
        BookingServiceEventFilterUiState(
            selectedProducts = initialServiceEvents ?: BookingsFilterOption.ServiceEvents.DEFAULT,
            onProductSelected = ::onProductSelected,
            onSearchQueryChanged = ::onSearchQueryChanged
        )
    )
    val uiState: StateFlow<BookingServiceEventFilterUiState> = _uiState

    init {
        loadBookableProducts()
    }

    private fun loadBookableProducts() {
        val bookableProducts = productListRepository.getProductList(
            productFilterOptions = mapOf(
                WCProductStore.ProductFilterOption.TYPE to ProductType.BOOKING.value
            )
        )

        _uiState.update { currentState ->
            currentState.copy(
                availableProducts = bookableProducts
                    .map { product ->
                        BookableProduct(
                            id = product.remoteId,
                            name = product.name
                        )
                    }
                    .sortedBy { it.name }
            )
        }
    }

    private fun onProductSelected(product: BookableProduct?) {
        val newSelectedProductsState = if (product == BookableProduct.Any) {
            BookingsFilterOption.ServiceEvents.DEFAULT
        } else {
            val productSet = _uiState.value.selectedProducts.values.toMutableSet()
            val productInfo = product?.let {
                BookingsFilterOption.ProductInfo(
                    productId = it.id ?: 0L,
                    productName = it.name ?: ""
                )
            }

            if (productSet.contains(productInfo)) {
                productSet.remove(productInfo)
            } else {
                productInfo?.let { productSet.add(it) }
            }
            BookingsFilterOption.ServiceEvents(productSet)
        }

        _uiState.update { it.copy(selectedProducts = newSelectedProductsState) }
        onFilterChanged(newSelectedProductsState)
    }

    private fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            initial: BookingsFilterOption.ServiceEvents?,
            onFilterChanged: (BookingsFilterOption.ServiceEvents) -> Unit
        ): BookingServiceEventFilterViewModel
    }
}
