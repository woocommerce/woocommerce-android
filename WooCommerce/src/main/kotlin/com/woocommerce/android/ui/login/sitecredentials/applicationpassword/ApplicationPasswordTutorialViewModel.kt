package com.woocommerce.android.ui.login.sitecredentials.applicationpassword

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.network.UserAgent
import javax.inject.Inject

@HiltViewModel
class ApplicationPasswordTutorialViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    val userAgent: UserAgent,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val _viewState = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = ViewState()
    )
    val viewState = _viewState.asLiveData()

    fun onContinueClicked() {
        analyticsTracker.track(AnalyticsEvent.LOGIN_SITE_CREDENTIALS_APP_PASSWORD_EXPLANATION_CONTINUE_BUTTON_TAPPED)
        _viewState.update { it.copy(authorizationStarted = true) }
    }

    fun onContactSupportClicked() {
        analyticsTracker.track(AnalyticsEvent.LOGIN_SITE_CREDENTIALS_APP_PASSWORD_EXPLANATION_CONTACT_SUPPORT_TAPPED)
        triggerEvent(OnContactSupport)
    }

    fun onWebPageLoaded(url: String) {
        analyticsTracker.track(AnalyticsEvent.APPLICATION_PASSWORDS_AUTHORIZATION_WEB_VIEW_SHOWN)
        if (url.startsWith(REDIRECTION_URL)) {
            triggerEvent(ExitWithResult(url))
        }
    }

    fun onNavigationButtonClicked() {
        if (_viewState.value.authorizationStarted) {
            analyticsTracker.track(AnalyticsEvent.LOGIN_SITE_CREDENTIALS_APP_PASSWORD_LOGIN_EXIT_CONFIRMATION)
            triggerEvent(ShowExitConfirmationDialog)
        } else {
            analyticsTracker.track(AnalyticsEvent.LOGIN_SITE_CREDENTIALS_APP_PASSWORD_LOGIN_DISMISSED)
            triggerEvent(ExitWithResult(""))
        }
    }

    fun onExitConfirmed() {
        analyticsTracker.track(AnalyticsEvent.LOGIN_SITE_CREDENTIALS_APP_PASSWORD_LOGIN_DISMISSED)
        triggerEvent(ExitWithResult(""))
    }

    fun onWebViewDataAvailable(
        authorizationUrl: String?,
        errorMessage: String?
    ) {
        _viewState.update {
            it.copy(
                authorizationUrl = authorizationUrl,
                errorMessage = errorMessage
            )
        }
    }

    object OnContactSupport : Event()
    object ShowExitConfirmationDialog : Event()

    @Parcelize
    data class ViewState(
        val authorizationStarted: Boolean = false,
        val authorizationUrl: String? = null,
        val errorMessage: String? = null,
    ) : Parcelable

    companion object {
        private const val REDIRECTION_URL = "woocommerce://login"
    }
}
