package com.woocommerce.android.ui.pushnotifications.introduction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppUrls
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.isVersionAtLeast
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.jetpack.FetchJetpackStatus
import com.woocommerce.android.ui.jetpack.FetchJetpackStatus.JetpackStatusFetchResponse
import com.woocommerce.android.util.FetchActiveWCPluginVersion
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPushNotificationsIntroductionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val fetchJetpackStatus: FetchJetpackStatus,
    private val fetchActiveWCPluginVersion: FetchActiveWCPluginVersion,
    private val selectedSite: SelectedSite,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {
    companion object {
        const val PUSH_NOTIFICATIONS_MIN_WC_VERSION = "10.6.0" // TODO CHECK CORRECT VERSION LATER
        const val BUTTON_LABEL_CONTINUE = "continue"
        const val BUTTON_LABEL_UPDATE_PLUGIN = "update_plugin"
        const val BUTTON_LABEL_NOT_NOW = "not_now"
    }

    private val _viewState = MutableStateFlow<ViewState>(ViewState.Loading)
    val viewState = _viewState.asLiveData()

    init {
        fetchStatus()
    }

    private fun fetchStatus() {
        launch {
            val site = selectedSite.get()

            val result = fetchJetpackStatus(
                site = site,
                useApplicationPasswords = true
            )

            result.fold(
                onSuccess = { response ->
                    when (response) {
                        is JetpackStatusFetchResponse.ConnectionForbidden -> {
                            _viewState.value = ViewState.ForbiddenError
                        }

                        is JetpackStatusFetchResponse.Success -> {
                            if (response.status.isSiteConnected) {
                                checkWCVersion()
                            } else {
                                _viewState.value = ViewState.NotConnected
                            }
                        }
                    }
                },
                onFailure = { _viewState.value = ViewState.GenericError }
            )

            if (_viewState.value is ViewState.NotConnected || _viewState.value is ViewState.UpdateRequired) {
                trackIntroductionView()
            }
        }
    }

    private suspend fun checkWCVersion() {
        val wcVersion = fetchActiveWCPluginVersion() ?: run {
            _viewState.value = ViewState.GenericError
            return
        }

        if (wcVersion.isVersionAtLeast(PUSH_NOTIFICATIONS_MIN_WC_VERSION)) {
            _viewState.value = ViewState.GenericError
        } else {
            _viewState.value = ViewState.UpdateRequired
        }
    }

    fun onContinueClick() {
        val buttonLabel = if (_viewState.value is ViewState.UpdateRequired) {
            BUTTON_LABEL_UPDATE_PLUGIN
        } else {
            BUTTON_LABEL_CONTINUE
        }
        trackIntroductionButtonTap(buttonLabel)
        triggerEvent(NavigateToConnectionSteps(isSiteConnectedToJetpack = _viewState.value !is ViewState.NotConnected))
    }

    fun onNotNowClick() {
        trackIntroductionButtonTap(BUTTON_LABEL_NOT_NOW)
        triggerEvent(Exit)
    }

    fun onCloseClick() {
        analyticsTrackerWrapper.track(AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_INTRODUCTION_CLOSE)
        triggerEvent(Exit)
    }

    fun onWhatIsWPComClick() {
        analyticsTrackerWrapper.track(AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_INTRODUCTION_LINK_TAP)
        triggerEvent(OpenUrlEvent(AppUrls.LOGIN_WITH_EMAIL_WHAT_IS_WORDPRESS_COM_ACCOUNT))
    }

    fun onContactSupportClick() {
        triggerEvent(Event.NavigateToHelpScreen(HelpOrigin.WOO_PUSH_NOTIFICATIONS_SETUP))
    }

    private fun trackIntroductionView() {
        analyticsTrackerWrapper.track(AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_INTRODUCTION_VIEW)
    }

    private fun trackIntroductionButtonTap(buttonLabel: String) {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_INTRODUCTION_BUTTON_TAP,
            mapOf(AnalyticsTracker.KEY_BUTTON_LABEL to buttonLabel)
        )
    }

    sealed interface ViewState {
        data object Loading : ViewState
        data object NotConnected : ViewState
        data object UpdateRequired : ViewState
        data object ForbiddenError : ViewState
        data object GenericError : ViewState
    }

    data class NavigateToConnectionSteps(
        val isSiteConnectedToJetpack: Boolean
    ) : Event()

    data class OpenUrlEvent(val url: String) : Event()
}
