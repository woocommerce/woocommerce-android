package com.woocommerce.android.ui.woopos.eligibility

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.tab.WooPosCanBeLaunchedInTab
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

sealed interface WooPosEligibilityRetryState {
    data class Loading(val title: String, val suggestionText: String) : WooPosEligibilityRetryState

    sealed interface Ineligible : WooPosEligibilityRetryState {
        val title: String
        val suggestionText: String
    }

    data class RetryableIneligible(
        override val title: String,
        override val suggestionText: String,
    ) : Ineligible
}

@HiltViewModel
class WooPosEligibilityViewModel @Inject constructor(
    private val canBeLaunchedInTab: WooPosCanBeLaunchedInTab,
    private val tracker: WooPosAnalyticsTracker,
    private val resourceProvider: ResourceProvider,
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
) : ViewModel() {

    private val _retryState = MutableStateFlow<WooPosEligibilityRetryState?>(null)
    val retryState: StateFlow<WooPosEligibilityRetryState?> = _retryState

    private val _navigateToPos = Channel<Unit>(Channel.BUFFERED)
    val navigateToPos = _navigateToPos.receiveAsFlow()

    private var currentReason: WooPosLaunchability.NonLaunchabilityReason? = null

    suspend fun initialize(reason: WooPosLaunchability.NonLaunchabilityReason) {
        currentReason = reason
        _retryState.value = buildIneligibleState(reason)
        tracker.track(WooPosAnalyticsEvent.Event.IneligibleUIShown(reason))
    }

    fun retryEligibilityCheckTapped() {
        viewModelScope.launch {
            trackIneligibleRetryTapped()
        }
        recheckEligibility()
    }

    private fun recheckEligibility() {
        viewModelScope.launch {
            val currentState = _retryState.value as WooPosEligibilityRetryState.Ineligible
            _retryState.value = WooPosEligibilityRetryState.Loading(
                title = currentState.title,
                suggestionText = currentState.suggestionText,
            )

            selectedSite.getOrNull()?.let { site ->
                wooCommerceStore.fetchWooCommerceSite(site).model?.let { selectedSite.set(it) }
            }
            val result = canBeLaunchedInTab(forceRefresh = true)

            when (result) {
                is WooPosLaunchability.Launchable -> {
                    currentReason = null
                    _navigateToPos.trySend(Unit)
                }
                is WooPosLaunchability.NotLaunchable -> {
                    currentReason = result.reason
                    tracker.track(WooPosAnalyticsEvent.Event.IneligibleUIShown(result.reason))
                    _retryState.value = buildIneligibleState(result.reason)
                }
            }
        }
    }

    private fun buildIneligibleState(
        reason: WooPosLaunchability.NonLaunchabilityReason
    ): WooPosEligibilityRetryState.Ineligible {
        return WooPosEligibilityRetryState.RetryableIneligible(
            title = resourceProvider.getString(R.string.woopos_eligibility_screen_unable_to_load),
            suggestionText = getSuggestionText(reason),
        )
    }

    private fun getSuggestionText(reason: WooPosLaunchability.NonLaunchabilityReason): String {
        return when (reason) {
            WooPosLaunchability.NonLaunchabilityReason.UnsupportedWooCommerceVersion ->
                resourceProvider.getString(
                    R.string.woopos_eligibility_reason_unsupported_woocommerce_version,
                    WooPosCanBeLaunchedInTab.MINIMUM_SUPPORTED_WC_VERSION
                )
            WooPosLaunchability.NonLaunchabilityReason.SiteSettingsUnavailable ->
                resourceProvider.getString(R.string.woopos_eligibility_reason_check_connection)
            WooPosLaunchability.NonLaunchabilityReason.NoSiteSelected,
            WooPosLaunchability.NonLaunchabilityReason.UnknownNoPositiveCache ->
                resourceProvider.getString(R.string.woopos_eligibility_reason_check_connection)
        }
    }

    private suspend fun trackIneligibleRetryTapped() {
        val reason = currentReason ?: return
        tracker.track(WooPosAnalyticsEvent.Event.IneligibleUIRetryTapped(reason))
    }
}
