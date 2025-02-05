package org.wordpress.android.fluxc.model.google

import org.wordpress.android.fluxc.model.google.WCGoogleAdsCampaign.Status

data class WCGoogleAdsPrograms(
    val campaigns: List<WCGoogleAdsProgramCampaign>?,
    val intervals: List<WCGoogleAdsProgramInterval>?,
    val totals: WCGoogleAdsProgramTotals?
)

data class WCGoogleAdsProgramCampaign(
    val id: Long?,
    val name: String?,
    val status: Status?,
    val subtotal: WCGoogleAdsProgramTotals
)

data class WCGoogleAdsProgramInterval(
    val interval: String?,
    val subtotal: WCGoogleAdsProgramTotals
)

data class WCGoogleAdsProgramTotals(
    val sales: Double?,
    val spend: Double?,
    val impressions: Double?,
    val clicks: Double?,
    val conversions: Double?
)

data class WCGoogleAdsCardStats(
    val impressions: Double,
    val clicks: Double
)
