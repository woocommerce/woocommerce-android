package com.woocommerce.android.ui.blaze

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore
import javax.inject.Inject

class ObserveMostRecentBlazeCampaign @Inject constructor(
    private val selectedSite: SelectedSite,
    private val blazeCampaignsStore: BlazeCampaignsStore
) {
    operator fun invoke(forceRefresh: Boolean): Flow<Result<BlazeCampaignModel?>> = flow {
        if (forceRefresh) {
            blazeCampaignsStore.fetchBlazeCampaigns(selectedSite.get()).let {
                if (it.isError) {
                    emit(Result.failure(OnChangedException(it.error)))
                    return@flow
                }
            }
        }

        emitAll(blazeCampaignsStore.observeMostRecentBlazeCampaign(selectedSite.get()).map { Result.success(it) })
    }.onStart {
        if (!forceRefresh) {
            // When not forcing a refresh, we'll start by emitting the cached value, and here we'll refresh
            blazeCampaignsStore.fetchBlazeCampaigns(selectedSite.get())
        }
    }
}
