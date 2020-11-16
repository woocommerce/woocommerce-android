package com.woocommerce.android.ui.products

import android.content.DialogInterface
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.products.ProductStockStatus.Companion.fromString
import com.woocommerce.android.ui.products.ProductType.OTHER
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStockStatus
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption.STOCK_STATUS
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption.TYPE
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption.STATUS

class ProductFilterListViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(savedState, dispatchers) {
    companion object {
        private const val KEY_PRODUCT_FILTER_OPTIONS = "key_product_filter_options"
        const val ARG_PRODUCT_FILTER_STOCK_STATUS = "arg_product_filter_stock_status"
        const val ARG_PRODUCT_FILTER_STATUS = "arg_product_filter_status"
        const val ARG_PRODUCT_FILTER_TYPE_STATUS = "arg_product_type"
    }

    private val arguments: ProductFilterListFragmentArgs by savedState.navArgs()

    private val _filterListItems = MutableLiveData<List<FilterListItemUiModel>>()
    final val filterListItems: LiveData<List<FilterListItemUiModel>> = _filterListItems

    private val _filterOptionListItems = MutableLiveData<List<FilterListOptionItemUiModel>>()
    final val filterOptionListItems: LiveData<List<FilterListOptionItemUiModel>> = _filterOptionListItems

    final val productFilterListViewStateData = LiveDataDelegate(savedState, ProductFilterListViewState())
    private var productFilterListViewState by productFilterListViewStateData

    final val productFilterOptionListViewStateData = LiveDataDelegate(savedState, ProductFilterOptionListViewState())
    private var productFilterOptionListViewState by productFilterOptionListViewStateData

    /**
     * Holds the filter properties (stock_status, status, type) already selected by the user in a [Map]
     * where the key is the [ProductFilterOption] and value is the slug associated with the property.
     *
     * If no filters are previously selected, the map is empty.
     */
    private final val productFilterOptions: MutableMap<ProductFilterOption, String> by lazy {
        val params = savedState.get<MutableMap<ProductFilterOption, String>>(KEY_PRODUCT_FILTER_OPTIONS)
                ?: mutableMapOf()
        arguments.selectedStockStatus?.let { params.put(STOCK_STATUS, it) }
        arguments.selectedProductType?.let { params.put(TYPE, it) }
        arguments.selectedProductStatus?.let { params.put(STATUS, it) }
        savedState[KEY_PRODUCT_FILTER_OPTIONS] = params
        params
    }

    fun getFilterString() = productFilterOptions.values.joinToString(", ")

    fun getFilterByStockStatus() = productFilterOptions[STOCK_STATUS]

    fun getFilterByProductStatus() = productFilterOptions[STATUS]

    fun getFilterByProductType() = productFilterOptions[TYPE]

    fun loadFilters() {
        _filterListItems.value = buildFilterListItemUiModel()

        val screenTitle = if (productFilterOptions.isNotEmpty()) {
            resourceProvider.getString(string.product_list_filters_count, productFilterOptions.size)
        } else resourceProvider.getString(string.product_list_filters)

        productFilterListViewState = productFilterListViewState.copy(
                screenTitle = screenTitle,
                displayClearButton = productFilterOptions.isNotEmpty()
        )
    }

    fun loadFilterOptions(selectedFilterListItemPosition: Int) {
        _filterListItems.value?.let {
            val filterItem = it[selectedFilterListItemPosition]
            _filterOptionListItems.value = filterItem.filterOptionListItems
            productFilterOptionListViewState = productFilterOptionListViewState.copy(
                    screenTitle = filterItem.filterItemName
            )
        }
    }

    fun onBackButtonClicked(): Boolean {
        return if (hasChanges()) {
            triggerEvent(ShowDialog.buildDiscardDialogEvent(
                positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                    triggerEvent(Exit)
                },
                negativeButtonId = string.keep_changes
            ))
            false
        } else {
            true
        }
    }

    fun onClearFilterSelected() {
        productFilterOptions.clear()
        loadFilters()
    }

    fun onFilterOptionItemSelected(
        selectedFilterListItemPosition: Int,
        selectedFilterItem: FilterListOptionItemUiModel
    ) {
        _filterListItems.value?.let {
            // iterate through the filter option list and update the `isSelected`value to reflect the item selected and
            // update the UI of this change
            val filterItem = it[selectedFilterListItemPosition]
            val filterOptionItemList = filterItem.filterOptionListItems.map { filterOptionItem ->
                filterOptionItem.copy(
                        isSelected = filterOptionItem.filterOptionItemValue == selectedFilterItem.filterOptionItemValue
                )
            }
            _filterOptionListItems.value = filterOptionItemList

            // update the filter options map - which is used to load the filter list screen
            // if the selected filter option item is the default filter item i.e. ANY,
            // then remove the filter from the map, since this means the filter for that option has been cleared,
            // otherwise update the filter item.
            if (selectedFilterItem.filterOptionItemName == resourceProvider.getString(string.product_filter_default)) {
                productFilterOptions.remove(filterItem.filterItemKey)
            } else {
                productFilterOptions[filterItem.filterItemKey] = selectedFilterItem.filterOptionItemValue
            }
        }
    }

    private fun buildFilterListItemUiModel(): List<FilterListItemUiModel> {
        return listOf(
                FilterListItemUiModel(
                        STOCK_STATUS,
                        resourceProvider.getString(string.product_stock_status),
                        addDefaultFilterOption(
                                CoreProductStockStatus.values().map {
                                    FilterListOptionItemUiModel(
                                            resourceProvider.getString(fromString(it.value).stringResource),
                                            filterOptionItemValue = it.value,
                                            isSelected = productFilterOptions[STOCK_STATUS] == it.value
                                    )
                                }.toMutableList(), productFilterOptions[STOCK_STATUS].isNullOrEmpty()
                        )
                ),
                FilterListItemUiModel(
                        STATUS,
                        resourceProvider.getString(string.product_status),
                        addDefaultFilterOption(
                                ProductStatus.values().map {
                                    FilterListOptionItemUiModel(
                                            resourceProvider.getString(it.stringResource),
                                            filterOptionItemValue = it.value,
                                            isSelected = productFilterOptions[STATUS] == it.value
                                    )
                                }.toMutableList(), productFilterOptions[STATUS].isNullOrEmpty()
                        )
                ),
                FilterListItemUiModel(
                        TYPE,
                        resourceProvider.getString(string.product_type),
                        addDefaultFilterOption(
                                ProductType.values().filterNot { it == OTHER }.map {
                                    FilterListOptionItemUiModel(
                                            resourceProvider.getString(it.stringResource),
                                            filterOptionItemValue = it.value,
                                            isSelected = productFilterOptions[TYPE] == it.value
                                    )
                                }.toMutableList(), productFilterOptions[TYPE].isNullOrEmpty()
                        )
                )
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
            add(0, FilterListOptionItemUiModel(
                filterOptionItemName = resourceProvider.getString(string.product_filter_default),
                filterOptionItemValue = "",
                isSelected = isDefaultFilterOptionSelected
            ))
        }
    }

    private fun hasChanges(): Boolean {
        return (
            arguments.selectedProductStatus != getFilterByProductStatus() ||
                arguments.selectedProductType != getFilterByProductType() ||
                arguments.selectedStockStatus != getFilterByStockStatus()
            )
    }

    @Parcelize
    data class ProductFilterListViewState(
        val screenTitle: String? = null,
        val displayClearButton: Boolean? = null
    ) : Parcelable

    @Parcelize
    data class ProductFilterOptionListViewState(
        val screenTitle: String? = null
    ) : Parcelable

    /**
     * [filterItemKey] includes the [ProductFilterOption] which can be [STATUS], [TYPE] or [STOCK_STATUS]
     * [filterItemName] is the display name of the filter list item i.e Product Status, Stock Status
     * [filterOptionListItems] includes a list of [FilterListOptionItemUiModel]
     */
    @Parcelize
    data class FilterListItemUiModel(
        val filterItemKey: ProductFilterOption,
        val filterItemName: String,
        val filterOptionListItems: List<FilterListOptionItemUiModel>
    ) : Parcelable {
        fun isSameFilter(updatedFilterOption: FilterListItemUiModel): Boolean {
            if (this.filterItemName == updatedFilterOption.filterItemName &&
                    this.filterItemKey == updatedFilterOption.filterItemKey &&
                    this.filterOptionListItems.isSameFilterOptions(updatedFilterOption.filterOptionListItems)) {
                return true
            }
            return false
        }

        /**
         * Compares this filter's options with the passed list, returns true only if both lists contain
         * the same filter options in the same order
         */
        private fun List<FilterListOptionItemUiModel>.isSameFilterOptions(
            updatedFilterOptions: List<FilterListOptionItemUiModel>
        ): Boolean {
            if (this.size != updatedFilterOptions.size) {
                return false
            }

            for (i in this.indices) {
                if (!this[i].isSameFilterOption(updatedFilterOptions[i])) {
                    return false
                }
            }
            return true
        }
    }

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
     */
    @Parcelize
    data class FilterListOptionItemUiModel(
        val filterOptionItemName: String,
        val filterOptionItemValue: String,
        val isSelected: Boolean = false
    ) : Parcelable {
        fun isSameFilterOption(updatedFilterOption: FilterListOptionItemUiModel): Boolean {
            if (this.isSelected == updatedFilterOption.isSelected &&
                    this.filterOptionItemName == updatedFilterOption.filterOptionItemName &&
                    this.filterOptionItemValue == updatedFilterOption.filterOptionItemValue) {
                return true
            }
            return false
        }
    }

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductFilterListViewModel>
}
