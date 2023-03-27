package com.woocommerce.android.ui.products.selector

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppConstants
import com.woocommerce.android.R.string
import com.woocommerce.android.extensions.isInteger
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductNavigationTarget.NavigateToProductFilter
import com.woocommerce.android.ui.products.ProductNavigationTarget.NavigateToVariationSelector
import com.woocommerce.android.ui.products.ProductStatus
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
import kotlinx.coroutines.Job
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
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
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
    private val selectedItems = savedState.getStateFlow(
        viewModelScope,
        navArgs.selectedItems.toList(),
        "key_selected_items"
    )
    private val filterState = savedState.getStateFlow(viewModelScope, FilterState())
    private val productsRestrictions = navArgs.restrictions
    private val products = listHandler.productsFlow.map { products ->
        products.filter { product ->
            productsRestrictions.map { restriction -> restriction(product) }.fold(true) { acc, result -> acc && result }
        }
    }

    private var fetchProductsJob: Job? = null
    private var loadMoreJob: Job? = null

    val viewState = combine(
        flow = products,
        flow2 = loadingState.withIndex()
            .debounce {
                if (it.index != 0 && it.value == IDLE) {
                    // When resetting to IDLE, wait a bit to make sure the list has been fetched from DB
                    STATE_UPDATE_DELAY
                } else 0L
            }
            .map { it.value },
        flow3 = selectedItems,
        flow4 = filterState,
        flow5 = searchQuery
    ) { products, loadingState, selectedIds, filterState, searchQuery ->
        ViewState(
            loadingState = loadingState,
            products = products.map { it.toUiModel(selectedIds) },
            selectedItemsCount = selectedIds.size,
            filterState = filterState,
            searchQuery = searchQuery
        )
    }.asLiveData()

    init {
        monitorSearchQuery()
        monitorProductFilters()
        viewModelScope.launch {
            fetchProducts(forceRefresh = true)
        }
    }

    private fun Product.toUiModel(selectedItems: Collection<SelectedItem>): ProductListItem {
        fun getProductSelection(): SelectionState {
            return if (productType == VARIABLE && numVariations > 0) {
                val intersection = variationIds.intersect(selectedItems.variationIds.toSet())
                when {
                    intersection.isEmpty() -> UNSELECTED
                    intersection.size < variationIds.size -> PARTIALLY_SELECTED
                    else -> SELECTED
                }
            } else {
                val selectedProductsIds = selectedItems.map { it.id }.toSet()
                if (selectedProductsIds.contains(remoteId)) SELECTED else UNSELECTED
            }
        }

        val stockStatus = when (stockStatus) {
            InStock -> {
                if (productType == VARIABLE) {
                    resourceProvider.getString(string.product_stock_status_instock_with_variations, numVariations)
                } else {
                    getStockStatusLabel()
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
            selectedVariationIds = variationIds.intersect(selectedItems.variationIds.toSet()),
            selectionState = getProductSelection()
        )
    }

    private fun Product.getStockStatusLabel() = if (isStockManaged) {
        val quantity = if (stockQuantity.isInteger()) stockQuantity.toInt() else stockQuantity
        resourceProvider.getString(
            string.product_stock_status_instock_quantified,
            quantity.toString()
        )
    } else {
        resourceProvider.getString(string.product_stock_status_instock)
    }

    fun onClearButtonClick() {
        launch {
            delay(STATE_UPDATE_DELAY) // let the animation play out before hiding the button
            selectedItems.value = emptyList()
        }
    }

    fun onFilterButtonClick() {
        triggerEvent(
            NavigateToProductFilter(
                filterState.value.filterOptions[ProductFilterOption.STOCK_STATUS],
                filterState.value.filterOptions[ProductFilterOption.TYPE],
                filterState.value.filterOptions[ProductFilterOption.STATUS],
                filterState.value.filterOptions[ProductFilterOption.CATEGORY],
                filterState.value.productCategoryName
            )
        )
    }

    fun onProductClick(item: ProductListItem) {
        if (item.type == VARIABLE && item.numVariations > 0) {
            triggerEvent(NavigateToVariationSelector(item.id, item.selectedVariationIds))
        } else if (item.type != VARIABLE) {
            selectedItems.update { items ->
                val selectedProductItems = items.filter {
                    it is SelectedItem.ProductOrVariation || it is SelectedItem.Product
                }
                if (selectedProductItems.map { it.id }.contains(item.id)) {
                    val productItemToUnselect = selectedProductItems.filter { it.id == item.id }.toSet()
                    selectedItems.value - productItemToUnselect
                } else {
                    selectedItems.value + SelectedItem.Product(item.id)
                }
            }
        }
    }

    fun onDoneButtonClick() {
        triggerEvent(ExitWithResult(selectedItems.value))
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onClearFiltersButtonClick() {
        filterState.value = FilterState(emptyMap(), null)
    }

    fun onFiltersChanged(
        stockStatus: String?,
        productStatus: String?,
        productType: String?,
        productCategory: String?,
        productCategoryName: String?
    ) {
        val filterOptions = mutableMapOf<ProductFilterOption, String>().apply {
            stockStatus?.let { this[ProductFilterOption.STOCK_STATUS] = it }
            productStatus?.let { this[ProductFilterOption.STATUS] = it }
            productType?.let { this[ProductFilterOption.TYPE] = it }
            productCategory?.let { this[ProductFilterOption.CATEGORY] = it }
        }
        filterState.value = FilterState(filterOptions, productCategoryName)
    }

    fun onLoadMore() {
        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
            loadingState.value = APPENDING
            listHandler.loadMore()
            loadingState.value = IDLE
        }
    }

    fun onSelectedVariationsUpdated(result: VariationSelectionResult) {
        viewModelScope.launch {
            selectedItems.update { items ->
                val oldIds = variationSelectorRepository.getProduct(result.productId)?.variationIds ?: emptyList()

                val oldItems = items.filter {
                    it is SelectedItem.ProductOrVariation && oldIds.contains(it.id)
                } + items.filter {
                    it is SelectedItem.ProductVariation && it.productId == result.productId
                }

                val newItems = result.selectedVariationIds.map { variationId ->
                    SelectedItem.ProductVariation(productId = result.productId, variationId = variationId)
                }

                selectedItems.value - oldItems.toSet() + newItems
            }
        }
    }

    private fun monitorSearchQuery() {
        viewModelScope.launch {
            searchQuery
                .withIndex()
                .filterNot {
                    // Skip initial value to avoid double fetching products
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
                    fetchProducts(query = query)
                }
        }
    }

    private fun monitorProductFilters() {
        viewModelScope.launch {
            filterState
                .withIndex()
                .filterNot {
                    // Skip initial value to avoid double fetching product categories
                    it.index == 0 && it.value.filterOptions.isEmpty()
                }
                .map { it.value }
                .collectLatest { filters ->
                    fetchProducts(filters = filters)
                }
        }
    }

    private suspend fun fetchProducts(
        filters: FilterState = filterState.value,
        query: String = "",
        forceRefresh: Boolean = false
    ) {
        loadMoreJob?.cancel()
        fetchProductsJob?.cancel()
        fetchProductsJob = viewModelScope.launch {
            loadingState.value = LOADING
            listHandler.loadFromCacheAndFetch(
                filters = filters.filterOptions,
                searchQuery = query,
                forceRefresh = forceRefresh
            ).onFailure {
                val message = if (query.isEmpty()) string.product_selector_loading_failed
                else string.product_selector_search_failed
                triggerEvent(ShowSnackbar(message))
            }
            loadingState.value = IDLE
        }
    }

    data class ViewState(
        val loadingState: LoadingState,
        val products: List<ProductListItem>,
        val selectedItemsCount: Int,
        val filterState: FilterState,
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

    @Parcelize
    data class FilterState(
        val filterOptions: Map<ProductFilterOption, String> = emptyMap(),
        val productCategoryName: String? = null
    ) : Parcelable

    enum class LoadingState {
        IDLE, LOADING, APPENDING
    }

    @Parcelize
    sealed class SelectedItem(val id: Long) : Parcelable {
        @Parcelize
        data class ProductOrVariation(val productOrVariationId: Long) : SelectedItem(productOrVariationId)

        @Parcelize
        data class Product(val productId: Long) : SelectedItem(productId)

        @Parcelize
        data class ProductVariation(val productId: Long, val variationId: Long) : SelectedItem(variationId)
    }

    @Parcelize
    sealed class ProductSelectorRestriction : (Product) -> Boolean, Parcelable {
        @Parcelize
        object OnlyPublishedProducts : ProductSelectorRestriction() {
            override fun invoke(product: Product): Boolean {
                return product.status == ProductStatus.PUBLISH
            }
        }
        @Parcelize
        object NoVariableProductsWithNoVariations : ProductSelectorRestriction() {
            override fun invoke(product: Product): Boolean {
                return !(product.productType == VARIABLE && product.numVariations == 0)
            }
        }
    }
}

val Collection<ProductSelectorViewModel.SelectedItem>.variationIds: List<Long>
    get() {
        return filterIsInstance<ProductSelectorViewModel.SelectedItem.ProductOrVariation>().map { it.id } +
            filterIsInstance<ProductSelectorViewModel.SelectedItem.ProductVariation>().map { it.variationId }
    }
