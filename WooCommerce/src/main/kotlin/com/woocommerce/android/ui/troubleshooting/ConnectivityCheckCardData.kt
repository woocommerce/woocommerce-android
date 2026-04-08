package com.woocommerce.android.ui.troubleshooting

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.troubleshooting.useCases.InternetConnectionCheckUseCase
import com.woocommerce.android.ui.troubleshooting.useCases.StoreConnectionCheckUseCase
import com.woocommerce.android.ui.troubleshooting.useCases.StoreOrdersCheckUseCase
import com.woocommerce.android.ui.troubleshooting.useCases.StoreProductsCheckUseCase
import com.woocommerce.android.ui.troubleshooting.useCases.WPComConnectionCheckUseCase
import kotlinx.parcelize.Parcelize

@Parcelize
enum class ConnectivityCheckType(
    @StringRes val title: Int,
    @StringRes val suggestion: Int,
    @DrawableRes val icon: Int,
    val analyticsValue: String,
    val operationName: String
) : Parcelable {
    INTERNET(
        title = R.string.orderlist_connectivity_tool_internet_check_title,
        suggestion = R.string.orderlist_connectivity_tool_internet_check_suggestion,
        icon = R.drawable.ic_wifi,
        analyticsValue = AnalyticsTracker.VALUE_CONNECTIVITY_INTERNET,
        operationName = InternetConnectionCheckUseCase.OPERATION_NAME
    ),
    WP_COM(
        title = R.string.orderlist_connectivity_tool_wordpress_check_title,
        suggestion = R.string.orderlist_connectivity_tool_wordpress_check_suggestion,
        icon = R.drawable.ic_storage,
        analyticsValue = AnalyticsTracker.VALUE_CONNECTIVITY_WP_COM,
        operationName = WPComConnectionCheckUseCase.OPERATION_NAME
    ),
    STORE(
        title = R.string.orderlist_connectivity_tool_store_check_title,
        suggestion = R.string.orderlist_connectivity_tool_generic_error_suggestion,
        icon = R.drawable.ic_more_menu_store,
        analyticsValue = AnalyticsTracker.VALUE_CONNECTIVITY_SITE,
        operationName = StoreConnectionCheckUseCase.OPERATION_NAME
    ),
    ORDERS(
        title = R.string.orderlist_connectivity_tool_store_orders_check_title,
        suggestion = R.string.orderlist_connectivity_tool_generic_error_suggestion,
        icon = R.drawable.ic_clipboard,
        analyticsValue = AnalyticsTracker.VALUE_CONNECTIVITY_ORDERS,
        operationName = StoreOrdersCheckUseCase.OPERATION_NAME
    ),
    PRODUCTS(
        title = R.string.orderlist_connectivity_tool_store_products_check_title,
        suggestion = R.string.orderlist_connectivity_tool_generic_error_suggestion,
        icon = R.drawable.ic_tintable_product,
        analyticsValue = AnalyticsTracker.VALUE_CONNECTIVITY_PRODUCTS,
        operationName = StoreProductsCheckUseCase.OPERATION_NAME
    )
}

@Parcelize
data class ConnectivityCheckCardData(
    val type: ConnectivityCheckType,
    val status: ConnectivityCheckStatus = ConnectivityCheckStatus.NotStarted
) : Parcelable

@Parcelize
sealed class ConnectivityCheckStatus : Parcelable {
    open val durationMs: Long get() = 0L

    data object NotStarted : ConnectivityCheckStatus()
    data object InProgress : ConnectivityCheckStatus()

    data class Success(override val durationMs: Long = 0L) : ConnectivityCheckStatus(), Parcelable

    data class Failure(
        val error: FailureType? = null,
        val technicalDetails: String? = null,
        override val durationMs: Long = 0L
    ) : ConnectivityCheckStatus(), Parcelable
}

@Parcelize
enum class FailureType(val message: Int) : Parcelable {
    TIMEOUT(R.string.orderlist_connectivity_tool_timeout_error_suggestion),
    PARSE(R.string.orderlist_connectivity_tool_parsing_error_suggestion),
    JETPACK(R.string.orderlist_connectivity_tool_jetpack_error_suggestion),
    GENERIC(R.string.orderlist_connectivity_tool_generic_error_suggestion)
}
