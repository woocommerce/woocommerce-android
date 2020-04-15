package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.annotation.StringRes
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.ui.products.ProductFilterOption.FilterProductStatus
import com.woocommerce.android.ui.products.ProductFilterOption.FilterProductType
import com.woocommerce.android.ui.products.ProductFilterOption.FilterStockStatus
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize

@OpenClassOnDebug
class ProductFilterListViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers
) : ScopedViewModel(savedState, dispatchers) {
    final val productFilterListViewStateData = LiveDataDelegate(savedState, ProductFilterListViewState())
    private var viewState by productFilterListViewStateData

    init {
        loadFilters()
    }

    private fun loadFilters() {
        viewState = viewState.copy(filterList = buildFilterListItemUiModel())
    }

    private fun buildFilterListItemUiModel(): List<FilterListItemUIModel> {
        return listOf(
                FilterListItemUIModel(
                        R.string.product_stock_status,
                        ProductStockStatus.toFilterProductStockStatusList()
                                .apply { add(0, FilterStockStatus(string.product_filter_default, "")) }
                ),
                FilterListItemUIModel(
                        R.string.product_status,
                        ProductStatus.toFilterProductStatusList()
                                .apply { add(0, FilterProductStatus(string.product_filter_default, "")) }
                ),
                FilterListItemUIModel(
                        R.string.product_type,
                        ProductType.toFilterProductTypeList()
                                .apply { add(0, FilterProductType(string.product_filter_default, "")) }
                )
        )
    }

    @Parcelize
    data class ProductFilterListViewState(
        val filterList: List<FilterListItemUIModel>? = null
    ) : Parcelable

    @Parcelize
    data class FilterListItemUIModel(
        @StringRes val key: Int,
        val childListItems: List<ProductFilterOption>
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductFilterListViewModel>
}
