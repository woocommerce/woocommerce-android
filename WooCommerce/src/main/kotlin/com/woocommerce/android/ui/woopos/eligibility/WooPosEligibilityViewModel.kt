package com.woocommerce.android.ui.woopos.eligibility

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.tab.WooPosCanBeLaunchedInTab
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface WooPosEligibilityRetryState {
    object Loading : WooPosEligibilityRetryState
    object Eligible : WooPosEligibilityRetryState
    data class Ineligible(val reason: WooPosLaunchability.NonLaunchabilityReason) : WooPosEligibilityRetryState
}

@HiltViewModel
class WooPosEligibilityViewModel @Inject constructor(
    private val canBeLaunchedInTab: WooPosCanBeLaunchedInTab,
    private val tracker: WooPosAnalyticsTracker
) : ViewModel() {

    private val _retryState = MutableStateFlow<WooPosEligibilityRetryState>(WooPosEligibilityRetryState.Loading)
    val retryState: StateFlow<WooPosEligibilityRetryState> = _retryState

    suspend fun initialize(reason: WooPosLaunchability.NonLaunchabilityReason) {
        _retryState.value = WooPosEligibilityRetryState.Ineligible(reason)
        tracker.track(WooPosAnalyticsEvent.Event.IneligibleUIShown(reason))
    }

    fun retryEligibilityCheckTapped() {
        viewModelScope.launch {
            trackIneligibleRetryTapped()
            _retryState.value = WooPosEligibilityRetryState.Loading

            val result = canBeLaunchedInTab(forceRefresh = true)

            _retryState.value = when (result) {
                is WooPosLaunchability.Launchable -> WooPosEligibilityRetryState.Eligible
                is WooPosLaunchability.NotLaunchable -> WooPosEligibilityRetryState.Ineligible(result.reason)
            }
        }
    }

    private suspend fun trackIneligibleRetryTapped() {
        val reason = (_retryState.value as? WooPosEligibilityRetryState.Ineligible)?.reason ?: return
        tracker.track(WooPosAnalyticsEvent.Event.IneligibleUIRetryTapped(reason))
    }
}
