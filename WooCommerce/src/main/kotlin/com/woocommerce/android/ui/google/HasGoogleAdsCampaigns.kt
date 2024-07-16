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
     * @return Returns a Result containing a Boolean:
     *         - Result.success(true) if there are any Google Ads campaigns
     *         - Result.success(false) if there are no Google Ads campaigns
     *         - Result.failure(exception) if an error occurred during the check
     */
    suspend operator fun invoke(): Result<Boolean> =
        googleRepository.fetchGoogleAdsCampaigns(excludeRemovedCampaigns = true).fold(
            onSuccess = { campaigns ->
                Result.success(campaigns.isNotEmpty())
            },
            onFailure = { error ->
                WooLog.e(WooLog.T.GOOGLE_ADS, "Failed to fetch Google Ads campaigns: ${error.message}")
                Result.failure(error)
            }
        )
}
