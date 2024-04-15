package com.woocommerce.android.ui.blaze

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore
import javax.inject.Inject

class ObserveMostRecentBlazeCampaign @Inject constructor(
    private val selectedSite: SelectedSite,
    private val blazeCampaignsStore: BlazeCampaignsStore
) {
    operator fun invoke(): Flow<BlazeCampaignModel?> = flow {
        blazeCampaignsStore.fetchBlazeCampaigns(selectedSite.get())

        emitAll(blazeCampaignsStore.observeMostRecentBlazeCampaign(selectedSite.get()))
    }
}
