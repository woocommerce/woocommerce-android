package com.woocommerce.android.ui.blaze.campaigs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.blaze.BlazeCampaignStat
import com.woocommerce.android.ui.blaze.BlazeCampaignUi
import com.woocommerce.android.ui.blaze.BlazeProductUi
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource
import com.woocommerce.android.ui.blaze.CampaignStatusUi
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.persistence.blaze.BlazeCampaignsDao.BlazeCampaignEntity
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore
import javax.inject.Inject

@HiltViewModel

class BlazeCampaignListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val blazeCampaignsStore: BlazeCampaignsStore,
    private val selectedSite: SelectedSite,
    private val blazeUrlsHelper: BlazeUrlsHelper
) : ScopedViewModel(savedStateHandle) {
    private var totalPages = 1
    private var currentPage = 1
    private val isLoadingMore = MutableStateFlow(false)
    val state = combine(
        blazeCampaignsStore.observeBlazeCampaigns(selectedSite.get()),
        isLoadingMore
    ) { campaigns, loadingMore ->
        BlazeCampaignListState(
            campaigns = campaigns.map { mapToUiState(it) },
            onAddNewCampaignClicked = { onAddNewCampaignClicked() },
            isLoading = loadingMore
        )
    }.asLiveData()

    init {
        loadCampaignsFor(currentPage)
    }

    fun onEndOfTheListReached() {
        if (state.value?.isLoading == false) {
            isLoadingMore.value = true
            loadCampaignsFor(++currentPage)
            isLoadingMore.value = false
        }
    }

    private fun loadCampaignsFor(page: Int) {
        if (page <= totalPages) {
            launch {
                val result = blazeCampaignsStore.fetchBlazeCampaigns(selectedSite.get(), page)
                if (result.isError || result.model == null) {
                    triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.blaze_campaign_list_error_fetching_campaigns))
                } else {
                    totalPages = result.model?.totalPages ?: 1
                }
            }
        }
    }

    private fun onCampaignClicked(campaignId: Int) {
        val url = blazeUrlsHelper.buildCampaignDetailsUrl(campaignId)
        triggerEvent(
            ShowCampaignDetails(
                url = url,
                urlToTriggerExit = blazeUrlsHelper.buildCampaignsListUrl()
            )
        )
    }

    private fun mapToUiState(campaignEntity: BlazeCampaignEntity) =
        ClickableCampaign(
            campaignUi = BlazeCampaignUi(
                product = BlazeProductUi(
                    name = campaignEntity.title,
                    imgUrl = campaignEntity.imageUrl.orEmpty(),
                ),
                status = CampaignStatusUi.fromString(campaignEntity.uiStatus),
                stats = listOf(
                    BlazeCampaignStat(
                        name = R.string.blaze_campaign_status_impressions,
                        value = campaignEntity.impressions
                    ),
                    BlazeCampaignStat(
                        name = R.string.blaze_campaign_status_clicks,
                        value = campaignEntity.clicks
                    ),
                    BlazeCampaignStat(
                        name = R.string.blaze_campaign_status_clicks,
                        value = campaignEntity.budgetCents
                    )
                )
            ),
            onCampaignClicked = { onCampaignClicked(campaignEntity.campaignId) }
        )

    private fun onAddNewCampaignClicked() {
        val url = blazeUrlsHelper.buildUrlForSite(BlazeFlowSource.MY_STORE_BANNER)
        triggerEvent(LaunchBlazeCampaignCreation(url, BlazeFlowSource.CAMPAIGN_LIST))
    }

    data class BlazeCampaignListState(
        val campaigns: List<ClickableCampaign>,
        val onAddNewCampaignClicked: () -> Unit,
        val isLoading: Boolean,
    )

    data class ClickableCampaign(
        val campaignUi: BlazeCampaignUi,
        val onCampaignClicked: () -> Unit,
    )

    data class LaunchBlazeCampaignCreation(val url: String, val source: BlazeFlowSource) : MultiLiveEvent.Event()
    data class ShowCampaignDetails(
        val url: String,
        val urlToTriggerExit: String
    ) : MultiLiveEvent.Event()
}
