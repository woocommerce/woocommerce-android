package com.woocommerce.android.ui.login.storecreation.mystoresummary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.support.help.HelpOrigin.STORE_CREATION
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class MyStoreSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    newStore: NewStore,
    analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {

    val viewState = MutableStateFlow(
        MyStoreSummaryState(
            name = newStore.data.name,
            domain = newStore.data.domain ?: "",
            category = newStore.data.industry,
            country = newStore.data.country
        )
    ).asLiveData()

    init {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SITE_CREATION_STEP,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_SUMMARY
            )
        )
    }

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onHelpPressed() {
        triggerEvent(MultiLiveEvent.Event.NavigateToHelpScreen(STORE_CREATION))
    }

    fun onContinueClicked() {
        triggerEvent(NavigateToNextStep)
    }

    data class MyStoreSummaryState(
        val name: String? = null,
        val domain: String,
        val category: String? = null,
        val country: String? = null,
    )

    object NavigateToNextStep : MultiLiveEvent.Event()
}
