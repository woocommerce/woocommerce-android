package com.woocommerce.android.ui.analytics.informationcard

class AnalyticsInformationSectionContract {
    data class ViewState(
        val title: String,
        val value: String,
        val delta: Int,
    ) {
        fun getSign(): String = if (delta > 0) "+" else "-"
    }
}
