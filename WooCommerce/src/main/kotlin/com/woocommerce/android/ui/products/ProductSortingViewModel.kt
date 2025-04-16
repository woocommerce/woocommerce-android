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
import javax.inject.Inject

@HiltViewModel
class ProductSortingViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val productListRepository: ProductListRepository
) : ScopedViewModel(savedState) {
    companion object {
        val SORTING_OPTIONS = listOf(
            SortingListItemUIModel(R.string.product_list_sorting_newest_to_oldest, SortingListItemUIModel.Sorting.DATE_DESC),
            SortingListItemUIModel(R.string.product_list_sorting_oldest_to_newest, SortingListItemUIModel.Sorting.DATE_ASC),
            SortingListItemUIModel(R.string.product_list_sorting_a_to_z, SortingListItemUIModel.Sorting.TITLE_ASC),
            SortingListItemUIModel(R.string.product_list_sorting_z_to_a, SortingListItemUIModel.Sorting.TITLE_DESC)
        )
    }

    var sortingChoice: SortingListItemUIModel.Sorting
        private set

    init {
        sortingChoice = when (productListRepository.productSortingChoice) {
            ProductSorting.TITLE_ASC -> SortingListItemUIModel.Sorting.TITLE_ASC
            ProductSorting.TITLE_DESC -> SortingListItemUIModel.Sorting.TITLE_DESC
            ProductSorting.DATE_ASC -> SortingListItemUIModel.Sorting.DATE_ASC
            ProductSorting.DATE_DESC -> SortingListItemUIModel.Sorting.DATE_DESC
            ProductSorting.POPULARITY_ASC, ProductSorting.POPULARITY_DESC ->
                error("Invalid sorting choice $productListRepository.productSortingChoice")
        }
    }

    fun onSortingOptionChanged(option: SortingListItemUIModel.Sorting) {
        // order="name/date,ascending,descending"
        val order = when (option) {
            SortingListItemUIModel.Sorting.TITLE_ASC -> AnalyticsTracker.VALUE_SORT_NAME_ASC
            SortingListItemUIModel.Sorting.TITLE_DESC -> AnalyticsTracker.VALUE_SORT_NAME_DESC
            SortingListItemUIModel.Sorting.DATE_ASC -> AnalyticsTracker.VALUE_SORT_DATE_ASC
            SortingListItemUIModel.Sorting.DATE_DESC -> AnalyticsTracker.VALUE_SORT_DATE_DESC
        }
        AnalyticsTracker.track(
            AnalyticsEvent.PRODUCT_LIST_SORTING_OPTION_SELECTED,
            mapOf(AnalyticsTracker.KEY_SORT_ORDER to order)
        )
        productListRepository.productSortingChoice = when (option) {
            SortingListItemUIModel.Sorting.TITLE_ASC -> ProductSorting.TITLE_ASC
            SortingListItemUIModel.Sorting.TITLE_DESC -> ProductSorting.TITLE_DESC
            SortingListItemUIModel.Sorting.DATE_ASC -> ProductSorting.DATE_ASC
            SortingListItemUIModel.Sorting.DATE_DESC -> ProductSorting.DATE_DESC
        }
        EventBus.getDefault().post(OnProductSortingChanged)
        triggerEvent(Exit)
    }

    @Parcelize
    data class SortingListItemUIModel(
        @StringRes val stringResource: Int,
        val value: Sorting,
    ) : Parcelable {
        enum class Sorting {
            TITLE_ASC,
            TITLE_DESC,
            DATE_ASC,
            DATE_DESC,
        }
    }
}
