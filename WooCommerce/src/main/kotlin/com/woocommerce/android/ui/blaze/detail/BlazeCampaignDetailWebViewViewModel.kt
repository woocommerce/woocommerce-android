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
    val blazeUrlsHelper: BlazeUrlsHelper
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: BlazeCampaignDetailWebViewFragmentArgs by savedStateHandle.navArgs()
    val urlToLoad = navArgs.urlToLoad

    fun onUrlLoaded(url: String) {
        if (blazeUrlsHelper.buildCampaignsListUrl().contains(url)) {
            triggerEvent(Exit)
        }
    }

    fun onDismiss() {
        triggerEvent(Exit)
    }
}
