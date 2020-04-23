package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.annotation.StringRes
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
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
    companion object {
        val SORTING_OPTIONS = listOf(
            SortingListItemUIModel(R.string.product_list_sorting_newest_to_oldest, DATE_DESC),
            SortingListItemUIModel(R.string.product_list_sorting_oldest_to_newest, DATE_ASC),
            SortingListItemUIModel(R.string.product_list_sorting_a_to_z, TITLE_ASC),
            SortingListItemUIModel(R.string.product_list_sorting_z_to_a, TITLE_DESC)
        )
    }

    var sortingChoice: ProductSorting
        private set

    init {
        sortingChoice = DATE_DESC
    }

    fun onSortingOptionChanged(option: ProductSorting) {
        triggerEvent(Exit)
    }

    @Parcelize
    data class SortingListItemUIModel(
        @StringRes val stringResource: Int,
        val value: ProductSorting
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductSortingViewModel>
}
