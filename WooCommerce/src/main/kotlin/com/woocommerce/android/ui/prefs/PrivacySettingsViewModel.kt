package com.woocommerce.android.ui.prefs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.combineWith
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val appPrefs: AppPrefsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val resourceProvider: ResourceProvider,
    private val repository: PrivacySettingsRepository,
) : ScopedViewModel(savedState) {

    private val args: PrivacySettingsFragmentArgs by savedState.navArgs()

    private val analyticsEnabled: LiveData<Boolean> = analyticsTrackerWrapper
        .observeSendUsageStats()
        .asLiveData()

    private val _state = MutableLiveData(defaultState())

    val state: LiveData<State> = _state.combineWith(analyticsEnabled) { state, analyticsEnabled ->
        state?.copy(sendUsageStats = analyticsEnabled == true) ?: defaultState()
    }

    private fun defaultState() = State(
        sendUsageStats = analyticsTrackerWrapper.sendUsageStats,
        crashReportingEnabled = getCrashReportingEnabled(),
        progressBarVisible = false,
    )

    init {
        initialize()
    }

    fun initialize() {
        if (repository.isUserWPCOM()) {
            launch {
                _state.value = _state.value?.copy(progressBarVisible = true)
                val event = repository.updateAccountSettings()
                _state.value = _state.value?.copy(progressBarVisible = false)

                event.onFailure {
                    triggerEvent(
                        MultiLiveEvent.Event.ShowActionSnackbar(
                            message = resourceProvider.getString(R.string.settings_tracking_analytics_error_fetch),
                            actionText = resourceProvider.getString(R.string.retry),
                        ) {
                            initialize()
                        }
                    )
                }

                if (args.requestedAnalyticsValue != RequestedAnalyticsValue.NONE) {
                    val checked =
                        args.requestedAnalyticsValue == RequestedAnalyticsValue.ENABLED

                    triggerEvent(
                        MultiLiveEvent.Event.ShowActionSnackbar(
                            message = resourceProvider.getString(R.string.settings_tracking_analytics_error_update),
                            actionText = resourceProvider.getString(R.string.retry),
                        ) { onSendStatsSettingChanged(checked) }
                    )
                }
            }
        }
    }

    private fun getCrashReportingEnabled() = appPrefs.isCrashReportingEnabled()

    fun onPoliciesClicked() {
        triggerEvent(PrivacySettingsEvent.OpenPolicies)
    }

    private fun setCrashReportingEnabled(enabled: Boolean) {
        appPrefs.setCrashReportingEnabled(enabled)
    }

    fun onWebOptionsClicked() {
        triggerEvent(PrivacySettingsEvent.ShowWebOptions)
    }

    fun onUsageTrackerClicked() {
        triggerEvent(PrivacySettingsEvent.ShowUsageTracker)
    }

    fun onCrashReportingSettingChanged(checked: Boolean) {
        _state.value = _state.value?.copy(crashReportingEnabled = checked)
        setCrashReportingEnabled(checked)
    }

    fun onSendStatsSettingChanged(checked: Boolean) {
        analyticsTrackerWrapper.sendUsageStats = checked
        launch {
            if (repository.isUserWPCOM()) {
                _state.value = _state.value?.copy(progressBarVisible = true)

                val event = repository.updateTracksSetting(checked)

                _state.value = _state.value?.copy(progressBarVisible = false)

                event.fold(
                    onSuccess = {
                        appPrefs.savedPrivacyBannerSettings = true
                        analyticsTrackerWrapper.sendUsageStats = checked
                    },
                    onFailure = {
                        analyticsTrackerWrapper.sendUsageStats = !checked
                        triggerEvent(
                            MultiLiveEvent.Event.ShowActionSnackbar(
                                message = resourceProvider.getString(R.string.settings_tracking_analytics_error_update),
                                actionText = resourceProvider.getString(R.string.retry),
                            ) { onSendStatsSettingChanged(checked) }
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
        object OpenPolicies : PrivacySettingsEvent()
        object ShowWebOptions : PrivacySettingsEvent()
        object ShowUsageTracker : PrivacySettingsEvent()
    }
}
