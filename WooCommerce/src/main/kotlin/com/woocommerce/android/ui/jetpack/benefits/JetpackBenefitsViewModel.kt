package com.woocommerce.android.ui.jetpack.benefits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent.JETPACK_INSTALL_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType.ApplicationPasswords
import com.woocommerce.android.tools.SiteConnectionType.JetpackConnectionPackage
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JetpackBenefitsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val fetchJetpackStatus: FetchJetpackStatus
) : ScopedViewModel(savedStateHandle) {
    private val _isLoadingDialogShown = MutableStateFlow(false)
    val isLoadingDialogShown = _isLoadingDialogShown.asLiveData()

    fun onInstallClick() = launch {
        AnalyticsTracker.track(
            stat = JETPACK_INSTALL_BUTTON_TAPPED,
            properties = mapOf(AnalyticsTracker.KEY_JETPACK_INSTALLATION_SOURCE to "benefits_modal")
        )

        when (selectedSite.connectionType) {
            JetpackConnectionPackage -> triggerEvent(StartJetpackCPInstallation)
            ApplicationPasswords -> {
                _isLoadingDialogShown.value = true
                val jetpackStatusResult = fetchJetpackStatus()
                jetpackStatusResult.fold(
                    onSuccess = { triggerEvent(StartApplicationPasswordsInstallation(it)) },
                    onFailure = { triggerEvent(ShowSnackbar(string.error_generic)) }
                )
                _isLoadingDialogShown.value = false
            }

            else -> error("Non supported site type ${selectedSite.connectionType} in Jetpack Benefits screen")
        }
    }

    fun onDismiss() = triggerEvent(Exit)

    object StartJetpackCPInstallation : Event()
    data class StartApplicationPasswordsInstallation(
        val jetpackStatus: JetpackStatus
    ) : Event()
}
