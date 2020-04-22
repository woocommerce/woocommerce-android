package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.annotation.StringRes
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.DATE_ASC
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.DATE_DESC
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.TITLE_ASC
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.TITLE_DESC

class ProductSortingViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers
): ScopedViewModel(savedState, dispatchers) {
    val productSortingViewState = LiveDataDelegate(savedState, ViewState())
    private var viewState by productSortingViewState

    init {
        viewState = viewState.copy(sortingOptions = generateSortingOptions())
    }

    fun onSortingOptionChanged(option: ProductSorting) {
        viewState = viewState.copy(sortingOptions = generateSortingOptions(option))
    }

    private fun generateSortingOptions(choice: ProductSorting = DATE_ASC): List<SortingListItemUIModel> {
        return listOf(
            SortingListItemUIModel(R.string.product_list_sorting_newest_to_oldest, DATE_DESC,choice == DATE_ASC),
                SortingListItemUIModel(R.string.product_list_sorting_oldest_to_newest, DATE_ASC, choice == DATE_ASC),
                SortingListItemUIModel(R.string.product_list_sorting_a_to_z, TITLE_ASC, choice == TITLE_ASC),
                SortingListItemUIModel(R.string.product_list_sorting_z_to_a, TITLE_DESC, choice == TITLE_DESC)
        )
    }

    @Parcelize
    data class ViewState(
        val sortingOptions: List<SortingListItemUIModel>? = null
    ) : Parcelable

    @Parcelize
    data class SortingListItemUIModel(
        @StringRes val stringResource: Int,
        val value: ProductSorting,
        val isSelected: Boolean
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductSortingViewModel>
}
