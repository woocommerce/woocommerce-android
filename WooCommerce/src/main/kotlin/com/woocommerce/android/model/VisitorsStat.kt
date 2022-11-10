package com.woocommerce.android.model

data class VisitorsStat(
    val visitorsCount: Int,
    val viewsCount: Int,
    val avgVisitorsDelta: DeltaPercentage,
    val avgViewsDelta: DeltaPercentage
)
