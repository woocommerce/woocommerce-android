package com.woocommerce.android.ui.woopos.eligibility

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.tab.WooPosCanBeLaunchedInTab
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability
import com.woocommerce.android.ui.woopos.util.WooPosGetStoreCountryCode
import com.woocommerce.android.ui.woopos.util.WooPosGetStoreCountryName
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface WooPosEligibilityRetryState {
    data class Loading(val suggestionText: String?) : WooPosEligibilityRetryState
    object Eligible : WooPosEligibilityRetryState
    data class Ineligible(val suggestionText: String) : WooPosEligibilityRetryState
}

@HiltViewModel
class WooPosEligibilityViewModel @Inject constructor(
    private val canBeLaunchedInTab: WooPosCanBeLaunchedInTab,
    private val tracker: WooPosAnalyticsTracker,
    private val resourceProvider: ResourceProvider,
    private val getCountryName: WooPosGetStoreCountryName,
    private val getCountryCode: WooPosGetStoreCountryCode
) : ViewModel() {

    private val _retryState = MutableStateFlow<WooPosEligibilityRetryState>(WooPosEligibilityRetryState.Loading(null))
    val retryState: StateFlow<WooPosEligibilityRetryState> = _retryState

    private var currentReason: WooPosLaunchability.NonLaunchabilityReason? = null

    suspend fun initialize(reason: WooPosLaunchability.NonLaunchabilityReason) {
        currentReason = reason
        val suggestionText = getSuggestionText(reason)
        _retryState.value = WooPosEligibilityRetryState.Ineligible(suggestionText)
        tracker.track(WooPosAnalyticsEvent.Event.IneligibleUIShown(reason))
    }

    fun retryEligibilityCheckTapped() {
        viewModelScope.launch {
            trackIneligibleRetryTapped()
            val currentSuggestionText = (_retryState.value as? WooPosEligibilityRetryState.Ineligible)?.suggestionText
            _retryState.value = WooPosEligibilityRetryState.Loading(currentSuggestionText)

            val result = canBeLaunchedInTab(forceRefresh = true)

            _retryState.value = when (result) {
                is WooPosLaunchability.Launchable -> {
                    currentReason = null
                    WooPosEligibilityRetryState.Eligible
                }
                is WooPosLaunchability.NotLaunchable -> {
                    currentReason = result.reason
                    val suggestionText = getSuggestionText(result.reason)
                    WooPosEligibilityRetryState.Ineligible(suggestionText)
                }
            }
        }
    }

    private suspend fun getSuggestionText(reason: WooPosLaunchability.NonLaunchabilityReason): String {
        return when (reason) {
            WooPosLaunchability.NonLaunchabilityReason.WooCommercePluginNotFound ->
                resourceProvider.getString(R.string.woopos_eligibility_reason_woocommerce_plugin_not_found)
            WooPosLaunchability.NonLaunchabilityReason.UnsupportedWooCommerceVersion ->
                resourceProvider.getString(
                    R.string.woopos_eligibility_reason_unsupported_woocommerce_version,
                    WooPosCanBeLaunchedInTab.MINIMUM_SUPPORTED_WC_VERSION
                )
            WooPosLaunchability.NonLaunchabilityReason.SiteSettingsUnavailable ->
                resourceProvider.getString(R.string.woopos_eligibility_reason_check_connection)
            WooPosLaunchability.NonLaunchabilityReason.FeatureSwitchDisabled ->
                resourceProvider.getString(R.string.woopos_eligibility_reason_feature_switch_disabled)
            WooPosLaunchability.NonLaunchabilityReason.UnsupportedCurrency -> {
                val countryCode = getCountryCode()
                val supportedCurrency = WooPosCanBeLaunchedInTab.SUPPORTED_COUNTRY_CURRENCY_PAIRS
                    .find { (country, _) -> country.equals(countryCode, ignoreCase = true) }
                    ?.second?.uppercase()

                val countryName = getCountryName()
                if (countryName != null && supportedCurrency != null) {
                    resourceProvider.getString(
                        R.string.woopos_eligibility_reason_unsupported_currency,
                        countryName,
                        supportedCurrency
                    )
                } else {
                    ""
                }
            }
            WooPosLaunchability.NonLaunchabilityReason.NoSiteSelected ->
                resourceProvider.getString(R.string.woopos_eligibility_reason_check_connection)
        }
    }

    private suspend fun trackIneligibleRetryTapped() {
        val reason = currentReason ?: return
        tracker.track(WooPosAnalyticsEvent.Event.IneligibleUIRetryTapped(reason))
    }
}
