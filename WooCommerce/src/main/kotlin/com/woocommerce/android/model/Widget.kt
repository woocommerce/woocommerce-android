package com.woocommerce.android.model

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.ui.mystore.data.DashboardDataModel
import com.woocommerce.android.ui.mystore.data.DashboardWidgetDataModel

data class Widget(
    val type: Type,
    val isAdded: Boolean
) {
    enum class Type(@StringRes val titleResource: Int) {
        ONBOARDING(R.string.my_store_widget_onboarding_title),
        STATS(R.string.my_store_widget_stats_title),
        POPULAR_PRODUCTS(R.string.my_store_widget_top_products_title),
        BLAZE(R.string.my_store_widget_blaze_title)
    }
}

fun Widget.toDataModel(): DashboardWidgetDataModel =
    DashboardWidgetDataModel.newBuilder()
        .setType(type.name)
        .setIsAdded(isAdded)
        .build()

fun DashboardWidgetDataModel.toModel() =
    Widget(Widget.Type.valueOf(type), isAdded)

fun DashboardDataModel.toWidgetModelList(): List<Widget> =
    widgetList.map { it.toModel() }
