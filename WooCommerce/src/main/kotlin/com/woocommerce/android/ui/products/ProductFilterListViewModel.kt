package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.products.ProductStockStatus.Companion.fromString
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStockStatus
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption.STOCK_STATUS
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption.TYPE
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption.STATUS

@OpenClassOnDebug
class ProductFilterListViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(savedState, dispatchers) {
    companion object {
        private const val KEY_PRODUCT_FILTER_OPTIONS = "key_product_filter_options"
    }

    private val arguments: ProductFilterListFragmentArgs by savedState.navArgs()

    private val _filterListItems = MutableLiveData<List<FilterListItemUiModel>>()
    final val filterListItems: LiveData<List<FilterListItemUiModel>> = _filterListItems

    private val _filterChildListItems = MutableLiveData<List<FilterListChildItemUiModel>>()
    final val filterChildListItems: LiveData<List<FilterListChildItemUiModel>> = _filterChildListItems

    final val productFilterListViewStateData = LiveDataDelegate(savedState, ProductFilterListViewState())
    private var productFilterListViewState by productFilterListViewStateData

    final val productFilterChildListViewStateData = LiveDataDelegate(savedState, ProductFilterChildListViewState())
    private var productFilterChildListViewState by productFilterChildListViewStateData

    private final val productFilterOptions: MutableMap<ProductFilterOption, String> by lazy {
        val params = savedState.get<MutableMap<ProductFilterOption, String>>(KEY_PRODUCT_FILTER_OPTIONS)
                ?: mutableMapOf()
        arguments.selectedStockStatus?.let { params.put(STOCK_STATUS, it) }
        arguments.selectedProductType?.let { params.put(TYPE, it) }
        arguments.selectedProductStatus?.let { params.put(STATUS, it) }
        savedState[KEY_PRODUCT_FILTER_OPTIONS] = params
        params
    }

    fun loadFilters() {
        _filterListItems.value = buildFilterListItemUiModel()

        val screenTitle = if (productFilterOptions.isNotEmpty()) {
            resourceProvider.getString(string.product_list_filters_count, productFilterOptions.size)
        } else resourceProvider.getString(string.product_list_filters)

        productFilterListViewState = productFilterListViewState.copy(
                screenTitle = screenTitle
        )
    }

    fun loadChildFilters(selectedFilterListItemPosition: Int) {
        _filterListItems.value?.let {
            val filterChildItem = it[selectedFilterListItemPosition]
            _filterChildListItems.value = filterChildItem.childListItems
            productFilterChildListViewState = productFilterChildListViewState.copy(
                    screenTitle = filterChildItem.filterItemName
            )
        }
    }

    fun onChildFilterItemSelected(
        selectedFilterListItemPosition: Int,
        selectedFilterItem: FilterListChildItemUiModel
    ) {
        _filterListItems.value?.let {
            val filterItem = it[selectedFilterListItemPosition]
            val filterChildItemList = filterItem.childListItems.map { filterChildItem ->
                filterChildItem.copy(
                        isSelected = filterChildItem.filterChildItemValue == selectedFilterItem.filterChildItemValue
                )
            }

            if (selectedFilterItem.filterChildItemName == resourceProvider.getString(string.product_filter_default)) {
                productFilterOptions.remove(filterItem.filterItemKey)
            } else {
                productFilterOptions[filterItem.filterItemKey] = selectedFilterItem.filterChildItemValue
            }
            _filterChildListItems.value = filterChildItemList
        }

        triggerEvent(Exit)
    }

    private fun buildFilterListItemUiModel(): List<FilterListItemUiModel> {
        return listOf(
                FilterListItemUiModel(
                        STOCK_STATUS,
                        resourceProvider.getString(string.product_stock_status),
                        addDefaultFilterOption(
                                CoreProductStockStatus.values().map {
                                    FilterListChildItemUiModel(
                                            resourceProvider.getString(fromString(it.value).stringResource),
                                            filterChildItemValue = it.value,
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
                                    FilterListChildItemUiModel(
                                            resourceProvider.getString(it.stringResource),
                                            filterChildItemValue = it.value,
                                            isSelected = productFilterOptions[STATUS] == it.value
                                    )
                                }.toMutableList(), productFilterOptions[STATUS].isNullOrEmpty()
                        )
                ),
                FilterListItemUiModel(
                        TYPE,
                        resourceProvider.getString(string.product_type),
                        addDefaultFilterOption(
                                ProductType.values().map {
                                    FilterListChildItemUiModel(
                                            resourceProvider.getString(it.stringResource),
                                            filterChildItemValue = it.value,
                                            isSelected = productFilterOptions[TYPE] == it.value
                                    )
                                }.toMutableList(), productFilterOptions[TYPE].isNullOrEmpty()
                        )
                )
        )
    }

    private fun addDefaultFilterOption(
        filterChildList: MutableList<FilterListChildItemUiModel>,
        isDefaultChildItemSelected: Boolean
    ): MutableList<FilterListChildItemUiModel> {
        return filterChildList.apply {
            add(0, FilterListChildItemUiModel(
                filterChildItemName = resourceProvider.getString(string.product_filter_default),
                filterChildItemValue = "",
                isSelected = isDefaultChildItemSelected
            ))
        }
    }

    @Parcelize
    data class ProductFilterListViewState(
        val screenTitle: String? = null
    ) : Parcelable

    @Parcelize
    data class ProductFilterChildListViewState(
        val screenTitle: String? = null
    ) : Parcelable

    @Parcelize
    data class FilterListItemUiModel(
        val filterItemKey: ProductFilterOption,
        val filterItemName: String,
        val childListItems: List<FilterListChildItemUiModel>
    ) : Parcelable

    @Parcelize
    data class FilterListChildItemUiModel(
        val filterChildItemName: String,
        val filterChildItemValue: String,
        val isSelected: Boolean = false
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductFilterListViewModel>
}
