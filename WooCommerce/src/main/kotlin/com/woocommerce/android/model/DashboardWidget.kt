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
        ONBOARDING(R.string.my_store_widget_onboarding_title, "store_setup"),
        STATS(R.string.my_store_widget_stats_title, "performance"),
        POPULAR_PRODUCTS(R.string.my_store_widget_top_products_title, "top_performers"),
        BLAZE(R.string.my_store_widget_blaze_title, "blaze"),
        REVIEWS(R.string.my_store_widget_reviews_title, "reviews", isSupported = FeatureFlag.DYNAMIC_DASHBOARD_M2.isEnabled()),
        ORDERS(R.string.my_store_widget_orders_title, "orders", isSupported = FeatureFlag.DYNAMIC_DASHBOARD_M2.isEnabled()),
        COUPONS(R.string.my_store_widget_coupons_title, "coupons", isSupported = FeatureFlag.DYNAMIC_DASHBOARD_M2.isEnabled()),
        INBOX(R.string.inbox_screen_title, "inbox", isSupported = FeatureFlag.MORE_MENU_INBOX.isEnabled()),
        PRODUCT_STOCK(R.string.my_store_widget_product_stock_title, "product_stock", isSupported = FeatureFlag.DYNAMIC_DASHBOARD_M2.isEnabled());

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
