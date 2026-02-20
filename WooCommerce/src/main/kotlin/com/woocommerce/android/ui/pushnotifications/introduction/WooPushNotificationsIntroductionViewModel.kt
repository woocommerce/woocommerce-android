package com.woocommerce.android.ui.pushnotifications.introduction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppUrls
import com.woocommerce.android.extensions.isVersionAtLeast
import com.woocommerce.android.model.JetpackConnectionStatus
import com.woocommerce.android.model.JetpackSiteRegistrationStatus
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.jetpack.FetchJetpackStatus
import com.woocommerce.android.ui.jetpack.FetchJetpackStatus.JetpackStatusFetchResponse
import com.woocommerce.android.util.FetchActiveWCPluginVersion
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPushNotificationsIntroductionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val fetchJetpackStatus: FetchJetpackStatus,
    private val fetchActiveWCPluginVersion: FetchActiveWCPluginVersion,
    private val selectedSite: SelectedSite
) : ScopedViewModel(savedStateHandle) {

    companion object {
        const val PUSH_NOTIFICATIONS_MIN_WC_VERSION = "10.6.0" // TODO CHECK CORRECT VERSION LATER

        private val JetpackStatus.isSiteConnected: Boolean
            get() = when (jetpackConnectionStatus) {
                is JetpackConnectionStatus.AccountConnected -> true
                is JetpackConnectionStatus.AccountNotConnected ->
                    jetpackConnectionStatus.siteRegistrationStatus == JetpackSiteRegistrationStatus.REGISTERED
            }
    }

    private val _viewState = MutableStateFlow(ViewState())
    val viewState = _viewState.asLiveData()

    fun onContinueClick() {
        launch {
            _viewState.update { it.copy(isLoading = true, errorType = null) }

            val site = selectedSite.get()

            val result = fetchJetpackStatus(
                site = site,
                useApplicationPasswords = true
            )

            result.fold(
                onSuccess = { response ->
                    when (response) {
                        is JetpackStatusFetchResponse.ConnectionForbidden -> {
                            _viewState.update {
                                it.copy(isLoading = false, errorType = ErrorType.Forbidden)
                            }
                        }

                        is JetpackStatusFetchResponse.Success -> {
                            if (response.status.isSiteConnected) {
                                checkWCVersion()
                            } else {
                                _viewState.update { it.copy(isLoading = false) }
                                triggerEvent(StartWPComLogin)
                            }
                        }
                    }
                },
                onFailure = {
                    _viewState.update {
                        it.copy(isLoading = false, errorType = ErrorType.Generic)
                    }
                }
            )
        }
    }

    private suspend fun checkWCVersion() {
        val wcVersion = fetchActiveWCPluginVersion()
        if (wcVersion != null && wcVersion.isVersionAtLeast(PUSH_NOTIFICATIONS_MIN_WC_VERSION)) {
            _viewState.update {
                it.copy(isLoading = false, errorType = ErrorType.Generic)
            }
        } else {
            _viewState.update { it.copy(isLoading = false) }
            triggerEvent(NavigateToConnectionSteps)
        }
    }

    fun onNotNowClick() {
        triggerEvent(Exit)
    }

    fun onWhatIsWPComClick() {
        triggerEvent(OpenUrlEvent(AppUrls.LOGIN_WITH_EMAIL_WHAT_IS_WORDPRESS_COM_ACCOUNT))
    }

    fun onContactSupportClick() {
        triggerEvent(Event.NavigateToHelpScreen(HelpOrigin.WOO_PUSH_NOTIFICATIONS_SETUP))
    }

    data class ViewState(
        val isLoading: Boolean = false,
        val errorType: ErrorType? = null
    ) {
        val isError: Boolean get() = errorType != null
    }

    enum class ErrorType {
        Generic,
        Forbidden
    }

    data object StartWPComLogin : Event()

    data object NavigateToConnectionSteps : Event()

    data class OpenUrlEvent(val url: String) : Event()
}
