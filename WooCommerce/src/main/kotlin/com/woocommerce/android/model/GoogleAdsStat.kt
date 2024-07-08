package com.woocommerce.android.model

data class GoogleAdsStat(
    val campaigns: List<Campaign>
) {
    companion object {
        val EMPTY = GoogleAdsStat(
            campaigns = emptyList()
        )
    }
}

data class Campaign(
    val id: Long
)
