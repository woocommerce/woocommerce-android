package com.woocommerce.android.ui.products

import android.os.Parcelable
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
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStockStatus
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption

@OpenClassOnDebug
class ProductFilterListViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(savedState, dispatchers) {
    private val arguments: ProductFilterListFragmentArgs by savedState.navArgs()

    final val productFilterListViewStateData = LiveDataDelegate(savedState, ProductFilterListViewState())
    private var viewState by productFilterListViewStateData

    fun loadFilters() {
        viewState = viewState.copy(filterList = buildFilterListItemUiModel())
    }

    private fun buildFilterListItemUiModel(): List<FilterListItemUiModel> {
        return listOf(
                FilterListItemUiModel(
                        ProductFilterOption.STOCK_STATUS,
                        resourceProvider.getString(string.product_stock_status),
                        addDefaultFilterOption(
                                CoreProductStockStatus.values().map {
                                    FilterListChildItemUiModel(
                                            resourceProvider.getString(fromString(it.value).stringResource),
                                            filterChildItemValue = it.value,
                                            isSelected = arguments.selectedStockStatus == it.value
                                    )
                                }.toMutableList(), arguments.selectedStockStatus.isNullOrEmpty()
                        )
                ),
                FilterListItemUiModel(
                        ProductFilterOption.STATUS,
                        resourceProvider.getString(string.product_status),
                        addDefaultFilterOption(
                                ProductStatus.values().map {
                                    FilterListChildItemUiModel(
                                            resourceProvider.getString(it.stringResource),
                                            filterChildItemValue = it.value,
                                            isSelected = arguments.selectedProductStatus == it.value
                                    )
                                }.toMutableList(), arguments.selectedProductStatus.isNullOrEmpty()
                        )
                ),
                FilterListItemUiModel(
                        ProductFilterOption.TYPE,
                        resourceProvider.getString(string.product_type),
                        addDefaultFilterOption(
                                ProductType.values().map {
                                    FilterListChildItemUiModel(
                                            resourceProvider.getString(it.stringResource),
                                            filterChildItemValue = it.value,
                                            isSelected = arguments.selectedProductType == it.value
                                    )
                                }.toMutableList(), arguments.selectedProductType.isNullOrEmpty()
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
        val filterList: List<FilterListItemUiModel>? = null
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
