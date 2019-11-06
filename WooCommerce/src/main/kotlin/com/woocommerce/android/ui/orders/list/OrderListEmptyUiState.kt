package com.woocommerce.android.ui.orders.list

import androidx.annotation.DrawableRes
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.ui.orders.list.OrderListType.ALL
import com.woocommerce.android.ui.orders.list.OrderListType.PROCESSING
import com.woocommerce.android.ui.orders.list.OrderListType.SEARCH

sealed class OrderListEmptyUiState(
    val title: UiString? = null,
    @DrawableRes val imgResId: Int? = null,
    val buttonText: UiString? = null,
    val onButtonClick: (() -> Unit)? = null,
    val emptyViewVisible: Boolean = true
) {
    /**
     * Base class for displaying "empty" list results.
     */
    class EmptyList(title: UiString, imgResId: Int?) : OrderListEmptyUiState(title, imgResId)

    /**
     * Use this to hide the empty view when there is data available to view.
     */
    object DataShown : OrderListEmptyUiState(emptyViewVisible = false)

    /**
     * The view to display while data is loading. This is the view that should be visible
     * while orders are being fetched. The next step from here would either be to handle no orders,
     * or to display the orders fetched.
     */
    object Loading : OrderListEmptyUiState(title = UiStringRes(R.string.orderlist_fetching))

    /**
     * There was an error fetching orders. Display an error message along with a button
     * to "Retry".
     */
    class ErrorWithRetry(
        title: UiString,
        buttonText: UiString? = null,
        onButtonClick: (() -> Unit)? = null
    ) : OrderListEmptyUiState(
            title = title,
            imgResId = R.drawable.ic_woo_error_state,
            buttonText = buttonText,
            onButtonClick = onButtonClick)
}

/**
 * Use this method as a builder for creating "empty" UI views.
 */
fun createEmptyUiState(
    orderListType: OrderListType,
    isNetworkAvailable: Boolean,
    isLoadingData: Boolean,
    isListEmpty: Boolean,
    hasOrders: Boolean,
    isError: Boolean = false,
    fetchFirstPage: () -> Unit
): OrderListEmptyUiState {
    return if (isListEmpty) {
        when {
            isError && isListEmpty -> createErrorListUiState(isNetworkAvailable, fetchFirstPage)
            isLoadingData -> {
                // don't show intermediate screen when loading search results
                if (orderListType == SEARCH) {
                    OrderListEmptyUiState.DataShown
                } else {
                    OrderListEmptyUiState.Loading
                }
            }
            else -> {
                if (isNetworkAvailable) {
                    createEmptyListUiState(orderListType, hasOrders)
                } else {
                    createErrorListUiState(isNetworkAvailable, fetchFirstPage)
                }
            }
        }
    } else {
        OrderListEmptyUiState.DataShown
    }
}

/**
 * Takes care of creating empty list error views.
 */
private fun createErrorListUiState(
    isNetworkAvailable: Boolean,
    fetchFirstPage: () -> Unit
): OrderListEmptyUiState {
    val errorText = if (isNetworkAvailable) {
        UiStringRes(R.string.orderlist_error_fetch_generic)
    } else {
        UiStringRes(R.string.error_generic_network)
    }
    return OrderListEmptyUiState.ErrorWithRetry(errorText, UiStringRes(R.string.retry), fetchFirstPage)
}

/**
 * Takes care of creating empty list views.
 */
private fun createEmptyListUiState(
    orderListType: OrderListType,
    hasOrders: Boolean
): OrderListEmptyUiState {
    return when (orderListType) {
        SEARCH -> {
            OrderListEmptyUiState.EmptyList(UiStringRes(R.string.orders_empty_message_with_search), null)
        }
        PROCESSING -> {
            if (hasOrders) {
                // User has processed all orders
                OrderListEmptyUiState.EmptyList(
                        UiStringRes(R.string.orders_processed_empty_message),
                        R.drawable.ic_gridicons_checkmark)
            } else {
                // Waiting for orders to process
                OrderListEmptyUiState.EmptyList(
                        UiStringRes(R.string.orders_empty_message_with_processing),
                        R.drawable.ic_hourglass_empty)
            }
        }
        ALL -> {
            OrderListEmptyUiState.EmptyList(
                    UiStringRes(R.string.orders_empty_message_with_filter),
                    R.drawable.ic_hourglass_empty)
        }
    }
}
