package com.woocommerce.android.ui.login.accountmismatch

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.support.help.HelpOrigin.LOGIN_SITE_ADDRESS
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.login.UnifiedLoginTracker
import com.woocommerce.android.ui.login.accountmismatch.AccountMismatchErrorViewModel.AccountMismatchErrorType.WPCOM_ACCOUNT_MISMATCH
import com.woocommerce.android.ui.login.accountmismatch.AccountMismatchRepository.JetpackConnectionStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.NavigateToHelpScreen
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUiStringSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest.XmlRpcErrorType.AUTH_REQUIRED
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationError
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.HTTP_AUTH_ERROR
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.INCORRECT_USERNAME_OR_PASSWORD
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.INVALID_OTP
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.INVALID_TOKEN
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.NEEDS_2FA
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.NOT_AUTHENTICATED
import javax.inject.Inject

@HiltViewModel
class AccountMismatchErrorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val accountMismatchRepository: AccountMismatchRepository,
    private val resourceProvider: ResourceProvider,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val unifiedLoginTracker: UnifiedLoginTracker,
    val wpComWebViewAuthenticator: WPComWebViewAuthenticator,
    val userAgent: UserAgent
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val JETPACK_PLANS_URL = "wordpress.com/jetpack/connect/plans"
    }

    private val navArgs: AccountMismatchErrorFragmentArgs by savedStateHandle.navArgs()
    private val userAccount = accountRepository.getUserAccount()
    private val siteUrl = navArgs.siteUrl
    private val site: Deferred<SiteModel?>
        get() = async {
            accountMismatchRepository.getSiteByUrl(siteUrl)
        }

    private val step = savedStateHandle.getStateFlow<Step>(viewModelScope, Step.MainContent)
    private val _loadingDialogMessage = MutableStateFlow<Int?>(null)
    val loadingDialogMessage = _loadingDialogMessage.asLiveData()

    val viewState: LiveData<ViewState> = step.map { step ->
        when (step) {
            Step.MainContent -> prepareMainState()
            is Step.SiteCredentials -> prepareSiteCredentialsState(step)
            is Step.JetpackConnection -> prepareJetpackConnectionState(step.connectionUrl)
            Step.FetchJetpackEmail -> ViewState.FetchingJetpackEmailViewState
            Step.FetchingJetpackEmailFailed -> ViewState.JetpackEmailErrorState {
                this.step.value = Step.FetchJetpackEmail
            }
        }
    }.asLiveData()

    init {
        handleFetchingJetpackEmail()
        if (navArgs.primaryButton == AccountMismatchPrimaryButton.CONNECT_JETPACK) {
            analyticsTrackerWrapper.track(AnalyticsEvent.LOGIN_JETPACK_CONNECTION_ERROR_SHOWN)
        }
    }

    private fun prepareMainState() = ViewState.MainState(
        userInfo = userAccount?.let {
            UserInfo(
                avatarUrl = it.avatarUrl.orEmpty(),
                username = it.userName,
                displayName = it.displayName.orEmpty()
            )
        },
        message = when (navArgs.errorType) {
            AccountMismatchErrorType.WPCOM_ACCOUNT_MISMATCH ->
                resourceProvider.getString(R.string.login_wpcom_account_mismatch, siteUrl)
            AccountMismatchErrorType.ACCOUNT_NOT_CONNECTED ->
                resourceProvider.getString(R.string.login_jetpack_not_connected, siteUrl)
        },
        primaryButtonText = when (navArgs.primaryButton) {
            AccountMismatchPrimaryButton.CONNECT_JETPACK -> R.string.login_account_mismatch_connect_jetpack
            AccountMismatchPrimaryButton.CONNECT_WPCOM_SITE -> R.string.login_account_mismatch_connect_wpcom
            AccountMismatchPrimaryButton.NONE -> null
        },
        primaryButtonAction = {
            when (navArgs.primaryButton) {
                AccountMismatchPrimaryButton.CONNECT_JETPACK -> startJetpackConnection()
                AccountMismatchPrimaryButton.CONNECT_WPCOM_SITE -> {
                    // We are re-using the same event as Jetpack connection here
                    analyticsTrackerWrapper.track(AnalyticsEvent.LOGIN_JETPACK_CONNECT_BUTTON_TAPPED)
                    triggerEvent(
                        ShowDialog(
                            titleId = R.string.login_account_mismatch_connect_wpcom_dialog_title,
                            messageId = R.string.login_account_mismatch_connect_wpcom_dialog_message,
                            positiveButtonId = R.string.continue_button
                        )
                    )
                }
                AccountMismatchPrimaryButton.NONE ->
                    error("NONE as primary button shouldn't trigger the callback")
            }
        },
        secondaryButtonText = R.string.login_try_another_account,
        secondaryButtonAction = { loginWithDifferentAccount() },
        inlineButtonText = if (navArgs.errorType == WPCOM_ACCOUNT_MISMATCH) R.string.login_need_help_finding_email
        else null,
        inlineButtonAction = { helpFindingEmail() },
        showJetpackTermsConsent = navArgs.primaryButton == AccountMismatchPrimaryButton.CONNECT_JETPACK,
        onBackPressed = { triggerEvent(Exit) }
    )

    private fun prepareSiteCredentialsState(stepData: Step.SiteCredentials) = ViewState.SiteCredentialsViewState(
        siteUrl = siteUrl,
        username = stepData.username,
        password = stepData.password,
        errorMessage = stepData.currentErrorMessage,
        onUsernameChanged = {
            step.value = Step.SiteCredentials(username = it, password = stepData.password)
        },
        onPasswordChanged = {
            step.value = Step.SiteCredentials(username = stepData.username, password = it)
        },
        onContinueClick = {
            launch {
                _loadingDialogMessage.value = R.string.logging_in
                accountMismatchRepository.checkJetpackConnection(siteUrl, stepData.username, stepData.password).fold(
                    onSuccess = {
                        _loadingDialogMessage.value = null
                        when (it) {
                            is JetpackConnectionStatus.ConnectedToDifferentAccount -> {
                                // TODO we should probably offer a better UX handling here:
                                //  explain the situation to the user
                                triggerEvent(
                                    OnJetpackConnectedEvent(
                                        email = it.wpcomEmail,
                                        isAuthenticated = false
                                    )
                                )
                            }
                            JetpackConnectionStatus.NotConnected -> startJetpackConnection()
                        }
                    },
                    onFailure = { exception ->
                        _loadingDialogMessage.value = null
                        if (exception is OnChangedException && exception.error is AuthenticationError) {
                            val errorMessage = exception.error.toErrorMessage()
                            if (errorMessage == null) {
                                val message = exception.error.message?.takeIf { it.isNotEmpty() }
                                    ?.let { UiStringText(it) } ?: UiStringRes(R.string.error_generic)
                                triggerEvent(ShowUiStringSnackbar(message))
                            }
                            step.update { step ->
                                (step as Step.SiteCredentials).copy(currentErrorMessage = errorMessage)
                            }
                        } else {
                            triggerEvent(ShowSnackbar(R.string.error_generic))
                        }
                    }
                )
            }
        },
        onBackPressed = { step.value = Step.MainContent },
        onLoginWithAnotherAccountClick = ::loginWithDifferentAccount
    )

    private fun prepareJetpackConnectionState(connectionUrl: String) = ViewState.JetpackWebViewState(
        connectionUrl = connectionUrl,
        successConnectionUrls = listOf(siteUrl, JETPACK_PLANS_URL),
        onBackPressed = {
            analyticsTrackerWrapper.track(AnalyticsEvent.LOGIN_JETPACK_CONNECT_DISMISSED)
            step.value = Step.MainContent
        },
        onConnected = {
            analyticsTrackerWrapper.track(AnalyticsEvent.LOGIN_JETPACK_CONNECT_COMPLETED)
            step.value = Step.FetchJetpackEmail
        }
    )

    private fun loginWithDifferentAccount() {
        if (!accountRepository.isUserLoggedIn()) {
            triggerEvent(NavigateToLoginScreen)
        } else {
            launch {
                accountRepository.logout().let {
                    if (it) {
                        appPrefsWrapper.removeLoginSiteAddress()
                        triggerEvent(NavigateToLoginScreen)
                    }
                }
            }
        }
    }

    private fun startJetpackConnection() = launch {
        analyticsTrackerWrapper.track(AnalyticsEvent.LOGIN_JETPACK_CONNECT_BUTTON_TAPPED)
        val site = site.await()
        if (site == null || site.username.isNullOrEmpty() || site.password.isNullOrEmpty()) {
            unifiedLoginTracker.track(step = UnifiedLoginTracker.Step.USERNAME_PASSWORD)
            step.value = Step.SiteCredentials()
            return@launch
        }
        _loadingDialogMessage.value = R.string.loading
        accountMismatchRepository.fetchJetpackConnectionUrl(site).fold(
            onSuccess = {
                _loadingDialogMessage.value = null
                step.value = Step.JetpackConnection(it)
            },
            onFailure = {
                _loadingDialogMessage.value = null
                step.value = Step.MainContent
                triggerEvent(ShowSnackbar(R.string.login_jetpack_connection_url_failed))
                analyticsTrackerWrapper.track(
                    stat = AnalyticsEvent.LOGIN_JETPACK_CONNECTION_URL_FETCH_FAILED,
                    errorContext = this.javaClass.simpleName,
                    errorType = (it as? OnChangedException)?.error?.javaClass?.simpleName,
                    errorDescription = it.message
                )
            }
        )
    }

    private fun handleFetchingJetpackEmail() = launch {
        step.filter { it is Step.FetchJetpackEmail }
            .collect {
                val site = site.await() ?: error("The site is not cached")
                accountMismatchRepository.fetchJetpackConnectedEmail(site).fold(
                    onSuccess = { email ->
                        val isUserAuthenticated = accountRepository.isUserLoggedIn() &&
                            accountRepository.getUserAccount()?.email == email
                        triggerEvent(OnJetpackConnectedEvent(email, isAuthenticated = isUserAuthenticated))
                    },
                    onFailure = {
                        step.value = Step.FetchingJetpackEmailFailed
                        analyticsTrackerWrapper.track(
                            stat = AnalyticsEvent.LOGIN_JETPACK_CONNECTION_VERIFICATION_FAILED,
                            errorContext = this.javaClass.simpleName,
                            errorType = (it as? OnChangedException)?.error?.javaClass?.simpleName,
                            errorDescription = it.message
                        )
                    }
                )
            }
    }

    private fun helpFindingEmail() {
        triggerEvent(NavigateToEmailHelpDialogEvent)
    }

    fun onHelpButtonClick() {
        triggerEvent(NavigateToHelpScreen(LOGIN_SITE_ADDRESS))
    }

    private fun AuthenticationError.toErrorMessage() = when (type) {
        INCORRECT_USERNAME_OR_PASSWORD, NOT_AUTHENTICATED, HTTP_AUTH_ERROR ->
            if (type == HTTP_AUTH_ERROR && xmlRpcErrorType == AUTH_REQUIRED) {
                R.string.login_error_xml_rpc_auth_error_communicating
            } else {
                R.string.username_or_password_incorrect
            }
        INVALID_OTP, INVALID_TOKEN, AUTHORIZATION_REQUIRED, NEEDS_2FA ->
            R.string.login_2fa_not_supported_self_hosted_site
        else -> {
            null
        }
    }

    sealed interface ViewState {
        val onBackPressed: () -> Unit
            get() = {}

        data class MainState(
            val userInfo: UserInfo?,
            val message: String,
            @StringRes val primaryButtonText: Int?,
            val primaryButtonAction: () -> Unit,
            @StringRes val secondaryButtonText: Int,
            val secondaryButtonAction: () -> Unit,
            @StringRes val inlineButtonText: Int?,
            val inlineButtonAction: () -> Unit,
            val showJetpackTermsConsent: Boolean,
            override val onBackPressed: () -> Unit
        ) : ViewState

        data class SiteCredentialsViewState(
            val siteUrl: String,
            val username: String,
            val password: String,
            @StringRes val errorMessage: Int?,
            val onUsernameChanged: (String) -> Unit,
            val onPasswordChanged: (String) -> Unit,
            val onContinueClick: () -> Unit,
            val onLoginWithAnotherAccountClick: () -> Unit,
            override val onBackPressed: () -> Unit
        ) : ViewState {
            val isValid = username.isNotBlank() && password.isNotBlank()
        }

        data class JetpackWebViewState(
            val connectionUrl: String,
            val successConnectionUrls: List<String>,
            val onConnected: () -> Unit,
            override val onBackPressed: () -> Unit
        ) : ViewState

        object FetchingJetpackEmailViewState : ViewState
        data class JetpackEmailErrorState(val retry: () -> Unit) : ViewState
    }

    data class UserInfo(
        val avatarUrl: String,
        val displayName: String,
        val username: String
    )

    object NavigateToEmailHelpDialogEvent : MultiLiveEvent.Event()
    object NavigateToLoginScreen : MultiLiveEvent.Event()
    data class OnJetpackConnectedEvent(val email: String, val isAuthenticated: Boolean) : MultiLiveEvent.Event()

    private sealed interface Step : Parcelable {
        @Parcelize
        object MainContent : Step

        @Parcelize
        data class SiteCredentials(
            val username: String = "",
            val password: String = "",
            @StringRes val currentErrorMessage: Int? = null
        ) : Step

        @Parcelize
        data class JetpackConnection(val connectionUrl: String) : Step

        @Parcelize
        object FetchJetpackEmail : Step

        @Parcelize
        object FetchingJetpackEmailFailed : Step
    }

    enum class AccountMismatchPrimaryButton {
        CONNECT_JETPACK, CONNECT_WPCOM_SITE, NONE
    }

    /**
     * This state is just to allow different wordings depending on which flow resulted in this error screen depending
     * on whether we can confirm that the site is connected to a different account or not.
     */
    enum class AccountMismatchErrorType {
        WPCOM_ACCOUNT_MISMATCH,
        ACCOUNT_NOT_CONNECTED
    }
}
