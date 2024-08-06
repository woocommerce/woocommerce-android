package com.woocommerce.android.model

data class GoogleAdsStat(
    val googleAdsCampaigns: List<GoogleAdsCampaign>?,
    val totals: GoogleAdsTotals,
    val totalsDeltaPercentage: GoogleAdsTotalsDeltaPercentage
) {
    val noCampaignsAvailable
        get() = googleAdsCampaigns?.isEmpty() ?: false

    companion object {
        val EMPTY = GoogleAdsStat(
            googleAdsCampaigns = null,
            totals = GoogleAdsTotals(
                sales = 0.0,
                spend = 0.0,
                clicks = 0,
                impressions = 0,
                conversions = 0
            ),
            totalsDeltaPercentage = GoogleAdsTotalsDeltaPercentage(
                salesDelta = DeltaPercentage.NotExist,
                spendDelta = DeltaPercentage.NotExist,
                clicksDelta = DeltaPercentage.NotExist,
                impressionsDelta = DeltaPercentage.NotExist,
                conversionsDelta = DeltaPercentage.NotExist
            )
        )
    }
}

data class GoogleAdsCampaign(
    val id: Long,
    val name: String,
    val subtotal: GoogleAdsTotals
)

data class GoogleAdsTotals(
    val sales: Double,
    val spend: Double,
    val clicks: Int,
    val impressions: Int,
    val conversions: Int
)

data class GoogleAdsTotalsDeltaPercentage(
    val salesDelta: DeltaPercentage,
    val spendDelta: DeltaPercentage,
    val clicksDelta: DeltaPercentage,
    val impressionsDelta: DeltaPercentage,
    val conversionsDelta: DeltaPercentage
)

enum class StatType(val tracksValue: String) {
    TOTAL_SALES("sales"),
    SPEND("spend"),
    CLICKS("clicks"),
    IMPRESSIONS("impressions"),
    CONVERSIONS("conversions")
}
