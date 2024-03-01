package com.woocommerce.android.ui.analytics.hub.informationcard

data class AnalyticsHubInformationSectionViewState(
    val title: String,
    val value: String,
    val delta: Int?,
    val chartInfo: List<Float>
) : AnalyticsCardViewState {
    val sign: String
        get() = when {
            delta == null -> ""
            delta == 0 -> ""
            delta > 0 -> "+"
            else -> "-"
        }
}
