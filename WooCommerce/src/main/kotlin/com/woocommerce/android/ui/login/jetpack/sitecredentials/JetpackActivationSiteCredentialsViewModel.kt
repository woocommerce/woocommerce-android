package com.woocommerce.android.ui.login.jetpack.sitecredentials

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.JetpackConnectionStatus
import com.woocommerce.android.model.JetpackSiteRegistrationStatus
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.ui.compose.DialogState
import com.woocommerce.android.ui.jetpack.FetchJetpackStatus
import com.woocommerce.android.ui.jetpack.FetchJetpackStatus.JetpackStatusFetchResponse
import com.woocommerce.android.ui.login.WPApiSiteRepository
import com.woocommerce.android.ui.login.WPApiSiteRepository.CookieNonceAuthenticationException
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUiStringSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore.SiteError
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

@HiltViewModel
class JetpackActivationSiteCredentialsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val wpApiSiteRepository: WPApiSiteRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val fetchJetpackStatus: FetchJetpackStatus,
    private val appPrefs: AppPrefsWrapper
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: JetpackActivationSiteCredentialsFragmentArgs by savedStateHandle.navArgs()

    private var isShowingAccountConnectionDialog = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = false,
        key = "is-showing-account-connection-dialog"
    )
    private var connectedWPComEmail: String?
        get() = savedState["connected-wpcom-email"]
        set(value) = savedState.set("connected-wpcom-email", value)

    private val _viewState = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = JetpackActivationSiteCredentialsViewState(
            isJetpackInstalled = navArgs.jetpackStatus.isJetpackInstalled,
            siteUrl = UrlUtils.removeScheme(navArgs.siteUrl)
        )
    )
    val viewState = _viewState.asLiveData()

    val dialogState = isShowingAccountConnectionDialog.map {
        when (it) {
            true -> DialogState(
                title = R.string.login_jetpack_user_already_connected_dialog_title,
                message = R.string.login_jetpack_user_already_connected_dialog_message,
                positiveButton = DialogState.DialogButton(
                    text = R.string.login_jetpack_user_already_connected_dialog_proceed_button,
                    onClick = ::signInUsingConnectedWPComAccount
                ),
                negativeButton = DialogState.DialogButton(
                    text = R.string.login_jetpack_user_already_connected_dialog_cancel_button,
                    onClick = {
                        _viewState.update {
                            it.copy(username = "", password = "")
                        }
                        isShowingAccountConnectionDialog.value = false
                    }
                )
            )

            false -> null
        }
    }.asLiveData()

    init {
        analyticsTrackerWrapper.track(AnalyticsEvent.LOGIN_JETPACK_SITE_CREDENTIAL_SCREEN_VIEWED)
    }

    fun onUsernameChanged(username: String) {
        _viewState.update { state ->
            state.copy(
                username = username,
                errorMessage = null
            )
        }
    }

    fun onPasswordChanged(password: String) {
        _viewState.update { state ->
            state.copy(
                password = password,
                errorMessage = null
            )
        }
    }

    fun onCloseClick() {
        analyticsTrackerWrapper.track(AnalyticsEvent.LOGIN_JETPACK_SITE_CREDENTIAL_SCREEN_DISMISSED)
        triggerEvent(Exit)
    }

    fun onResetPasswordClick() {
        analyticsTrackerWrapper.track(AnalyticsEvent.LOGIN_JETPACK_SITE_CREDENTIAL_RESET_PASSWORD_BUTTON_TAPPED)
        triggerEvent(ResetPassword(navArgs.siteUrl))
    }

    fun onContinueClick() = launch {
        analyticsTrackerWrapper.track(AnalyticsEvent.LOGIN_JETPACK_SITE_CREDENTIAL_INSTALL_BUTTON_TAPPED)
        _viewState.update { it.copy(isLoading = true) }

        val state = _viewState.value
        wpApiSiteRepository.loginAndFetchSite(
            url = navArgs.siteUrl,
            username = state.username,
            password = state.password
        ).fold(
            onSuccess = {
                analyticsTrackerWrapper.track(AnalyticsEvent.LOGIN_JETPACK_SITE_CREDENTIAL_DID_FINISH_LOGIN)
                fetchJetpackStatusAndContinue(it)
            },
            onFailure = { exception ->
                val authenticationError = exception as? CookieNonceAuthenticationException
                val siteError = (exception as? OnChangedException)?.error as? SiteError

                _viewState.update { state ->
                    state.copy(errorMessage = authenticationError?.errorMessage)
                }

                if (authenticationError?.errorMessage == null) {
                    triggerEvent(ShowUiStringSnackbar(UiStringRes(R.string.error_generic)))
                }

                analyticsTrackerWrapper.track(
                    stat = AnalyticsEvent.LOGIN_JETPACK_SITE_CREDENTIAL_DID_SHOW_ERROR_ALERT,
                    errorContext = exception.javaClass.simpleName,
                    errorType = authenticationError?.errorType?.name ?: siteError?.type?.name,
                    errorDescription = exception.message
                )
            }
        )

        _viewState.update { it.copy(isLoading = false) }
    }

    private suspend fun fetchJetpackStatusAndContinue(site: SiteModel) {
        fetchJetpackStatus(
            site = site,
            useApplicationPasswords = false,
            isJetpackInstalled = navArgs.jetpackStatus.isJetpackInstalled
        ).fold(
            onSuccess = {
                val jetpackStatus = when (it) {
                    is JetpackStatusFetchResponse.Success -> {
                        if (it.status.jetpackConnectionStatus is JetpackConnectionStatus.AccountConnected) {
                            connectedWPComEmail = it.status.jetpackConnectionStatus.wpComEmail
                            isShowingAccountConnectionDialog.value = true
                            return@fold
                        }
                        it.status
                    }

                    is JetpackStatusFetchResponse.ConnectionForbidden -> {
                        // When we can't fetch the connection data, we know that the site is not registered with Jetpack
                        // The user won't be to connect to Jetpack, and the next screen will show the error message
                        // So we can just proceed with default values
                        JetpackStatus(
                            isJetpackInstalled = navArgs.jetpackStatus.isJetpackInstalled,
                            jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
                                siteRegistrationStatus = JetpackSiteRegistrationStatus.NOT_REGISTERED,
                                blogId = null
                            )
                        )
                    }
                }
                triggerEvent(NavigateToJetpackActivationSteps(navArgs.siteUrl, jetpackStatus))
            },
            onFailure = {
                triggerEvent(ShowUiStringSnackbar(UiStringRes(R.string.error_generic)))
            }
        )
    }

    private fun signInUsingConnectedWPComAccount() {
        connectedWPComEmail?.let {
            // Save the address of the site the user is trying to connect to to be used later in the login screen
            appPrefs.setLoginSiteAddress(navArgs.siteUrl)
            triggerEvent(OpenWordPressComLogin(it))
        }
    }

    @Parcelize
    data class JetpackActivationSiteCredentialsViewState(
        val isJetpackInstalled: Boolean,
        val siteUrl: String,
        val username: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val errorMessage: UiString? = null
    ) : Parcelable {
        @IgnoredOnParcel
        val isValid = username.isNotBlank() && password.isNotBlank()
    }

    data class NavigateToJetpackActivationSteps(
        val siteUrl: String,
        val jetpackStatus: JetpackStatus
    ) : MultiLiveEvent.Event()

    data class OpenWordPressComLogin(val email: String) : MultiLiveEvent.Event()

    data class ResetPassword(val siteUrl: String) : MultiLiveEvent.Event()
}
