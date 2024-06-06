package com.woocommerce.android.model

import android.os.Parcelable
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.ui.mystore.data.DashboardWidgetDataModel
import com.woocommerce.android.util.FeatureFlag
import kotlinx.parcelize.Parcelize

@Parcelize
data class DashboardWidget(
    val type: Type,
    val isSelected: Boolean,
    val status: Status,
) : Parcelable {
    val title: Int
        get() = type.titleResource

    val isAvailable: Boolean
        get() = status == Status.Available

    val isVisible: Boolean
        get() = isSelected && isAvailable

    enum class Type(
        @StringRes val titleResource: Int,
        val trackingIdentifier: String,
        private val isSupported: Boolean = true,
    ) {
        ONBOARDING(
            titleResource = R.string.my_store_widget_onboarding_title,
            trackingIdentifier = "store_setup"
        ),
        STATS(
            titleResource = R.string.my_store_widget_stats_title,
            trackingIdentifier = "performance"
        ),
        POPULAR_PRODUCTS(
            titleResource = R.string.my_store_widget_top_products_title,
            trackingIdentifier = "top_performers"
        ),
        BLAZE(
            titleResource = R.string.my_store_widget_blaze_title,
            trackingIdentifier = "blaze"
        ),
        INBOX(
            titleResource = R.string.inbox_screen_title,
            trackingIdentifier = "inbox",
            isSupported = FeatureFlag.INBOX.isEnabled()
        ),
        REVIEWS(
            titleResource = R.string.my_store_widget_reviews_title,
            trackingIdentifier = "reviews",
            isSupported = FeatureFlag.DYNAMIC_DASHBOARD_M2.isEnabled()
        ),
        COUPONS(
            titleResource = R.string.my_store_widget_coupons_title,
            trackingIdentifier = "coupons",
            isSupported = FeatureFlag.DYNAMIC_DASHBOARD_M2.isEnabled()
        ),
        STOCK(
            titleResource = R.string.my_store_widget_product_stock_title,
            trackingIdentifier = "stock",
            isSupported = FeatureFlag.DYNAMIC_DASHBOARD_M2.isEnabled()
        ),
        ORDERS(
            titleResource = R.string.my_store_widget_orders_title,
            trackingIdentifier = "orders",
            isSupported = FeatureFlag.DYNAMIC_DASHBOARD_M2.isEnabled()
        );

        companion object {
            val supportedWidgets: List<Type> = Type.entries.filter { it.isSupported }
        }
    }

    sealed interface Status : Parcelable {
        @Parcelize
        data object Available : Status

        @Parcelize
        data class Unavailable(@StringRes val badgeText: Int) : Status

        @Parcelize
        data object Hidden : Status
    }
}

fun DashboardWidget.toDataModel(): DashboardWidgetDataModel =
    DashboardWidgetDataModel.newBuilder()
        .setType(type.name)
        .setIsAdded(isSelected)
        .build()
