package com.woocommerce.android.ui.blaze.campaigs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R.string
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.blaze.BlazeCampaignStat
import com.woocommerce.android.ui.blaze.BlazeCampaignUi
import com.woocommerce.android.ui.blaze.BlazeProductUi
import com.woocommerce.android.ui.blaze.CampaignStatusUi
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore
import javax.inject.Inject

@HiltViewModel

class BlazeCampaignListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val blazeCampaignsStore: BlazeCampaignsStore,
    private val selectedSite: SelectedSite
) : ScopedViewModel(savedStateHandle) {
    val state = blazeCampaignsStore.observeBlazeCampaigns(
        selectedSite.get()
    )
        .map { campaigns ->
            BlazeCampaignListState(
                campaigns = campaigns
                    .map {
                        BlazeCampaignUi(
                            product = BlazeProductUi(
                                name = it.title,
                                imgUrl = it.imageUrl.orEmpty(),
                            ),
                            status = CampaignStatusUi.fromString(it.uiStatus),
                            stats = listOf(
                                BlazeCampaignStat(
                                    name = string.blaze_campaign_status_impressions,
                                    value = it.impressions
                                ),
                                BlazeCampaignStat(
                                    name = string.blaze_campaign_status_clicks,
                                    value = it.clicks
                                ),
                                BlazeCampaignStat(
                                    name = string.blaze_campaign_status_clicks,
                                    value = it.budgetCents
                                )
                            )
                        )
                    },
                isLoading = false
            )
        }
        .asLiveData()

    init {
        launch {
            blazeCampaignsStore.fetchBlazeCampaigns(selectedSite.get())
        }
    }

    fun onCampaignSelected() {
        // TODO
    }

    fun onAddNewCampaignClicked() {
        // TODO
    }

    data class BlazeCampaignListState(
        val campaigns: List<BlazeCampaignUi>,
        val isLoading: Boolean,
    )
}
