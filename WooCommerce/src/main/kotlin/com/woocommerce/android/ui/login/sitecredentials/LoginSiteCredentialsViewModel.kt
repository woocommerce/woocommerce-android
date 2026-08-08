package com.woocommerce.android.ui.login.sitecredentials

import android.os.Parcelable
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
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.WPApiSiteRepository
import com.woocommerce.android.ui.login.WPApiSiteRepository.CookieNonceAuthenticationException
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.CookieNonceAuthenticationEndpoints
import org.wordpress.android.fluxc.network.rest.wpapi.CookieNonceAuthenticationEndpoints.AdminBaseVerification
import org.wordpress.android.fluxc.network.rest.wpapi.CookieNonceAuthenticationEndpoints.Endpoint as ValidationEndpoint
import org.wordpress.android.fluxc.network.rest.wpapi.CookieNonceAuthenticationEndpoints.ValidationError.INVALID_URL
import org.wordpress.android.fluxc.network.rest.wpapi.CookieNonceAuthenticationEndpoints.ValidationResult
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.CookieNonceErrorType.BASIC_AUTH_REQUIRED
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.CookieNonceErrorType.CUSTOM_ADMIN_URL
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.CookieNonceErrorType.CUSTOM_LOGIN_URL
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.CookieNonceErrorType.INVALID_CREDENTIALS
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.CookieNonceErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsConfiguration
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
    private val resourceProvider: ResourceProvider,
    private val applicationPasswordsConfiguration: ApplicationPasswordsConfiguration
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
        private const val HAS_RECONCILED_SITE_URL_KEY = "has-reconciled-site-url"
        private const val LOGIN_ENTRY_URL_KEY = "login-entry-url"
        private const val ADMIN_BASE_URL_KEY = "admin-base-url"
    }

    private var siteAddress: String
        get() = savedStateHandle[SITE_ADDRESS_KEY]!!
        set(value) {
            savedStateHandle[SITE_ADDRESS_KEY] = value
        }

    // The URL the WP.com `connect/site-info` endpoint reports as the canonical site URL is
    // sometimes wrong — its cache can return an http URL for a site that 301s to https. Once
    // FluxC's WP-API discovery resolves the real scheme, we adopt it and re-attempt login
    // (guarded so a misbehaving server can't trigger a ping-pong update).
    private var hasReconciledSiteUrl: Boolean
        get() = savedStateHandle[HAS_RECONCILED_SITE_URL_KEY] ?: false
        set(value) {
            savedStateHandle[HAS_RECONCILED_SITE_URL_KEY] = value
        }

    private val authError = savedStateHandle.getNullableStateFlow(
        scope = viewModelScope,
        initialValue = null,
        clazz = AuthenticationError::class.java,
        key = "site-credentials-auth-error"
    )
    private val fetchedSiteId = savedStateHandle.getStateFlow(viewModelScope, -1, "site-id")

    private val loadingMessage = savedStateHandle.getStateFlow(viewModelScope, 0, "loading-message")
    private val endpointRecovery = savedStateHandle.getNullableStateFlow(
        scope = viewModelScope,
        initialValue = null,
        clazz = EndpointRecovery::class.java,
        key = "endpoint-recovery"
    )

    private val SiteModel?.fullAuthorizationUrl: String?
        get() = this?.applicationPasswordsAuthorizeUrl
            ?.let { url ->
                "$url?app_name=${applicationPasswordsConfiguration.applicationName}&success_url=$REDIRECTION_URL"
            }

    val viewState = combine(
        flowOf(siteAddress.removeSchemeAndSuffix()),
        savedStateHandle.getStateFlow(USERNAME_KEY, ""),
        savedStateHandle.getStateFlow(PASSWORD_KEY, ""),
        loadingMessage.map { message -> message.takeIf { it != 0 } },
        combine(authError, endpointRecovery) { error, recovery -> error to recovery }
    ) { siteAddress, username, password, loadingMessage, errors ->
        ViewState(
            siteUrl = siteAddress,
            username = username,
            password = password,
            loadingMessage = loadingMessage,
            authenticationError = errors.first,
            endpointRecovery = errors.second
        )
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

    fun onEndpointUrlChanged(url: String) {
        endpointRecovery.value = endpointRecovery.value?.copy(url = url, errorMessage = null)
    }

    private suspend fun continueEndpointRecovery(recovery: EndpointRecovery) {
        val endpoints = currentAuthenticationEndpoints(recovery).withSiteSchemeFrom(recovery.url)
        when (val validation = endpoints.validate()) {
            is ValidationResult.Valid -> {
                val validatedEndpoints = endpoints.withValidatedUrls(validation)
                endpointRecovery.value = recovery.copy(
                    url = when (recovery.type) {
                        EndpointType.LOGIN -> requireNotNull(validatedEndpoints.loginEntryUrl)
                        EndpointType.ADMIN -> requireNotNull(validatedEndpoints.adminBaseUrl)
                    },
                    errorMessage = null
                )
                login(validatedEndpoints, recovery.type)
            }

            is ValidationResult.Invalid -> {
                val errorMessage = validation.error.toUiString()
                if (validation.endpoint == recovery.type.validationEndpoint) {
                    endpointRecovery.value = recovery.copy(errorMessage = errorMessage)
                } else {
                    endpointRecovery.value = recovery.copy(errorMessage = null)
                    authError.value = AuthenticationError(
                        errorMessage = if (validation.endpoint == ValidationEndpoint.CANONICAL) {
                            UiStringRes(R.string.error_generic)
                        } else {
                            errorMessage
                        },
                        showWpAdminFallbackOption = true
                    )
                }
            }
        }
    }

    fun onEndpointRecoveryCancelClick() {
        endpointRecovery.value = null
    }

    fun onContinueClick() = launch {
        endpointRecovery.value?.let {
            continueEndpointRecovery(it)
            return@launch
        }
        loginAnalyticsListener.trackSubmitClicked()
        val site = fetchedSiteId.value.takeIf { it != -1 }?.let { wpApiSiteRepository.getSiteByLocalId(it) }
        if (site?.username != null) {
            // The login already succeeded, proceed to fetching user info
            fetchUserInfo()
        } else {
            login()
        }
    }

    fun onErrorDialogDismissed() {
        authError.value = null
    }

    fun onResetPasswordClick() {
        triggerEvent(ShowResetPasswordScreen(siteAddress))
    }

    fun onStartWebAuthorizationClick() {
        launch {
            authError.value = null
            fetchSiteForTutorial()
        }
    }

    fun onPasswordTutorialAborted() {
        fetchedSiteId.value = -1
    }

    fun onBackClick() {
        triggerEvent(Exit)
    }

    fun onHelpButtonClick() {
        viewState.value?.let {
            triggerEvent(ShowHelpScreen(siteAddress, it.username))
        }
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
        fetchedSiteId.value = -1
        login()
    }

    private suspend fun login(
        endpoints: CookieNonceAuthenticationEndpoints = currentAuthenticationEndpoints(),
        retryingEndpoint: EndpointType? = null
    ) {
        val state = requireNotNull(this@LoginSiteCredentialsViewModel.viewState.value)
        loadingMessage.value = R.string.logging_in
        wpApiSiteRepository.login(
            url = siteAddress,
            username = state.username,
            password = state.password,
            endpoints = endpoints
        ).fold(
            onSuccess = {
                promoteValidatedEndpoints(endpoints)
                endpointRecovery.value = null
                fetchSite()
            },
            onFailure = { exception ->
                val authenticationError = exception as? CookieNonceAuthenticationException
                val loginEntryWasVerified = authenticationError?.loginEntryVerified == true ||
                    authenticationError?.errorType == CUSTOM_ADMIN_URL
                val hasVerifiedCustomLoginEntry = endpoints.loginEntryUrl != null && loginEntryWasVerified

                if (retryingEndpoint == EndpointType.LOGIN && hasVerifiedCustomLoginEntry) {
                    promoteValidatedEndpoint(EndpointType.LOGIN, endpoints)
                    endpointRecovery.value = null
                }

                when (authenticationError?.errorType) {
                    CUSTOM_LOGIN_URL -> {
                        if (hasVerifiedCustomLoginEntry) {
                            authError.value = AuthenticationError(
                                errorMessage = authenticationError.errorMessage,
                                showWpAdminFallbackOption = false
                            )
                        } else {
                            handleEndpointRecovery(
                                EndpointType.LOGIN,
                                retryingEndpoint == EndpointType.LOGIN
                            )
                        }
                    }

                    CUSTOM_ADMIN_URL -> {
                        handleEndpointRecovery(
                            EndpointType.ADMIN,
                            retryingEndpoint == EndpointType.ADMIN
                        )
                    }

                    INVALID_CREDENTIALS -> authError.value = AuthenticationError(
                        errorMessage = authenticationError.errorMessage,
                        showWpAdminFallbackOption = endpoints.loginEntryUrl == null
                    )

                    BASIC_AUTH_REQUIRED -> authError.value = AuthenticationError(
                        errorMessage = authenticationError.errorMessage,
                        showWpAdminFallbackOption = false
                    )

                    else -> {
                        if (hasVerifiedCustomLoginEntry) {
                            authError.value = AuthenticationError(
                                errorMessage = requireNotNull(authenticationError).errorMessage,
                                showWpAdminFallbackOption = false
                            )
                        } else if (authenticationError?.errorType == INVALID_RESPONSE &&
                            endpoints.loginEntryUrl != null
                        ) {
                            showEndpointRecovery(EndpointType.LOGIN, showError = true)
                        } else if (retryingEndpoint != null || endpoints.loginEntryUrl != null) {
                            authError.value = AuthenticationError(
                                errorMessage = authenticationError?.errorMessage ?: UiStringRes(R.string.error_generic),
                                showWpAdminFallbackOption = false
                            )
                        } else {
                            fetchSiteForTutorial(detectedErrorMessage = authenticationError?.errorMessage)
                            analyticsTracker.track(AnalyticsEvent.LOGIN_SITE_CREDENTIALS_INVALID_LOGIN_PAGE_DETECTED)
                        }
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

    private suspend fun fetchSiteForTutorial(detectedErrorMessage: UiString? = null) {
        loadingMessage.value = R.string.login_site_credentials_fetching_site
        wpApiSiteRepository.fetchSite(url = siteAddress).fold(
            onSuccess = { site ->
                val canonicalUrl = site.url
                if (!hasReconciledSiteUrl && !canonicalUrl.isNullOrEmpty() && canonicalUrl != siteAddress) {
                    hasReconciledSiteUrl = true
                    siteAddress = canonicalUrl
                    login()
                    return@fold
                }
                if (site.hasWooCommerce) {
                    fetchedSiteId.value = site.id
                    loadingMessage.value = 0
                    val errorMessage = detectedErrorMessage
                        ?.toPresentableString()
                        ?: resourceProvider.getString(R.string.error_generic)
                    if (site.fullAuthorizationUrl.isNotNullOrEmpty()) {
                        triggerEvent(
                            ShowApplicationPasswordTutorialScreen(
                                url = site.fullAuthorizationUrl!!,
                                errorMessage = errorMessage
                            )
                        )
                    } else {
                        analyticsTracker.track(
                            AnalyticsEvent.APPLICATION_PASSWORDS_AUTHORIZATION_URL_NOT_AVAILABLE,
                            properties = mapOf(
                                AnalyticsTracker.KEY_SITE_URL to siteAddress
                            )
                        )
                        triggerEvent(ShowApplicationPasswordsUnavailableScreen(siteAddress, site.isJetpackConnected))
                    }
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
        loadingMessage.value = R.string.logging_in
        wpApiSiteRepository.fetchSite(
            url = siteAddress,
            username = viewState?.username,
            password = viewState?.password
        ).fold(
            onSuccess = { site ->
                if (site.hasWooCommerce) {
                    persistAuthenticationEndpoints(site)
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

        this.authError.value = AuthenticationError(
            errorMessage = UiStringRes(R.string.login_site_credentials_fetching_site_failed)
        )

        val error = (exception as? OnChangedException)?.error ?: exception
        trackLoginFailure(
            step = Step.AUTHENTICATION,
            errorContext = error.javaClass.simpleName,
            errorType = siteError?.type?.name,
            errorDescription = exception.message
        )
    }

    private suspend fun persistAuthenticationEndpoints(site: SiteModel) {
        val endpoints = currentAuthenticationEndpoints(recovery = null)
        if (endpoints.loginEntryUrl == null && endpoints.adminBaseUrl == null) {
            fetchedSiteId.value = site.id
            fetchUserInfo(site)
        } else {
            wpApiSiteRepository.saveAuthenticationEndpoints(site, endpoints).fold(
                onSuccess = {
                    fetchedSiteId.value = it.id
                    fetchUserInfo(it)
                },
                onFailure = ::handleEndpointPersistenceFailure
            )
        }
    }

    private suspend fun fetchUserInfo(site: SiteModel? = null) {
        val resolvedSite = site ?: requireNotNull(wpApiSiteRepository.getSiteByLocalId(fetchedSiteId.value)) {
            "Site credentials login: Site not found in DB after login"
        }
        loadingMessage.value = R.string.logging_in
        checkEligibilityAndCompleteLogin(resolvedSite)
        loadingMessage.value = 0
    }

    private suspend fun checkEligibilityAndCompleteLogin(site: SiteModel) {
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
            onFailure = ::handleUserInfoFailure
        )
    }

    private fun handleEndpointPersistenceFailure(exception: Throwable) {
        triggerEvent(ShowSnackbar(R.string.login_site_credentials_endpoint_persistence_failed))
        val siteError = (exception as? OnChangedException)?.error as? SiteError
        val error = (exception as? OnChangedException)?.error ?: exception
        trackLoginFailure(
            step = Step.ENDPOINT_PERSISTENCE,
            errorContext = error.javaClass.simpleName,
            errorType = siteError?.type?.name,
            errorDescription = exception.message
        )
    }

    private fun handleUserInfoFailure(exception: Throwable) {
        triggerEvent(ShowSnackbar(R.string.user_role_access_error_fetch_failed))
        val applicationPasswordError = (exception as? ApplicationPasswordGenerationException)?.networkError
        if (applicationPasswordError != null) {
            trackLoginFailure(
                step = Step.APPLICATION_PASSWORD_GENERATION,
                errorContext = applicationPasswordError.javaClass.simpleName,
                errorType = applicationPasswordError.type.name,
                errorDescription = exception.message
            )
        } else {
            val wooError = (exception as? WooException)?.error
            trackLoginFailure(
                step = Step.USER_ROLE,
                errorContext = (wooError ?: exception).javaClass.simpleName,
                errorType = wooError?.type?.name,
                errorDescription = exception.message
            )
        }
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

    private fun currentAuthenticationEndpoints(
        recovery: EndpointRecovery? = endpointRecovery.value
    ): CookieNonceAuthenticationEndpoints {
        val savedLoginUrl = savedStateHandle.get<String>(LOGIN_ENTRY_URL_KEY)
        val savedAdminUrl = savedStateHandle.get<String>(ADMIN_BASE_URL_KEY)
        return CookieNonceAuthenticationEndpoints(
            siteUrl = siteAddress,
            loginEntryUrl = if (recovery?.type == EndpointType.LOGIN) recovery.url else savedLoginUrl,
            adminBaseUrl = if (recovery?.type == EndpointType.ADMIN) recovery.url else savedAdminUrl,
            adminBaseVerification = if (recovery?.type == EndpointType.ADMIN) {
                AdminBaseVerification.AUTHENTICATED_DASHBOARD
            } else {
                AdminBaseVerification.NONE
            }
        )
    }

    private fun promoteValidatedEndpoints(endpoints: CookieNonceAuthenticationEndpoints) {
        val validation = endpoints.withSiteSchemeFrom(
            endpoints.loginEntryUrl ?: endpoints.adminBaseUrl
        ).validate() as? ValidationResult.Valid ?: return
        val validatedEndpoints = endpoints.withValidatedUrls(validation)
        EndpointType.entries.forEach { type ->
            promoteEndpoint(type, validatedEndpoints)
        }
    }

    private fun promoteValidatedEndpoint(
        type: EndpointType,
        endpoints: CookieNonceAuthenticationEndpoints
    ) {
        val validation = endpoints.withSiteSchemeFrom(type.urlFrom(endpoints)).validate()
            as? ValidationResult.Valid ?: return
        promoteEndpoint(type, endpoints.withValidatedUrls(validation))
    }

    private fun promoteEndpoint(type: EndpointType, endpoints: CookieNonceAuthenticationEndpoints) {
        when (type) {
            EndpointType.LOGIN -> endpoints.loginEntryUrl?.let { savedStateHandle[LOGIN_ENTRY_URL_KEY] = it }
            EndpointType.ADMIN -> endpoints.adminBaseUrl?.let { savedStateHandle[ADMIN_BASE_URL_KEY] = it }
        }
    }

    private fun showEndpointRecovery(type: EndpointType, showError: Boolean) {
        val endpoints = currentAuthenticationEndpoints(recovery = null)
        val defaults = endpoints.withSiteSchemeFrom(
            type.urlFrom(endpoints) ?: endpoints.loginEntryUrl ?: endpoints.adminBaseUrl
        ).validate() as? ValidationResult.Valid
        val url = endpointRecovery.value?.takeIf { it.type == type }?.url ?: when (type) {
            EndpointType.LOGIN -> endpoints.loginEntryUrl ?: defaults?.loginEntryUrl?.toString().orEmpty()
            EndpointType.ADMIN -> endpoints.adminBaseUrl ?: defaults?.adminBaseUrl?.toString().orEmpty()
        }
        endpointRecovery.value = EndpointRecovery(
            type = type,
            url = url,
            errorMessage = UiStringRes(type.missingUrlError).takeIf { showError }
        )
    }

    private suspend fun handleEndpointRecovery(type: EndpointType, isRetry: Boolean) {
        if (!isRetry && !hasReconciledSiteUrl) {
            val canonicalUrl = wpApiSiteRepository.fetchSite(url = siteAddress).getOrNull()?.url
            val reconciledSiteAddress = canonicalUrl?.toSafeReconciledSiteAddress()
            if (reconciledSiteAddress != null) {
                hasReconciledSiteUrl = true
                siteAddress = reconciledSiteAddress
                login()
                return
            }
        }
        showEndpointRecovery(type, isRetry)
    }

    private fun String.toSafeReconciledSiteAddress(): String? {
        if (isEmpty() || this == siteAddress) return null
        val currentEndpoints = CookieNonceAuthenticationEndpoints(siteAddress)
            .withSiteSchemeFrom(this)
            .validate() as? ValidationResult.Valid ?: return null
        val candidateEndpoints = CookieNonceAuthenticationEndpoints(this).validate()
            as? ValidationResult.Valid ?: return null
        return takeIf { currentEndpoints.allows(candidateEndpoints.siteUrl) }
    }

    private fun CookieNonceAuthenticationEndpoints.withSiteSchemeFrom(
        endpointUrl: String?
    ): CookieNonceAuthenticationEndpoints {
        if (siteUrl.toHttpUrlOrNull() != null) return this
        val scheme = endpointUrl?.toHttpUrlOrNull()?.scheme ?: "https"
        val schemeLessSiteUrl = siteUrl.substringAfter("://").trimStart('/')
        return copy(
            siteUrl = "$scheme://$schemeLessSiteUrl"
        )
    }

    private fun CookieNonceAuthenticationEndpoints.withValidatedUrls(
        validation: ValidationResult.Valid
    ) = copy(
        siteUrl = validation.siteUrl.toString(),
        loginEntryUrl = loginEntryUrl?.let { validation.loginEntryUrl.toString() },
        adminBaseUrl = adminBaseUrl?.let { validation.adminBaseUrl.toString() }
    )

    private fun CookieNonceAuthenticationEndpoints.ValidationError.toUiString() = UiStringRes(
        if (this == INVALID_URL) {
            R.string.login_site_credentials_endpoint_invalid_url_error
        } else {
            R.string.login_site_credentials_endpoint_same_site_error
        }
    )

    @Suppress("SpreadOperator")
    private fun UiString.toPresentableString(): String = when (this) {
        is UiStringRes -> resourceProvider.getString(
            stringRes,
            *params.map { it.toPresentableString() }.toTypedArray()
        )
        is UiStringText -> text
    }

    data class ViewState(
        val siteUrl: String,
        val username: String = "",
        val password: String = "",
        @StringRes val loadingMessage: Int? = null,
        val authenticationError: AuthenticationError? = null,
        val endpointRecovery: EndpointRecovery? = null
    ) {
        val isValid = username.isNotBlank() && password.isNotBlank()
    }

    @Parcelize
    data class AuthenticationError(
        val errorMessage: UiString,
        val showWpAdminFallbackOption: Boolean = true
    ) : Parcelable

    enum class EndpointType(
        @StringRes val missingUrlError: Int,
        val validationEndpoint: ValidationEndpoint
    ) {
        LOGIN(R.string.login_site_credentials_login_url_not_found_error, ValidationEndpoint.LOGIN),
        ADMIN(R.string.login_site_credentials_admin_url_not_found_error, ValidationEndpoint.ADMIN);

        fun urlFrom(endpoints: CookieNonceAuthenticationEndpoints) = when (this) {
            LOGIN -> endpoints.loginEntryUrl
            ADMIN -> endpoints.adminBaseUrl
        }
    }

    @Parcelize
    data class EndpointRecovery(
        val type: EndpointType,
        val url: String,
        val errorMessage: UiString? = null
    ) : Parcelable

    @VisibleForTesting
    enum class Step {
        AUTHENTICATION, APPLICATION_PASSWORD_GENERATION, ENDPOINT_PERSISTENCE, USER_ROLE
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
