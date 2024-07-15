package com.woocommerce.android.ui.dashboard.google

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetAction
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetMenu
import com.woocommerce.android.ui.dashboard.DashboardViewModel.RefreshEvent
import com.woocommerce.android.ui.google.ObserveMostRecentGoogleAdsCampaign
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest

@HiltViewModel(assistedFactory = DashboardGoogleAdsViewModel.Factory::class)
class DashboardGoogleAdsViewModel @AssistedInject constructor(
    savedStateHandle: SavedStateHandle,
    @Assisted private val parentViewModel: DashboardViewModel,
    private val observeMostRecentGoogleAdsCampaign: ObserveMostRecentGoogleAdsCampaign
) : ScopedViewModel(savedStateHandle) {
    private val _refreshTrigger = MutableSharedFlow<RefreshEvent>(extraBufferCapacity = 1)
    private val refreshTrigger = merge(_refreshTrigger, (parentViewModel.refreshTrigger))
        .onStart { emit(RefreshEvent()) }

    @OptIn(ExperimentalCoroutinesApi::class)
    val viewState = refreshTrigger
        .transformLatest {
            emit(DashboardGoogleAdsState.Loading)

            emitAll(
                observeMostRecentGoogleAdsCampaign()
                    .map { result ->
                        result.fold(
                            onSuccess = { campaign ->
                                if (campaign == null) {
                                    DashboardGoogleAdsState.NoCampaigns(
                                        onCreateCampaignClicked = { },
                                        menu = DashboardWidgetMenu(emptyList())
                                    )
                                } else {
                                    DashboardGoogleAdsState.HasCampaigns(
                                        onCreateCampaignClicked = { },
                                        showAllCampaignsButton = DashboardWidgetAction(
                                            R.string.dashboard_google_ads_card_view_all_campaigns_button
                                        ) { },
                                        menu = DashboardWidgetMenu(emptyList())
                                    )
                                }
                            },
                            onFailure = {
                                // When unable to fetch campaigns, instead of showing error,
                                // assume that there are no campaigns so that merchants can still create campaign.
                                DashboardGoogleAdsState.NoCampaigns(
                                    onCreateCampaignClicked = { },
                                    menu = DashboardWidgetMenu(emptyList())
                                )
                            }
                        )
                    }
            )
        }
        .asLiveData()

    sealed class DashboardGoogleAdsState(
        open val menu: DashboardWidgetMenu,
        val mainButton: DashboardWidgetAction? = null
    ) {
        data object Loading : DashboardGoogleAdsState(DashboardWidgetMenu(emptyList()))

        data class NoCampaigns(
            val onCreateCampaignClicked: () -> Unit,
            override val menu: DashboardWidgetMenu
        ) : DashboardGoogleAdsState(menu)

        data class HasCampaigns(
            val onCreateCampaignClicked: () -> Unit,
            val showAllCampaignsButton: DashboardWidgetAction,
            override val menu: DashboardWidgetMenu
        ) : DashboardGoogleAdsState(menu, showAllCampaignsButton)
    }

    object LaunchGoogleAdsCampaignCreation : MultiLiveEvent.Event()
    object ShowAllCampaigns : MultiLiveEvent.Event()

    @AssistedFactory
    interface Factory {
        fun create(parentViewModel: DashboardViewModel?): DashboardGoogleAdsViewModel
    }
}
