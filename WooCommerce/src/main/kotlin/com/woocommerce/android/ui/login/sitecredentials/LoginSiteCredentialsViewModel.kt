package com.woocommerce.android.ui.login.sitecredentials

import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.R
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.LOGIN_SITE_CREDENTIALS_LOGIN_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.applicationpasswords.ApplicationPasswordGenerationException
import com.woocommerce.android.applicationpasswords.ApplicationPasswordsNotifier
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.WPApiSiteRepository
import com.woocommerce.android.ui.login.WPApiSiteRepository.CookieNonceAuthenticationException
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.module.ApplicationPasswordsClientId
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.CookieNonceErrorType.INVALID_CREDENTIALS
import org.wordpress.android.fluxc.store.SiteStore.SiteError
import org.wordpress.android.login.LoginAnalyticsListener
import org.wordpress.android.util.UrlUtils
import java.net.URI
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
    private val userAgent: UserAgent,
    private val resourceProvider: ResourceProvider,
    @ApplicationPasswordsClientId private val applicationPasswordsClientId: String
) : ScopedViewModel(savedStateHandle) {
    companion object {
        const val SITE_ADDRESS_KEY = "site-address"
        const val USERNAME_KEY = "username"
        const val PASSWORD_KEY = "password"
        const val IS_JETPACK_CONNECTED_KEY = "is-jetpack-connected"
        private const val REDIRECTION_URL = "woocommerce://login"
        private const val SUCCESS_PARAMETER = "success"
        private const val USERNAME_PARAMETER = "user_login"
        private const val PASSWORD_PARAMETER = "password"
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

    private val loadingMessage = savedStateHandle.getStateFlow(viewModelScope, 0, "loading-message")

    @OptIn(ExperimentalCoroutinesApi::class)
    val viewState = state.flatMapLatest {
        // Reset loading and error state when the state changes
        loadingMessage.value = 0
        errorDialogMessage.value = null

        when (it) {
            State.NativeLogin -> prepareNativeLoginViewState()
            State.WebAuthorization -> prepareWebAuthorizationViewState()
            State.RetryWebAuthorization -> prepareWebAuthorizationViewState()
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

    fun onPasswordTutorialAborted() {
        fetchedSiteId.value = -1
    }

    fun onBackClick() {
        if (state.value == State.WebAuthorization) {
            fetchedSiteId.value = -1
            state.value = State.NativeLogin
        } else {
            triggerEvent(Exit)
        }
    }

    fun onHelpButtonClick() {
        viewState.value?.let {
            triggerEvent(ShowHelpScreen(siteAddress, (it as? ViewState.NativeLoginViewState)?.username.orEmpty()))
        }
    }

    /**
     * This is currently a unreachable event due to the current usage of the application passwords feature
     * available in the [ShowApplicationPasswordTutorialScreen] event, but it's kept here for future reference
     * in case we need to start the Authorization from here back again.
     */
    fun onStartWebAuthorizationClick() {
        state.value = State.WebAuthorization
        analyticsTracker.track(AnalyticsEvent.APPLICATION_PASSWORDS_AUTHORIZATION_WEB_VIEW_SHOWN)
    }

    fun onWebAuthorizationUrlLoaded(url: String) {
        if (url.startsWith(REDIRECTION_URL)) {
            launch {
                val uri = URI.create(url)
                val params = uri.query!!.split("&")
                    .map { it.split("=") }
                    .associate { it[0] to it[1] }

                val isSuccess = params[SUCCESS_PARAMETER]?.toBoolean() ?: true
                if (!isSuccess) {
                    fetchedSiteId.value = -1
                    state.value = State.NativeLogin

                    analyticsTracker.track(AnalyticsEvent.APPLICATION_PASSWORDS_AUTHORIZATION_REJECTED)
                    triggerEvent(ShowSnackbar(R.string.login_site_credentials_web_authorization_connection_rejected))
                    return@launch
                }

                analyticsTracker.track(AnalyticsEvent.APPLICATION_PASSWORDS_AUTHORIZATION_APPROVED)
                val username = requireNotNull(params[USERNAME_PARAMETER])
                val password = requireNotNull(params[PASSWORD_PARAMETER])

                wpApiSiteRepository.saveApplicationPassword(fetchedSiteId.value, username, password)
                fetchUserInfo()
            }
        }
    }

    fun onWooInstallationAttempted() = launch {
        // Retry fetching the site to check if WooCommerce was installed
        fetchSite()
    }

    fun retryApplicationPasswordsCheck() = launch {
        if (state.value == State.NativeLogin) {
            // When using native login, retry fetching user info
            fetchUserInfo()
        } else {
            // When using web authorization, retry fetching the site
            fetchSite()
            state.value = State.RetryWebAuthorization
        }
    }

    private fun prepareNativeLoginViewState(): Flow<ViewState.NativeLoginViewState> = combine(
        flowOf(siteAddress.removeSchemeAndSuffix()),
        savedStateHandle.getStateFlow(USERNAME_KEY, ""),
        savedStateHandle.getStateFlow(PASSWORD_KEY, ""),
        loadingMessage.map { message -> message.takeIf { it != 0 } },
        errorDialogMessage
    ) { siteAddress, username, password, loadingMessage, errorDialog ->
        ViewState.NativeLoginViewState(
            siteUrl = siteAddress,
            username = username,
            password = password,
            loadingMessage = loadingMessage,
            errorDialogMessage = errorDialog
        )
    }

    private fun prepareWebAuthorizationViewState(): Flow<ViewState.WebAuthorizationViewState> {
        if (fetchedSiteId.value == -1) {
            launch { fetchSite() }
        }

        return combine(
            loadingMessage.map { message -> message.takeIf { it != 0 } },
            errorDialogMessage,
            fetchedSiteId.map { if (it == -1) null else wpApiSiteRepository.getSiteByLocalId(it) }
        ) { loadingMessage, errorDialogMessage, site ->
            ViewState.WebAuthorizationViewState(
                authorizationUrl = generateAuthorizationUrl(site),
                userAgent = userAgent,
                loadingMessage = loadingMessage,
                errorDialogMessage = errorDialogMessage
            )
        }
    }

    private fun generateAuthorizationUrl(site: SiteModel?) =
        site?.applicationPasswordsAuthorizeUrl
            ?.let { url -> "$url?app_name=$applicationPasswordsClientId&success_url=$REDIRECTION_URL" }

    private suspend fun login() {
        val state = requireNotNull(this@LoginSiteCredentialsViewModel.viewState.value as ViewState.NativeLoginViewState)
        loadingMessage.value = R.string.logging_in
        wpApiSiteRepository.login(
            url = siteAddress,
            username = state.username,
            password = state.password
        ).fold(
            onSuccess = {
                fetchSite()
            },
            onFailure = { exception ->
                val authenticationError = exception as? CookieNonceAuthenticationException

                when (authenticationError?.errorType) {
                    INVALID_CREDENTIALS -> errorDialogMessage.value = authenticationError.errorMessage
                    else -> {
                        fetchSiteForTutorial(
                            username = state.username,
                            password = state.password,
                            detectedErrorMessage = authenticationError?.errorMessage
                        )
                        analyticsTracker.track(AnalyticsEvent.LOGIN_SITE_CREDENTIALS_INVALID_LOGIN_PAGE_DETECTED)
                    }
                }

                trackLoginFailure(
                    step = Step.AUTHENTICATION,
                    errorContext = exception.javaClass.simpleName,
                    errorType = authenticationError?.errorType?.name,
                    errorDescription = exception.message,
                    statusCode = authenticationError?.networkStatusCode
                )
            }
        )
        loadingMessage.value = 0
    }

    private suspend fun fetchSiteForTutorial(
        username: String,
        password: String,
        detectedErrorMessage: UiString? = null
    ) {
        loadingMessage.value = R.string.login_site_credentials_fetching_site
        wpApiSiteRepository.fetchSite(
            url = siteAddress,
            username = username,
            password = password
        ).fold(
            onSuccess = { site ->
                if (site.hasWooCommerce) {
                    fetchedSiteId.value = site.id
                    loadingMessage.value = 0
                    val errorMessage = detectedErrorMessage
                        ?.toPresentableString()
                        ?: resourceProvider.getString(R.string.error_generic)
                    ShowApplicationPasswordTutorialScreen(
                        url = generateAuthorizationUrl(site).orEmpty(),
                        errorMessage = errorMessage
                    ).let { triggerEvent(it) }
                } else {
                    triggerEvent(ShowNonWooErrorScreen(siteAddress))
                }
            },
            onFailure = {
                loadingMessage.value = 0
                handleSiteFetchingError(it)
            }
        )
    }

    private suspend fun fetchSite() {
        val viewState = viewState.value
        loadingMessage.value = if (state.value == State.WebAuthorization) {
            R.string.login_site_credentials_fetching_site
        } else {
            R.string.logging_in
        }
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
                        analyticsTracker.track(AnalyticsEvent.APPLICATION_PASSWORDS_AUTHORIZATION_URL_NOT_AVAILABLE)
                        triggerEvent(ShowApplicationPasswordsUnavailableScreen(siteAddress, site.isJetpackConnected))
                    }
                } else {
                    triggerEvent(ShowNonWooErrorScreen(siteAddress))
                }
            },
            onFailure = { handleSiteFetchingError(it) }
        )
        loadingMessage.value = 0
    }

    private fun handleSiteFetchingError(exception: Throwable) {
        val siteError = (exception as? OnChangedException)?.error as? SiteError

        this.errorDialogMessage.value = UiStringRes(R.string.login_site_credentials_fetching_site_failed)

        val error = (exception as? OnChangedException)?.error ?: exception
        trackLoginFailure(
            step = Step.AUTHENTICATION,
            errorContext = error.javaClass.simpleName,
            errorType = siteError?.type?.name,
            errorDescription = exception.message
        )
    }

    private suspend fun fetchUserInfo() {
        loadingMessage.value = R.string.logging_in
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
        loadingMessage.value = 0
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

    private fun UiString.toPresentableString() = when (this) {
        is UiStringRes -> resourceProvider.getString(stringRes)
        is UiStringText -> text
    }

    private enum class State {
        NativeLogin, WebAuthorization, RetryWebAuthorization
    }

    sealed interface ViewState {
        data class NativeLoginViewState(
            val siteUrl: String,
            val username: String = "",
            val password: String = "",
            @StringRes val loadingMessage: Int? = null,
            val errorDialogMessage: UiString? = null
        ) : ViewState {
            val isValid = username.isNotBlank() && password.isNotBlank()
        }

        data class WebAuthorizationViewState(
            val authorizationUrl: String?,
            val userAgent: UserAgent,
            @StringRes val loadingMessage: Int? = null,
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

    data class ShowApplicationPasswordTutorialScreen(
        val url: String,
        val errorMessage: String
    ) : MultiLiveEvent.Event()
}
