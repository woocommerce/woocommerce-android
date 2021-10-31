package com.woocommerce.android.ui.orders.filters.model

import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.viewmodel.ResourceProvider

fun List<OrderListFilterOptionUiModel>.markOptionAllIfNothingSelected() =
    if (!isAnyFilterOptionSelected()) {
        map {
            when (it.key) {
                OrderListFilterOptionUiModel.DEFAULT_ALL_KEY -> it.copy(isSelected = true)
                else -> it
            }
        }
    } else this

fun List<OrderListFilterOptionUiModel>.isAnyFilterOptionSelected() =
    any { it.isSelected && it.key != OrderListFilterOptionUiModel.DEFAULT_ALL_KEY }

fun List<OrderListFilterOptionUiModel>.getDisplayValue(
    selectedFilterCategoryKey: OrderFiltersRepository.OrderListFilterCategory,
    resourceProvider: ResourceProvider
): String =
    if (isAnyFilterOptionSelected()) {
        when (selectedFilterCategoryKey) {
            OrderFiltersRepository.OrderListFilterCategory.ORDER_STATUS -> getNumberOfSelectedFilterOptions()
                .toString()
            OrderFiltersRepository.OrderListFilterCategory.DATE_RANGE -> first { it.isSelected }.displayName
        }
    } else {
        resourceProvider.getString(R.string.orderfilters_default_filter_value)
    }

fun List<OrderListFilterOptionUiModel>.getNumberOfSelectedFilterOptions() =
    filter { it.isSelected && it.key != OrderListFilterOptionUiModel.DEFAULT_ALL_KEY }.count()

fun List<OrderListFilterOptionUiModel>.clearAllFilterSelections() =
    map { it.copy(isSelected = false) }
