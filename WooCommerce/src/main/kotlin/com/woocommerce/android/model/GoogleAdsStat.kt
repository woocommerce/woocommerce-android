package com.woocommerce.android.model

import com.woocommerce.android.util.CurrencyFormatter
import org.wordpress.android.fluxc.store.WCGoogleStore

data class GoogleAdsStat(
    val googleAdsCampaigns: List<GoogleAdsCampaign>,
    val totals: GoogleAdsTotals,
    val deltaPercentage: DeltaPercentage,
    val statType: StatType
) {
    companion object {
        val EMPTY = GoogleAdsStat(
            googleAdsCampaigns = emptyList(),
            totals = GoogleAdsTotals(
                sales = 0.0,
                spend = 0.0,
                clicks = 0,
                impressions = 0,
                conversions = 0
            ),
            deltaPercentage = DeltaPercentage.NotExist,
            statType = StatType.TOTAL_SALES
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
    val spend: Double?,
    val clicks: Int?,
    val impressions: Int?,
    val conversions: Int?
) {
    fun formatSales(currencyFormatter: CurrencyFormatter) =
        sales?.let {
            currencyFormatter.formatCurrency(it.toString())
        }.orEmpty()

    fun formatSpend(currencyFormatter: CurrencyFormatter) =
        spend?.let {
            currencyFormatter.formatCurrency(it.toString())
        }.orEmpty()
}

enum class StatType {
    TOTAL_SALES,
    SPEND,
    CLICKS,
    IMPRESSIONS,
    CONVERSIONS;

    fun toTotalsType() = when (this) {
        TOTAL_SALES -> WCGoogleStore.TotalsType.SALES
        SPEND -> WCGoogleStore.TotalsType.SPEND
        CLICKS -> WCGoogleStore.TotalsType.CLICKS
        IMPRESSIONS -> WCGoogleStore.TotalsType.IMPRESSIONS
        CONVERSIONS -> WCGoogleStore.TotalsType.CONVERSIONS
    }
}
