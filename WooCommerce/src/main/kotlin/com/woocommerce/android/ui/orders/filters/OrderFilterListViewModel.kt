package com.woocommerce.android.ui.orders.filters

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class OrderFilterListViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    private val networkStatus: NetworkStatus,
) : ScopedViewModel(savedState) {

    private val _filterListItems = MutableLiveData<List<FilterListItemUiModel>>()
    val filterListItems: LiveData<List<FilterListItemUiModel>> = _filterListItems

    init {
        _filterListItems.value = buildFilterListUiModel()
    }

    private fun buildFilterListUiModel(): List<FilterListItemUiModel> =
        listOf(
            FilterListItemUiModel(
                displayName = resourceProvider.getString(R.string.orderfilters_order_status_filter),
                selectedValue = resourceProvider.getString(R.string.orderfilters_default_filter_value)
            ),
            FilterListItemUiModel(
                displayName = resourceProvider.getString(R.string.orderfilters_date_range_filter),
                selectedValue = resourceProvider.getString(R.string.orderfilters_default_filter_value)
            )
        )

    fun onShowOrdersClicked() {
        //TODO
    }

    @Parcelize
    data class FilterListItemUiModel(
        val displayName: String,
        val selectedValue: String
    ) : Parcelable
}
