package com.woocommerce.android.ui.login.jetpack.start

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.support.help.HelpOrigin.JETPACK_INSTALLATION
import com.woocommerce.android.ui.login.jetpack.main.JetpackActivationMainViewModel
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.NavigateToHelpScreen
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

@HiltViewModel
class JetpackActivationStartViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val CONNECTION_DISMISSED_KEY = "connection-dismissed"
    }

    private val navArgs: JetpackActivationStartFragmentArgs by savedStateHandle.navArgs()

    private val isConnectionDismissed = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = false,
        key = CONNECTION_DISMISSED_KEY
    )

    val viewState: LiveData<JetpackActivationState> = isConnectionDismissed.map { isConnectionDismissed ->
        JetpackActivationState(
            url = UrlUtils.removeScheme(navArgs.siteUrl),
            faviconUrl = "${navArgs.siteUrl.trimEnd('/')}/favicon.ico",
            isJetpackInstalled = navArgs.isJetpackInstalled,
            isConnectionDismissed = isConnectionDismissed
        )
    }.asLiveData()

    init {
        analyticsTrackerWrapper.track(
            stat = if (navArgs.isJetpackInstalled) {
                AnalyticsEvent.LOGIN_JETPACK_CONNECTION_ERROR_SHOWN
            } else {
                AnalyticsEvent.LOGIN_JETPACK_REQUIRED_SCREEN_VIEWED
            }
        )
    }

    fun onHelpButtonClick() {
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.LOGIN_JETPACK_SETUP_GET_SUPPORT_BUTTON_TAPPED,
            properties = mapOf(
                AnalyticsTracker.KEY_JETPACK_INSTALLATION_STEP to
                    JetpackActivationMainViewModel.StepType.Connection.analyticsName
            ),
        )
        triggerEvent(NavigateToHelpScreen(JETPACK_INSTALLATION))
    }

    fun onBackButtonClick() {
        triggerEvent(Exit)
    }

    fun onConnectionDismissed() {
        analyticsTrackerWrapper.track(AnalyticsEvent.LOGIN_JETPACK_CONNECT_DISMISSED)
        isConnectionDismissed.value = true
    }

    fun onContinueButtonClick() {
        if (isConnectionDismissed.value) {
            analyticsTrackerWrapper.track(
                stat = AnalyticsEvent.LOGIN_JETPACK_SETUP_TRY_AGAIN_BUTTON_TAPPED,
                properties = mapOf(
                    AnalyticsTracker.KEY_JETPACK_INSTALLATION_STEP to
                        JetpackActivationMainViewModel.StepType.Connection.analyticsName
                )
            )
            isConnectionDismissed.value = false
            triggerEvent(
                ContinueJetpackConnection(
                    siteUrl = navArgs.siteUrl
                )
            )
        } else {
            analyticsTrackerWrapper.track(
                stat = if (navArgs.isJetpackInstalled) {
                    AnalyticsEvent.LOGIN_JETPACK_CONNECT_BUTTON_TAPPED
                } else {
                    AnalyticsEvent.LOGIN_JETPACK_SETUP_BUTTON_TAPPED
                }
            )
            triggerEvent(
                NavigateToSiteCredentialsScreen(
                    siteUrl = navArgs.siteUrl,
                    isJetpackInstalled = navArgs.isJetpackInstalled
                )
            )
        }
    }

    data class JetpackActivationState(
        val url: String,
        val faviconUrl: String,
        val isJetpackInstalled: Boolean,
        val isConnectionDismissed: Boolean
    )

    data class NavigateToSiteCredentialsScreen(
        val siteUrl: String,
        val isJetpackInstalled: Boolean
    ) : MultiLiveEvent.Event()

    data class ContinueJetpackConnection(val siteUrl: String) : MultiLiveEvent.Event()
}
