package com.woocommerce.android.ui.orders.connectivitytool

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.R

typealias OnReadMoreClicked = (url: String) -> Unit

sealed class ConnectivityCheckCardData(
    @StringRes val title: Int,
    @StringRes val suggestion: Int,
    @DrawableRes val icon: Int,
    val readMoreAction: OnReadMoreClicked? = null
) {
    data object InternetConnectivityCheckData : ConnectivityCheckCardData(
        title = R.string.orderlist_connectivity_tool_internet_check_title,
        suggestion = R.string.orderlist_connectivity_tool_internet_check_suggestion,
        icon = R.drawable.ic_wifi
    )

    data object WordPressConnectivityCheckData : ConnectivityCheckCardData(
        title = R.string.orderlist_connectivity_tool_wordpress_check_title,
        suggestion = R.string.orderlist_connectivity_tool_wordpress_check_suggestion,
        icon = R.drawable.ic_storage
    )

    class StoreConnectivityCheckData(
        readMoreAction: OnReadMoreClicked
    ) : ConnectivityCheckCardData(
        title = R.string.orderlist_connectivity_tool_store_check_title,
        suggestion = R.string.orderlist_connectivity_tool_store_check_suggestion,
        icon = R.drawable.ic_more_menu_store,
        readMoreAction = readMoreAction
    )

    class StoreOrdersConnectivityCheckData(
        readMoreAction: OnReadMoreClicked
    ) : ConnectivityCheckCardData(
        title = R.string.orderlist_connectivity_tool_store_orders_check_title,
        suggestion = R.string.orderlist_connectivity_tool_store_orders_check_suggestion,
        icon = R.drawable.ic_clipboard,
        readMoreAction = readMoreAction
    )
}
