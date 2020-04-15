package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.android.parcel.Parcelize

/**
 * [ProductFilterOption] is a utility sealed class that identifies the different filter options currently
 * supported in the app while providing a common interface for managing them as a single type:
 *
 * Mostly used by [ProductFilterListFragment] for displaying the various filters for products
 */
sealed class ProductFilterOption(@StringRes open val stringResource: Int, open val value: String) : Parcelable {
    @Parcelize
    class FilterStockStatus(
        override val stringResource: Int = 0,
        override val value: String = ""
    ) : ProductFilterOption(stringResource, value)

    @Parcelize
    class FilterProductStatus(
        override val stringResource: Int = 0,
        override val value: String = ""
    ) : ProductFilterOption(stringResource, value)

    @Parcelize
    class FilterProductType(
        override val stringResource: Int = 0,
        override val value: String = ""
    ) : ProductFilterOption(stringResource, value)
}
