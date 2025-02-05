package org.wordpress.android.fluxc.model.shippinglabels

enum class WCShippingLabelPaperSize(val stringValue: String) {
    A4("a4"),
    LABEL("label"),
    LEGAL("legal"),
    LETTER("letter");

    companion object {
        fun fromString(value: String): WCShippingLabelPaperSize {
            // When the value is unknown, WCS in wp-admin uses Label by default
            return WCShippingLabelPaperSize.values().find { it.stringValue == value } ?: LABEL
        }
    }
}
