package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.products.list.ProductListRepository
import com.woocommerce.android.ui.products.list.ProductListViewModel.OnProductSortingChanged
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import org.greenrobot.eventbus.EventBus
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.DATE_ASC
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.DATE_DESC
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.TITLE_ASC
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.TITLE_DESC
import javax.inject.Inject

@HiltViewModel
class ProductSortingViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val productListRepository: ProductListRepository
) : ScopedViewModel(savedState) {
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
        sortingChoice = productListRepository.productSortingChoice
    }

    fun onSortingOptionChanged(option: ProductSorting) {
        // order="name/date,ascending,descending"
        val order = when (option) {
            TITLE_ASC -> AnalyticsTracker.VALUE_SORT_NAME_ASC
            TITLE_DESC -> AnalyticsTracker.VALUE_SORT_NAME_DESC
            DATE_ASC -> AnalyticsTracker.VALUE_SORT_DATE_ASC
            DATE_DESC -> AnalyticsTracker.VALUE_SORT_DATE_DESC
        }
        AnalyticsTracker.track(
            AnalyticsEvent.PRODUCT_LIST_SORTING_OPTION_SELECTED,
            mapOf(AnalyticsTracker.KEY_SORT_ORDER to order)
        )
        productListRepository.productSortingChoice = option
        EventBus.getDefault().post(OnProductSortingChanged)
        triggerEvent(Exit)
    }

    @Parcelize
    data class SortingListItemUIModel(
        @StringRes val stringResource: Int,
        val value: ProductSorting
    ) : Parcelable
}
