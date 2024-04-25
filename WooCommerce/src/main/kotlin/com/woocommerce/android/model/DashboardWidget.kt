package com.woocommerce.android.model

import android.os.Parcelable
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.ui.mystore.data.DashboardWidgetDataModel
import kotlinx.parcelize.Parcelize

@Parcelize
data class DashboardWidget(
    val type: Type,
    val isVisible: Boolean,
    val status: Status,
) : Parcelable {
    val title: Int
        get() = type.titleResource

    val isAvailable: Boolean
        get() = status is Status.Available

    enum class Type(@StringRes val titleResource: Int) {
        ONBOARDING(R.string.my_store_widget_onboarding_title),
        STATS(R.string.my_store_widget_stats_title),
        POPULAR_PRODUCTS(R.string.my_store_widget_top_products_title),
        BLAZE(R.string.my_store_widget_blaze_title)
    }

    sealed interface Status: Parcelable {
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
        .setIsAdded(isVisible)
        .build()
