package com.woocommerce.android.ui.login.jetpack.sitecredentials

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUiStringSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCRequest.XmlRpcErrorType.AUTH_REQUIRED
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationError
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.HTTP_AUTH_ERROR
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.INCORRECT_USERNAME_OR_PASSWORD
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.INVALID_OTP
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.INVALID_TOKEN
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.NEEDS_2FA
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.NOT_AUTHENTICATED
import javax.inject.Inject

@HiltViewModel
class JetpackActivationSiteCredentialsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val wpApiSiteRepository: WPApiSiteRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: JetpackActivationSiteCredentialsFragmentArgs by savedStateHandle.navArgs()

    private val _viewState = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = JetpackActivationSiteCredentialsViewState(
            isJetpackInstalled = navArgs.isJetpackInstalled,
            siteUrl = navArgs.siteUrl
        )
    )
    val viewState = _viewState.asLiveData()

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
        triggerEvent(ResetPassword(_viewState.value.siteUrl))
    }

    fun onContinueClick() = launch {
        analyticsTrackerWrapper.track(AnalyticsEvent.LOGIN_JETPACK_SITE_CREDENTIAL_INSTALL_BUTTON_TAPPED)
        _viewState.update { it.copy(isLoading = true) }

        val state = _viewState.value
        wpApiSiteRepository.login(
            url = state.siteUrl,
            username = state.username,
            password = state.password
        ).fold(
            onSuccess = {
                analyticsTrackerWrapper.track(AnalyticsEvent.LOGIN_JETPACK_SITE_CREDENTIAL_DID_FINISH_LOGIN)
                triggerEvent(
                    NavigateToJetpackActivationSteps(
                        state.siteUrl,
                        navArgs.isJetpackInstalled
                    )
                )
            },
            onFailure = { exception ->
                var errorType: AuthenticationErrorType? = null

                if (exception is OnChangedException && exception.error is AuthenticationError) {
                    errorType = exception.error.type
                    val errorMessage = exception.error.toErrorMessage()
                    if (errorMessage == null) {
                        val message = exception.error.message?.takeIf { it.isNotEmpty() }
                            ?.let { UiStringText(it) } ?: UiStringRes(R.string.error_generic)
                        triggerEvent(ShowUiStringSnackbar(message))
                    }
                    _viewState.update { state ->
                        state.copy(errorMessage = errorMessage)
                    }
                } else {
                    triggerEvent(ShowSnackbar(R.string.error_generic))
                }
                analyticsTrackerWrapper.track(
                    stat = AnalyticsEvent.LOGIN_JETPACK_SITE_CREDENTIAL_DID_SHOW_ERROR_ALERT,
                    errorContext = this@JetpackActivationSiteCredentialsViewModel.javaClass.simpleName,
                    errorType = errorType?.name ?: exception::class.simpleName,
                    errorDescription = exception.message
                )
            }
        )

        _viewState.update { it.copy(isLoading = false) }
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

    @Parcelize
    data class JetpackActivationSiteCredentialsViewState(
        val isJetpackInstalled: Boolean,
        val siteUrl: String,
        val username: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        @StringRes val errorMessage: Int? = null
    ) : Parcelable {
        @IgnoredOnParcel
        val isValid = username.isNotBlank() && password.isNotBlank()
    }

    data class NavigateToJetpackActivationSteps(
        val siteUrl: String,
        val isJetpackInstalled: Boolean
    ) : MultiLiveEvent.Event()

    data class ResetPassword(val siteUrl: String) : MultiLiveEvent.Event()
}
