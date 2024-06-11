package com.woocommerce.android.ui.products.selector

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppConstants
import com.woocommerce.android.R.string
import com.woocommerce.android.extensions.combine
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.creation.configuration.ProductConfiguration
import com.woocommerce.android.ui.products.OrderCreationProductRestrictions
import com.woocommerce.android.ui.products.ProductNavigationTarget
import com.woocommerce.android.ui.products.ProductNavigationTarget.NavigateToProductFilter
import com.woocommerce.android.ui.products.ProductNavigationTarget.NavigateToVariationSelector
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.ProductType.VARIABLE
import com.woocommerce.android.ui.products.ProductType.VARIABLE_SUBSCRIPTION
import com.woocommerce.android.ui.products.ProductType.VARIATION
import com.woocommerce.android.ui.products.selector.ProductListHandler.SearchType
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.LoadingState.APPENDING
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.LoadingState.IDLE
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.LoadingState.LOADING
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.SelectionHandling.NORMAL
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.SelectionHandling.SIMPLE
import com.woocommerce.android.ui.products.selector.SelectionState.PARTIALLY_SELECTED
import com.woocommerce.android.ui.products.selector.SelectionState.SELECTED
import com.woocommerce.android.ui.products.selector.SelectionState.UNSELECTED
import com.woocommerce.android.ui.products.variations.selector.VariationSelectorRepository
import com.woocommerce.android.ui.products.variations.selector.VariationSelectorViewModel
import com.woocommerce.android.ui.products.variations.selector.VariationSelectorViewModel.VariationSelectionResult
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.PriceUtils
import com.woocommerce.android.util.getStockText
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class ProductSelectorViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val currencyFormatter: CurrencyFormatter,
    private val wooCommerceStore: WooCommerceStore,
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite,
    private val listHandler: ProductListHandler,
    private val variationSelectorRepository: VariationSelectorRepository,
    private val resourceProvider: ResourceProvider,
    private val tracker: ProductSelectorTracker,
    private val productsMapper: ProductsMapper,
    private val productRestrictions: OrderCreationProductRestrictions
) : ScopedViewModel(savedState) {
    companion object {
        private const val STATE_UPDATE_DELAY = 100L
        private const val NUMBER_OF_SUGGESTED_ITEMS = 5
    }

    private val currencyCode by lazy {
        wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode
    }

    private val navArgs: ProductSelectorFragmentArgs by savedState.navArgs()
    private val productSelectorFlow = navArgs.productSelectorFlow

    private val searchState = savedState.getStateFlow(this, SearchState())

    private val loadingState = MutableStateFlow(IDLE)
    private val _selectedItems: MutableStateFlow<List<SelectedItem>> = savedState.getStateFlow(
        viewModelScope,
        navArgs.selectedItems?.toList() ?: emptyList(),
        "key_selected_items"
    )
    val selectedItems: StateFlow<List<SelectedItem>> = _selectedItems
    private val filterState = savedState.getStateFlow(viewModelScope, FilterState())
    private val products = listHandler.productsFlow.map { products ->
        products.filterNot { product -> productRestrictions.isProductRestricted(product = product) }
    }
    private val popularProducts: MutableStateFlow<List<Product>> = MutableStateFlow(emptyList())
    private val recentProducts: MutableStateFlow<List<Product>> = MutableStateFlow(emptyList())

    private val selectionEnabled: MutableStateFlow<Boolean> =
        savedState.getStateFlow(viewModelScope, true, "key_selection_enabled")

    private val selectedItemsSource: MutableMap<Long, ProductSourceForTracking> = mutableMapOf()

    private var fetchProductsJob: Job? = null
    private var loadMoreJob: Job? = null

    val viewState = combine(
        flow = products,
        flow2 = popularProducts,
        flow3 = recentProducts,
        flow4 = loadingState.withIndex()
            .debounce {
                if (it.index != 0 && it.value == IDLE) {
                    // When resetting to IDLE, wait a bit to make sure the list has been fetched from DB
                    STATE_UPDATE_DELAY
                } else {
                    0L
                }
            }
            .map { it.value },
        flow5 = selectedItems,
        flow6 = filterState,
        flow7 = searchState,
        flow8 = selectionEnabled,
    ) { products, popularProducts, recentProducts, loadingState, selectedIds, filterState, searchState, enabled ->
        ViewState(
            loadingState = loadingState,
            products = products.map {
                mapProductsToUiModel(it, selectedIds)
            },
            popularProducts = getPopularProductsToDisplay(popularProducts, selectedIds),
            recentProducts = getRecentProductsToDisplay(recentProducts, selectedIds),
            selectedItemsCount = selectedIds.size,
            filterState = filterState,
            searchState = searchState,
            selectionMode = navArgs.selectionMode,
            screenTitleOverride = navArgs.screenTitleOverride,
            ctaButtonTextOverride = navArgs.ctaButtonTextOverride,
            selectionEnabled = enabled,
        )
    }.asLiveData()

    private fun mapProductsToUiModel(
        it: Product,
        selectedIds: List<SelectedItem>
    ) = when (navArgs.selectionHandling) {
        NORMAL -> it.toUiModel(selectedIds)
        SIMPLE -> it.toSimpleUiModel(selectedIds)
    }

    val selectionMode = navArgs.selectionMode

    init {
        if (navArgs.selectionMode == SelectionMode.SINGLE && (navArgs.selectedItems?.size ?: 0) > 1) {
            error("Single selection mode can only be used with a single selected item")
        }
        monitorSearchQuery()
        monitorProductFilters()
        viewModelScope.launch {
            loadPopularProducts()
            loadRecentProducts()
            fetchProducts(searchType = searchState.value.searchType)
        }
    }

    private fun getPopularProductsToDisplay(
        popularProducts: List<Product>,
        selectedIds: List<SelectedItem>
    ): List<ListItem> {
        return getProductItemsIfSearchQueryEmptyOrNoFilter(popularProducts, selectedIds)
    }

    private fun getRecentProductsToDisplay(
        recentProducts: List<Product>,
        selectedIds: List<SelectedItem>
    ): List<ListItem> {
        return getProductItemsIfSearchQueryEmptyOrNoFilter(recentProducts, selectedIds)
    }

    private fun getProductItemsIfSearchQueryEmptyOrNoFilter(
        productsList: List<Product>,
        selectedIds: List<SelectedItem>
    ): List<ListItem> {
        if (searchState.value.searchQuery.isNotNullOrEmpty() || filterState.value.filterOptions.isNotEmpty()) {
            return emptyList()
        }
        return productsList.map { mapProductsToUiModel(it, selectedIds) }
    }

    private suspend fun loadRecentProducts() {
        val recentlySoldOrders = getRecentlySoldOrders().take(NUMBER_OF_SUGGESTED_ITEMS)
        recentProducts.value = productsMapper.mapProductIdsToProduct(
            getProductIdsFromRecentlySoldOrders(
                recentlySoldOrders
            ).distinctBy { it }
        ).filterNot { product ->
            productRestrictions.isProductRestricted(product = product)
        }
    }

    private suspend fun loadPopularProducts() {
        val recentlySoldOrders = getRecentlySoldOrders()
        val productIdsWithPurchaseCount = getProductIdsWithNumberOfPurchases(recentlySoldOrders)
        val topPopularProductsSorted = productIdsWithPurchaseCount
            .asSequence()
            .map { it.toPair() }
            .toList()
            .sortedByDescending { it.second }
            .take(NUMBER_OF_SUGGESTED_ITEMS)
            .toMap()
        popularProducts.value = productsMapper.mapProductIdsToProduct(
            topPopularProductsSorted.keys.toList()
        ).filterNot { product ->
            productRestrictions.isProductRestricted(product = product)
        }
    }

    private suspend fun getRecentlySoldOrders() =
        orderStore.getPaidOrdersForSiteDesc(selectedSite.get()).filter { it.datePaid.isNotNullOrEmpty() }

    private fun getProductIdsWithNumberOfPurchases(recentlySoldOrdersList: List<OrderEntity>): Map<Long, Int> =
        recentlySoldOrdersList.asSequence()
            .flatMap { it.getLineItemList().mapNotNull { it.productId } }
            .groupingBy { it }
            .eachCount()

    private fun getProductIdsFromRecentlySoldOrders(
        recentlySoldOrdersList: List<OrderEntity>
    ) = recentlySoldOrdersList.flatMap { orderEntity ->
        orderEntity.getLineItemList().mapNotNull { it.productId }
    }

    private fun Product.toUiModel(selectedItems: Collection<SelectedItem>): ListItem {
        val isVariation = productType == VARIATION

        fun getProductSelection(): SelectionState {
            return if (isVariable() && numVariations > 0) {
                val intersection = variationIds.intersect(selectedItems.variationIds.toSet())
                when {
                    intersection.isEmpty() -> UNSELECTED
                    intersection.size < variationIds.size -> PARTIALLY_SELECTED
                    else -> SELECTED
                }
            } else if (isVariation) { // variation can be displayed in search results
                if (selectedItems.variationIds.contains(this.remoteId)) {
                    SELECTED
                } else {
                    UNSELECTED
                }
            } else {
                val selectedProductsIds = selectedItems.map { it.id }.toSet()
                if (selectedProductsIds.contains(remoteId)) SELECTED else UNSELECTED
            }
        }

        val stockStatus = getStockText(resourceProvider)

        val price = price?.let { PriceUtils.formatCurrency(price, currencyCode, currencyFormatter) }

        val stockAndPrice = listOfNotNull(stockStatus, price).joinToString(" \u2022 ")

        val isConfigurable = isConfigurable

        return when {
            isVariation -> {
                ListItem.VariationListItem(
                    parentId = parentId,
                    variationId = remoteId,
                    title = name,
                    type = productType,
                    imageUrl = firstImageUrl,
                    sku = sku.takeIf { it.isNotBlank() },
                    stockAndPrice = stockAndPrice,
                    selectionState = getProductSelection()
                )
            }

            isConfigurable -> {
                ListItem.ConfigurableListItem(
                    productId = remoteId,
                    title = name,
                    type = productType,
                    imageUrl = firstImageUrl,
                    sku = sku.takeIf { it.isNotBlank() },
                    stockAndPrice = stockAndPrice,
                    selectionState = getProductSelection()
                )
            }

            else -> {
                ListItem.ProductListItem(
                    productId = remoteId,
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
        }
    }

    private fun Product.toSimpleUiModel(selectedItems: Collection<SelectedItem>): ListItem {
        val stockStatus = getStockText(resourceProvider)
        val price = price?.let { PriceUtils.formatCurrency(price, currencyCode, currencyFormatter) }
        val stockAndPrice = listOfNotNull(stockStatus, price).joinToString(" \u2022 ")

        return ListItem.ProductListItem(
            productId = remoteId,
            title = name,
            type = productType,
            imageUrl = firstImageUrl,
            sku = sku.takeIf { it.isNotBlank() },
            stockAndPrice = stockAndPrice,
            numVariations = 0,
            selectionState = if (selectedItems.any { it.id == remoteId }) SELECTED else UNSELECTED
        )
    }

    fun onClearButtonClick() {
        launch {
            tracker.trackClearSelectionButtonClicked(
                productSelectorFlow,
                ProductSelectorTracker.ProductSelectorSource.ProductSelector
            )
            delay(STATE_UPDATE_DELAY) // let the animation play out before hiding the button
            _selectedItems.value = emptyList()
        }
    }

    fun onFilterButtonClick() {
        triggerEvent(
            NavigateToProductFilter(
                filterState.value.filterOptions[ProductFilterOption.STOCK_STATUS],
                filterState.value.filterOptions[ProductFilterOption.TYPE],
                filterState.value.filterOptions[ProductFilterOption.STATUS],
                filterState.value.filterOptions[ProductFilterOption.CATEGORY],
                filterState.value.productCategoryName,
                productRestrictions.restrictions
            )
        )
    }

    fun onProductClick(item: ListItem, productSourceForTracking: ProductSourceForTracking) {
        val productSource = updateProductSourceIfSearchIsEnabled(productSourceForTracking)
        when (item) {
            is ListItem.ProductListItem -> {
                handleProductTap(item, productSource)
            }

            is ListItem.VariationListItem -> {
                handleVariationItemTap(item, productSource)
            }

            is ListItem.ConfigurableListItem -> {
                handleConfigurableItemTap(item)
            }
        }
    }

    fun onEditConfiguration(item: ListItem.ConfigurableListItem) {
        val selectedItem = selectedItems.value.firstOrNull { it.id == item.id } as? SelectedItem.ConfigurableProduct
        selectedItem?.configuration?.let {
            triggerEvent(
                ProductNavigationTarget.EditProductConfiguration(
                    itemId = item.id,
                    productId = item.productId,
                    configuration = it
                )
            )
        } ?: triggerEvent(ProductNavigationTarget.NavigateToProductConfiguration(item.id))
    }

    private fun handleProductTap(
        item: ListItem.ProductListItem,
        productSource: ProductSourceForTracking
    ) {
        fun ListItem.ProductListItem.hasVariations() =
            isVariable() && numVariations > 0

        if (item.hasVariations()) {
            val variationSelectorScreenMode = when (navArgs.selectionMode) {
                SelectionMode.SINGLE, SelectionMode.MULTIPLE -> VariationSelectorViewModel.ScreenMode.FULLSCREEN
                SelectionMode.LIVE -> VariationSelectorViewModel.ScreenMode.DIALOG
            }
            triggerEvent(
                NavigateToVariationSelector(
                    productId = item.id,
                    selectedVariationIds = item.selectedVariationIds,
                    productSelectorFlow = productSelectorFlow,
                    productSourceForTracking = productSource,
                    selectionMode = navArgs.selectionMode,
                    screenMode = variationSelectorScreenMode
                )
            )
        } else {
            updateItemSelection(SelectedItem.Product(item.id), productSource)
        }
    }

    private fun handleVariationItemTap(
        item: ListItem.VariationListItem,
        productSource: ProductSourceForTracking
    ) {
        updateItemSelection(SelectedItem.ProductVariation(item.parentId, item.id), productSource)
    }

    private fun handleConfigurableItemTap(item: ListItem.ConfigurableListItem) {
        if (selectedItems.value.containsItemWith(item.id) && navArgs.selectionMode == SelectionMode.MULTIPLE) {
            tracker.trackItemUnselected(productSelectorFlow)
            selectedItemsSource.remove(item.id)
            _selectedItems.update { items -> items.filter { it.id != item.id } }
        } else {
            tracker.trackConfigurableTapped(productSelectorFlow)
            triggerEvent(
                ProductNavigationTarget.NavigateToProductConfiguration(item.id)
            )
        }
    }

    private fun updateItemSelection(item: SelectedItem, productSource: ProductSourceForTracking) {
        when (navArgs.selectionMode) {
            SelectionMode.SINGLE -> {
                tracker.trackItemSelected(productSelectorFlow)
                selectedItemsSource[item.id] = productSource
                _selectedItems.value = listOf(item)
            }

            SelectionMode.MULTIPLE, SelectionMode.LIVE -> {
                _selectedItems.update { items ->
                    if (items.containsItemWith(item.id)) {
                        tracker.trackItemUnselected(productSelectorFlow)
                        selectedItemsSource.remove(item.id)
                        items.filter { it.id != item.id }
                    } else {
                        selectedItemsSource[item.id] = productSource
                        tracker.trackItemSelected(productSelectorFlow)
                        items + item
                    }
                }
            }
        }
    }

    private fun updateProductSourceIfSearchIsEnabled(productSource: ProductSourceForTracking): ProductSourceForTracking {
        return when {
            searchState.value.searchQuery.isNotNullOrEmpty() -> {
                ProductSourceForTracking.SEARCH
            }

            else -> {
                productSource
            }
        }
    }

    fun onDoneButtonClick() {
        tracker.trackDoneButtonClicked(
            productSelectorFlow,
            selectedItems.value,
            selectedItemsSource.values.toList(),
            isFilterActive()
        )
        triggerEvent(ExitWithResult(selectedItems.value))
    }

    fun onNavigateBack() {
        if (searchState.value.isActive) {
            searchState.value = SearchState.EMPTY
        } else {
            triggerEvent(Exit)
        }
    }

    private fun isFilterActive() = filterState.value.filterOptions.isNotEmpty()

    fun onSearchQueryChanged(query: String) {
        searchState.value = searchState.value.copy(searchQuery = query, isActive = true)
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
        if (filterOptions.isNotEmpty()) {
            searchState.update {
                SearchState.EMPTY
            }
        }
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
            _selectedItems.update { items ->
                val oldIds = variationSelectorRepository.getProduct(result.productId)?.variationIds ?: emptyList()

                val oldItems = items.filter {
                    it is SelectedItem.ProductOrVariation && oldIds.contains(it.id)
                } + items.filter {
                    it is SelectedItem.ProductVariation && it.productId == result.productId
                }

                oldItems.forEach {
                    selectedItemsSource.remove(it.id)
                }

                val newItems = result.selectedVariationIds.map { variationId ->
                    selectedItemsSource[variationId] = result.productSourceForTracking
                    SelectedItem.ProductVariation(
                        productId = result.productId,
                        variationId = variationId,
                    )
                }

                _selectedItems.value - oldItems.toSet() + newItems
            }
        }
    }

    private fun monitorSearchQuery() {
        viewModelScope.launch {
            searchState
                .withIndex()
                .filterNot {
                    // Skip initial value to avoid double fetching products
                    it.index == 0 && it.value.searchQuery.isEmpty()
                }
                .map { it.value }
                .onEach {
                    loadingState.value = LOADING
                }
                .debounce { searchState ->
                    if (searchState.searchQuery.isEmpty()) 0L else AppConstants.SEARCH_TYPING_DELAY_MS
                }
                .collectLatest { searchState ->
                    fetchProducts(query = searchState.searchQuery, searchType = searchState.searchType)
                    if (searchState.isActive && searchState.searchQuery.isNotNullOrEmpty()) {
                        tracker.trackSearchTriggered(searchState.searchType)
                    }
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
        searchType: SearchType = SearchType.DEFAULT,
    ) {
        loadMoreJob?.cancel()
        fetchProductsJob?.cancel()
        fetchProductsJob = viewModelScope.launch {
            loadingState.value = LOADING
            listHandler.loadFromCacheAndFetch(
                filters = filters.filterOptions,
                searchQuery = query,
                searchType = searchType,
            ).onFailure {
                val message = if (query.isEmpty()) {
                    string.product_selector_loading_failed
                } else {
                    string.product_selector_search_failed
                }
                triggerEvent(ShowSnackbar(message))
            }
            loadingState.value = IDLE
        }
    }

    fun onSearchTypeChanged(@StringRes searchType: Int) {
        this.searchState.update {
            it.copy(searchType = SearchType.fromLabelResId(searchType)!!)
        }
    }

    fun onConfigurationSaved(productId: Long, productConfiguration: ProductConfiguration) {
        tracker.trackItemSelected(productSelectorFlow)
        _selectedItems.update { items ->
            val newItem = SelectedItem.ConfigurableProduct(productId, productConfiguration)
            when (navArgs.selectionMode) {
                SelectionMode.SINGLE -> listOf(newItem)
                SelectionMode.MULTIPLE, SelectionMode.LIVE -> items + newItem
            }
        }
    }

    fun onConfigurationEdited(productId: Long, productConfiguration: ProductConfiguration) {
        val items = _selectedItems.value.map { item ->
            if (item.id == productId && item is SelectedItem.ConfigurableProduct) {
                item.copy(configuration = productConfiguration)
            } else {
                item
            }
        }
        _selectedItems.update { items }
    }

    fun trackConfigurableProduct() {
        tracker.trackConfigurableItem(productSelectorFlow)
    }

    fun updateSelectedItems(selectedItems: List<SelectedItem>) {
        _selectedItems.value = selectedItems
    }

    fun onProductSelectionStateChanged(productSelectionEnabled: Boolean) {
        selectionEnabled.value = productSelectionEnabled
    }

    data class ViewState(
        val loadingState: LoadingState,
        val products: List<ListItem>,
        val popularProducts: List<ListItem>,
        val recentProducts: List<ListItem>,
        val selectedItemsCount: Int,
        val filterState: FilterState,
        val searchState: SearchState,
        val selectionMode: SelectionMode,
        val screenTitleOverride: String? = null,
        val ctaButtonTextOverride: String? = null,
        val selectionEnabled: Boolean = true,
    ) {
        val isDoneButtonEnabled: Boolean = selectionMode == SelectionMode.MULTIPLE || selectedItemsCount > 0
    }

    @Parcelize
    data class SearchState(
        val isActive: Boolean = false,
        val searchQuery: String = "",
        val searchType: SearchType = SearchType.DEFAULT,
    ) : Parcelable {
        companion object {
            val EMPTY = SearchState()
        }
    }

    sealed class ListItem(
        val id: Long,
        open val title: String,
        open val type: ProductType,
        open val imageUrl: String? = null,
        open val stockAndPrice: String? = null,
        open val sku: String? = null,
        open val selectionState: SelectionState = UNSELECTED
    ) {
        data class ProductListItem(
            val productId: Long,
            val numVariations: Int,
            val selectedVariationIds: Set<Long> = emptySet(),
            override val title: String,
            override val type: ProductType,
            override val imageUrl: String? = null,
            override val stockAndPrice: String? = null,
            override val sku: String? = null,
            override val selectionState: SelectionState = UNSELECTED
        ) : ListItem(
            id = productId,
            title = title,
            type = type,
            imageUrl = imageUrl,
            stockAndPrice = stockAndPrice,
            sku = sku,
            selectionState = selectionState
        )

        data class VariationListItem(
            val parentId: Long,
            val variationId: Long,
            override val title: String,
            override val type: ProductType,
            override val imageUrl: String? = null,
            override val stockAndPrice: String? = null,
            override val sku: String? = null,
            override val selectionState: SelectionState = UNSELECTED
        ) : ListItem(
            id = variationId,
            title = title,
            type = type,
            imageUrl = imageUrl,
            stockAndPrice = stockAndPrice,
            sku = sku,
            selectionState = selectionState
        )

        data class ConfigurableListItem(
            val productId: Long,
            override val title: String,
            override val type: ProductType,
            override val imageUrl: String? = null,
            override val stockAndPrice: String? = null,
            override val sku: String? = null,
            override val selectionState: SelectionState = UNSELECTED
        ) : ListItem(
            id = productId,
            title = title,
            type = type,
            imageUrl = imageUrl,
            stockAndPrice = stockAndPrice,
            sku = sku,
            selectionState = selectionState
        )
    }

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
        data class ProductOrVariation(
            val productOrVariationId: Long,
        ) : SelectedItem(productOrVariationId)

        @Parcelize
        data class Product(
            val productId: Long,
        ) : SelectedItem(productId)

        @Parcelize
        data class ProductVariation(
            val productId: Long,
            val variationId: Long,
        ) : SelectedItem(variationId)

        @Parcelize
        data class ConfigurableProduct(
            val productId: Long,
            val configuration: ProductConfiguration,
        ) : SelectedItem(productId)
    }

    enum class ProductSelectorFlow {
        OrderCreation, OrderEditing, CouponEdition, Undefined
    }

    enum class SelectionMode {
        SINGLE,
        MULTIPLE,

        /**
         * Used e.g. in two-pane UI when product selector is always visible next to order summary.
         * In this mode the confirmation button and toolbar are hidden.
         * The variation selector will appear in a dialog mode.
         */
        LIVE
    }

    enum class SelectionHandling {

        /**
         * Treat all products as a single item, and return just the product ID.
         */
        SIMPLE,

        /**
         * Handle product selection depending on the product type.
         */
        NORMAL
    }
}

private fun Product.isVariable() = productType == VARIABLE || productType == VARIABLE_SUBSCRIPTION

private fun ProductSelectorViewModel.ListItem.isVariable() = (type == VARIABLE || type == VARIABLE_SUBSCRIPTION)

private fun Collection<ProductSelectorViewModel.SelectedItem>.containsItemWith(id: Long): Boolean {
    return any { it.id == id }
}

val Collection<ProductSelectorViewModel.SelectedItem>.variationIds: List<Long>
    get() {
        return filterIsInstance<ProductSelectorViewModel.SelectedItem.ProductOrVariation>().map { it.id } +
            filterIsInstance<ProductSelectorViewModel.SelectedItem.ProductVariation>().map { it.variationId }
    }

enum class ProductSourceForTracking {
    POPULAR,
    LAST_SOLD,
    ALPHABETICAL,
    SEARCH,
}
