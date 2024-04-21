package com.woocommerce.android.model

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.ui.mystore.data.DashboardWidgetDataModel

data class DashboardWidget(
    val type: Type,
    val isVisible: Boolean,
    val isAvailable: Boolean
) {
    enum class Type(@StringRes val titleResource: Int) {
        ONBOARDING(R.string.my_store_widget_onboarding_title),
        STATS(R.string.my_store_widget_stats_title),
        POPULAR_PRODUCTS(R.string.my_store_widget_top_products_title),
        BLAZE(R.string.my_store_widget_blaze_title)
    }
}

fun DashboardWidget.toDataModel(): DashboardWidgetDataModel =
    DashboardWidgetDataModel.newBuilder()
        .setType(type.name)
        .setIsAdded(isVisible)
        .build()
