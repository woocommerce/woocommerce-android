package com.woocommerce.android.ui.blaze.detail

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper
import com.woocommerce.android.ui.blaze.detail.BlazeCampaignDetailWebViewViewModel.BlazeAction.CampaignStopped
import com.woocommerce.android.ui.blaze.detail.BlazeCampaignDetailWebViewViewModel.BlazeAction.None
import com.woocommerce.android.ui.blaze.detail.BlazeCampaignDetailWebViewViewModel.BlazeAction.PromoteProductAgain
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignDetailWebViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val blazeUrlsHelper: BlazeUrlsHelper
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: BlazeCampaignDetailWebViewFragmentArgs by savedStateHandle.navArgs()

    var viewState = ViewState(
        urlToLoad = blazeUrlsHelper.buildCampaignDetailsUrl(navArgs.campaignId),
        blazeAction = None
    )

    fun onUrlLoaded(url: String) {
        when {
            blazeUrlsHelper.buildCampaignsListUrl() == url -> onDismiss()
            url.contains(blazeUrlsHelper.getCampaignStopUrlPath(navArgs.campaignId)) -> {
                viewState = viewState.copy(blazeAction = CampaignStopped)
            }

            url.contains(BlazeUrlsHelper.PROMOTE_AGAIN_URL_PATH) -> {
                viewState = viewState.copy(
                    blazeAction = PromoteProductAgain(
                        productId = blazeUrlsHelper.extractProductIdFromPromoteAgainUrl(url)
                    )
                )
                onDismiss()
            }
        }
    }

    fun onDismiss() {
        when (viewState.blazeAction) {
            None -> triggerEvent(Exit)
            else -> triggerEvent(ExitWithResult(viewState.blazeAction))
        }
    }

    data class ViewState(
        val urlToLoad: String,
        val blazeAction: BlazeAction,
    )

    @Parcelize
    sealed interface BlazeAction : Parcelable {
        @Parcelize
        data object CampaignStopped : BlazeAction

        @Parcelize
        data class PromoteProductAgain(val productId: Long?) : BlazeAction

        @Parcelize
        data object None : BlazeAction
    }
}
