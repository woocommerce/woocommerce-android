package org.wordpress.android.fluxc.model.google

import org.wordpress.android.fluxc.network.rest.wpcom.wc.google.WCGoogleAdsProgramsDTO
import javax.inject.Inject

class WCGoogleAdsProgramsMapper @Inject constructor() {
    fun mapToModel(dto: WCGoogleAdsProgramsDTO): WCGoogleAdsPrograms {
        return WCGoogleAdsPrograms(
            campaigns = dto.campaigns?.map {
                WCGoogleAdsProgramCampaign(
                    id = it.id,
                    name = it.name,
                    status = it.status?.let { status ->
                        WCGoogleAdsCampaign.Status.fromString(status)
                    },
                    subtotal = WCGoogleAdsProgramTotals(
                        sales = it.subtotals?.sales,
                        spend = it.subtotals?.spend,
                        impressions = it.subtotals?.impressions,
                        clicks = it.subtotals?.clicks,
                        conversions = it.subtotals?.conversions
                    )
                )
            },
            intervals = dto.intervals?.map {
                WCGoogleAdsProgramInterval(
                    interval = it.interval,
                    subtotal = WCGoogleAdsProgramTotals(
                        sales = it.subtotals?.sales,
                        spend = it.subtotals?.spend,
                        impressions = it.subtotals?.impressions,
                        clicks = it.subtotals?.clicks,
                        conversions = it.subtotals?.conversions
                    )
                )
            },
            totals = dto.totals?.let {
                WCGoogleAdsProgramTotals(
                    sales = it.sales,
                    spend = it.spend,
                    impressions = it.impressions,
                    clicks = it.clicks,
                    conversions = it.conversions
                )
            }
        )
    }
}
