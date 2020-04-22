package com.woocommerce.android.ui.products

import androidx.annotation.StringRes
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting

class ProductSortingListViewModel {
    class SortingListItemUiModel(
        @StringRes val stringResource: Int,
        val value: ProductSorting,
        val isSelected: Boolean
    )
}