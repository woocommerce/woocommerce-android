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
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineStart.LAZY
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import javax.inject.Inject

@HiltViewModel
class AccountMismatchErrorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val accountMismatchRepository: AccountMismatchRepository,
    private val resourceProvider: ResourceProvider,
    private val userAgent: UserAgent,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val JETPACK_PLANS_URL = "wordpress.com/jetpack/connect/plans"
    }

    private val navArgs: AccountMismatchErrorFragmentArgs by savedStateHandle.navArgs()
    private val userAccount = accountRepository.getUserAccount()
    private val siteUrl = navArgs.siteUrl
    private val site: Deferred<SiteModel> = async(start = LAZY) {
        accountMismatchRepository.getSiteByUrl(siteUrl) ?: error("The site is not cached")
    }

    private val step = savedStateHandle.getStateFlow<Step>(viewModelScope, Step.MainContent)
    private val _loadingDialogMessage = MutableStateFlow<Int?>(null)
    val loadingDialogMessage = _loadingDialogMessage.asLiveData()

    val viewState: LiveData<ViewState> = step.map { step ->
        when (step) {
            Step.MainContent -> prepareMainState()
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
            AccountMismatchPrimaryButton.CONNECT_JETPACK -> R.string.login_connect_jetpack_button
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
        successConnectionUrls = listOf(siteUrl, JETPACK_PLANS_URL),
        userAgent = userAgent.userAgent,
        onDismiss = {
            analyticsTrackerWrapper.track(AnalyticsEvent.LOGIN_JETPACK_CONNECT_DISMISSED)
            step.value = Step.MainContent
        },
        onConnected = {
            analyticsTrackerWrapper.track(AnalyticsEvent.LOGIN_JETPACK_CONNECT_COMPLETED)
            step.value = Step.FetchJetpackEmail
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
        analyticsTrackerWrapper.track(AnalyticsEvent.LOGIN_JETPACK_CONNECT_BUTTON_TAPPED)
        _loadingDialogMessage.value = R.string.loading
        val site = site.await()
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
                val site = site.await()
                accountMismatchRepository.fetchJetpackConnectedEmail(site).fold(
                    onSuccess = {
                        triggerEvent(OnJetpackConnectedEvent(it))
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
        triggerEvent(NavigateToHelpScreen)
    }

    sealed interface ViewState {
        data class MainState(
            val userInfo: UserInfo?,
            val message: String,
            @StringRes val primaryButtonText: Int?,
            val primaryButtonAction: () -> Unit,
            @StringRes val secondaryButtonText: Int,
            val secondaryButtonAction: () -> Unit,
            @StringRes val inlineButtonText: Int?,
            val inlineButtonAction: () -> Unit
        ) : ViewState

        data class JetpackWebViewState(
            val connectionUrl: String,
            val successConnectionUrls: List<String>,
            val userAgent: String,
            val onDismiss: () -> Unit,
            val onConnected: () -> Unit
        ) : ViewState

        object FetchingJetpackEmailViewState : ViewState
        data class JetpackEmailErrorState(val retry: () -> Unit) : ViewState
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
    data class OnJetpackConnectedEvent(val email: String) : MultiLiveEvent.Event()

    private sealed interface Step : Parcelable {
        @Parcelize
        object MainContent : Step

        @Parcelize
        data class JetpackConnection(val connectionUrl: String) : Step

        @Parcelize
        object FetchJetpackEmail : Step

        @Parcelize
        object FetchingJetpackEmailFailed : Step
    }

    enum class AccountMismatchPrimaryButton {
        SHOW_SITE_PICKER, ENTER_NEW_SITE_ADDRESS, CONNECT_JETPACK, NONE
    }
}
