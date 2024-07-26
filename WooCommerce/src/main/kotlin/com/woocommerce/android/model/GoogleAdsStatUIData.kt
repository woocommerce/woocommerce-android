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

    val deltaPercentage: Int?

    init {
        when (rawStat.statType) {
            StatType.TOTAL_SALES -> {
                mainTotalStatTitle = resourceProvider.getString(R.string.analytics_google_ads_filter_total_sales)
                mainTotalStat = rawStat.totals.sales?.let {
                    currencyFormatter.formatCurrency(it.toString())
                }.orEmpty()
                statItems = rawStat.googleAdsCampaigns.map { campaign ->
                    GoogleAdsStatUIDataItem(
                        name = campaign.name,
                        mainStat = campaign.subtotal?.sales?.let {
                            currencyFormatter.formatCurrency(it.toString())
                        }.orEmpty(),
                        secondaryStat = campaign.subtotal?.spend?.let {
                            currencyFormatter.formatCurrency(it.toString())
                        }.orEmpty()
                    )
                }
            }

            StatType.SPEND -> {
                mainTotalStatTitle = resourceProvider.getString(R.string.analytics_google_ads_filter_spend)
                mainTotalStat = rawStat.totals.spend?.let {
                    currencyFormatter.formatCurrency(it.toString())
                }.orEmpty()
                statItems = rawStat.googleAdsCampaigns.map { campaign ->
                    GoogleAdsStatUIDataItem(
                        name = campaign.name,
                        mainStat = campaign.subtotal?.spend?.let {
                            currencyFormatter.formatCurrency(it.toString())
                        }.orEmpty(),
                        secondaryStat = campaign.subtotal?.sales?.let {
                            currencyFormatter.formatCurrency(it.toString())
                        }.orEmpty()
                    )
                }
            }

            StatType.CLICKS -> {
                mainTotalStatTitle = resourceProvider.getString(R.string.analytics_google_ads_filter_clicks)
                mainTotalStat = rawStat.totals.clicks?.toString().orEmpty()
                statItems = rawStat.googleAdsCampaigns.map { campaign ->
                    GoogleAdsStatUIDataItem(
                        name = campaign.name,
                        mainStat = campaign.subtotal?.clicks?.toString().orEmpty(),
                        secondaryStat = campaign.subtotal?.spend?.let {
                            currencyFormatter.formatCurrency(it.toString())
                        }.orEmpty()
                    )
                }
            }

            StatType.IMPRESSIONS -> {
                mainTotalStatTitle = resourceProvider.getString(R.string.analytics_google_ads_filter_impressions)
                mainTotalStat = rawStat.totals.impressions?.toString().orEmpty()
                statItems = rawStat.googleAdsCampaigns.map { campaign ->
                    GoogleAdsStatUIDataItem(
                        name = campaign.name,
                        mainStat = campaign.subtotal?.impressions?.toString().orEmpty(),
                        secondaryStat = campaign.subtotal?.spend?.let {
                            currencyFormatter.formatCurrency(it.toString())
                        }.orEmpty()
                    )
                }
            }

            StatType.CONVERSIONS -> {
                mainTotalStatTitle = resourceProvider.getString(R.string.analytics_google_ads_filter_conversion)
                mainTotalStat = rawStat.totals.conversions?.toString().orEmpty()
                statItems = rawStat.googleAdsCampaigns.map { campaign ->
                    GoogleAdsStatUIDataItem(
                        name = campaign.name,
                        mainStat = campaign.subtotal?.conversions?.toString().orEmpty(),
                        secondaryStat = campaign.subtotal?.spend?.let {
                            currencyFormatter.formatCurrency(it.toString())
                        }.orEmpty()
                    )
                }
            }
        }

        statSecondColumnTitle = mainTotalStat

        deltaPercentage = when (rawStat.statType) {
            StatType.TOTAL_SALES -> rawStat.totalsDeltaPercentage.salesDelta
            StatType.SPEND -> rawStat.totalsDeltaPercentage.spendDelta
            StatType.CLICKS -> rawStat.totalsDeltaPercentage.clicksDelta
            StatType.IMPRESSIONS -> rawStat.totalsDeltaPercentage.impressionsDelta
            StatType.CONVERSIONS -> rawStat.totalsDeltaPercentage.conversionsDelta
        }.run { this as? DeltaPercentage.Value }?.value
    }
}

data class GoogleAdsStatUIDataItem(
    val name: String?,
    val mainStat: String?,
    val secondaryStat: String?
)
