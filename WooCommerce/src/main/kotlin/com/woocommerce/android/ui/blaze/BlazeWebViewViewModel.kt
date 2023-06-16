package com.woocommerce.android.ui.blaze

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.blaze.IsBlazeEnabled.BlazeFlowSource
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.fluxc.network.UserAgent
import javax.inject.Inject

@HiltViewModel

class BlazeWebViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val wpComWebViewAuthenticator: WPComWebViewAuthenticator,
    val userAgent: UserAgent,
    val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: BlazeWebViewFragmentArgs by savedStateHandle.navArgs()

    val viewState = navArgs.let {
        BlazeWebViewState(
            urlToLoad = it.urlToLoad,
            currentLoadedUrl = it.urlToLoad,
            source = it.source
        )
    }

    fun onUrlLoaded(url: String) {
        if (url.contains(IsBlazeEnabled.BLAZE_CREATION_FLOW_PRODUCT, ignoreCase = true)) {
            Log.i("Test", "MIERDA URL LOADED")
        }
    }

    fun onClose() {
        triggerEvent(Exit)
    }

    data class BlazeWebViewState(
        val urlToLoad: String,
        val currentLoadedUrl: String,
        val source: BlazeFlowSource
    )
}
