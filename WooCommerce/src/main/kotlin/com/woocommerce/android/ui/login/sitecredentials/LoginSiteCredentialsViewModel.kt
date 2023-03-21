package com.woocommerce.android.ui.login.sitecredentials

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.R
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent.LOGIN_SITE_CREDENTIALS_LOGIN_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.applicationpasswords.ApplicationPasswordGenerationException
import com.woocommerce.android.applicationpasswords.ApplicationPasswordsNotifier
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.WPApiSiteRepository
import com.woocommerce.android.ui.login.WPApiSiteRepository.CookieNonceAuthenticationException
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.module.ApplicationPasswordsClientId
import org.wordpress.android.fluxc.store.SiteStore.SiteError
import org.wordpress.android.login.LoginAnalyticsListener
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

@HiltViewModel
class LoginSiteCredentialsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val wpApiSiteRepository: WPApiSiteRepository,
    private val selectedSite: SelectedSite,
    private val loginAnalyticsListener: LoginAnalyticsListener,
    applicationPasswordsNotifier: ApplicationPasswordsNotifier,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val appPrefs: AppPrefsWrapper,
    @ApplicationPasswordsClientId private val applicationPasswordsClientId: String
) : ScopedViewModel(savedStateHandle) {
    companion object {
        const val SITE_ADDRESS_KEY = "site-address"
        const val USERNAME_KEY = "username"
        const val PASSWORD_KEY = "password"
        const val IS_JETPACK_CONNECTED_KEY = "is-jetpack-connected"
        private const val REDIRECTION_URL = "woocommerce://login"
    }

    private val siteAddress: String = savedStateHandle[SITE_ADDRESS_KEY]!!

    private val state = savedStateHandle.getStateFlow(viewModelScope, State.NativeLogin)
    private val errorDialogMessage = savedStateHandle.getNullableStateFlow(
        scope = viewModelScope,
        initialValue = null,
        clazz = UiString::class.java,
        key = "error-message"
    )
    private val fetchedSiteId = savedStateHandle.getStateFlow(viewModelScope, -1, "site-id")

    private val isLoading = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val viewState = state.flatMapLatest {
        // Reset loading and error state when the state changes
        isLoading.value = false
        errorDialogMessage.value = null

        when (it) {
            State.NativeLogin -> prepareNativeLoginViewState()
            State.WebAuthorization -> prepareWebAuthorizationViewState()
        }
    }.asLiveData()

    init {
        loginAnalyticsListener.trackUsernamePasswordFormViewed()
        applicationPasswordsNotifier.featureUnavailableEvents
            .onEach {
                triggerEvent(
                    ShowApplicationPasswordsUnavailableScreen(
                        siteAddress = siteAddress,
                        isJetpackConnected = savedStateHandle[IS_JETPACK_CONNECTED_KEY]!!
                    )
                )
            }
            .launchIn(this)
    }

    fun onUsernameChanged(username: String) {
        savedState[USERNAME_KEY] = username
        fetchedSiteId.value = -1
    }

    fun onPasswordChanged(password: String) {
        savedState[PASSWORD_KEY] = password
        fetchedSiteId.value = -1
    }

    fun onContinueClick() = launch {
        loginAnalyticsListener.trackSubmitClicked()
        if (fetchedSiteId.value != -1) {
            // The login already succeeded, proceed to fetching user info
            fetchUserInfo()
        } else {
            login()
        }
    }

    fun onErrorDialogDismissed() {
        errorDialogMessage.value = null
    }

    fun onResetPasswordClick() {
        triggerEvent(ShowResetPasswordScreen(siteAddress))
    }

    fun onBackClick() {
        triggerEvent(Exit)
    }

    fun onHelpButtonClick() {
        viewState.value?.let {
            triggerEvent(ShowHelpScreen(siteAddress, (it as? ViewState.NativeLoginViewState)?.username.orEmpty()))
        }
    }

    fun onWooInstallationAttempted() = launch {
        // Retry login to re-fetch the site
        login()
    }

    fun retryApplicationPasswordsCheck() = launch {
        // Retry fetching user info, it will use Application Passwords
        fetchUserInfo()
    }

    private fun prepareNativeLoginViewState(): Flow<ViewState.NativeLoginViewState> = combine(
        flowOf(siteAddress.removeSchemeAndSuffix()),
        savedStateHandle.getStateFlow(USERNAME_KEY, ""),
        savedStateHandle.getStateFlow(PASSWORD_KEY, ""),
        isLoading,
        errorDialogMessage
    ) { siteAddress, username, password, isLoading, errorDialog ->
        ViewState.NativeLoginViewState(
            siteUrl = siteAddress,
            username = username,
            password = password,
            isLoading = isLoading,
            errorDialogMessage = errorDialog
        )
    }

    private fun prepareWebAuthorizationViewState(): Flow<ViewState.WebAuthorizationViewState> {
        if (fetchedSiteId.value == -1) {
            launch { fetchSite() }
        }

        return combine(
            isLoading,
            errorDialogMessage,
            fetchedSiteId.filter { it != -1 }
                .map { wpApiSiteRepository.getSiteByLocalId(it) }
        ) { isLoading, errorDialogMessage, site ->
            val authorizationUrl = site?.applicationPasswordsAuthorizeUrl?.let { url ->
                "$url?app_name=$applicationPasswordsClientId&success_url=$REDIRECTION_URL"
            }
            ViewState.WebAuthorizationViewState(
                authorizationUrl = authorizationUrl,
                isLoading = isLoading,
                errorDialogMessage = errorDialogMessage
            )
        }
    }

    private suspend fun login() {
        val state = requireNotNull(this@LoginSiteCredentialsViewModel.viewState.value as ViewState.NativeLoginViewState)
        isLoading.value = true
        wpApiSiteRepository.loginAndFetchSite(
            url = siteAddress,
            username = state.username,
            password = state.password
        ).fold(
            onSuccess = { site ->
                if (site.hasWooCommerce) {
                    fetchedSiteId.value = site.id
                    fetchUserInfo()
                } else {
                    triggerEvent(ShowNonWooErrorScreen(siteAddress))
                }
            },
            onFailure = { exception ->
                val authenticationError = exception as? CookieNonceAuthenticationException
                val siteError = (exception as? OnChangedException)?.error as? SiteError

                this.errorDialogMessage.value = authenticationError?.errorMessage ?: UiStringRes(R.string.error_generic)

                val error = (exception as? OnChangedException)?.error ?: exception
                trackLoginFailure(
                    step = Step.AUTHENTICATION,
                    errorContext = error.javaClass.simpleName,
                    errorType = authenticationError?.errorType ?: siteError?.type?.name,
                    errorDescription = exception.message,
                    statusCode = authenticationError?.networkStatusCode
                )
            }
        )
        isLoading.value = false
    }

    private suspend fun fetchSite() {
        val viewState = viewState.value
        isLoading.value = true
        wpApiSiteRepository.fetchSite(
            url = siteAddress,
            username = (viewState as? ViewState.NativeLoginViewState)?.username,
            password = (viewState as? ViewState.NativeLoginViewState)?.password
        ).fold(
            onSuccess = { site ->
                if (site.hasWooCommerce) {
                    fetchedSiteId.value = site.id
                    // In case of the native login, then continue with the login flow
                    // Otherwise, the web authorization flow will handle the login
                    if (state.value == State.NativeLogin) {
                        fetchUserInfo()
                    } else if (site.applicationPasswordsAuthorizeUrl == null) {
                        triggerEvent(ShowApplicationPasswordsUnavailableScreen(siteAddress, site.isJetpackConnected))
                    }
                } else {
                    triggerEvent(ShowNonWooErrorScreen(siteAddress))
                }
            },
            onFailure = { exception ->
                val siteError = (exception as? OnChangedException)?.error as? SiteError

                // TODO: Add specific error message
                this.errorDialogMessage.value = UiStringRes(R.string.error_generic)

                val error = (exception as? OnChangedException)?.error ?: exception
                trackLoginFailure(
                    step = Step.AUTHENTICATION,
                    errorContext = error.javaClass.simpleName,
                    errorType = siteError?.type?.name,
                    errorDescription = exception.message
                )
            }
        )
        isLoading.value = false
    }

    private suspend fun fetchUserInfo() {
        isLoading.value = true
        val site = requireNotNull(wpApiSiteRepository.getSiteByLocalId(fetchedSiteId.value)) {
            "Site credentials login: Site not found in DB after login"
        }
        wpApiSiteRepository.checkIfUserIsEligible(site).fold(
            onSuccess = { isEligible ->
                if (isEligible) {
                    // Track success only if the user is eligible, for the other cases, the user eligibility screen will
                    // handle the flow
                    loginAnalyticsListener.trackAnalyticsSignIn(false)
                }
                appPrefs.removeLoginSiteAddress()
                selectedSite.set(site)
                triggerEvent(LoggedIn(selectedSite.getSelectedSiteId()))
            },
            onFailure = { exception ->
                triggerEvent(ShowSnackbar(R.string.error_generic))
                when (exception) {
                    is ApplicationPasswordGenerationException -> {
                        trackLoginFailure(
                            step = Step.APPLICATION_PASSWORD_GENERATION,
                            errorContext = exception.networkError.javaClass.simpleName,
                            errorType = exception.networkError.type.name,
                            errorDescription = exception.message
                        )
                    }

                    else -> {
                        val wooError = (exception as? WooException)?.error
                        trackLoginFailure(
                            step = Step.USER_ROLE,
                            errorContext = (wooError ?: exception).javaClass.simpleName,
                            errorType = wooError?.type?.name,
                            errorDescription = exception.message
                        )
                    }
                }
            }
        )
        isLoading.value = false
    }

    private fun trackLoginFailure(
        step: Step,
        errorContext: String?,
        errorType: String?,
        errorDescription: String?,
        statusCode: Int? = null
    ) {
        loginAnalyticsListener.trackFailure(
            message = errorDescription
        )

        analyticsTracker.track(
            LOGIN_SITE_CREDENTIALS_LOGIN_FAILED,
            mapOf(
                AnalyticsTracker.KEY_STEP to step.name.lowercase(),
                AnalyticsTracker.KEY_NETWORK_STATUS_CODE to statusCode?.toString().orEmpty()
            ),
            errorContext = errorContext,
            errorType = errorType,
            errorDescription = errorDescription,
        )
    }

    private fun String.removeSchemeAndSuffix() = UrlUtils.removeScheme(UrlUtils.removeXmlrpcSuffix(this))

    private enum class State {
        NativeLogin, WebAuthorization
    }

    sealed interface ViewState {
        data class NativeLoginViewState(
            val siteUrl: String,
            val username: String = "",
            val password: String = "",
            val isLoading: Boolean = false,
            val errorDialogMessage: UiString? = null
        ) : ViewState {
            val isValid = username.isNotBlank() && password.isNotBlank()
        }

        data class WebAuthorizationViewState(
            val authorizationUrl: String?,
            val isLoading: Boolean = false,
            val errorDialogMessage: UiString? = null
        ) : ViewState
    }

    @VisibleForTesting
    enum class Step {
        AUTHENTICATION, APPLICATION_PASSWORD_GENERATION, USER_ROLE
    }

    data class LoggedIn(val localSiteId: Int) : MultiLiveEvent.Event()
    data class ShowResetPasswordScreen(val siteAddress: String) : MultiLiveEvent.Event()
    data class ShowNonWooErrorScreen(val siteAddress: String) : MultiLiveEvent.Event()
    data class ShowApplicationPasswordsUnavailableScreen(
        val siteAddress: String,
        val isJetpackConnected: Boolean
    ) : MultiLiveEvent.Event()

    data class ShowHelpScreen(
        val siteAddress: String,
        val username: String?
    ) : MultiLiveEvent.Event()
}
