package com.woocommerce.android.ui.orders.connectivitytool

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.R
import kotlinx.parcelize.Parcelize

typealias OnReadMoreClicked = (url: String) -> Unit

@Parcelize
sealed class ConnectivityCheckCardData(
    @StringRes val title: Int,
    @StringRes val suggestion: Int,
    @DrawableRes val icon: Int,
    open val connectivityCheckStatus: ConnectivityCheckStatus,
    open val readMoreAction: OnReadMoreClicked? = null
) : Parcelable {
    class InternetConnectivityCheckData(
        override val connectivityCheckStatus: ConnectivityCheckStatus
    ) : ConnectivityCheckCardData(
        title = R.string.orderlist_connectivity_tool_internet_check_title,
        suggestion = R.string.orderlist_connectivity_tool_internet_check_suggestion,
        icon = R.drawable.ic_wifi,
        connectivityCheckStatus = connectivityCheckStatus
    )

    class WordPressConnectivityCheckData(
        override val connectivityCheckStatus: ConnectivityCheckStatus
    ) : ConnectivityCheckCardData(
        title = R.string.orderlist_connectivity_tool_wordpress_check_title,
        suggestion = R.string.orderlist_connectivity_tool_wordpress_check_suggestion,
        icon = R.drawable.ic_storage,
        connectivityCheckStatus = connectivityCheckStatus
    )

    class StoreConnectivityCheckData(
        override val connectivityCheckStatus: ConnectivityCheckStatus,
        override val readMoreAction: OnReadMoreClicked
    ) : ConnectivityCheckCardData(
        title = R.string.orderlist_connectivity_tool_store_check_title,
        suggestion = R.string.orderlist_connectivity_tool_store_check_suggestion,
        icon = R.drawable.ic_more_menu_store,
        connectivityCheckStatus = connectivityCheckStatus,
        readMoreAction = readMoreAction
    )

    class StoreOrdersConnectivityCheckData(
        override val connectivityCheckStatus: ConnectivityCheckStatus,
        override val readMoreAction: OnReadMoreClicked
    ) : ConnectivityCheckCardData(
        title = R.string.orderlist_connectivity_tool_store_orders_check_title,
        suggestion = R.string.orderlist_connectivity_tool_store_orders_check_suggestion,
        icon = R.drawable.ic_clipboard,
        connectivityCheckStatus = connectivityCheckStatus,
        readMoreAction = readMoreAction
    )
}

enum class ConnectivityCheckStatus {
    NotStarted,
    InProgress,
    Success,
    Failure;

    fun isFinished() = this == Success || this == Failure
}
