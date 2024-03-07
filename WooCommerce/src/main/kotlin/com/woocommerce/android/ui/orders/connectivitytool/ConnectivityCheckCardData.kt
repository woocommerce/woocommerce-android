package com.woocommerce.android.ui.orders.connectivitytool

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.NotStarted
import kotlinx.parcelize.Parcelize

typealias OnReadMoreClicked = () -> Unit

@Parcelize
sealed class ConnectivityCheckCardData(
    @StringRes val title: Int,
    @DrawableRes val icon: Int,
    @StringRes open val suggestion: Int,
    open val connectivityCheckStatus: ConnectivityCheckStatus,
    open val readMoreAction: OnReadMoreClicked? = null
) : Parcelable {
    val isFinished: Boolean
        get() = connectivityCheckStatus.isFinished()

    data class InternetConnectivityCheckData(
        override val connectivityCheckStatus: ConnectivityCheckStatus = NotStarted
    ) : ConnectivityCheckCardData(
        title = R.string.orderlist_connectivity_tool_internet_check_title,
        suggestion = R.string.orderlist_connectivity_tool_internet_check_suggestion,
        icon = R.drawable.ic_wifi,
        connectivityCheckStatus = connectivityCheckStatus
    )

    data class WordPressConnectivityCheckData(
        override val connectivityCheckStatus: ConnectivityCheckStatus = NotStarted
    ) : ConnectivityCheckCardData(
        title = R.string.orderlist_connectivity_tool_wordpress_check_title,
        suggestion = R.string.orderlist_connectivity_tool_wordpress_check_suggestion,
        icon = R.drawable.ic_storage,
        connectivityCheckStatus = connectivityCheckStatus
    )

    data class StoreConnectivityCheckData(
        override val suggestion: Int = R.string.orderlist_connectivity_tool_generic_error_suggestion,
        override val connectivityCheckStatus: ConnectivityCheckStatus = NotStarted,
        override val readMoreAction: OnReadMoreClicked? = null
    ) : ConnectivityCheckCardData(
        title = R.string.orderlist_connectivity_tool_store_check_title,
        suggestion = suggestion,
        icon = R.drawable.ic_more_menu_store,
        connectivityCheckStatus = connectivityCheckStatus,
        readMoreAction = readMoreAction
    )

    data class StoreOrdersConnectivityCheckData(
        override val suggestion: Int = R.string.orderlist_connectivity_tool_generic_error_suggestion,
        override val connectivityCheckStatus: ConnectivityCheckStatus = NotStarted,
        override val readMoreAction: OnReadMoreClicked? = null
    ) : ConnectivityCheckCardData(
        title = R.string.orderlist_connectivity_tool_store_orders_check_title,
        suggestion = suggestion,
        icon = R.drawable.ic_clipboard,
        connectivityCheckStatus = connectivityCheckStatus,
        readMoreAction = readMoreAction
    )
}

@Parcelize
sealed class ConnectivityCheckStatus: Parcelable {
    data object NotStarted: ConnectivityCheckStatus()
    data object InProgress: ConnectivityCheckStatus()
    data object Success: ConnectivityCheckStatus()
    data class Failure(val error: FailureType? = null): ConnectivityCheckStatus()

    fun isFinished() = this is Success || this is Failure
}

enum class FailureType(val message: Int) {
    TIMEOUT(R.string.orderlist_connectivity_tool_timeout_error_suggestion),
    PARSE(R.string.orderlist_connectivity_tool_parsing_error_suggestion),
    JETPACK(R.string.orderlist_connectivity_tool_jetpack_error_suggestion),
    GENERIC(R.string.orderlist_connectivity_tool_generic_error_suggestion)
}
