package com.woocommerce.android.ui.jetpack.benefits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent.JETPACK_BENEFITS_LOGIN_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.JETPACK_INSTALL_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.JETPACK_SETUP_CONNECTION_CHECK_COMPLETED
import com.woocommerce.android.analytics.AnalyticsEvent.JETPACK_SETUP_CONNECTION_CHECK_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.JETPACK_SETUP_LOGIN_FLOW
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.model.UserRole
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.common.UserEligibilityFetcher
import com.woocommerce.android.ui.jetpack.benefits.FetchJetpackStatus.JetpackStatusFetchResponse
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import javax.inject.Inject

@HiltViewModel
class JetpackBenefitsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val userEligibilityFetcher: UserEligibilityFetcher,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val fetchJetpackStatus: FetchJetpackStatus,
    private val wpComAccessToken: AccessToken
) : ScopedViewModel(savedStateHandle) {

    companion object {
        const val ERROR_CODE_FORBIDDEN = 403
        const val ERROR_CODE_NOT_FOUND = 404
    }

    private val _viewState = MutableStateFlow(
        ViewState(
            isUsingJetpackCP = selectedSite.connectionType == SiteConnectionType.JetpackConnectionPackage,
            isLoadingDialogShown = false,
            isNativeJetpackActivationAvailable = FeatureFlag.REST_API_I2.isEnabled()
        )
    )
    val viewState = _viewState.asLiveData()

    private val isAppPasswords = selectedSite.connectionType == SiteConnectionType.ApplicationPasswords

    fun onInstallClick() = launch {
        when (selectedSite.connectionType) {
            SiteConnectionType.JetpackConnectionPackage -> {
                AnalyticsTracker.track(
                    stat = JETPACK_INSTALL_BUTTON_TAPPED,
                    properties = mapOf(AnalyticsTracker.KEY_JETPACK_INSTALLATION_SOURCE to "benefits_modal")
                )

                triggerEvent(StartJetpackActivationForJetpackCP)
            }
            SiteConnectionType.ApplicationPasswords -> {
                AnalyticsTracker.track(stat = JETPACK_BENEFITS_LOGIN_BUTTON_TAPPED)

                _viewState.update { it.copy(isLoadingDialogShown = true) }

                val jetpackStatusResult = fetchJetpackStatus()
                handleJetpackStatusResult(jetpackStatusResult)

                _viewState.update { it.copy(isLoadingDialogShown = false) }
            }

            else -> error("Non supported site type ${selectedSite.connectionType} in Jetpack Benefits screen")
        }
    }

    private fun handleJetpackStatusResult(
        result: Result<JetpackStatusFetchResponse>
    ) {
        fun startJetpackActivation(jetpackStatus: JetpackStatus) {
            triggerEvent(StartJetpackActivationForApplicationPasswords(selectedSite.get().url, jetpackStatus))
        }

        fun showGenericError() = triggerEvent(ShowSnackbar(string.error_generic))

        fun logError(statusCode: Int, reason: String) = logError("HTTP Code $statusCode: $reason")

        fun logSuccess(jetpackStatus: JetpackStatus) {
            analyticsTrackerWrapper.track(
                stat = JETPACK_SETUP_CONNECTION_CHECK_COMPLETED,
                properties = mapOf(
                    AnalyticsTracker.KEY_JETPACK_SETUP_IS_ALREADY_CONNECTED to jetpackStatus.isJetpackConnected,
                    AnalyticsTracker.KEY_JETPACK_SETUP_REQUIRES_CONNECTION_ONLY to
                        (jetpackStatus.isJetpackInstalled && !jetpackStatus.isJetpackConnected)
                )
            )
        }

        fun handleUserEligibility(statusCode: Int, jetpackStatus: JetpackStatus? = null) {
            launch {
                userEligibilityFetcher.fetchUserInfo().fold(
                    onSuccess = { user ->
                        val hasInstallCapability = user.roles.contains(UserRole.Administrator)

                        when {
                            hasInstallCapability && statusCode == ERROR_CODE_NOT_FOUND && jetpackStatus != null -> {
                                startJetpackActivation(jetpackStatus)
                            }
                            else -> {
                                triggerEvent(OpenJetpackEligibilityError(user.username, user.roles.first().value))

                                logError(
                                    statusCode,
                                    "${user.roles.first().value}: User not authorized to install Jetpack"
                                )
                            }
                        }
                    },
                    onFailure = {
                        showGenericError()
                        logError(statusCode, it.message.orEmpty())
                    }
                )
            }
        }

        result.fold(
            onSuccess = { fetchResponse ->
                when (fetchResponse) {
                    is JetpackStatusFetchResponse.Success -> {
                        if (fetchResponse.status.isJetpackInstalled) {
                            startJetpackActivation(fetchResponse.status)
                            logSuccess(fetchResponse.status)
                        } else {
                            handleUserEligibility(ERROR_CODE_NOT_FOUND, fetchResponse.status)
                        }
                    }
                    JetpackStatusFetchResponse.ConnectionForbidden -> handleUserEligibility(ERROR_CODE_FORBIDDEN)
                }
            },
            onFailure = {
                showGenericError()
                logError(it.message)
            }
        )
    }

    private fun logError(message: String?) {
        if (isAppPasswords) {
            analyticsTrackerWrapper.track(
                stat = JETPACK_SETUP_CONNECTION_CHECK_FAILED,
                properties = mapOf(
                    AnalyticsTracker.KEY_FAILURE to "Domain '${selectedSite.get().url}': ${message ?: "Unknown error"}"
                )
            )
        }
    }

    fun onDismiss() {
        triggerEvent(Exit)
        wpComAccessToken.set(null)

        if (isAppPasswords) {
            analyticsTrackerWrapper.track(
                JETPACK_SETUP_LOGIN_FLOW,
                mapOf(
                    AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_JETPACK_INSTALLATION_STEP_BENEFITS,
                    AnalyticsTracker.KEY_TAP to AnalyticsTracker.VALUE_DISMISS
                )
            )
        }
    }

    data class ViewState(
        val isUsingJetpackCP: Boolean,
        val isLoadingDialogShown: Boolean,
        val isNativeJetpackActivationAvailable: Boolean
    )

    object StartJetpackActivationForJetpackCP : Event()
    data class StartJetpackActivationForApplicationPasswords(
        val siteUrl: String,
        val jetpackStatus: JetpackStatus
    ) : Event()
    data class OpenWpAdminJetpackActivation(val activationUrl: String) : Event()
    data class OpenJetpackEligibilityError(val username: String, val role: String) : Event()
}
