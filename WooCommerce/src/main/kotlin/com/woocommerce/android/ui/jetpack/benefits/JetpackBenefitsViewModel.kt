package com.woocommerce.android.ui.jetpack.benefits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent.JETPACK_INSTALL_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
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
import javax.inject.Inject

@HiltViewModel
class JetpackBenefitsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val fetchJetpackStatus: FetchJetpackStatus,
    private val userEligibilityFetcher: UserEligibilityFetcher
) : ScopedViewModel(savedStateHandle) {

    private val _viewState = MutableStateFlow(
        ViewState(
            isUsingJetpackCP = selectedSite.connectionType == SiteConnectionType.JetpackConnectionPackage,
            isLoadingDialogShown = false,
            isNativeJetpackActivationAvailable = FeatureFlag.REST_API_I2.isEnabled()
        )
    )
    val viewState = _viewState.asLiveData()

    fun onInstallClick() = launch {
        AnalyticsTracker.track(
            stat = JETPACK_INSTALL_BUTTON_TAPPED,
            properties = mapOf(AnalyticsTracker.KEY_JETPACK_INSTALLATION_SOURCE to "benefits_modal")
        )

        when (selectedSite.connectionType) {
            SiteConnectionType.JetpackConnectionPackage -> triggerEvent(StartJetpackActivationForJetpackCP)
            SiteConnectionType.ApplicationPasswords -> {
                _viewState.update { it.copy(isLoadingDialogShown = true) }

                val jetpackStatusResult = fetchJetpackStatus()
                handleJetpackStatusResult(jetpackStatusResult)

                _viewState.update { it.copy(isLoadingDialogShown = false) }
            }

            else -> error("Non supported site type ${selectedSite.connectionType} in Jetpack Benefits screen")
        }
    }

    @Suppress("LongMethod")
    private fun handleJetpackStatusResult(
        result: Result<Pair<JetpackStatus, JetpackStatusFetchResponse>>
    ) {
        result.fold(
            onSuccess = {
                when (it.second) {
                    JetpackStatusFetchResponse.SUCCESS -> {
                        triggerEvent(
                            StartJetpackActivationForApplicationPasswords(
                                siteUrl = selectedSite.get().url,
                                jetpackStatus = it.first
                            )
                        )
                    }
                    JetpackStatusFetchResponse.FORBIDDEN -> {
                        launch {
                            userEligibilityFetcher.fetchUserInfo().fold(
                                onSuccess = { user ->
                                    triggerEvent(
                                        OpenJetpackEligibilityError(
                                            user.username,
                                            user.roles.first().value
                                        )
                                    )
                                },
                                onFailure = {
                                    triggerEvent(ShowSnackbar(string.error_generic))
                                }
                            )
                        }
                    }
                    JetpackStatusFetchResponse.NOT_FOUND -> {
                        launch {
                            userEligibilityFetcher.fetchUserInfo().fold(
                                onSuccess = { user ->
                                    val hasInstallCapability = user.roles.contains(UserRole.Administrator)
                                    if (hasInstallCapability) {
                                        triggerEvent(
                                            StartJetpackActivationForApplicationPasswords(
                                                siteUrl = selectedSite.get().url,
                                                jetpackStatus = it.first
                                            )
                                        )
                                    } else {
                                        triggerEvent(
                                            OpenJetpackEligibilityError(
                                                user.username,
                                                user.roles.first().value
                                            )
                                        )
                                    }
                                },
                                onFailure = {
                                    triggerEvent(ShowSnackbar(string.error_generic))
                                }
                            )
                        }
                    }
                }
            },
            onFailure = {
                triggerEvent(ShowSnackbar(string.error_generic))
            }
        )
    }

    fun onDismiss() = triggerEvent(Exit)

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
