package com.woocommerce.android.ui.blaze

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore
import javax.inject.Inject

class BlazeRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val blazeCampaignsStore: BlazeCampaignsStore
) {
    suspend fun getMostRecentCampaign() = blazeCampaignsStore.getMostRecentBlazeCampaign(selectedSite.get())
}
