package com.woocommerce.android.ui.blaze

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_ENTRY_POINT_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.blaze.IsBlazeEnabled.BlazeFlowSource
import com.woocommerce.android.ui.blaze.IsBlazeEnabled.BlazeFlowSource.MY_STORE_BANNER
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlazeBannerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val isBlazeEnabled: IsBlazeEnabled,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {

    private val _isBlazeBannerVisible = MutableLiveData(false)
    val isBlazeBannerVisible = _isBlazeBannerVisible

    init {
        launch {
            if (isBlazeEnabled()) {
                _isBlazeBannerVisible.value = true
            }
        }
    }

    fun onBlazeBannerDismissed() {
        TODO()
    }

    fun onTryBlazeBannerClicked() {
        analyticsTrackerWrapper.track(
            stat = BLAZE_ENTRY_POINT_TAPPED,
            properties = mapOf(AnalyticsTracker.KEY_BLAZE_SOURCE to MY_STORE_BANNER.trackingName)
        )
        triggerEvent(
            OpenBlazeEvent(
                url = isBlazeEnabled.buildUrlForSite(MY_STORE_BANNER),
                source = MY_STORE_BANNER
            )
        )
    }

    data class OpenBlazeEvent(val url: String, val source: BlazeFlowSource) : MultiLiveEvent.Event()
}
