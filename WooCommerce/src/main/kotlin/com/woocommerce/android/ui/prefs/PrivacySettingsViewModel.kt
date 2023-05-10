package com.woocommerce.android.ui.prefs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.AccountStore
import javax.inject.Inject

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val accountStore: AccountStore,
    private val appPrefs: AppPrefsWrapper,
    private val resourceProvider: ResourceProvider,
    private val repository: PrivacySettingsRepository,
) : ScopedViewModel(savedState) {

    private val _state = MutableLiveData(
        State(
            sendUsageStats = getSendUsageStats(),
            crashReportingEnabled = getCrashReportingEnabled(),
            progressBarVisible = false,
        )
    )
    val state: LiveData<State> = _state

    init {
        initialize()
    }

    fun initialize() {
        launch {
            val event = repository.fetchAccountSettings()

            event.fold(
                onSuccess = {
                    appPrefs.sendUsageStats(!accountStore.account.tracksOptOut)
                    _state.value =
                        _state.value?.copy(sendUsageStats = !accountStore.account.tracksOptOut)
                },
                onFailure = {
                    triggerEvent(
                        MultiLiveEvent.Event.ShowActionSnackbar(
                            resourceProvider.getString(R.string.settings_tracking_analytics_error_fetch)
                        ) {
                            initialize()
                        }
                    )
                }
            )
        }
    }

    private fun getSendUsageStats() = !accountStore.account.tracksOptOut

    private fun getCrashReportingEnabled() = appPrefs.isCrashReportingEnabled()

    private fun setCrashReportingEnabled(enabled: Boolean) {
        appPrefs.setCrashReportingEnabled(enabled)
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
        launch {
            if (accountStore.hasAccessToken()) {

                _state.value = _state.value?.copy(
                    sendUsageStats = checked, progressBarVisible = true
                )

                val event = repository.updateTracksSetting(checked)

                _state.value = _state.value?.copy(progressBarVisible = false)

                event.fold(
                    onSuccess = {
                        appPrefs.sendUsageStats(checked)
                    },
                    onFailure = {
                        _state.value = _state.value?.copy(sendUsageStats = !checked)
                        triggerEvent(
                            MultiLiveEvent.Event.ShowActionSnackbar(
                                resourceProvider.getString(R.string.settings_tracking_analytics_error_update)
                            ) { onSendStatsSettingChanged(state.value!!.sendUsageStats.not()) }
                        )
                    }
                )
            }
        }
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
