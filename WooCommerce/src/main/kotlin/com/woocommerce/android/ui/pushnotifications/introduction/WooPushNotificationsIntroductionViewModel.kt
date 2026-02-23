package com.woocommerce.android.ui.pushnotifications.introduction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppUrls
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.jetpack.FetchJetpackStatus
import com.woocommerce.android.ui.jetpack.FetchJetpackStatus.JetpackStatusFetchResponse
import com.woocommerce.android.ui.pushnotifications.CheckWooPluginPushNotificationsSupport
import com.woocommerce.android.ui.pushnotifications.CheckWooPluginPushNotificationsSupport.Result.Compatible
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPushNotificationsIntroductionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val fetchJetpackStatus: FetchJetpackStatus,
    private val checkWCPluginSupport: CheckWooPluginPushNotificationsSupport,
    private val selectedSite: SelectedSite
) : ScopedViewModel(savedStateHandle) {

    private val _viewState = MutableStateFlow<ViewState>(ViewState.Loading)
    val viewState = _viewState.asLiveData()

    init {
        fetchStatus()
    }

    private fun fetchStatus() {
        launch {
            val site = selectedSite.get()

            val result = fetchJetpackStatus(
                site = site,
                useApplicationPasswords = true
            )

            result.fold(
                onSuccess = { response ->
                    when (response) {
                        is JetpackStatusFetchResponse.ConnectionForbidden -> {
                            _viewState.value = ViewState.ForbiddenError
                        }

                        is JetpackStatusFetchResponse.Success -> {
                            if (response.status.isSiteConnected) {
                                checkWCVersion()
                            } else {
                                _viewState.value = ViewState.NotConnected
                            }
                        }
                    }
                },
                onFailure = {
                    _viewState.value = ViewState.GenericError
                }
            )
        }
    }

    private suspend fun checkWCVersion() {
        when (checkWCPluginSupport()) {
            Compatible -> _viewState.value = ViewState.GenericError
            is CheckWooPluginPushNotificationsSupport.Result.UpdateRequired ->
                _viewState.value = ViewState.UpdateRequired
            CheckWooPluginPushNotificationsSupport.Result.Error -> _viewState.value = ViewState.GenericError
        }
    }

    fun onContinueClick() {
        when (_viewState.value) {
            ViewState.UpdateRequired -> {
                triggerEvent(
                    NavigateToConnectionSteps(
                        isSiteConnectedToJetpack = true,
                        shouldAutoOpenUpdatePlugin = true
                    )
                )
            }
            ViewState.NotConnected -> {
                triggerEvent(
                    NavigateToConnectionSteps(
                        isSiteConnectedToJetpack = false,
                        shouldAutoOpenUpdatePlugin = false
                    )
                )
            }
            else -> Unit
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

    sealed interface ViewState {
        data object Loading : ViewState
        data object NotConnected : ViewState
        data object UpdateRequired : ViewState
        data object ForbiddenError : ViewState
        data object GenericError : ViewState
    }

    data class NavigateToConnectionSteps(
        val isSiteConnectedToJetpack: Boolean,
        val shouldAutoOpenUpdatePlugin: Boolean
    ) : Event()

    data class OpenUrlEvent(val url: String) : Event()
}
