package org.wordpress.android.fluxc.model.google

import org.wordpress.android.fluxc.model.google.WCGoogleAdsCampaign.Status
import org.wordpress.android.fluxc.network.rest.wpcom.wc.google.WCGoogleAdsCampaignDTO
import javax.inject.Inject

class WCGoogleAdsCampaignMapper @Inject constructor() {
    fun mapToModel(dto: WCGoogleAdsCampaignDTO): WCGoogleAdsCampaign {
        return WCGoogleAdsCampaign(
            id = dto.id,
            name = dto.name,
            status = dto.status?.let { Status.fromString(it) },
            type = dto.rawType,
            amount = dto.amount,
            countryISOCode = dto.country,
            targetedCountryISOCodes = dto.targetedLocations
        )
    }
}
