package com.woocommerce.android.model

import com.woocommerce.android.R
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ResourceProvider

class GoogleAdsStatUIData(
    rawStat: GoogleAdsStat,
    currencyFormatter: CurrencyFormatter,
    resourceProvider: ResourceProvider
) {
    val mainTotalStat: String
    val mainTotalStatTitle: String

    val statItems: List<GoogleAdsStatUIDataItem>
    val statFirstColumnTitle: String = resourceProvider.getString(R.string.analytics_google_ads_programs_card_title)
    val statSecondColumnTitle: String

    init {
        mainTotalStatTitle = when (rawStat.statType) {
            StatType.TOTAL_SALES -> resourceProvider.getString(R.string.analytics_google_ads_filter_total_sales)
            StatType.SPEND -> resourceProvider.getString(R.string.analytics_google_ads_filter_spend)
            StatType.CLICKS -> resourceProvider.getString(R.string.analytics_google_ads_filter_clicks)
            StatType.IMPRESSIONS -> resourceProvider.getString(R.string.analytics_google_ads_filter_impressions)
            StatType.CONVERSIONS -> resourceProvider.getString(R.string.analytics_google_ads_filter_conversion)
        }

        mainTotalStat = when (rawStat.statType) {
            StatType.TOTAL_SALES -> rawStat.totals.sales?.let {
                currencyFormatter.formatCurrency(it.toString())
            }.orEmpty()
            StatType.SPEND -> rawStat.totals.spend?.let {
                currencyFormatter.formatCurrency(it.toString())
            }.orEmpty()
            StatType.CLICKS -> rawStat.totals.clicks?.toString().orEmpty()
            StatType.IMPRESSIONS -> rawStat.totals.impressions?.toString().orEmpty()
            StatType.CONVERSIONS -> rawStat.totals.conversions?.toString().orEmpty()
        }
        statSecondColumnTitle = mainTotalStat

        statItems = rawStat.googleAdsCampaigns.map { campaign ->
            GoogleAdsStatUIDataItem(
                name = campaign.name,
                mainStat = when (rawStat.statType) {
                    StatType.TOTAL_SALES -> campaign.subtotal?.sales?.let {
                        currencyFormatter.formatCurrency(it.toString())
                    }.orEmpty()
                    StatType.SPEND -> campaign.subtotal?.spend?.let {
                        currencyFormatter.formatCurrency(it.toString())
                    }.orEmpty()
                    StatType.CLICKS -> campaign.subtotal?.clicks?.toString().orEmpty()
                    StatType.IMPRESSIONS -> campaign.subtotal?.impressions?.toString().orEmpty()
                    StatType.CONVERSIONS -> campaign.subtotal?.conversions?.toString().orEmpty()
                },
                secondaryStat = when (rawStat.statType) {
                    StatType.SPEND -> campaign.subtotal?.sales?.let {
                        currencyFormatter.formatCurrency(it.toString())
                    }.orEmpty()

                    else -> campaign.subtotal?.spend?.let {
                        currencyFormatter.formatCurrency(it.toString())
                    }.orEmpty()
                }
            )
        }
    }
}

data class GoogleAdsStatUIDataItem(
    val name: String?,
    val mainStat: String?,
    val secondaryStat: String?
)
