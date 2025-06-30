package com.woocommerce.android.ui.login.accountmismatch

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.support.help.HelpOrigin.LOGIN_SITE_ADDRESS
import com.woocommerce.android.ui.common.webview.WebViewAuthenticator
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.NavigateToHelpScreen
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.UserAgent
import javax.inject.Inject

@HiltViewModel
class AccountMismatchErrorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val resourceProvider: ResourceProvider,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    val webViewAuthenticator: WebViewAuthenticator,
    val userAgent: UserAgent
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: AccountMismatchErrorFragmentArgs by savedStateHandle.navArgs()
    private val userAccount = accountRepository.getUserAccount()
    private val siteUrl = navArgs.siteUrl

    val viewState: LiveData<ViewState> = flowOf(
        prepareMainState()
    ).asLiveData()

    init {
        if (navArgs.primaryButton == AccountMismatchPrimaryButton.CONNECT_JETPACK) {
            analyticsTrackerWrapper.track(AnalyticsEvent.LOGIN_JETPACK_CONNECTION_ERROR_SHOWN)
        }
    }

    private fun prepareMainState() = ViewState(
        userInfo = userAccount?.let {
            UserInfo(
                avatarUrl = it.avatarUrl.orEmpty(),
                username = it.userName,
                displayName = it.displayName.orEmpty()
            )
        },
        message = resourceProvider.getString(R.string.login_jetpack_not_connected, siteUrl),
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
        inlineButtonText = R.string.login_need_help_finding_email,
        inlineButtonAction = { helpFindingEmail() },
        showJetpackTermsConsent = navArgs.primaryButton == AccountMismatchPrimaryButton.CONNECT_JETPACK,
        onBackPressed = { triggerEvent(Exit) }
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
        triggerEvent(
            NavigateToJetpackActivationSteps(
                siteUrl = siteUrl,
                // Pass a default value, we'll update it later after the user signs in
                // See JetpackActivationSiteCredentialsViewModel
                jetpackStatus = JetpackStatus(
                    isJetpackInstalled = true,
                    jetpackConnectionStatus = com.woocommerce.android.model.JetpackConnectionStatus.AccountNotConnected(
                        siteRegistrationStatus = com.woocommerce.android.model.JetpackSiteRegistrationStatus.UNKNOWN,
                        blogId = null
                    )
                )
            )
        )
    }

    private fun helpFindingEmail() {
        triggerEvent(NavigateToEmailHelpDialogEvent)
    }

    fun onHelpButtonClick() {
        triggerEvent(NavigateToHelpScreen(LOGIN_SITE_ADDRESS))
    }

    data class ViewState(
        val userInfo: UserInfo?,
        val message: String,
        @StringRes val primaryButtonText: Int?,
        val primaryButtonAction: () -> Unit,
        @StringRes val secondaryButtonText: Int,
        val secondaryButtonAction: () -> Unit,
        @StringRes val inlineButtonText: Int?,
        val inlineButtonAction: () -> Unit,
        val showJetpackTermsConsent: Boolean,
        val onBackPressed: () -> Unit
    )

    data class UserInfo(
        val avatarUrl: String,
        val displayName: String,
        val username: String
    )

    object NavigateToEmailHelpDialogEvent : MultiLiveEvent.Event()
    object NavigateToLoginScreen : MultiLiveEvent.Event()
    data class OnJetpackConnectedEvent(val email: String, val isAuthenticated: Boolean) : MultiLiveEvent.Event()
    data class NavigateToJetpackActivationSteps(
        val siteUrl: String,
        val jetpackStatus: JetpackStatus
    ) : MultiLiveEvent.Event()

    enum class AccountMismatchPrimaryButton {
        CONNECT_JETPACK, CONNECT_WPCOM_SITE, NONE
    }
}
