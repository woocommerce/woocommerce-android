package com.woocommerce.android.ui.blaze

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore
import javax.inject.Inject

class ObserveMostRecentBlazeCampaign @Inject constructor(
    private val selectedSite: SelectedSite,
    private val blazeCampaignsStore: BlazeCampaignsStore
) {
    operator fun invoke(forceRefresh: Boolean): Flow<BlazeCampaignModel?> = flow {
        if (forceRefresh) {
            blazeCampaignsStore.fetchBlazeCampaigns(selectedSite.get())
        }

        emitAll(blazeCampaignsStore.observeMostRecentBlazeCampaign(selectedSite.get()))
    }.onStart {
        if (!forceRefresh) {
            // When not forcing a refresh, we'll start by emitting the cached value, and here we'll refresh
            blazeCampaignsStore.fetchBlazeCampaigns(selectedSite.get())
        }
    }
}
