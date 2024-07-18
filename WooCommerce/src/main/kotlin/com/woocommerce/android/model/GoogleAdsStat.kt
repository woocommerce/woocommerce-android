package com.woocommerce.android.model

data class GoogleAdsStat(
    val googleAdsCampaigns: List<GoogleAdsCampaign>,
    val totals: GoogleAdsTotals = GoogleAdsTotals(
        sales = 0.0,
        spend = 0.0
    )
) {
    companion object {
        val EMPTY = GoogleAdsStat(
            googleAdsCampaigns = emptyList(),
            totals = GoogleAdsTotals(
                sales = 0.0,
                spend = 0.0
            )
        )
    }
}

data class GoogleAdsCampaign(
    val id: Long?,
    val name: String?,
    val subtotal: GoogleAdsTotals?
)

data class GoogleAdsTotals(
    val sales: Double?,
    val spend: Double?
)
