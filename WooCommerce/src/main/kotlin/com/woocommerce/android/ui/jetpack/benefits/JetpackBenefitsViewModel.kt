package com.woocommerce.android.ui.jetpack.benefits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent.JETPACK_BENEFITS_DIALOG_WPADMIN_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.JETPACK_INSTALL_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
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
    private val fetchJetpackStatus: FetchJetpackStatus,
    private val wpComAccessToken: AccessToken
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val JETPACK_CONNECT_URL = "https://wordpress.com/jetpack/connect"
        private const val JETPACK_CONNECTED_REDIRECT_URL = "woocommerce://jetpack-connected"
    }

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
                jetpackStatusResult.fold(
                    onSuccess = {
                        triggerEvent(
                            StartJetpackActivationForApplicationPasswords(
                                siteUrl = selectedSite.get().url,
                                jetpackStatus = it
                            )
                        )
                    },
                    onFailure = { triggerEvent(ShowSnackbar(string.error_generic)) }
                )
                _viewState.update { it.copy(isLoadingDialogShown = false) }
            }

            else -> error("Non supported site type ${selectedSite.connectionType} in Jetpack Benefits screen")
        }
    }

    fun onDismiss() {
        wpComAccessToken.set(null)
        triggerEvent(Exit)
    }

    fun onOpenWpAdminJetpackActivationClicked() {
        AnalyticsTracker.track(
            stat = JETPACK_BENEFITS_DIALOG_WPADMIN_BUTTON_TAPPED,
            properties = mapOf(AnalyticsTracker.KEY_JETPACK_INSTALLATION_SOURCE to "benefits_modal")
        )

        val url = "$JETPACK_CONNECT_URL?url=${selectedSite.get().url}" +
            "&mobile_redirect=$JETPACK_CONNECTED_REDIRECT_URL&from=mobile"
        triggerEvent(OpenWpAdminJetpackActivation(url))
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
}
