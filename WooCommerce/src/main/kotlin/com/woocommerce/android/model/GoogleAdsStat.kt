package com.woocommerce.android.model

data class GoogleAdsStat(
    val campaigns: List<Campaign>
)

data class Campaign(
    val id: Long
)
