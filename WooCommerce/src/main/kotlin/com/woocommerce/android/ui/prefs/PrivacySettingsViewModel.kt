package com.woocommerce.android.ui.prefs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    savedState: SavedStateHandle,
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
        if (repository.isUserWPCOM()) {
            launch {
                _state.value = _state.value?.copy(progressBarVisible = true)
                val event = repository.fetchAccountSettings()
                _state.value = _state.value?.copy(progressBarVisible = false)

                event.fold(
                    onSuccess = {
                        appPrefs.setSendUsageStats(!repository.userOptOutFromTracks())
                        _state.value =
                            _state.value?.copy(sendUsageStats = !repository.userOptOutFromTracks())
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
    }

    private fun getSendUsageStats() =
        if (repository.isUserWPCOM()) !repository.userOptOutFromTracks() else appPrefs.getSendUsageStats()

    private fun getCrashReportingEnabled() = appPrefs.isCrashReportingEnabled()

    fun onPoliciesClicked() {
        triggerEvent(PrivacySettingsEvent.OpenPolicies)
    }

    private fun setCrashReportingEnabled(enabled: Boolean) {
        appPrefs.setCrashReportingEnabled(enabled)
    }

    fun onAdvertisingOptionsClicked() {
        triggerEvent(PrivacySettingsEvent.ShowAdvertisingOptions)
    }

    fun onUsageTrackerClicked() {
        triggerEvent(PrivacySettingsEvent.ShowUsageTracker)
    }

    fun onCrashReportingSettingChanged(checked: Boolean) {
        _state.value = _state.value?.copy(crashReportingEnabled = checked)
        setCrashReportingEnabled(checked)
    }

    fun onSendStatsSettingChanged(checked: Boolean) {
        launch {
            if (repository.isUserWPCOM()) {

                _state.value = _state.value?.copy(
                    sendUsageStats = checked, progressBarVisible = true
                )

                val event = repository.updateTracksSetting(checked)

                _state.value = _state.value?.copy(progressBarVisible = false)

                event.fold(
                    onSuccess = {
                        appPrefs.setSendUsageStats(checked)
                    },
                    onFailure = {
                        _state.value = _state.value?.copy(sendUsageStats = !checked)
                        triggerEvent(
                            MultiLiveEvent.Event.ShowActionSnackbar(
                                resourceProvider.getString(R.string.settings_tracking_analytics_error_update)
                            ) { onSendStatsSettingChanged(!checked) }
                        )
                    }
                )
            } else {
                _state.value = _state.value?.copy(sendUsageStats = checked)
                appPrefs.setSendUsageStats(checked)
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
        object ShowAdvertisingOptions : PrivacySettingsEvent()
        object ShowUsageTracker : PrivacySettingsEvent()
    }
}
