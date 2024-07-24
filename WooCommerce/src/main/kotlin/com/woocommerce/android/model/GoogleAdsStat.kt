package com.woocommerce.android.model

import com.woocommerce.android.util.CurrencyFormatter

data class GoogleAdsStat(
    val googleAdsCampaigns: List<GoogleAdsCampaign>,
    val totals: GoogleAdsTotals = GoogleAdsTotals(
        sales = 0.0,
        spend = 0.0
    ),
    val deltaPercentage: DeltaPercentage
) {
    companion object {
        val EMPTY = GoogleAdsStat(
            googleAdsCampaigns = emptyList(),
            totals = GoogleAdsTotals(
                sales = 0.0,
                spend = 0.0
            ),
            deltaPercentage = DeltaPercentage.NotExist
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
