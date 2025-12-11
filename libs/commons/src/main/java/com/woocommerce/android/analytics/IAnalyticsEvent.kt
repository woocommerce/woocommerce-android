package com.woocommerce.android.analytics

interface IAnalyticsEvent {
    val siteless: Boolean
    val name: String
    val isPosEvent: Boolean
}
