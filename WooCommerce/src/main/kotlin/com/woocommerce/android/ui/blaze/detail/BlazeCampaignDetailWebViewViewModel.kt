package com.woocommerce.android.ui.blaze.detail

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignDetailWebViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val blazeUrlsHelper: BlazeUrlsHelper
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: BlazeCampaignDetailWebViewFragmentArgs by savedStateHandle.navArgs()

    val viewState = ViewState(
        urlToLoad = blazeUrlsHelper.buildCampaignDetailsUrl(navArgs.campaignId),
        campaignCancelled = false
    )

    fun onUrlLoaded(url: String) {
        when {
            blazeUrlsHelper.buildCampaignsListUrl().contains(url) -> triggerEvent(Exit)
            url.contains(blazeUrlsHelper.getCampaignStopUrlPath(navArgs.campaignId)) -> {
                viewState.copy(campaignCancelled = true)
            }
        }
    }

    fun onDismiss() {
        triggerEvent(Exit)
    }

    data class ViewState(
        val urlToLoad: String,
        val campaignCancelled: Boolean,
    )
}
