package com.woocommerce.android.ui.prefs.privacy.banner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.prefs.PrivacySettingsRepository
import com.woocommerce.android.ui.prefs.RequestedAnalyticsValue
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrivacyBannerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val repository: PrivacySettingsRepository,
) : ScopedViewModel(savedStateHandle) {

    private val initialUserPreference: Boolean = analyticsTrackerWrapper.sendUsageStats

    private val _state: MutableLiveData<State> = MutableLiveData(
        State(
            analyticsSwitchEnabled = initialUserPreference,
            loading = false
        )
    )

    val analyticsState: LiveData<State> = _state

    init {
        analyticsTrackerWrapper.track(AnalyticsEvent.PRIVACY_CHOICES_BANNER_PRESENTED)
    }

    fun onSwitchChanged(checked: Boolean) {
        _state.value = _state.value?.copy(analyticsSwitchEnabled = checked)
    }

    fun onSettingsPressed() {
        analyticsTrackerWrapper.track(AnalyticsEvent.PRIVACY_CHOICES_BANNER_SETTINGS_BUTTON_TAPPED)

        val analyticsPreference = _state.value?.analyticsSwitchEnabled ?: false

        if (analyticsPreference == initialUserPreference) {
            appPrefsWrapper.savedPrivacyBannerSettings = true
            triggerEvent(ShowSettings)
            return
        }

        launch {
            if (repository.isUserWPCOM()) {
                _state.value = _state.value?.copy(loading = true)
                val event =
                    repository.updateTracksSetting(_state.value?.analyticsSwitchEnabled ?: false)
                _state.value = _state.value?.copy(loading = false)

                event.fold(
                    onSuccess = {
                        appPrefsWrapper.savedPrivacyBannerSettings = true
                        analyticsTrackerWrapper.sendUsageStats = analyticsPreference
                        triggerEvent(ShowSettings)
                    },
                    onFailure = {
                        triggerEvent(
                            ShowErrorOnSettings(
                                requestedAnalyticsValue = if (analyticsPreference) {
                                    RequestedAnalyticsValue.ENABLED
                                } else {
                                    RequestedAnalyticsValue.DISABLE
                                }
                            )
                        )
                    }
                )
            } else {
                appPrefsWrapper.savedPrivacyBannerSettings = true
                analyticsTrackerWrapper.sendUsageStats = analyticsPreference
                triggerEvent(ShowSettings)
            }
        }
    }

    fun onSavePressed() {
        analyticsTrackerWrapper.track(AnalyticsEvent.PRIVACY_CHOICES_BANNER_SAVE_BUTTON_TAPPED)
        val analyticsPreference = _state.value?.analyticsSwitchEnabled ?: false

        launch {
            if (repository.isUserWPCOM()) {
                _state.value = _state.value?.copy(loading = true)
                val event =
                    repository.updateTracksSetting(_state.value?.analyticsSwitchEnabled ?: false)
                _state.value = _state.value?.copy(loading = false)

                event.fold(
                    onSuccess = {
                        appPrefsWrapper.savedPrivacyBannerSettings = true
                        analyticsTrackerWrapper.sendUsageStats = analyticsPreference
                        triggerEvent(Dismiss)
                    },
                    onFailure = {
                        triggerEvent(ShowError(requestedAnalyticsValue = analyticsPreference))
                    }
                )
            } else {
                appPrefsWrapper.savedPrivacyBannerSettings = true
                analyticsTrackerWrapper.sendUsageStats = analyticsPreference
                triggerEvent(Dismiss)
            }
        }
    }

    data class State(
        val analyticsSwitchEnabled: Boolean,
        val loading: Boolean,
    )

    object Dismiss : MultiLiveEvent.Event()
    data class ShowError(val requestedAnalyticsValue: Boolean) : MultiLiveEvent.Event()
    object ShowSettings : MultiLiveEvent.Event()
    data class ShowErrorOnSettings(
        val requestedAnalyticsValue: RequestedAnalyticsValue
    ) : MultiLiveEvent.Event()
}
