package com.woocommerce.android.ui.orders.filters.model

import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.DATE_RANGE
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.ORDER_STATUS
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

fun List<OrderFilterOptionUiModel>.getDisplayValue(
    selectedFilterCategoryKey: OrderListFilterCategory,
    resourceProvider: ResourceProvider
): String =
    if (isAnyFilterOptionSelected()) {
        when (selectedFilterCategoryKey) {
            ORDER_STATUS -> getNumberOfSelectedFilterOptions()
                .toString()
            DATE_RANGE -> first { it.isSelected }.displayName
        }
    } else {
        resourceProvider.getString(R.string.orderfilters_default_filter_value)
    }

fun List<OrderFilterOptionUiModel>.getNumberOfSelectedFilterOptions() =
    filter { it.isSelected && it.key != OrderFilterOptionUiModel.DEFAULT_ALL_KEY }.count()

fun List<OrderFilterOptionUiModel>.clearAllFilterSelections() =
    map { it.copy(isSelected = false) }
