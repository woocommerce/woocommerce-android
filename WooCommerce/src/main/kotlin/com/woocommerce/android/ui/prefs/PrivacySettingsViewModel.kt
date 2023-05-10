package com.woocommerce.android.ui.prefs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.util.dispatchAndAwait
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.PushAccountSettingsPayload
import javax.inject.Inject

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val dispatcher: Dispatcher,
    private val accountStore: AccountStore,
) : ScopedViewModel(savedState) {
    companion object {
        private const val SETTING_TRACKS_OPT_OUT = "tracks_opt_out"
    }

    private val _state = MutableLiveData(
        State(
            sendUsageStats = getSendUsageStats(),
            crashReportingEnabled = getCrashReportingEnabled(),
            progressBarVisible = false,
        )
    )
    val state: LiveData<State> = _state

    private fun getSendUsageStats() = !accountStore.account.tracksOptOut

    private fun setSendUsageStats(sendUsageStats: Boolean) {
        AnalyticsTracker.sendUsageStats = sendUsageStats

        launch {
            if (accountStore.hasAccessToken()) {
                val payload = PushAccountSettingsPayload().apply {
                    params = mapOf(SETTING_TRACKS_OPT_OUT to !sendUsageStats)
                }

                val action = AccountActionBuilder.newPushSettingsAction(payload)
                dispatcher.dispatchAndAwait<PushAccountSettingsPayload?, OnAccountChanged>(action)
            }
        }
    }

    private fun getCrashReportingEnabled() = AppPrefs.isCrashReportingEnabled()

    private fun setCrashReportingEnabled(enabled: Boolean) {
        AppPrefs.setCrashReportingEnabled(enabled)
    }

    fun onPrivacyPolicyClicked() {
        AnalyticsTracker.track(AnalyticsEvent.PRIVACY_SETTINGS_PRIVACY_POLICY_LINK_TAPPED)
        triggerEvent(PrivacySettingsEvent.ShowPrivacyPolicy)
    }

    fun onAdvertisingOptionsClicked() {
        triggerEvent(PrivacySettingsEvent.ShowAdvertisingOptions)
    }

    fun onCookiePolicyClicked() {
        AnalyticsTracker.track(AnalyticsEvent.PRIVACY_SETTINGS_THIRD_PARTY_TRACKING_INFO_LINK_TAPPED)
        triggerEvent(PrivacySettingsEvent.ShowCookiePolicy)
    }

    fun onCrashReportingSettingChanged(checked: Boolean) {
        _state.value = _state.value?.copy(crashReportingEnabled = checked)
        setCrashReportingEnabled(checked)
    }

    fun onSendStatsSettingChanged(checked: Boolean) {
        _state.value = _state.value?.copy(sendUsageStats = checked)
        setSendUsageStats(checked)
    }

    data class State(
        val sendUsageStats: Boolean,
        val crashReportingEnabled: Boolean,
        val progressBarVisible: Boolean,
    )

    sealed class PrivacySettingsEvent : MultiLiveEvent.Event() {
        object ShowAdvertisingOptions : PrivacySettingsEvent()
        object ShowCookiePolicy : PrivacySettingsEvent()
        object ShowPrivacyPolicy : PrivacySettingsEvent()
    }
}
