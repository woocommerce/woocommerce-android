package com.woocommerce.android.ui.google

import com.woocommerce.android.util.WooLog
import javax.inject.Inject

class HasGoogleAdsCampaigns @Inject constructor(
    private val googleRepository: GoogleRepository
) {

    /**
     * Check if there are any Google Ads campaigns.
     * Ignore removed (deleted) campaigns and only check for available campaigns.
     *
     * @return Returns true if there are any Google Ads campaigns, otherwise returns false.
     */
    suspend operator fun invoke(): Boolean {
        googleRepository.fetchGoogleAdsCampaigns(excludeRemovedCampaigns = true).fold(
            onSuccess = { campaigns ->
                return campaigns.isNotEmpty()
            },
            onFailure = { error ->
                WooLog.e(WooLog.T.GOOGLE_ADS, "Failed to fetch Google Ads campaigns: ${error.message} ")
                return false
            }
        )
    }
}
