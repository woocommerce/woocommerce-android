package org.wordpress.android.fluxc.model

data class WCProductBundleItemReport(
    val name: String,
    val image: String?,
    val itemsSold: Int,
    val netRevenue: Double
)
