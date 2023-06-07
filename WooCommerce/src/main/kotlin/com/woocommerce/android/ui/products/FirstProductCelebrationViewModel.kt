package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FirstProductCelebrationViewModel @Inject constructor(
    private val tracker: AnalyticsTrackerWrapper,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: FirstProductCelebrationDialogArgs by savedStateHandle.navArgs()

    init {
        tracker.track(AnalyticsEvent.FIRST_CREATED_PRODUCT_SHOWN)
    }
    fun onShareButtonClicked() {
        tracker.track(AnalyticsEvent.FIRST_CREATED_PRODUCT_SHARE_TAPPED)
        triggerEvent(ProductNavigationTarget.ShareProduct(navArgs.permalink, navArgs.productName))
    }

    fun onDismissButtonClicked() {
        triggerEvent(Exit)
    }
}
