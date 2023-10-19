package com.woocommerce.android.ui.blaze.campaigs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R.string
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.blaze.BlazeCampaignStat
import com.woocommerce.android.ui.blaze.BlazeCampaignUi
import com.woocommerce.android.ui.blaze.BlazeProductUi
import com.woocommerce.android.ui.blaze.CampaignStatusUi
import com.woocommerce.android.ui.blaze.IsBlazeEnabled
import com.woocommerce.android.ui.blaze.IsBlazeEnabled.BlazeFlowSource
import com.woocommerce.android.viewmodel.MultiLiveEvent
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
    private val selectedSite: SelectedSite,
    private val isBlazeEnabled: IsBlazeEnabled
) : ScopedViewModel(savedStateHandle) {
    val state = blazeCampaignsStore.observeBlazeCampaigns(
        selectedSite.get()
    )
        .map { campaigns ->
            BlazeCampaignListState(
                campaigns = campaigns
                    .map {
                        CampaignState(
                            campaignUi = BlazeCampaignUi(
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
                            ),
                            onCampaignClicked = { onCampaignClicked(it.campaignId) }
                        )
                    },
                onAddNewCampaignClicked = { onAddNewCampaignClicked() },
                isLoading = false
            )
        }
        .asLiveData()

    init {
        launch {
            blazeCampaignsStore.fetchBlazeCampaigns(selectedSite.get())
        }
    }

    private fun onCampaignClicked(campaignId: Int) {
        val url = isBlazeEnabled.buildCampaignDetailsUrl(campaignId)
        triggerEvent(
            ShowCampaignDetails(
                url = url,
                urlToTriggerExit = isBlazeEnabled.buildCampaignsListUrl()
            )
        )
    }

    private fun onAddNewCampaignClicked() {
        val url = isBlazeEnabled.buildUrlForSite(BlazeFlowSource.MY_STORE_BANNER)
        triggerEvent(LaunchBlazeCampaignCreation(url, BlazeFlowSource.CAMPAIGN_LIST))
    }

    data class BlazeCampaignListState(
        val campaigns: List<CampaignState>,
        val onAddNewCampaignClicked: () -> Unit,
        val isLoading: Boolean,
    )

    data class CampaignState(
        val campaignUi: BlazeCampaignUi,
        val onCampaignClicked: () -> Unit,
    )

    data class LaunchBlazeCampaignCreation(val url: String, val source: BlazeFlowSource) : MultiLiveEvent.Event()
    data class ShowCampaignDetails(
        val url: String,
        val urlToTriggerExit: String
    ) : MultiLiveEvent.Event()
}
