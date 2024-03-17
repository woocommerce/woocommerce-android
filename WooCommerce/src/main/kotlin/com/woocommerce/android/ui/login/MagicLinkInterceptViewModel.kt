package com.woocommerce.android.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MagicLinkInterceptViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val magicLinkInterceptRepository: MagicLinkInterceptRepository,
    private val selectedSite: SelectedSite,
    private val accountRepository: AccountRepository
) : ScopedViewModel(savedState) {
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _showRetryOption = SingleLiveEvent<Boolean>()
    val showRetryOption: LiveData<Boolean> = _showRetryOption

    private var flow: MagicLinkFlow? = null
    private var source: MagicLinkSource? = null

    fun handleMagicLink(authToken: String, flow: MagicLinkFlow?, source: MagicLinkSource?) {
        this.flow = flow
        this.source = source
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
        val source = this.source
        when (requestResult) {
            RequestResult.SUCCESS -> {
                if (flow == MagicLinkFlow.SiteCredentialsToWPCom &&
                    source != null &&
                    selectedSite.connectionType == SiteConnectionType.ApplicationPasswords
                ) {
                    triggerEvent(
                        ContinueJetpackActivation(
                            jetpackStatus = source.inferJetpackStatus(),
                            siteUrl = selectedSite.get().url
                        )
                    )
                } else {
                    triggerEvent(OpenSitePicker)
                }
            }

            // Errors can occur if the auth token was not updated to the FluxC cache
            // or if the user is not logged in
            // Either way, display error message and redirect user to login screen
            RequestResult.ERROR -> {
                triggerEvent(ShowSnackbar(string.magic_link_update_error))
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

    override fun onCleared() {
        super.onCleared()
        magicLinkInterceptRepository.onCleanup()
    }

    private fun MagicLinkSource.inferJetpackStatus(): JetpackStatus {
        val isJetpackInstalled = this != MagicLinkSource.JetpackInstallation
        val isJetpackConnected = this == MagicLinkSource.WPComAuthentication
        val wpComEmail = if (isJetpackConnected) {
            accountRepository.getUserAccount()?.email
        } else {
            null
        }

        return JetpackStatus(
            isJetpackInstalled = isJetpackInstalled,
            isJetpackConnected = isJetpackConnected,
            wpComEmail = wpComEmail
        )
    }

    object OpenSitePicker : MultiLiveEvent.Event()
    object OpenLogin : MultiLiveEvent.Event()
    data class ContinueJetpackActivation(
        val jetpackStatus: JetpackStatus,
        val siteUrl: String
    ) : MultiLiveEvent.Event()
}
