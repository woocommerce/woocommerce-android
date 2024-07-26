package com.woocommerce.android.model

import com.woocommerce.android.R
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ResourceProvider

class GoogleAdsStatUIData(
    rawStats: GoogleAdsStat,
    selectedStatType: StatType,
    currencyFormatter: CurrencyFormatter,
    resourceProvider: ResourceProvider
) {
    val mainTotalStat: String
    val mainTotalStatTitle: String

    val statItems: List<GoogleAdsStatUIDataItem>
    val statSecondColumnTitle: String

    val deltaPercentage: Int?

    init {
        when (selectedStatType) {
            StatType.TOTAL_SALES -> {
                mainTotalStatTitle = resourceProvider.getString(R.string.analytics_google_ads_filter_total_sales)
                mainTotalStat = currencyFormatter.formatCurrency(rawStats.totals.sales.toString())
                statItems = rawStats.googleAdsCampaigns.map { campaign ->
                    GoogleAdsStatUIDataItem(
                        name = campaign.name,
                        mainStat = currencyFormatter.formatCurrency(campaign.subtotal.sales.toString()),
                        secondaryStat = resourceProvider.getString(
                            R.string.analytics_spend_subtitle_value,
                            currencyFormatter.formatCurrency(campaign.subtotal.spend.toString())
                        )
                    )
                }
            }

            StatType.SPEND -> {
                mainTotalStatTitle = resourceProvider.getString(R.string.analytics_google_ads_filter_spend)
                mainTotalStat = currencyFormatter.formatCurrency(rawStats.totals.spend.toString())
                statItems = rawStats.googleAdsCampaigns.map { campaign ->
                    GoogleAdsStatUIDataItem(
                        name = campaign.name,
                        mainStat = currencyFormatter.formatCurrency(campaign.subtotal.spend.toString()),
                        secondaryStat = resourceProvider.getString(
                            R.string.analytics_total_sales_subtitle_value,
                            currencyFormatter.formatCurrency(campaign.subtotal.sales.toString())
                        )
                    )
                }
            }

            StatType.CLICKS -> {
                mainTotalStatTitle = resourceProvider.getString(R.string.analytics_google_ads_filter_clicks)
                mainTotalStat = rawStats.totals.clicks.toString()
                statItems = rawStats.googleAdsCampaigns.map { campaign ->
                    GoogleAdsStatUIDataItem(
                        name = campaign.name,
                        mainStat = campaign.subtotal.clicks.toString(),
                        secondaryStat = resourceProvider.getString(
                            R.string.analytics_spend_subtitle_value,
                            currencyFormatter.formatCurrency(campaign.subtotal.spend.toString())
                        )
                    )
                }
            }

            StatType.IMPRESSIONS -> {
                mainTotalStatTitle = resourceProvider.getString(R.string.analytics_google_ads_filter_impressions)
                mainTotalStat = rawStats.totals.impressions.toString()
                statItems = rawStats.googleAdsCampaigns.map { campaign ->
                    GoogleAdsStatUIDataItem(
                        name = campaign.name,
                        mainStat = campaign.subtotal.impressions.toString(),
                        secondaryStat = resourceProvider.getString(
                            R.string.analytics_spend_subtitle_value,
                            currencyFormatter.formatCurrency(campaign.subtotal.spend.toString())
                        )
                    )
                }
            }

            StatType.CONVERSIONS -> {
                mainTotalStatTitle = resourceProvider.getString(R.string.analytics_google_ads_filter_conversion)
                mainTotalStat = rawStats.totals.conversions.toString()
                statItems = rawStats.googleAdsCampaigns.map { campaign ->
                    GoogleAdsStatUIDataItem(
                        name = campaign.name,
                        mainStat = campaign.subtotal.conversions.toString(),
                        secondaryStat = resourceProvider.getString(
                            R.string.analytics_spend_subtitle_value,
                            currencyFormatter.formatCurrency(campaign.subtotal.spend.toString())
                        )
                    )
                }
            }
        }

        statSecondColumnTitle = mainTotalStatTitle

        deltaPercentage = when (selectedStatType) {
            StatType.TOTAL_SALES -> rawStats.totalsDeltaPercentage.salesDelta
            StatType.SPEND -> rawStats.totalsDeltaPercentage.spendDelta
            StatType.CLICKS -> rawStats.totalsDeltaPercentage.clicksDelta
            StatType.IMPRESSIONS -> rawStats.totalsDeltaPercentage.impressionsDelta
            StatType.CONVERSIONS -> rawStats.totalsDeltaPercentage.conversionsDelta
        }.run { this as? DeltaPercentage.Value }?.value
    }
}

data class GoogleAdsStatUIDataItem(
    val name: String?,
    val mainStat: String?,
    val secondaryStat: String?
)

enum class GoogleStatsFilterOptions(val resourceId: Int) {
    TotalSales(R.string.analytics_google_ads_filter_total_sales),
    Spend(R.string.analytics_google_ads_filter_spend),
    Conversions(R.string.analytics_google_ads_filter_conversion),
    Clicks(R.string.analytics_google_ads_filter_clicks),
    Impressions(R.string.analytics_google_ads_filter_impressions);

    fun toStatsType() = when (this) {
        TotalSales -> StatType.TOTAL_SALES
        Spend -> StatType.SPEND
        Conversions -> StatType.CONVERSIONS
        Clicks -> StatType.CLICKS
        Impressions -> StatType.IMPRESSIONS
    }

    companion object {
        fun fromTranslatedString(
            translatedValue: String,
            resourceProvider: ResourceProvider
        ) = when (translatedValue) {
            resourceProvider.getString(TotalSales.resourceId) -> TotalSales
            resourceProvider.getString(Spend.resourceId) -> Spend
            resourceProvider.getString(Conversions.resourceId) -> Conversions
            resourceProvider.getString(Clicks.resourceId) -> Clicks
            resourceProvider.getString(Impressions.resourceId) -> Impressions
            else -> null
        }
    }
}
