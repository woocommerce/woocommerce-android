package com.woocommerce.android.ui.login.sitecredentials

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.R
import com.woocommerce.android.applicationpasswords.ApplicationPasswordsNotifier
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.UserEligibilityFetcher
import com.woocommerce.android.ui.login.WPApiSiteRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUiStringSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest.XmlRpcErrorType.AUTH_REQUIRED
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationError
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.HTTP_AUTH_ERROR
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.INCORRECT_USERNAME_OR_PASSWORD
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.INVALID_OTP
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.INVALID_TOKEN
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.NEEDS_2FA
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.NOT_AUTHENTICATED
import org.wordpress.android.login.LoginAnalyticsListener
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

@HiltViewModel
class LoginSiteCredentialsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val wpApiSiteRepository: WPApiSiteRepository,
    private val selectedSite: SelectedSite,
    private val loginAnalyticsListener: LoginAnalyticsListener,
    private val resourceProvider: ResourceProvider,
    applicationPasswordsNotifier: ApplicationPasswordsNotifier,
    private val userEligibilityFetcher: UserEligibilityFetcher
) : ScopedViewModel(savedStateHandle) {
    companion object {
        const val SITE_ADDRESS_KEY = "site-address"
        const val USERNAME_KEY = "username"
        const val PASSWORD_KEY = "password"
    }

    private val siteAddress: String = savedStateHandle[SITE_ADDRESS_KEY]!!

    private val errorMessage = savedStateHandle.getStateFlow(viewModelScope, 0)
    private val isLoading = MutableStateFlow(false)

    val state = combine(
        flowOf(siteAddress.removeSchemeAndSuffix()),
        savedStateHandle.getStateFlow(USERNAME_KEY, ""),
        savedStateHandle.getStateFlow(PASSWORD_KEY, ""),
        isLoading,
        errorMessage.map { it.takeIf { it != 0 } }
    ) { siteAddress, username, password, isLoading, errorMessage ->
        LoginSiteCredentialsViewState(
            siteUrl = siteAddress,
            username = username,
            password = password,
            isLoading = isLoading,
            errorMessage = errorMessage
        )
    }.asLiveData()

    init {
        loginAnalyticsListener.trackUsernamePasswordFormViewed()
        applicationPasswordsNotifier.featureUnavailableEvents
            .onEach {
                triggerEvent(ShowApplicationPasswordsUnavailableScreen(siteAddress))
            }
            .launchIn(this)
    }

    fun onUsernameChanged(username: String) {
        savedState[USERNAME_KEY] = username
        errorMessage.value = 0
    }

    fun onPasswordChanged(password: String) {
        savedState[PASSWORD_KEY] = password
        errorMessage.value = 0
    }

    fun onContinueClick() = launch {
        loginAnalyticsListener.trackSubmitClicked()
        if (selectedSite.exists()) {
            // The login already succeeded, proceed to fetching user info
            fetchUserInfo()
        } else {
            login()
        }
    }

    private suspend fun login() {
        val state = requireNotNull(this@LoginSiteCredentialsViewModel.state.value)
        isLoading.value = true
        wpApiSiteRepository.login(
            url = siteAddress,
            username = state.username,
            password = state.password
        ).fold(
            onSuccess = {
                checkWooStatus(it)
            },
            onFailure = { exception ->
                var errorMessage: Int? = null
                if (exception is OnChangedException && exception.error is AuthenticationError) {
                    errorMessage = exception.error.toErrorMessage()
                    if (errorMessage == null) {
                        val message = exception.error.message?.takeIf { it.isNotEmpty() }
                            ?.let { UiStringText(it) } ?: UiStringRes(R.string.error_generic)
                        triggerEvent(ShowUiStringSnackbar(message))
                    }
                    this@LoginSiteCredentialsViewModel.errorMessage.value = errorMessage ?: 0
                } else {
                    triggerEvent(ShowSnackbar(R.string.error_generic))
                }

                // Track errors
                val errorType = (exception as? OnChangedException)?.error ?: exception
                loginAnalyticsListener.trackLoginFailed(
                    errorContext = errorType.javaClass.simpleName,
                    errorType = (errorType as? AuthenticationError)?.type?.toString(),
                    errorDescription = exception.message
                )
                loginAnalyticsListener.trackFailure(
                    message = errorMessage?.let { resourceProvider.getString(it) } ?: exception.message
                )
            }
        )
        isLoading.value = false
    }

    private suspend fun checkWooStatus(site: SiteModel) {
        isLoading.value = true
        wpApiSiteRepository.checkWooStatus(site = site).fold(
            onSuccess = { isWooInstalled ->
                if (isWooInstalled) {
                    loginAnalyticsListener.trackAnalyticsSignIn(false)
                    selectedSite.set(site)
                    fetchUserInfo()
                } else {
                    triggerEvent(ShowNonWooErrorScreen(siteAddress))
                }
            },
            onFailure = {
                triggerEvent(ShowSnackbar(R.string.error_generic))
            }
        )
        isLoading.value = false
    }

    private suspend fun fetchUserInfo() {
        isLoading.value = true
        userEligibilityFetcher.fetchUserInfo().fold(
            onSuccess = {
                triggerEvent(LoggedIn(selectedSite.getSelectedSiteId()))
            },
            onFailure = {
                triggerEvent(ShowSnackbar(R.string.error_generic))
            }
        )
        isLoading.value = false
    }

    fun onResetPasswordClick() {
        triggerEvent(ShowResetPasswordScreen(siteAddress))
    }

    fun onBackClick() {
        triggerEvent(Exit)
    }

    fun onWooInstallationAttempted() = launch {
        checkWooStatus(wpApiSiteRepository.getSiteByUrl(siteAddress)!!)
    }

    fun retryApplicationPasswordsCheck() = launch {
        checkWooStatus(wpApiSiteRepository.getSiteByUrl(siteAddress)!!)
    }

    private fun String.removeSchemeAndSuffix() = UrlUtils.removeScheme(UrlUtils.removeXmlrpcSuffix(this))

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

    @Parcelize
    data class LoginSiteCredentialsViewState(
        val siteUrl: String,
        val username: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        @StringRes val errorMessage: Int? = null
    ) : Parcelable {
        @IgnoredOnParcel
        val isValid = username.isNotBlank() && password.isNotBlank()
    }

    data class LoggedIn(val localSiteId: Int) : MultiLiveEvent.Event()
    data class ShowResetPasswordScreen(val siteAddress: String) : MultiLiveEvent.Event()
    data class ShowNonWooErrorScreen(val siteAddress: String) : MultiLiveEvent.Event()
    data class ShowApplicationPasswordsUnavailableScreen(val siteAddress: String) : MultiLiveEvent.Event()
}
