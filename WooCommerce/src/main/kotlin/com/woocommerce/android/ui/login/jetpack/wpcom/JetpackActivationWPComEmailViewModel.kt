package com.woocommerce.android.ui.login.jetpack.wpcom

import androidx.core.util.PatternsCompat
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

    private val email = savedStateHandle.getStateFlow(scope = viewModelScope, initialValue = "", key = "email")
    private val errorMessage =
        savedStateHandle.getStateFlow(scope = viewModelScope, initialValue = 0, key = "error-message")
    private val isLoadingDialogShown = MutableStateFlow(false)

    val viewState = combine(email, isLoadingDialogShown, errorMessage) { email, isLoadingDialogShown, errorMessage ->
        ViewState(
            email = email,
            isJetpackInstalled = navArgs.jetpackStatus.isJetpackInstalled,
            isLoadingDialogShown = isLoadingDialogShown,
            errorMessage = errorMessage.takeIf { it != 0 }
        )
    }.asLiveData()

    fun onEmailChanged(email: String) {
        errorMessage.value = 0
        this.email.value = email
    }

    fun onCloseClick() {
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
        val email = email.value.trim()
        if (!PatternsCompat.EMAIL_ADDRESS.matcher(email).matches()) {
            errorMessage.value = R.string.email_invalid
            return@launch
        }

        analyticsTrackerWrapper.track(
            JETPACK_SETUP_LOGIN_FLOW,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_JETPACK_SETUP_STEP_EMAIL_ADDRESS,
                AnalyticsTracker.KEY_TAP to AnalyticsTracker.VALUE_SUBMIT
            )
        )

        isLoadingDialogShown.value = true
        wpComLoginRepository.fetchAuthOptions(email).fold(
            onSuccess = {
                if (it.isPasswordless) {
                    triggerEvent(ShowMagicLinkScreen(email, navArgs.jetpackStatus))
                } else {
                    triggerEvent(ShowPasswordScreen(email, navArgs.jetpackStatus))
                }
            },
            onFailure = {
                val failure = (it as? OnChangedException)?.error as? AuthOptionsError

                when (failure?.type) {
                    AuthOptionsErrorType.UNKNOWN_USER -> {
                        errorMessage.value = R.string.email_not_registered_wpcom
                    }

                    AuthOptionsErrorType.EMAIL_LOGIN_NOT_ALLOWED -> {
                        TODO()
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
        val email: String,
        val isJetpackInstalled: Boolean,
        val isLoadingDialogShown: Boolean = false,
        val errorMessage: Int? = null
    ) {
        val enableSubmit = email.isNotBlank()
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
