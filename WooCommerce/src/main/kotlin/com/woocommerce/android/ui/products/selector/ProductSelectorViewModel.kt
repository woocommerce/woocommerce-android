package com.woocommerce.android.ui.products.selector

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppConstants
import com.woocommerce.android.R.string
import com.woocommerce.android.extensions.isInteger
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductListHandler
import com.woocommerce.android.ui.products.ProductStockStatus.Custom
import com.woocommerce.android.ui.products.ProductStockStatus.InStock
import com.woocommerce.android.ui.products.ProductStockStatus.NotAvailable
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.ProductType.VARIABLE
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.ProductListItem.SelectionState.SELECTED
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.ProductListItem.SelectionState.UNSELECTED
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class ProductSelectorViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val currencyFormatter: CurrencyFormatter,
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
    private val productListHandler: ProductListHandler,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(savedState) {
    private val currencyCode by lazy {
        wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode
    }

    private val navArgs: ProductSelectorFragmentArgs by savedState.navArgs()

    private val searchQuery = savedState.getNullableStateFlow(this, null, clazz = String::class.java)
    private val isLoading = MutableStateFlow(false)
    private val selectedProductIds = MutableStateFlow(navArgs.productIds.toList())

    val productsState = combine(
        productListHandler.productsFlow,
        isLoading,
        searchQuery,
        selectedProductIds
    ) { products, isLoading, searchQuery, selectedIds ->
        ProductSelectorState(
            isLoading = isLoading,
            products = products.map { it.toUiModel(selectedIds.contains(it.remoteId)) },
            searchQuery = searchQuery,
        )
    }
        .asLiveData()

    init {
        if (searchQuery.value == null) {
            viewModelScope.launch {
                isLoading.value = true
                productListHandler.fetchProducts(forceRefresh = true)
                isLoading.value = false
            }
        }

        monitorSearchQuery()
    }

    private fun Product.toUiModel(isChecked: Boolean): ProductListItem {
        val stockStatus = when (stockStatus) {
            InStock -> {
                if (productType > VARIABLE) {
                    resourceProvider.getString(string.product_stock_status_instock_with_variations, numVariations)
                } else {
                    val quantity = if (stockQuantity.isInteger()) stockQuantity.toInt() else stockQuantity
                    resourceProvider.getString(string.product_stock_status_instock_quantified, quantity.toString())
                }
            }
            NotAvailable, is Custom -> null
            else -> resourceProvider.getString(stockStatus.stringResource)
        }

        val price = price?.let { formatCurrency(price, currencyCode) }

        val stockAndPrice = listOfNotNull(stockStatus, price).joinToString(" \u2022 ")

        return ProductListItem(
            id = remoteId,
            title = name,
            type = productType,
            imageUrl = firstImageUrl,
            sku = sku,
            stockAndPrice = stockAndPrice,
            numVariations = numVariations,
            selectionState = if (isChecked) SELECTED else UNSELECTED
        )
    }

    fun onProductClick(item: ProductListItem) {
        if (item.type == VARIABLE) {
            triggerEvent(NavigateToVariationListEvent(item.id))
        } else {
            if (selectedProductIds.value.contains(item.id)) {
                selectedProductIds.value = selectedProductIds.value - item.id
            } else {
                selectedProductIds.value = selectedProductIds.value + item.id
            }
        }
    }

    fun onLoadMore() {
        viewModelScope.launch {
            productListHandler.loadMore()
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onSearchStateChanged(open: Boolean) {
        searchQuery.value = if (open) {
            searchQuery.value.orEmpty()
        } else {
            null
        }
    }

    private fun monitorSearchQuery() {
        viewModelScope.launch {
            searchQuery
                .onEach {
                    isLoading.value = true
                }
                .debounce {
                    if (it.isNullOrEmpty()) 0L else AppConstants.SEARCH_TYPING_DELAY_MS
                }.collectLatest {
                    try {
                        productListHandler.fetchProducts(searchQuery = it)
                    } finally {
                        isLoading.value = false
                    }
                }
        }
    }

    private fun formatCurrency(amount: BigDecimal?, currencyCode: String?): String {
        return if (amount != null) {
            currencyCode?.let { currencyFormatter.formatCurrency(amount, it) }
                ?: amount.toString()
        } else {
            ""
        }
    }

    data class ProductSelectorState(
        val isLoading: Boolean = false,
        val searchQuery: String? = null,
        val products: List<ProductListItem> = emptyList()
    ) {
        val isSearchOpen = searchQuery != null
    }

    data class ProductListItem(
        val id: Long,
        val title: String,
        val type: ProductType,
        val imageUrl: String? = null,
        val numVariations: Int,
        val stockAndPrice: String? = null,
        val sku: String? = null,
        val selectionState: SelectionState = UNSELECTED
    ) {
        enum class SelectionState {
            SELECTED,
            UNSELECTED,
            PARTIALLY_SELECTED
        }
    }

    data class NavigateToVariationListEvent(val productId: Long) : MultiLiveEvent.Event()
}
