package com.woocommerce.android.ui.login.storecreation.profiler

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StoreProfilerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newStore: NewStore,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val resourceProvider: ResourceProvider,
) : BaseStoreProfilerViewModel(savedStateHandle, newStore) {
    override val hasSearchableContent: Boolean
        get() = false

    init {
        analyticsTracker.track(
            AnalyticsEvent.SITE_CREATION_STEP,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_PROFILER_FEATURES
            )
        )
    }

    override fun getProfilerStepDescription(): String {
        TODO("Not yet implemented")
    }

    override fun getProfilerStepTitle(): String {
        TODO("Not yet implemented")
    }

    override fun onContinueClicked() {
        TODO("Not yet implemented")
    }
}
