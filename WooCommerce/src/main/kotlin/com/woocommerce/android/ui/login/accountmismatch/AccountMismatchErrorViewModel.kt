package com.woocommerce.android.ui.login.accountmismatch

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.network.UserAgent
import javax.inject.Inject

@HiltViewModel
class AccountMismatchErrorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val accountMismatchRepository: AccountMismatchRepository,
    private val resourceProvider: ResourceProvider,
    private val userAgent: UserAgent
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: AccountMismatchErrorFragmentArgs by savedStateHandle.navArgs()
    private val userAccount = accountRepository.getUserAccount()
    private val siteUrl = appPrefsWrapper.getLoginSiteAddress()!!

    private val step = savedStateHandle.getStateFlow<Step>(viewModelScope, Step.Idle)

    val viewState: LiveData<ViewState> = step.map { step ->
        when (step) {
            Step.Idle, Step.FetchingJetpackConnectionUrl -> prepareMainState(
                isFetchingJetpackUrl = step is Step.FetchingJetpackConnectionUrl
            )
            is Step.JetpackConnection -> prepareJetpackConnectionState(step.connectionUrl)
        }
    }.asLiveData()

    private fun prepareMainState(isFetchingJetpackUrl: Boolean) = ViewState.MainState(
        userInfo = userAccount?.let {
            UserInfo(
                avatarUrl = it.avatarUrl.orEmpty(),
                username = it.userName,
                displayName = it.displayName.orEmpty()
            )
        },
        isFetchingJetpackUrl = isFetchingJetpackUrl,
        message = if (accountRepository.isUserLoggedIn()) {
            // When the user is already connected using WPCom account, show account mismatch error
            resourceProvider.getString(R.string.login_wpcom_account_mismatch, siteUrl)
        } else {
            // Explain that account is not connected to Jetpack
            resourceProvider.getString(R.string.login_jetpack_not_connected, siteUrl)
        },
        primaryButtonText = when (navArgs.primaryButton) {
            AccountMismatchPrimaryButton.SHOW_SITE_PICKER -> R.string.login_view_connected_stores
            AccountMismatchPrimaryButton.ENTER_NEW_SITE_ADDRESS -> R.string.login_site_picker_try_another_address
            AccountMismatchPrimaryButton.CONNECT_JETPACK -> {
                if (FeatureFlag.LOGIN_JETPACK_CONNECTION.isEnabled()) R.string.login_connect_jetpack_button else null
            }
            AccountMismatchPrimaryButton.NONE -> null
        },
        primaryButtonAction = {
            when (navArgs.primaryButton) {
                AccountMismatchPrimaryButton.SHOW_SITE_PICKER -> showConnectedStores()
                AccountMismatchPrimaryButton.ENTER_NEW_SITE_ADDRESS -> navigateToSiteAddressScreen()
                AccountMismatchPrimaryButton.CONNECT_JETPACK -> startJetpackConnection()
                AccountMismatchPrimaryButton.NONE ->
                    error("NONE as primary button shouldn't trigger the callback")
            }
        },
        secondaryButtonText = R.string.login_try_another_account,
        secondaryButtonAction = { loginWithDifferentAccount() },
        inlineButtonText = if (accountRepository.isUserLoggedIn()) R.string.login_need_help_finding_email
        else null,
        inlineButtonAction = { helpFindingEmail() }
    )

    private fun prepareJetpackConnectionState(connectionUrl: String) = ViewState.JetpackWebViewState(
        connectionUrl = connectionUrl,
        siteUrl = siteUrl,
        userAgent = userAgent.userAgent,
        onDismiss = {
            step.value = Step.Idle
        },
        onConnected = {
            step.value = Step.Idle
        }
    )

    private fun showConnectedStores() {
        triggerEvent(Exit)
    }

    private fun navigateToSiteAddressScreen() {
        triggerEvent(NavigateToSiteAddressEvent)
    }

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
        state.value = Step.FetchingJetpackConnectionUrl
        val site = accountMismatchRepository.getSiteByUrl(siteUrl) ?: error("The site is not cached")
        accountMismatchRepository.fetchJetpackConnectionUrl(site).fold(
            onSuccess = {
                state.value = Step.JetpackConnection(it)
            },
            onFailure = {
                state.value = Step.Idle
                // TODO show an error snackbar
            }
        )
    }

    private fun helpFindingEmail() {
        triggerEvent(NavigateToEmailHelpDialogEvent)
    }

    fun onHelpButtonClick() {
        triggerEvent(NavigateToHelpScreen)
    }

    sealed interface ViewState {
        data class MainState(
            val userInfo: UserInfo?,
            val message: String,
            val isFetchingJetpackUrl: Boolean,
            @StringRes val primaryButtonText: Int?,
            val primaryButtonAction: () -> Unit,
            @StringRes val secondaryButtonText: Int,
            val secondaryButtonAction: () -> Unit,
            @StringRes val inlineButtonText: Int?,
            val inlineButtonAction: () -> Unit
        ) : ViewState

        data class JetpackWebViewState(
            val connectionUrl: String,
            val siteUrl: String,
            val userAgent: String,
            val onDismiss: () -> Unit,
            val onConnected: () -> Unit
        ) : ViewState
    }

    data class UserInfo(
        val avatarUrl: String,
        val displayName: String,
        val username: String
    )

    object NavigateToHelpScreen : MultiLiveEvent.Event()
    object NavigateToSiteAddressEvent : MultiLiveEvent.Event()
    object NavigateToEmailHelpDialogEvent : MultiLiveEvent.Event()
    object NavigateToLoginScreen : MultiLiveEvent.Event()

    private sealed interface Step : Parcelable {
        @Parcelize
        object Idle : Step

        @Parcelize
        object FetchingJetpackConnectionUrl : Step

        @Parcelize
        data class JetpackConnection(val connectionUrl: String) : Step
    }

    enum class AccountMismatchPrimaryButton {
        SHOW_SITE_PICKER, ENTER_NEW_SITE_ADDRESS, CONNECT_JETPACK, NONE
    }
}
