package com.woocommerce.android.ui.prefs.privacy.banner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.prefs.PrivacySettingsRepository
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

    private val _state: MutableLiveData<State> = MutableLiveData(
        State(
            analyticsSwitchEnabled = analyticsTrackerWrapper.sendUsageStats,
            loading = false
        )
    )

    val analyticsState: LiveData<State> = _state

    fun onSwitchChanged(checked: Boolean) {
        _state.value = _state.value?.copy(analyticsSwitchEnabled = checked)
    }

    fun onSavePressed() {
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
                        triggerEvent(ShowError(requestedChange = analyticsPreference))
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
    data class ShowError(val requestedChange: Boolean) : MultiLiveEvent.Event()
}
