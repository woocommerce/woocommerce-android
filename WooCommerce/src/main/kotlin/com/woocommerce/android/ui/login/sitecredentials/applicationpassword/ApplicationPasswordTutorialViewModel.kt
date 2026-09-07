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
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
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
            return
        }

        recoverAuthorizationIfNeeded(url)
    }

    fun onWebNavigationRequested(url: String): Boolean {
        return recoverAuthorizationIfNeeded(url)
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
        verifiedLoginUrl: String?,
        applicationPasswordAuthorizationUrl: String,
        errorMessage: String?
    ) {
        _viewState.update { state ->
            if (state.webViewUrl != null) {
                state
            } else {
                state.copy(
                    webViewUrl = buildWebViewUrl(
                        verifiedLoginUrl = verifiedLoginUrl,
                        applicationPasswordAuthorizationUrl = applicationPasswordAuthorizationUrl
                    ),
                    applicationPasswordAuthorizationUrl = applicationPasswordAuthorizationUrl,
                    errorMessage = errorMessage
                )
            }
        }
    }

    private fun buildWebViewUrl(
        verifiedLoginUrl: String?,
        applicationPasswordAuthorizationUrl: String
    ): String {
        val loginUrl = verifiedLoginUrl?.toHttpUrlOrNull() ?: return applicationPasswordAuthorizationUrl
        return loginUrl.newBuilder()
            .setQueryParameter("redirect_to", applicationPasswordAuthorizationUrl)
            .build()
            .toString()
    }

    private fun ViewState.shouldRecoverAuthorization(loadedUrl: String): Boolean {
        val authorizationPageUrl = applicationPasswordAuthorizationUrl?.toHttpUrlOrNull()
        val startUrl = webViewUrl?.toHttpUrlOrNull()
        val currentUrl = loadedUrl.toHttpUrlOrNull()
        return !authorizationRecoveryAttempted &&
            webViewUrl != applicationPasswordAuthorizationUrl &&
            authorizationPageUrl != null &&
            startUrl != null &&
            currentUrl != null &&
            currentUrl.isAuthorizationRecoveryLanding(authorizationPageUrl, startUrl)
    }

    private fun HttpUrl.isAuthorizationRecoveryLanding(
        authorizationPageUrl: HttpUrl,
        startUrl: HttpUrl
    ) = hasSameOrigin(authorizationPageUrl) &&
        isWithinAdminDirectory(authorizationPageUrl) &&
        encodedPath != authorizationPageUrl.encodedPath &&
        encodedPath != startUrl.encodedPath

    private fun HttpUrl.hasSameOrigin(other: HttpUrl) =
        scheme == other.scheme && host == other.host && port == other.port

    private fun HttpUrl.isWithinAdminDirectory(authorizationPageUrl: HttpUrl): Boolean {
        val adminDirectorySegments = authorizationPageUrl.encodedPathSegments.dropLast(1)
        return adminDirectorySegments.isNotEmpty() &&
            encodedPathSegments.size >= adminDirectorySegments.size &&
            encodedPathSegments.take(adminDirectorySegments.size) == adminDirectorySegments
    }

    private fun recoverAuthorizationIfNeeded(url: String): Boolean {
        if (!_viewState.value.shouldRecoverAuthorization(url)) return false

        _viewState.update {
            it.copy(
                webViewUrl = it.applicationPasswordAuthorizationUrl,
                authorizationRecoveryAttempted = true
            )
        }
        return true
    }

    object OnContactSupport : Event()
    object ShowExitConfirmationDialog : Event()

    @Parcelize
    data class ViewState(
        val authorizationStarted: Boolean = false,
        val webViewUrl: String? = null,
        val applicationPasswordAuthorizationUrl: String? = null,
        val authorizationRecoveryAttempted: Boolean = false,
        val errorMessage: String? = null,
    ) : Parcelable

    companion object {
        private const val REDIRECTION_URL = "woocommerce://login"
    }
}
