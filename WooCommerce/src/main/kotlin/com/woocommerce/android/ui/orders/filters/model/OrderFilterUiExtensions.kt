package com.woocommerce.android.ui.orders.filters.model

import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.ResourceProvider

fun List<OrderFilterOptionUiModel>.markOptionAllIfNothingSelected() =
    if (!isAnyFilterOptionSelected()) {
        map {
            when (it.key) {
                OrderFilterOptionUiModel.DEFAULT_ALL_KEY -> it.copy(isSelected = true)
                else -> it
            }
        }
    } else this

fun List<OrderFilterOptionUiModel>.isAnyFilterOptionSelected() =
    any { it.isSelected && it.key != OrderFilterOptionUiModel.DEFAULT_ALL_KEY }

fun List<OrderFilterOptionUiModel>.getNumberOfSelectedFilterOptions() =
    filter { it.isSelected && it.key != OrderFilterOptionUiModel.DEFAULT_ALL_KEY }.count()

fun List<OrderFilterOptionUiModel>.clearAllFilterSelections() =
    map { it.copy(isSelected = false) }

fun MutableList<OrderFilterOptionUiModel>.addFilterOptionAll(resourceProvider: ResourceProvider) {
    add(
        index = 0,
        OrderFilterOptionUiModel(
            key = OrderFilterOptionUiModel.DEFAULT_ALL_KEY,
            displayName = resourceProvider.getString(R.string.orderfilters_default_filter_value),
            isSelected = !isAnyFilterOptionSelected()
        )
    )
}
