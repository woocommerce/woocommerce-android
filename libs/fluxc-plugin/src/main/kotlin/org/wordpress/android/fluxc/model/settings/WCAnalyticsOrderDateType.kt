package org.wordpress.android.fluxc.model.settings

enum class WCAnalyticsOrderDateType(val value: String) {
    PAID("date_paid"),
    CREATED("date_created"),
    COMPLETED("date_completed");

    companion object {
        fun fromValue(value: String?): WCAnalyticsOrderDateType =
            entries.find { it.value == value } ?: PAID
    }
}
