package com.woocommerce.android.ui.blaze.campaigs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.blaze.BlazeCampaignStat
import com.woocommerce.android.ui.blaze.BlazeCampaignUi
import com.woocommerce.android.ui.blaze.BlazeProductUi
import com.woocommerce.android.ui.blaze.CampaignStatusUi.Active
import com.woocommerce.android.ui.blaze.CampaignStatusUi.InModeration
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel

class BlazeCampaignListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private val _state = MutableStateFlow(
        BlazeCampaignListState(
            campaigns = listOf(
                BlazeCampaignUi(
                    product = BlazeProductUi(
                        name = "Product name",
                        imgUrl = "https://hips.hearstapps.com/hmg-prod/images/gh-082420-ghi-best-sofas-1598293488.png",
                    ),
                    status = Active,
                    stats = listOf(
                        BlazeCampaignStat(
                            name = string.blaze_campaign_status_impressions,
                            value = 100
                        ),
                        BlazeCampaignStat(
                            name = string.blaze_campaign_status_clicks,
                            value = 10
                        ),
                        BlazeCampaignStat(
                            name = string.blaze_campaign_status_budget,
                            value = 1000
                        ),
                    ),
                ),
                BlazeCampaignUi(
                    product = BlazeProductUi(
                        name = "Product name",
                        imgUrl = "",
                    ),
                    status = InModeration,
                    stats = listOf(
                        BlazeCampaignStat(
                            name = string.blaze_campaign_status_impressions,
                            value = 100
                        ),
                        BlazeCampaignStat(
                            name = string.blaze_campaign_status_clicks,
                            value = 10
                        ),
                        BlazeCampaignStat(
                            name = string.blaze_campaign_status_budget,
                            value = 1000
                        ),
                    ),
                )
            ),
            isLoading = false
        )
    )
    val state = _state.asLiveData()

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
