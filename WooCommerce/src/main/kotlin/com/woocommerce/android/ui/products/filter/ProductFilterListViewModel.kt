package com.woocommerce.android.ui.products.filter

import android.os.Parcelable
import androidx.annotation.DimenRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.PluginUrls
import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.model.WooPlugin
import com.woocommerce.android.model.sortCategories
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.PluginRepository
import com.woocommerce.android.ui.products.ProductFilterProductRestrictions
import com.woocommerce.android.ui.products.ProductRestriction
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.categories.ProductCategoriesRepository
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStockStatus
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption.CATEGORY
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption.STATUS
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption.STOCK_STATUS
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption.TYPE
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.WooCommerceStore.WooPlugin.WOO_COMPOSITE_PRODUCTS
import org.wordpress.android.fluxc.store.WooCommerceStore.WooPlugin.WOO_PRODUCT_BUNDLES
import org.wordpress.android.fluxc.store.WooCommerceStore.WooPlugin.WOO_SUBSCRIPTIONS
import javax.inject.Inject

@HiltViewModel
class ProductFilterListViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    private val productCategoriesRepository: ProductCategoriesRepository,
    private val networkStatus: NetworkStatus,
    private val productRestrictions: ProductFilterProductRestrictions,
    private val pluginRepository: PluginRepository,
    private val selectedSite: SelectedSite,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ScopedViewModel(savedState) {
    companion object {
        private const val KEY_PRODUCT_FILTER_OPTIONS = "key_product_filter_options"
        private const val KEY_PRODUCT_FILTER_SELECTED_CATEGORY_NAME = "key_product_filter_selected_category_name"
    }

    private val arguments: ProductFilterListFragmentArgs by savedState.navArgs()

    private var pluginsInformation: Map<String, WooPlugin> = HashMap()

    private val _filterListItems = MutableLiveData<List<FilterListItemUiModel>>()
    val filterListItems: LiveData<List<FilterListItemUiModel>> = _filterListItems

    private val _filterOptionListItems = MutableLiveData<List<FilterListOptionItemUiModel>>()
    val filterOptionListItems: LiveData<List<FilterListOptionItemUiModel>> = _filterOptionListItems

    val productFilterListViewStateData = LiveDataDelegate(savedState, ProductFilterListViewState())
    private var productFilterListViewState by productFilterListViewStateData

    val productFilterOptionListViewStateData =
        LiveDataDelegate(savedState, ProductFilterOptionListViewState())
    private var productFilterOptionListViewState by productFilterOptionListViewStateData

    private var productCategories: List<ProductCategory> = emptyList()

    /**
     * Holds the filter properties (stock_status, status, type, category) already selected by the user in a [Map]
     * where the key is the [ProductFilterOption] and value is the slug associated with the property.
     *
     * If no filters are previously selected, the map is empty.
     */
    private val productFilterOptions: MutableMap<ProductFilterOption, String> by lazy {
        val params = savedState.get<MutableMap<ProductFilterOption, String>>(KEY_PRODUCT_FILTER_OPTIONS)
            ?: mutableMapOf()
        arguments.selectedStockStatus?.let { params.put(STOCK_STATUS, it) }
        arguments.selectedProductType?.let { params.put(TYPE, it) }
        arguments.selectedProductStatus?.let { params.put(STATUS, it) }
        arguments.selectedProductCategoryId?.let { params.put(CATEGORY, it) }
        savedState[KEY_PRODUCT_FILTER_OPTIONS] = params
        params
    }

    private var selectedCategoryName: String? = null

    fun getFilterString() = productFilterOptions.values.joinToString(", ")

    private fun getFilterByStockStatus() = productFilterOptions[STOCK_STATUS]

    private fun getFilterByProductStatus() = productFilterOptions[STATUS]

    private fun getFilterByProductType() = productFilterOptions[TYPE]

    private fun getFilterByProductCategory() = productFilterOptions[CATEGORY]

    init {
        arguments.selectedProductCategoryName?.let { selectedCategoryName = it }
    }

    private suspend fun maybeLoadCategories() {
        if (productCategories.isEmpty() || isProductCategoriesPartiallyFilled()) {
            productCategories = if (networkStatus.isConnected()) {
                productCategoriesRepository.fetchProductCategories()
                    .getOrDefault(productCategoriesRepository.getProductCategoriesList())
            } else {
                productCategoriesRepository.getProductCategoriesList()
            }
        }
    }

    private fun isFromProductListWithExistingCategoryFilter(): Boolean {
        return getFilterByProductCategory() != null && selectedCategoryName != null && productCategories.isEmpty()
    }

    // Check whether productCategories list is only filled with a single, previously selected category.
    // This can result in a false positive if the site only has 1 category, but it should be OK
    // for the sake of simplicity, and because filtering by category is likely rarely done on sites with
    // just one category.
    private fun isProductCategoriesPartiallyFilled(): Boolean {
        selectedCategoryName?.let {
            return productCategories.size == 1
        } ?: return false
    }

    fun loadFilters() {
        savedState.get<String>(KEY_PRODUCT_FILTER_SELECTED_CATEGORY_NAME)?.let { selectedCategoryName = it }

        // If filter item screen is opened from product list screen, and there is existing filter by category,
        // then we partially fill productCategories with just that category.
        // This will allows displaying that category's name next to the "Category" name filter item.
        //
        // Also related: isProductCategoriesPartiallyFilled()
        if (isFromProductListWithExistingCategoryFilter()) {
            productCategories = listOf(
                ProductCategory(
                    getFilterByProductCategory()!!.toLong(),
                    selectedCategoryName!!
                )
            )
        }

        launch {
            productFilterOptionListViewState = productFilterOptionListViewState.copy(isSkeletonShown = true)
            getPluginInfo()
            _filterListItems.value = buildFilterListItemUiModel()
            productFilterOptionListViewState = productFilterOptionListViewState.copy(isSkeletonShown = false)
        }

        val screenTitle = if (productFilterOptions.isNotEmpty()) {
            resourceProvider.getString(R.string.product_list_filters_count, productFilterOptions.size)
        } else {
            resourceProvider.getString(R.string.product_list_filters)
        }

        productFilterListViewState = productFilterListViewState.copy(
            screenTitle = screenTitle,
            displayClearButton = productFilterOptions.isNotEmpty()
        )
    }

    private fun getTypeFilterWithExploreOptions(): MutableList<FilterListOptionItemUiModel> {
        return ProductType.values().filterNot { it == ProductType.OTHER }.map {
            when {
                it == ProductType.BUNDLE && isPluginInstalled(it) == false -> {
                    FilterListOptionItemUiModel.ExploreOptionItemUiModel(
                        resourceProvider.getString(it.stringResource),
                        ProductType.BUNDLE.value,
                        PluginUrls.BUNDLES_URL
                    )
                }

                it == ProductType.SUBSCRIPTION && isPluginInstalled(it) == false -> {
                    FilterListOptionItemUiModel.ExploreOptionItemUiModel(
                        resourceProvider.getString(it.stringResource),
                        ProductType.SUBSCRIPTION.value,
                        PluginUrls.SUBSCRIPTIONS_URL
                    )
                }

                it == ProductType.VARIABLE_SUBSCRIPTION && isPluginInstalled(it) == false -> {
                    FilterListOptionItemUiModel.ExploreOptionItemUiModel(
                        resourceProvider.getString(it.stringResource),
                        ProductType.VARIABLE_SUBSCRIPTION.value,
                        PluginUrls.SUBSCRIPTIONS_URL
                    )
                }

                it == ProductType.COMPOSITE && isPluginInstalled(it) == false -> {
                    FilterListOptionItemUiModel.ExploreOptionItemUiModel(
                        resourceProvider.getString(it.stringResource),
                        ProductType.COMPOSITE.value,
                        PluginUrls.COMPOSITE_URL
                    )
                }

                else -> {
                    FilterListOptionItemUiModel.DefaultFilterListOptionItemUiModel(
                        resourceProvider.getString(it.stringResource),
                        filterOptionItemValue = it.value,
                        isSelected = productFilterOptions[TYPE] == it.value
                    )
                }
            }
        }.sortedBy { it is FilterListOptionItemUiModel.ExploreOptionItemUiModel }
            .toMutableList()
    }

    fun loadFilterOptions(selectedFilterListItemPosition: Int) {
        _filterListItems.value?.let { filterListItem ->
            val filterItem = filterListItem[selectedFilterListItemPosition]
            if (filterItem.filterItemKey == CATEGORY) {
                launch {
                    productFilterOptionListViewState = productFilterOptionListViewState.copy(isSkeletonShown = true)
                    maybeLoadCategories()
                    val categoryOptions = productCategoriesToOptionListItems()
                    _filterOptionListItems.value = categoryOptions
                    updateCategoryFilterListItem(categoryOptions)
                    productFilterOptionListViewState = productFilterOptionListViewState.copy(isSkeletonShown = false)
                }
            } else {
                _filterOptionListItems.value = filterItem.filterOptionListItems
            }

            productFilterOptionListViewState = productFilterOptionListViewState.copy(
                screenTitle = filterItem.filterItemName
            )
        }
    }

    private fun productCategoriesToOptionListItems(): List<FilterListOptionItemUiModel> {
        return addDefaultFilterOption(
            productCategories
                .sortCategories(resourceProvider)
                .map { (category, margin, _) ->
                    FilterListOptionItemUiModel.DefaultFilterListOptionItemUiModel(
                        category.name,
                        category.remoteCategoryId.toString(),
                        isSelected = productFilterOptions[CATEGORY] == category.remoteCategoryId.toString(),
                        margin
                    )
                }.toMutableList(),
            productFilterOptions[CATEGORY].isNullOrEmpty()
        )
    }

    fun onBackButtonClicked(): Boolean {
        return if (hasChanges()) {
            triggerEvent(
                MultiLiveEvent.Event.ShowDialog.buildDiscardDialogEvent(
                    positiveBtnAction = { _, _ ->
                        triggerEvent(MultiLiveEvent.Event.Exit)
                    },
                    negativeButtonId = R.string.keep_changes
                )
            )
            false
        } else {
            true
        }
    }

    fun onClearFilterSelected() {
        productFilterOptions.clear()
        loadFilters()
    }

    private fun isPluginInstalled(productType: ProductType): Boolean? {
        return when (productType) {
            ProductType.BUNDLE -> pluginsInformation[WOO_PRODUCT_BUNDLES.pluginName]?.isInstalled
            ProductType.SUBSCRIPTION -> pluginsInformation[WOO_SUBSCRIPTIONS.pluginName]?.isInstalled
            ProductType.VARIABLE_SUBSCRIPTION ->
                pluginsInformation[WOO_SUBSCRIPTIONS.pluginName]?.isInstalled

            ProductType.COMPOSITE -> pluginsInformation[WOO_COMPOSITE_PRODUCTS.pluginName]?.isInstalled
            else -> null
        }
    }

    fun onFilterOptionItemSelected(
        selectedFilterListItemPosition: Int,
        selectedFilterItem: FilterListOptionItemUiModel
    ) {
        _filterListItems.value?.let {
            when (selectedFilterItem) {
                is FilterListOptionItemUiModel.DefaultFilterListOptionItemUiModel -> {
                    // iterate through the filter option list and update the `isSelected`value to reflect the item selected and
                    // update the UI of this change
                    val filterItem = it[selectedFilterListItemPosition]
                    val filterOptionItemList = filterItem.filterOptionListItems.map { filterOptionItem ->
                        (filterOptionItem as? FilterListOptionItemUiModel.DefaultFilterListOptionItemUiModel)
                            ?.copy(
                                isSelected =
                                filterOptionItem.filterOptionItemValue == selectedFilterItem.filterOptionItemValue
                            ) ?: filterOptionItem
                    }
                    _filterOptionListItems.value = filterOptionItemList

                    if (filterItem.filterItemKey == CATEGORY) {
                        selectedCategoryName = selectedFilterItem.filterOptionItemName
                        savedState[KEY_PRODUCT_FILTER_SELECTED_CATEGORY_NAME] = selectedCategoryName
                    }

                    // update the filter options map - which is used to load the filter list screen
                    // if the selected filter option item is the default filter item i.e. ANY,
                    // then remove the filter from the map, since this means the filter for that option has been cleared,
                    // otherwise update the filter item.
                    val defaultOption = resourceProvider.getString(R.string.product_filter_default)
                    if (selectedFilterItem.filterOptionItemName == defaultOption) {
                        productFilterOptions.remove(filterItem.filterItemKey)
                    } else {
                        productFilterOptions[filterItem.filterItemKey] = selectedFilterItem.filterOptionItemValue
                    }
                }

                is FilterListOptionItemUiModel.ExploreOptionItemUiModel -> {
                    analyticsTracker.track(
                        AnalyticsEvent.PRODUCT_FILTER_LIST_EXPLORE_BUTTON_TAPPED,
                        mapOf(AnalyticsTracker.KEY_TYPE to selectedFilterItem.filterOptionItemValue)
                    )
                    triggerEvent(MultiLiveEvent.Event.LaunchUrlInChromeTab(selectedFilterItem.url))
                }
            }
        }
    }

    fun onShowProductsClicked() {
        val result = ProductFilterResult(
            productStatus = getFilterByProductStatus(),
            stockStatus = getFilterByStockStatus(),
            productType = getFilterByProductType(),
            productCategory = getFilterByProductCategory(),
            productCategoryName = selectedCategoryName
        )
        triggerEvent(MultiLiveEvent.Event.ExitWithResult(result))
    }

    private fun buildFilterListItemUiModel(): List<FilterListItemUiModel> {
        val filterListItems = mutableListOf<FilterListItemUiModel>()
        filterListItems.add(
            FilterListItemUiModel(
                STOCK_STATUS,
                resourceProvider.getString(R.string.product_stock_status),
                addDefaultFilterOption(
                    CoreProductStockStatus.values().map {
                        FilterListOptionItemUiModel.DefaultFilterListOptionItemUiModel(
                            resourceProvider.getString(ProductStockStatus.fromString(it.value).stringResource),
                            filterOptionItemValue = it.value,
                            isSelected = productFilterOptions[STOCK_STATUS] == it.value
                        )
                    }.toMutableList(),
                    productFilterOptions[STOCK_STATUS].isNullOrEmpty()
                )
            )
        )
        if (!productRestrictions.restrictions.contains(ProductRestriction.NonPublishedProducts)) {
            filterListItems.add(
                FilterListItemUiModel(
                    STATUS,
                    resourceProvider.getString(R.string.product_status),
                    addDefaultFilterOption(
                        ProductStatus.values().map {
                            FilterListOptionItemUiModel.DefaultFilterListOptionItemUiModel(
                                resourceProvider.getString(it.stringResource),
                                filterOptionItemValue = it.value,
                                isSelected = productFilterOptions[STATUS] == it.value
                            )
                        }.toMutableList(),
                        productFilterOptions[STATUS].isNullOrEmpty()
                    )
                )
            )
        }
        val typeFilters = getTypeFilterWithExploreOptions()
        filterListItems.add(
            FilterListItemUiModel(
                TYPE,
                resourceProvider.getString(R.string.product_type),
                addDefaultFilterOption(
                    typeFilters,
                    productFilterOptions[TYPE].isNullOrEmpty()
                )
            )
        )
        filterListItems.add(buildCategoryFilterListItemUiModel())
        return filterListItems
    }

    private suspend fun getPluginInfo() {
        val site = selectedSite.getIfExists()
        pluginsInformation = site?.let { siteModel ->
            pluginRepository.getPluginsInfo(
                site = siteModel,
                plugins = listOf(
                    WooCommerceStore.WooPlugin.WOO_CORE,
                    WooCommerceStore.WooPlugin.WOO_SUBSCRIPTIONS,
                    WooCommerceStore.WooPlugin.WOO_PRODUCT_BUNDLES,
                    WooCommerceStore.WooPlugin.WOO_COMPOSITE_PRODUCTS
                )
            )
        } ?: emptyMap()
    }

    private fun buildCategoryFilterListItemUiModel(): FilterListItemUiModel {
        return FilterListItemUiModel(
            CATEGORY,
            resourceProvider.getString(R.string.product_category),
            productCategoriesToOptionListItems()
        )
    }

    /**
     * The [FilterListOptionItemUiModel] list includes a default option of `Any`
     * which is added to the list by this method before updating the UI
     */
    private fun addDefaultFilterOption(
        filterOptionList: MutableList<FilterListOptionItemUiModel>,
        isDefaultFilterOptionSelected: Boolean
    ): MutableList<FilterListOptionItemUiModel> {
        return filterOptionList.apply {
            add(
                0,
                FilterListOptionItemUiModel.DefaultFilterListOptionItemUiModel(
                    filterOptionItemName = resourceProvider.getString(R.string.product_filter_default),
                    filterOptionItemValue = "",
                    isSelected = isDefaultFilterOptionSelected
                )
            )
        }
    }

    private fun hasChanges(): Boolean {
        return (
            arguments.selectedProductStatus != getFilterByProductStatus() ||
                arguments.selectedProductType != getFilterByProductType() ||
                arguments.selectedStockStatus != getFilterByStockStatus() ||
                arguments.selectedProductCategoryId != getFilterByProductCategory()
            )
    }

    fun onLoadMoreRequested(selectedFilterItemPosition: Int) {
        if (!networkStatus.isConnected()) {
            return
        }

        _filterListItems.value?.let {
            val filterItem = it[selectedFilterItemPosition]

            // Load more is only needed for Category filter item.
            if (filterItem.filterItemKey == CATEGORY && productCategoriesRepository.canLoadMoreProductCategories) {
                launch {
                    productFilterOptionListViewState = productFilterOptionListViewState.copy(isLoadingMore = true)
                    productCategories = productCategoriesRepository.fetchProductCategories(loadMore = true)
                        .getOrElse {
                            triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.error_generic))
                            return@launch
                        }
                    val categoryOptions = productCategoriesToOptionListItems()
                    _filterOptionListItems.value = categoryOptions
                    updateCategoryFilterListItem(categoryOptions)
                    productFilterOptionListViewState = productFilterOptionListViewState.copy(isLoadingMore = false)
                }
            }
        }
    }

    private fun updateCategoryFilterListItem(categoryOptions: List<FilterListOptionItemUiModel>) {
        _filterListItems.value?.find {
            it.filterItemKey == CATEGORY
        }?.filterOptionListItems = categoryOptions
    }

    @Parcelize
    data class ProductFilterListViewState(
        val screenTitle: String? = null,
        val displayClearButton: Boolean? = null
    ) : Parcelable

    @Parcelize
    data class ProductFilterOptionListViewState(
        val screenTitle: String? = null,
        val isSkeletonShown: Boolean? = null,
        val isLoadingMore: Boolean? = null
    ) : Parcelable

    /**
     * [filterItemKey] includes the [ProductFilterOption] which can be [STATUS], [TYPE], [CATEGORY], or [STOCK_STATUS]
     * [filterItemName] is the display name of the filter list item i.e Product Status, Stock Status
     * [filterOptionListItems] includes a list of [FilterListOptionItemUiModel]
     */
    @Parcelize
    data class FilterListItemUiModel(
        val filterItemKey: ProductFilterOption,
        val filterItemName: String,
        var filterOptionListItems: List<FilterListOptionItemUiModel>
    ) : Parcelable

    @Parcelize
    sealed class FilterListOptionItemUiModel : Parcelable {
        /**
         * [filterOptionItemName] is the display name of the filter option
         * Eg: for stock status, this would be In Stock, Out of stock.
         * for product type, this would be Simple, Grouped.
         * for product type, this would be Pending, Draft
         *
         * [filterOptionItemValue] is the slug for the filter option.
         * Eg: for stock status, this would be instock, outofstock
         * for product type, this would be simple, grouped, variable
         * for product status, this would be pending, draft
         * for category, this would be category ID
         */
        data class DefaultFilterListOptionItemUiModel(
            val filterOptionItemName: String,
            val filterOptionItemValue: String,
            val isSelected: Boolean = false,
            var margin: Int = DEFAULT_FILTER_OPTION_MARGIN
        ) : FilterListOptionItemUiModel() {
            companion object {
                @DimenRes
                const val DEFAULT_FILTER_OPTION_MARGIN = 0
            }
        }

        data class ExploreOptionItemUiModel(
            val filterOptionItemName: String,
            val filterOptionItemValue: String,
            val url: String
        ) : FilterListOptionItemUiModel()
    }
}
