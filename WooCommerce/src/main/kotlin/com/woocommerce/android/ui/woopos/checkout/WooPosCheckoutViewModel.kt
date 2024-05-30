package com.woocommerce.android.ui.woopos.checkout

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalytics
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosCheckoutViewModel @Inject constructor(
    private val analyticsTracker: WooPosAnalyticsTracker,
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    init {
        launch {
            analyticsTracker.track(WooPosAnalytics.Event.Test)
        }
    }
}
