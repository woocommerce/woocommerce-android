package com.woocommerce.android.ui.google

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
        val campaigns = googleRepository.fetchGoogleAdsCampaigns(excludeRemovedCampaigns = true)
        return campaigns.isNotEmpty()
    }
}
