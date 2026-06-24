package com.woocommerce.android.ui.pushnotifications.introduction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppUrls
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.notifications.push.CheckWooPluginPushNotificationsSupport
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.tools.connectionType
import com.woocommerce.android.ui.jetpack.FetchJetpackStatus
import com.woocommerce.android.ui.jetpack.FetchJetpackStatus.JetpackStatusFetchResponse
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

@HiltViewModel
class WooPushNotificationsIntroductionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val fetchJetpackStatus: FetchJetpackStatus,
    private val checkWCPluginSupport: CheckWooPluginPushNotificationsSupport,
    private val selectedSite: SelectedSite,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val BUTTON_LABEL_CONTINUE = "continue"
        private const val BUTTON_LABEL_UPDATE_PLUGIN = "update_plugin"
        private const val BUTTON_LABEL_NOT_NOW = "not_now"
        private const val BUTTON_LABEL_SUPPORT = "support"
        private const val STATE_NOT_CONNECTED = "not_connected"
        private const val STATE_UPDATE_REQUIRED = "update_required"
        private const val STATE_CONNECTED = "connected"
        private const val ERROR_TYPE_NO_PERMISSION = "no_permission"
        private const val ERROR_TYPE_GENERIC = "generic"
    }

    private val _viewState = MutableStateFlow<ViewState>(ViewState.Loading)
    val viewState = _viewState.asLiveData()

    init {
        fetchStatus()
    }

    private fun fetchStatus() {
        launch {
            val site = selectedSite.get()
            if (site.connectionType == SiteConnectionType.Jetpack) {
                triggerEvent(Exit)
                return@launch
            }

            val viewState = checkIfJetpackIsConnected(site)
                .map { isConnected ->
                    if (!isConnected) {
                        return@map ViewState.NotConnected
                    }

                    when (checkWCPluginSupport(forceRefresh = true)) {
                        CheckWooPluginPushNotificationsSupport.Result.Compatible -> ViewState.Connected
                        is CheckWooPluginPushNotificationsSupport.Result.UpdateRequired -> ViewState.UpdateRequired
                        CheckWooPluginPushNotificationsSupport.Result.Error -> ViewState.GenericError
                    }
                }
                .getOrElse { exception ->
                    if (exception is JetpackForbiddenException) {
                        ViewState.ForbiddenError
                    } else {
                        ViewState.GenericError
                    }
                }

            _viewState.value = viewState

            when (viewState) {
                is ViewState.NotConnected -> trackIntroductionView(STATE_NOT_CONNECTED)
                is ViewState.UpdateRequired -> trackIntroductionView(STATE_UPDATE_REQUIRED)
                is ViewState.Connected -> trackIntroductionView(STATE_CONNECTED)
                is ViewState.ForbiddenError -> trackIntroductionError(ERROR_TYPE_NO_PERMISSION)
                is ViewState.GenericError -> trackIntroductionError(ERROR_TYPE_GENERIC)
                is ViewState.Loading -> {}
            }
        }
    }

    private suspend fun checkIfJetpackIsConnected(site: SiteModel): Result<Boolean> {
        return when (site.connectionType) {
            SiteConnectionType.ApplicationPasswords -> fetchJetpackStatus(
                site = site,
                useApplicationPasswords = true
            ).mapCatching { response ->
                when (response) {
                    is JetpackStatusFetchResponse.Success -> response.status.isSiteConnected
                    is JetpackStatusFetchResponse.ConnectionForbidden -> throw JetpackForbiddenException()
                }
            }

            SiteConnectionType.JetpackConnectionPackage -> {
                // The connection type is enough to determine that the site is connected to Jetpack
                Result.success(true)
            }

            SiteConnectionType.Jetpack -> Result.success(false)
        }
    }

    fun onContinueClick() {
        val currentState = _viewState.value
        val buttonLabel = if (currentState is ViewState.UpdateRequired) {
            BUTTON_LABEL_UPDATE_PLUGIN
        } else {
            BUTTON_LABEL_CONTINUE
        }
        trackIntroductionButtonTap(buttonLabel)
        triggerEvent(
            NavigateToConnectionSteps(
                isSiteConnectedToJetpack = currentState != ViewState.NotConnected,
                shouldAutoOpenUpdatePlugin = currentState is ViewState.UpdateRequired
            )
        )
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
        trackIntroductionButtonTap(BUTTON_LABEL_SUPPORT)
        triggerEvent(Event.NavigateToHelpScreen(HelpOrigin.WOO_PUSH_NOTIFICATIONS_SETUP))
    }

    private fun trackIntroductionView(state: String) {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_INTRODUCTION_VIEW,
            mapOf(AnalyticsTracker.KEY_STATE to state)
        )
    }

    private fun trackIntroductionError(errorType: String) {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_INTRODUCTION_ERROR,
            mapOf(AnalyticsTracker.KEY_ERROR_TYPE to errorType)
        )
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
        data object Connected : ViewState
        data object UpdateRequired : ViewState
        data object ForbiddenError : ViewState
        data object GenericError : ViewState
    }

    data class NavigateToConnectionSteps(
        val isSiteConnectedToJetpack: Boolean,
        val shouldAutoOpenUpdatePlugin: Boolean
    ) : Event()

    data class OpenUrlEvent(val url: String) : Event()
    private class JetpackForbiddenException : Exception()
}
