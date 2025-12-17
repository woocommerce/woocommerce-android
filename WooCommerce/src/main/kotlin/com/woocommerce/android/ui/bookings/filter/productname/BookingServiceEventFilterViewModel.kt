package com.woocommerce.android.ui.bookings.filter.productname

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.UiString.UiStringText
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
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import org.wordpress.android.fluxc.store.WCProductStore

@HiltViewModel(assistedFactory = BookingServiceEventFilterViewModel.Factory::class)
class BookingServiceEventFilterViewModel @AssistedInject constructor(
    @Assisted private val selectedServiceEvents: BookingsFilterOption.ServiceEvents?,
    @Assisted private val onFilterChanged: (BookingsFilterOption.ServiceEvents) -> Unit,
    private val productListRepository: ProductListRepository,
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {

    private val _uiState = MutableStateFlow(
        BookingServiceEventFilterUiState(
            selectedProducts = selectedServiceEvents ?: BookingsFilterOption.ServiceEvents.DEFAULT,
            isLoading = true,
            onProductSelected = ::onProductSelected,
            onSearchQueryChanged = ::onSearchQueryChanged
        )
    )
    val uiState: StateFlow<BookingServiceEventFilterUiState> = _uiState

    init {
        observeProducts()
        loadAllBookingProducts()
    }

    private fun observeProducts() = launch {
        productListRepository.observeProducts(
            filterOptions = mapOf(
                WCProductStore.ProductFilterOption.TYPE to ProductType.BOOKABLE_SERVICE.value
            )
        ).collect { products ->
            _uiState.update { currentState ->
                currentState.copy(
                    availableProducts = products
                        .sortedBy { it.name }
                        .map { product ->
                            BookableProduct(
                                id = product.remoteId,
                                name = UiStringText(product.name)
                            )
                        },
                    isLoading = false
                )
            }
        }
    }

    /**
     * Fetch booking products from the network with big page size to minimize request and ensure search bar
     * has all available results
     */
    private fun loadAllBookingProducts() = launch {
        val firstPage = productListRepository.fetchProductList(
            loadMore = false,
            productFilterOptions = mapOf(WCProductStore.ProductFilterOption.TYPE to ProductType.BOOKABLE_SERVICE.value),
            pageSize = BOOKABLE_PRODUCTS_FILTER_PAGE_SIZE
        )

        if (firstPage.isFailure) return@launch

        // Load subsequent pages while available
        while (productListRepository.canLoadMoreProducts) {
            val next = productListRepository.fetchProductList(
                loadMore = true,
                productFilterOptions = mapOf(
                    WCProductStore.ProductFilterOption.TYPE to ProductType.BOOKABLE_SERVICE.value
                ),
                pageSize = BOOKABLE_PRODUCTS_FILTER_PAGE_SIZE
            )
            if (next.isFailure) break
        }
    }

    private fun onProductSelected(product: BookableProduct?) {
        val newSelectedProductsState = if (product == BookableProduct.Any) {
            BookingsFilterOption.ServiceEvents.DEFAULT
        } else {
            val productSet = _uiState.value.selectedProducts.values.toMutableSet()
            val productInfo = product?.let {
                BookingsFilterOption.ProductInfo(
                    productId = it.id,
                    productName = (it.name as UiStringText).text
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

    companion object {
        private const val BOOKABLE_PRODUCTS_FILTER_PAGE_SIZE = 100 // Setting big page size to minimise num of requests
    }
}
