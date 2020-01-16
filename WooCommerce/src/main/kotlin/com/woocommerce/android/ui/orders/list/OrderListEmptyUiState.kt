package com.woocommerce.android.ui.orders.list

import android.os.Parcelable
import androidx.annotation.DrawableRes
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.ui.orders.list.OrderListType.ALL
import com.woocommerce.android.ui.orders.list.OrderListType.PROCESSING
import com.woocommerce.android.ui.orders.list.OrderListType.SEARCH
import kotlinx.android.parcel.Parcelize
import java.io.Serializable

sealed class OrderListEmptyUiState : Parcelable {
    /**
     * Base class for displaying "empty" list results.
     */
    @Parcelize
    data class EmptyList(val title: UiString, val imgResId: Int?) : OrderListEmptyUiState()

    /**
     * Use this to hide the empty view when there is data available to view.
     */
    @Parcelize
    object DataShown : OrderListEmptyUiState()

    /**
     * The view to display while data is loading. This is the view that should be visible
     * while orders are being fetched. The next step from here would either be to handle no orders,
     * or to display the orders fetched.
     */
    @Parcelize
    data class Loading(val title: UiString = UiStringRes(R.string.orderlist_fetching)) : OrderListEmptyUiState()

    /**
     * There was an error fetching orders. Display an error message along with a button
     * to "Retry".
     */
    @Parcelize
    data class ErrorWithRetry(
        val title: UiString,
        val buttonText: UiString? = null,
        val onClickFunc: Serializable? = null,
        @DrawableRes val imgResId: Int = R.drawable.ic_woo_error_state
    ) : OrderListEmptyUiState() {
        @Suppress("UNCHECKED_CAST")
        fun onButtonClick() = onClickFunc as? () -> Unit
    }
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
            isError -> createErrorListUiState(isNetworkAvailable, fetchFirstPage)
            isLoadingData -> {
                // don't show intermediate screen when loading search results
                if (orderListType == SEARCH) {
                    OrderListEmptyUiState.DataShown
                } else {
                    OrderListEmptyUiState.Loading()
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
    return OrderListEmptyUiState.ErrorWithRetry(errorText, UiStringRes(R.string.retry), fetchFirstPage as Serializable)
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
            OrderListEmptyUiState.EmptyList(
                    UiStringRes(R.string.empty_message_with_search),
                    R.drawable.img_light_empty_search)
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
