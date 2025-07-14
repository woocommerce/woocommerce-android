package com.woocommerce.android.ui.google

import com.woocommerce.android.WooException
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.model.google.WCGoogleAdsCampaign
import org.wordpress.android.fluxc.store.WCGoogleStore
import javax.inject.Inject

class GoogleRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val googleStore: WCGoogleStore
) {
    suspend fun isGoogleAdsAccountConnected(): Boolean {
        val result = googleStore.isGoogleAdsAccountConnected(selectedSite.get())
        when {
            result.isError -> {
                WooLog.e(WooLog.T.GOOGLE_ADS, "Error checking Google Ads connection: ${result.error}")
                return false
            }
            else -> return result.model ?: false
        }
    }

    suspend fun fetchGoogleAdsCampaigns(excludeRemovedCampaigns: Boolean = true): Result<List<WCGoogleAdsCampaign>> {
        val result = googleStore.fetchGoogleAdsCampaigns(selectedSite.get(), excludeRemovedCampaigns)
        return when {
            result.isError -> Result.failure(WooException(result.error))
            else -> Result.success(result.model ?: emptyList())
        }
    }
}
