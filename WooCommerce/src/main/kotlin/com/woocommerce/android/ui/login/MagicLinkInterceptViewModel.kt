package com.woocommerce.android.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.model.JetpackConnectionStatus
import com.woocommerce.android.model.JetpackSiteRegistrationStatus
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.jetpack.FetchJetpackStatus
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.LOGIN
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.wordpress.android.login.LoginAnalyticsListener
import javax.inject.Inject

@HiltViewModel
class MagicLinkInterceptViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val magicLinkInterceptRepository: MagicLinkInterceptRepository,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSite: SelectedSite,
    private val fetchJetpackStatus: FetchJetpackStatus,
    private val loginAnalyticsListener: LoginAnalyticsListener
) : ScopedViewModel(savedState) {
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _showRetryOption = MutableLiveData<Boolean>()
    val showRetryOption: LiveData<Boolean> = _showRetryOption

    private var flow: MagicLinkFlow? = null
    private var hasHandledMagicLink = false

    fun handleMagicLink(authToken: String, flow: MagicLinkFlow?) {
        if (hasHandledMagicLink) return
        hasHandledMagicLink = true

        val jetpackSetupRequestAge = System.currentTimeMillis() - appPrefsWrapper.jetpackSetupMagicLinkRequestedAt
        appPrefsWrapper.removeJetpackSetupMagicLinkRequestedAt()
        val isMagicLinkAllowed = when {
            selectedSite.connectionType == SiteConnectionType.ApplicationPasswords ->
                jetpackSetupRequestAge in 0..JETPACK_SETUP_MAGIC_LINK_VALIDITY_MS
            magicLinkInterceptRepository.hasWpComToken() -> false
            else -> true
        }
        if (!isMagicLinkAllowed) {
            WooLog.w(LOGIN, "Magic link ignored: user is already logged in or did not request Jetpack setup link")
            triggerEvent(IgnoreMagicLink)
            return
        }

        this.flow = flow
        launch {
            _isLoading.value = true
            handleRequestResultResponse(
                requestResult = magicLinkInterceptRepository.updateMagicLinkAuthToken(authToken)
            )
        }
    }

    fun fetchAccountInfo() {
        launch {
            _isLoading.value = true
            _showRetryOption.value = false
            handleRequestResultResponse(magicLinkInterceptRepository.fetchAccountInfo())
        }
    }

    private fun handleRequestResultResponse(requestResult: RequestResult) {
        _isLoading.value = false
        when (requestResult) {
            RequestResult.SUCCESS -> {
                loginAnalyticsListener.trackAnalyticsSignIn(true)
                when {
                    flow == MagicLinkFlow.JetpackConnection &&
                        selectedSite.connectionType == SiteConnectionType.ApplicationPasswords -> {
                        handleJetpackConnectionFlow()
                    }
                    else -> {
                        triggerEvent(OpenSitePicker)
                    }
                }
            }

            // Errors can occur if the auth token was not updated to the FluxC cache
            // or if the user is not logged in
            // Either way, display error message and redirect user to login screen
            RequestResult.ERROR -> {
                triggerEvent(ShowSnackbar(R.string.magic_link_update_error))
                triggerEvent(OpenLogin)
            }

            // If magic link update is successful, but account & site info could not be fetched,
            // display error message and provide option for user to retry the request
            RequestResult.RETRY -> {
                triggerEvent(ShowSnackbar(R.string.magic_link_fetch_account_error))
                _showRetryOption.value = true
            }

            RequestResult.NO_ACTION_NEEDED -> {}
            RequestResult.API_ERROR -> Unit // Do nothing
        }
    }

    private fun handleJetpackConnectionFlow() {
        _isLoading.value = true
        launch {
            fetchJetpackStatus(selectedSite.get(), useApplicationPasswords = true).fold(
                onSuccess = { result ->
                    _isLoading.value = false
                    val jetpackStatus = when (result) {
                        is FetchJetpackStatus.JetpackStatusFetchResponse.Success -> result.status

                        FetchJetpackStatus.JetpackStatusFetchResponse.ConnectionForbidden -> {
                            // Shouldn't happen unless the user changed their account's role after starting the setup.
                            // If this occurs, we'll just use a default value, and then the next screens
                            // will show an error message.
                            JetpackStatus(
                                isJetpackInstalled = false,
                                jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
                                    siteRegistrationStatus = JetpackSiteRegistrationStatus.NOT_REGISTERED,
                                    blogId = null
                                )
                            )
                        }
                    }
                    triggerEvent(ContinueJetpackActivation(jetpackStatus, selectedSite.get().url))
                },
                onFailure = {
                    triggerEvent(ShowSnackbar(R.string.magic_link_fetch_account_error))
                    _isLoading.value = false
                    _showRetryOption.value = true
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        magicLinkInterceptRepository.onCleanup()
    }

    object OpenSitePicker : MultiLiveEvent.Event()
    object OpenLogin : MultiLiveEvent.Event()
    object IgnoreMagicLink : MultiLiveEvent.Event()
    data class ContinueJetpackActivation(
        val jetpackStatus: JetpackStatus,
        val siteUrl: String
    ) : MultiLiveEvent.Event()
    data object CancelJetpackActivation : MultiLiveEvent.Event()

    companion object {
        private const val JETPACK_SETUP_MAGIC_LINK_VALIDITY_MS = 15 * 60 * 1000L
    }
}
