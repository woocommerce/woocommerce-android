package com.woocommerce.android.ui.login.jetpack.wpcom

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.analytics.AnalyticsEvent.JETPACK_SETUP_LOGIN_FLOW
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.ui.login.WPComLoginRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.AccountStore.AuthOptionsError
import org.wordpress.android.fluxc.store.AccountStore.AuthOptionsErrorType
import org.wordpress.android.login.R
import javax.inject.Inject

@HiltViewModel
class JetpackActivationWPComEmailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val wpComLoginRepository: WPComLoginRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: JetpackActivationWPComEmailFragmentArgs by savedStateHandle.navArgs()

    private val emailOrUsername = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = "",
        key = "email"
    )
    private val errorMessage = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = 0,
        key = "error-message"
    )
    private val isLoadingDialogShown = MutableStateFlow(false)

    val viewState = combine(
        emailOrUsername,
        isLoadingDialogShown,
        errorMessage
    ) { emailOrUsername, isLoadingDialogShown, errorMessage ->
        ViewState(
            emailOrUsername = emailOrUsername,
            isJetpackInstalled = navArgs.jetpackStatus.isJetpackInstalled,
            isLoadingDialogShown = isLoadingDialogShown,
            errorMessage = errorMessage.takeIf { it != 0 }
        )
    }.asLiveData()

    fun onEmailOrUsernameChanged(emailOrUsername: String) {
        errorMessage.value = 0
        this.emailOrUsername.value = emailOrUsername
    }

    fun onCloseClick() {
        wpComLoginRepository.clearAccessToken()
        triggerEvent(Exit)

        analyticsTrackerWrapper.track(
            JETPACK_SETUP_LOGIN_FLOW,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_JETPACK_SETUP_STEP_EMAIL_ADDRESS,
                AnalyticsTracker.KEY_TAP to AnalyticsTracker.VALUE_DISMISS
            )
        )
    }

    fun onContinueClick() = launch {
        val emailOrUsername = emailOrUsername.value.trim()

        analyticsTrackerWrapper.track(
            JETPACK_SETUP_LOGIN_FLOW,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_JETPACK_SETUP_STEP_EMAIL_ADDRESS,
                AnalyticsTracker.KEY_TAP to AnalyticsTracker.VALUE_SUBMIT
            )
        )

        isLoadingDialogShown.value = true
        wpComLoginRepository.fetchAuthOptions(emailOrUsername).fold(
            onSuccess = {
                if (it.isPasswordless) {
                    triggerEvent(ShowMagicLinkScreen(emailOrUsername, navArgs.jetpackStatus))
                } else {
                    triggerEvent(ShowPasswordScreen(emailOrUsername, navArgs.jetpackStatus))
                }
            },
            onFailure = {
                val failure = (it as? OnChangedException)?.error as? AuthOptionsError

                when (failure?.type) {
                    AuthOptionsErrorType.UNKNOWN_USER -> {
                        errorMessage.value = R.string.email_not_registered_wpcom
                    }

                    AuthOptionsErrorType.EMAIL_LOGIN_NOT_ALLOWED -> {
                        errorMessage.value = R.string.error_user_username_instead_of_email
                        this@JetpackActivationWPComEmailViewModel.emailOrUsername.value = ""
                    }

                    else -> {
                        triggerEvent(ShowSnackbar(R.string.error_generic))
                    }
                }

                analyticsTrackerWrapper.track(
                    JETPACK_SETUP_LOGIN_FLOW,
                    mapOf(
                        AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_JETPACK_SETUP_STEP_EMAIL_ADDRESS,
                        AnalyticsTracker.KEY_FAILURE to (failure?.type?.name ?: "Unknown error")
                    )
                )
            }
        )
        isLoadingDialogShown.value = false
    }

    data class ViewState(
        val emailOrUsername: String,
        val isJetpackInstalled: Boolean,
        val isLoadingDialogShown: Boolean = false,
        val errorMessage: Int? = null
    ) {
        val enableSubmit = emailOrUsername.isNotBlank()
    }

    data class ShowPasswordScreen(
        val emailOrUsername: String,
        val jetpackStatus: JetpackStatus
    ) : MultiLiveEvent.Event()

    data class ShowMagicLinkScreen(
        val emailOrUsername: String,
        val jetpackStatus: JetpackStatus
    ) : MultiLiveEvent.Event()
}
