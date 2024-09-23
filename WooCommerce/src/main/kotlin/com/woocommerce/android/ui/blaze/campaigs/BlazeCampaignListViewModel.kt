package com.woocommerce.android.ui.blaze.campaigs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_CAMPAIGN_DETAIL_SELECTED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.NumberExtensionsWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.blaze.BlazeCampaignUi
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource
import com.woocommerce.android.ui.blaze.detail.BlazeCampaignDetailWebViewViewModel
import com.woocommerce.android.ui.blaze.detail.BlazeCampaignDetailWebViewViewModel.BlazeAction.CampaignStopped
import com.woocommerce.android.ui.blaze.detail.BlazeCampaignDetailWebViewViewModel.BlazeAction.None
import com.woocommerce.android.ui.blaze.detail.BlazeCampaignDetailWebViewViewModel.BlazeAction.PromoteProductAgain
import com.woocommerce.android.ui.blaze.toUiState
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class BlazeCampaignListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val blazeCampaignsStore: BlazeCampaignsStore,
    private val selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val currencyFormatter: CurrencyFormatter,
    private val numberExtensionsWrapper: NumberExtensionsWrapper
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val LOADING_TRANSITION_DELAY = 200L
    }

    private val navArgs: BlazeCampaignListFragmentArgs by savedStateHandle.navArgs()

    private var totalItems = 0
    private val isLoadingMore = MutableStateFlow(false)
    private val isCampaignCelebrationShown = MutableStateFlow(false)

    val state = combine(
        blazeCampaignsStore.observeBlazeCampaigns(selectedSite.get()),
        isLoadingMore.withIndex().debounce { (index, isLoading) ->
            if (index != 0 && !isLoading) {
                // When resetting to not loading, wait a bit to make sure the campaigns list has been fetched from DB
                LOADING_TRANSITION_DELAY
            } else {
                0L
            }
        }.map { it.value },
        isCampaignCelebrationShown
    ) { campaigns, loadingMore, isBlazeCelebrationScreenShown ->
        BlazeCampaignListState(
            campaigns = campaigns.map {
                ClickableCampaign(
                    campaignUi = it.toUiState(currencyFormatter, numberExtensionsWrapper),
                    onCampaignClicked = { onCampaignClicked(it.campaignId) }
                )
            },
            onAddNewCampaignClicked = { onAddNewCampaignClicked() },
            isLoading = loadingMore,
            isCampaignCelebrationShown = isBlazeCelebrationScreenShown
        )
    }.asLiveData()

    init {
        if (navArgs.isPostCampaignCreation) {
            showCampaignCelebrationIfNeeded()
        }
        if (navArgs.campaignId != null) {
            triggerEvent(
                ShowCampaignDetails(campaignId = navArgs.campaignId!!)
            )
        }
        launch {
            loadCampaigns(offset = 0)
        }
    }

    fun onLoadMoreCampaigns() {
        val offset = state.value?.campaigns?.size ?: 0
        if (!isLoadingMore.value && offset < totalItems) {
            launch {
                isLoadingMore.value = true
                loadCampaigns(offset)
                isLoadingMore.value = false
            }
        }
    }

    fun onCampaignCelebrationDismissed() {
        isCampaignCelebrationShown.value = false
    }

    private suspend fun loadCampaigns(offset: Int) {
        val result = blazeCampaignsStore.fetchBlazeCampaigns(selectedSite.get(), offset)
        if (result.isError || result.model == null) {
            triggerEvent(Event.ShowSnackbar(R.string.blaze_campaign_list_error_fetching_campaigns))
        } else {
            totalItems = result.model?.totalItems ?: 0
        }
    }

    private fun onCampaignClicked(campaignId: String) {
        analyticsTrackerWrapper.track(
            stat = BLAZE_CAMPAIGN_DETAIL_SELECTED,
            properties = mapOf(AnalyticsTracker.KEY_BLAZE_SOURCE to BlazeFlowSource.CAMPAIGN_LIST.trackingName)
        )
        triggerEvent(
            ShowCampaignDetails(campaignId)
        )
    }

    private fun onAddNewCampaignClicked() {
        triggerEvent(LaunchBlazeCampaignCreation(BlazeFlowSource.CAMPAIGN_LIST))
    }

    private fun showCampaignCelebrationIfNeeded() {
        if (!appPrefsWrapper.isBlazeCelebrationScreenShown) {
            isCampaignCelebrationShown.value = true
            appPrefsWrapper.isBlazeCelebrationScreenShown = true
        }
    }

    fun onBlazeCampaignWebViewAction(action: BlazeCampaignDetailWebViewViewModel.BlazeAction) {
        when (action) {
            CampaignStopped -> launch { loadCampaigns(offset = 0) }
            is PromoteProductAgain -> triggerEvent(
                LaunchBlazeCampaignCreationForProduct(
                    productId = action.productId,
                    source = BlazeFlowSource.CAMPAIGN_LIST
                )
            )

            None -> Unit // Do nothing
        }
    }

    data class BlazeCampaignListState(
        val campaigns: List<ClickableCampaign>,
        val onAddNewCampaignClicked: () -> Unit,
        val isLoading: Boolean,
        val isCampaignCelebrationShown: Boolean
    )

    data class ClickableCampaign(
        val campaignUi: BlazeCampaignUi,
        val onCampaignClicked: () -> Unit,
    )

    data class LaunchBlazeCampaignCreation(val source: BlazeFlowSource) : Event()
    data class LaunchBlazeCampaignCreationForProduct(
        val productId: Long?,
        val source: BlazeFlowSource,
    ) : Event()

    data class ShowCampaignDetails(val campaignId: String) : Event()
}
