package com.woocommerce.android.ui.google

import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class IsGoogleForWooEnabled @Inject constructor(
    private val googleRepository: GoogleRepository
) {
    suspend operator fun invoke(): Boolean {
        return FeatureFlag.GOOGLE_ADS_M1.isEnabled() && googleRepository.isGoogleAdsAccountConnected()
    }
}
