package com.woocommerce.android.ui.login.jetpack.wpcom

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.gravatar.AvatarQueryOptions
import com.gravatar.AvatarUrl
import com.gravatar.DefaultAvatarOption
import com.gravatar.types.Email
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.JETPACK_SETUP_LOGIN_FLOW
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.login.WPComLoginRepository
import com.woocommerce.android.ui.login.jetpack.JetpackActivationRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.LaunchUrlInChromeTab
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationError
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType
import javax.inject.Inject

@HiltViewModel
class JetpackActivationWPComPasswordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    selectedSite: SelectedSite,
    jetpackAccountRepository: JetpackActivationRepository,
    private val wpComLoginRepository: WPComLoginRepository,
    private val accountRepository: AccountRepository,
    private val resourceProvider: ResourceProvider,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : JetpackActivationWPComPostLoginViewModel(
    savedStateHandle,
    selectedSite,
    jetpackAccountRepository,
    analyticsTrackerWrapper
) {
    companion object {
        private const val RESET_PASSWORD_URL = "https://wordpress.com/wp-login.php?action=lostpassword"
    }

    private val navArgs: JetpackActivationWPComPasswordFragmentArgs by savedStateHandle.navArgs()

    private val password = savedStateHandle.getStateFlow(scope = viewModelScope, initialValue = "", key = "password")
    private val errorMessage =
        savedStateHandle.getStateFlow(scope = viewModelScope, initialValue = 0, key = "error-message")
    private val isLoadingDialogShown = MutableStateFlow(false)

    val viewState = combine(
        password,
        isLoadingDialogShown,
        errorMessage,
        flowOf(Pair(navArgs.emailOrUsername, avatarUrlFromEmail(navArgs.emailOrUsername)))
    ) { password, isLoadingDialogShown, errorMessage, (emailOrUsername, avatarUrl) ->
        ViewState(
            emailOrUsername = emailOrUsername,
            password = password,
            avatarUrl = avatarUrl,
            isJetpackInstalled = navArgs.jetpackStatus.isJetpackInstalled,
            isLoadingDialogShown = isLoadingDialogShown,
            errorMessage = errorMessage.takeIf { it != 0 }
        )
    }.asLiveData()

    fun onPasswordChanged(password: String) {
        errorMessage.value = 0
        this.password.value = password
    }

    fun onCloseClick() {
        triggerEvent(Exit)

        analyticsTrackerWrapper.track(
            JETPACK_SETUP_LOGIN_FLOW,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_JETPACK_SETUP_STEP_PASSWORD,
                AnalyticsTracker.KEY_TAP to AnalyticsTracker.VALUE_DISMISS
            )
        )
    }

    fun onMagicLinkClick() {
        triggerEvent(
            ShowMagicLinkScreen(
                emailOrUsername = navArgs.emailOrUsername,
                jetpackStatus = navArgs.jetpackStatus
            )
        )
    }

    fun onResetPasswordClick() {
        triggerEvent(
            LaunchUrlInChromeTab(RESET_PASSWORD_URL)
        )
    }

    fun onContinueClick() = launch {
        analyticsTrackerWrapper.track(
            JETPACK_SETUP_LOGIN_FLOW,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_JETPACK_SETUP_STEP_PASSWORD,
                AnalyticsTracker.KEY_TAP to AnalyticsTracker.VALUE_SUBMIT
            )
        )

        isLoadingDialogShown.value = true
        wpComLoginRepository.login(navArgs.emailOrUsername, password.value).fold(
            onSuccess = {
                fetchAccount()
            },
            onFailure = {
                val failure = (it as? OnChangedException)?.error as? AuthenticationError

                when (failure?.type) {
                    AuthenticationErrorType.NEEDS_2FA -> {
                        triggerEvent(Show2FAScreen(navArgs.emailOrUsername, password.value, navArgs.jetpackStatus))
                    }

                    AuthenticationErrorType.INCORRECT_USERNAME_OR_PASSWORD,
                    AuthenticationErrorType.NOT_AUTHENTICATED -> {
                        errorMessage.value = R.string.password_incorrect
                    }

                    else -> {
                        triggerEvent(ShowSnackbar(R.string.error_generic))
                    }
                }

                analyticsTrackerWrapper.track(
                    JETPACK_SETUP_LOGIN_FLOW,
                    mapOf(
                        AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_JETPACK_SETUP_STEP_PASSWORD,
                        AnalyticsTracker.KEY_FAILURE to (failure?.type?.name ?: "Unknown error")
                    )
                )
            }
        )
        isLoadingDialogShown.value = false
    }

    private suspend fun fetchAccount() {
        accountRepository.fetchUserAccount().fold(
            onSuccess = {
                onLoginSuccess(navArgs.jetpackStatus)
            },
            onFailure = {
                triggerEvent(ShowSnackbar(R.string.error_fetch_my_profile))
            }
        )
    }

    private fun avatarUrlFromEmail(email: String): String {
        val avatarSize = resourceProvider.getDimensionPixelSize(R.dimen.image_minor_100)
        return AvatarUrl(
            Email(email),
            AvatarQueryOptions(preferredSize = avatarSize, defaultAvatarOption = DefaultAvatarOption.Status404)
        ).toString()
    }

    data class ViewState(
        val emailOrUsername: String,
        val password: String,
        val avatarUrl: String,
        val isJetpackInstalled: Boolean,
        val isLoadingDialogShown: Boolean = false,
        val errorMessage: Int? = null
    ) {
        val enableSubmit = password.isNotBlank()
    }

    data class Show2FAScreen(
        val emailOrUsername: String,
        val password: String,
        val jetpackStatus: JetpackStatus
    ) : MultiLiveEvent.Event()

    data class ShowMagicLinkScreen(
        val emailOrUsername: String,
        val jetpackStatus: JetpackStatus
    ) : MultiLiveEvent.Event()
}
