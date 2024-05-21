package com.woocommerce.android.model

import android.os.Parcelable
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.ui.mystore.data.DashboardWidgetDataModel
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
        val trackingIdentifier: String
    ) {
        ONBOARDING(R.string.my_store_widget_onboarding_title, "store_setup"),
        STATS(R.string.my_store_widget_stats_title, "performance"),
        POPULAR_PRODUCTS(R.string.my_store_widget_top_products_title, "top_performers"),
        BLAZE(R.string.my_store_widget_blaze_title, "blaze"),
        REVIEWS(R.string.my_store_widget_reviews_title, "reviews"),
        ORDERS(R.string.my_store_widget_orders_title, "orders"),
        COUPONS(R.string.my_store_widget_coupons_title, "coupons"),
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
