package com.woocommerce.android.ui.products.selector

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppConstants
import com.woocommerce.android.R.string
import com.woocommerce.android.extensions.isInteger
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductNavigationTarget.NavigateToVariationSelector
import com.woocommerce.android.ui.products.ProductStockStatus.Custom
import com.woocommerce.android.ui.products.ProductStockStatus.InStock
import com.woocommerce.android.ui.products.ProductStockStatus.NotAvailable
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.ProductType.VARIABLE
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.LoadingState.APPENDING
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.LoadingState.IDLE
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.LoadingState.LOADING
import com.woocommerce.android.ui.products.selector.SelectionState.PARTIALLY_SELECTED
import com.woocommerce.android.ui.products.selector.SelectionState.SELECTED
import com.woocommerce.android.ui.products.selector.SelectionState.UNSELECTED
import com.woocommerce.android.ui.products.variations.selector.VariationSelectorRepository
import com.woocommerce.android.ui.products.variations.selector.VariationSelectorViewModel.VariationSelectionResult
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.PriceUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class ProductSelectorViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val currencyFormatter: CurrencyFormatter,
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
    private val listHandler: ProductListHandler,
    private val variationSelectorRepository: VariationSelectorRepository,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(savedState) {
    companion object {
        private const val STATE_UPDATE_DELAY = 100L
    }

    private val currencyCode by lazy {
        wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode
    }

    private val navArgs: ProductSelectorFragmentArgs by savedState.navArgs()

    private val searchQuery = savedState.getStateFlow(this, "")
    private val loadingState = MutableStateFlow(IDLE)
    private val selectedProductIds = savedState.getStateFlow(viewModelScope, navArgs.productIds.toSet())

    val viewState = combine(
        flow = listHandler.productsFlow,
        flow2 = loadingState.withIndex()
            .debounce {
                if (it.index != 0 && it.value == IDLE) {
                    // When resetting to IDLE, wait a bit to make sure the list has been fetched from DB
                    STATE_UPDATE_DELAY
                } else 0L
            }
            .map { it.value },
        flow3 = selectedProductIds,
        flow4 = searchQuery
    ) { products, loadingState, selectedIds, searchQuery ->
        ViewState(
            loadingState = loadingState,
            products = products.map { it.toUiModel(selectedIds) },
            selectedItemsCount = selectedIds.size,
            searchQuery = searchQuery
        )
    }.asLiveData()

    init {
        monitorSearchQuery()
        viewModelScope.launch {
            loadingState.value = LOADING
            listHandler.fetchProducts(forceRefresh = true)
            loadingState.value = IDLE
        }
    }

    private fun Product.toUiModel(selectedIds: Set<Long>): ProductListItem {
        fun getProductSelection(): SelectionState {
            return if (productType == VARIABLE && numVariations > 0) {
                val intersection = variationIds.intersect(selectedIds.toSet())
                when {
                    intersection.isEmpty() -> UNSELECTED
                    intersection.size < variationIds.size -> PARTIALLY_SELECTED
                    else -> SELECTED
                }
            } else {
                if (selectedIds.contains(remoteId)) SELECTED else UNSELECTED
            }
        }

        val stockStatus = when (stockStatus) {
            InStock -> {
                if (productType == VARIABLE) {
                    resourceProvider.getString(string.product_stock_status_instock_with_variations, numVariations)
                } else {
                    val quantity = if (stockQuantity.isInteger()) stockQuantity.toInt() else stockQuantity
                    resourceProvider.getString(string.product_stock_status_instock_quantified, quantity.toString())
                }
            }
            NotAvailable, is Custom -> null
            else -> resourceProvider.getString(stockStatus.stringResource)
        }

        val price = price?.let { PriceUtils.formatCurrency(price, currencyCode, currencyFormatter) }

        val stockAndPrice = listOfNotNull(stockStatus, price).joinToString(" \u2022 ")

        return ProductListItem(
            id = remoteId,
            title = name,
            type = productType,
            imageUrl = firstImageUrl,
            sku = sku.takeIf { it.isNotBlank() },
            stockAndPrice = stockAndPrice,
            numVariations = numVariations,
            selectedVariationIds = variationIds.intersect(selectedIds),
            selectionState = getProductSelection()
        )
    }

    fun onClearButtonClick() {
        launch {
            delay(STATE_UPDATE_DELAY) // let the animation play out before hiding the button
            selectedProductIds.value = emptySet()
        }
    }

    fun onProductClick(item: ProductListItem) {
        if (item.type == VARIABLE && item.numVariations > 0) {
            triggerEvent(NavigateToVariationSelector(item.id, item.selectedVariationIds))
        } else {
            if (selectedProductIds.value.contains(item.id)) {
                selectedProductIds.value = selectedProductIds.value - item.id
            } else {
                selectedProductIds.value = selectedProductIds.value + item.id
            }
        }
    }

    fun onDoneButtonClick() {
        triggerEvent(ExitWithResult(selectedProductIds.value))
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onLoadMore() {
        viewModelScope.launch {
            loadingState.value = APPENDING
            listHandler.loadMore()
            loadingState.value = IDLE
        }
    }

    fun onSelectedVariationsUpdated(result: VariationSelectionResult) {
        viewModelScope.launch {
            val oldIds = variationSelectorRepository.getProduct(result.productId)?.variationIds ?: emptyList()
            selectedProductIds.update { selectedProductIds.value - oldIds.toSet() + result.selectedVariationIds }
        }
    }

    private fun monitorSearchQuery() {
        viewModelScope.launch {
            searchQuery
                .withIndex()
                .filterNot {
                    // Skip initial value to avoid double fetching product categories
                    it.index == 0 && it.value.isEmpty()
                }
                .map { it.value }
                .onEach {
                    loadingState.value = LOADING
                }
                .debounce {
                    if (it.isEmpty()) 0L else AppConstants.SEARCH_TYPING_DELAY_MS
                }
                .collectLatest { query ->
                    try {
                        listHandler.fetchProducts(searchQuery = query)
                            .onFailure {
                                val message = if (query.isEmpty()) string.product_selector_loading_failed
                                else string.product_selector_search_failed
                                triggerEvent(ShowSnackbar(message))
                            }
                    } finally {
                        loadingState.value = IDLE
                    }
                }
        }
    }

    data class ViewState(
        val loadingState: LoadingState,
        val products: List<ProductListItem>,
        val selectedItemsCount: Int,
        val searchQuery: String
    )

    data class ProductListItem(
        val id: Long,
        val title: String,
        val type: ProductType,
        val imageUrl: String? = null,
        val numVariations: Int,
        val stockAndPrice: String? = null,
        val sku: String? = null,
        val selectedVariationIds: Set<Long> = emptySet(),
        val selectionState: SelectionState = UNSELECTED
    )

    enum class LoadingState {
        IDLE, LOADING, APPENDING
    }
}
