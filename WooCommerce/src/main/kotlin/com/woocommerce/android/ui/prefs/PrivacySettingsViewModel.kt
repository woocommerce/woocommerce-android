package com.woocommerce.android.ui.prefs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.SETTING_CHANGE
import com.woocommerce.android.analytics.AnalyticsEvent.SETTING_CHANGE_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.SETTING_CHANGE_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.util.AnalyticsUtils
import com.woocommerce.android.util.dispatchAndAwait
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.AccountErrorType
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
        )
    )
    val state: LiveData<State> = _state

    private fun getSendUsageStats() = !accountStore.account.tracksOptOut

    private fun setSendUsageStats(sendUsageStats: Boolean) {
        // note that we don't init/disable Crashlytics here because that requires the app to be restarted
        AnalyticsTracker.sendUsageStats = sendUsageStats

        launch {
            // sync with wpcom if a token is available
            if (accountStore.hasAccessToken()) {
                AnalyticsTracker.track(
                    SETTING_CHANGE,
                    mapOf(
                        AnalyticsTracker.KEY_NAME to SETTING_TRACKS_OPT_OUT,
                        AnalyticsTracker.KEY_FROM to !sendUsageStats,
                        AnalyticsTracker.KEY_TO to sendUsageStats
                    )
                )

                val payload = PushAccountSettingsPayload().apply {
                    params = mapOf(SETTING_TRACKS_OPT_OUT to !sendUsageStats)
                }

                val action = AccountActionBuilder.newPushSettingsAction(payload)
                val event: OnAccountChanged = dispatcher.dispatchAndAwait(action)

                handleOnAccountChanged(event)
            }
        }
    }

    fun getCrashReportingEnabled() = AppPrefs.isCrashReportingEnabled()

    fun onPoliciesClicked() {
        triggerEvent(PrivacySettingsEvent.OpenPolicies)
    }

    private fun setCrashReportingEnabled(enabled: Boolean) {
        AppPrefs.setCrashReportingEnabled(enabled)
    }

    private fun handleOnAccountChanged(event: OnAccountChanged) {
        if (event.isError) {
            when (event.error.type) {
                AccountErrorType.SETTINGS_POST_ERROR -> {
                    AnalyticsTracker.track(
                        SETTING_CHANGE_FAILED,
                        this::class.java.simpleName,
                        event.error.type.toString(),
                        event.error.message
                    )
                }

                else -> {}
            }
        } else {
            when (event.causeOfChange) {
                AccountAction.PUSH_SETTINGS -> {
                    AnalyticsTracker.track(SETTING_CHANGE_SUCCESS)
                }

                else -> Unit // Do nothing
            }
        }
    }

    fun onAdvertisingOptionsClicked() {
        triggerEvent(PrivacySettingsEvent.ShowAdvertisingOptions)
    }

    fun onCrashReportingSettingChanged(checked: Boolean) {
        AnalyticsTracker.track(
            AnalyticsEvent.PRIVACY_SETTINGS_CRASH_REPORTING_TOGGLED,
            mapOf(
                AnalyticsTracker.KEY_STATE to AnalyticsUtils.getToggleStateLabel(checked)
            )
        )
        _state.value = _state.value?.copy(crashReportingEnabled = checked)
        setCrashReportingEnabled(checked)
    }

    fun onSendStatsSettingChanged(checked: Boolean) {
        AnalyticsTracker.track(
            AnalyticsEvent.PRIVACY_SETTINGS_COLLECT_INFO_TOGGLED,
            mapOf(
                AnalyticsTracker.KEY_STATE to
                    AnalyticsUtils.getToggleStateLabel(checked)
            )
        )
        _state.value = _state.value?.copy(sendUsageStats = checked)
        setSendUsageStats(checked)
    }

    data class State(
        val sendUsageStats: Boolean,
        val crashReportingEnabled: Boolean
    )

    sealed class PrivacySettingsEvent : MultiLiveEvent.Event() {
        object ShowAdvertisingOptions : PrivacySettingsEvent()
        object OpenPolicies : PrivacySettingsEvent()
    }
}
