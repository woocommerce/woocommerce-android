package com.woocommerce.android.ui.login.jetpack.dispatcher

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class JetpackActivationDispatcherViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    selectedSite: SelectedSite
) : ScopedViewModel(savedStateHandle) {
    private val args: JetpackActivationDispatcherFragmentArgs by savedState.navArgs()

    init {
        val jetpackStatus = args.jetpackStatus
        when (selectedSite.connectionType) {
            SiteConnectionType.ApplicationPasswords -> {
                if (jetpackStatus.isJetpackConnected && jetpackStatus.wpComEmail != null) {
                    // Jetpack is already connected and we know the address email, handle the authentication
                    triggerEvent(
                        StartWPComAuthenticationForEmail(
                            wpComEmail = jetpackStatus.wpComEmail,
                            jetpackStatus = jetpackStatus
                        )
                    )
                } else {
                    // Start regular WordPress.com authentication
                    triggerEvent(StartWPComLoginForJetpackActivation(jetpackStatus))
                }
            }

            else -> {
                // Handle connecting a new site
                triggerEvent(
                    StartJetpackActivationForNewSite(
                        args.siteUrl,
                        jetpackStatus.isJetpackInstalled
                    )
                )
            }
        }
    }

    data class StartJetpackActivationForNewSite(
        val siteUrl: String,
        val isJetpackInstalled: Boolean
    ) : MultiLiveEvent.Event()

    data class StartWPComLoginForJetpackActivation(
        val jetpackStatus: JetpackStatus
    ) : MultiLiveEvent.Event()

    data class StartWPComAuthenticationForEmail(
        val wpComEmail: String,
        val jetpackStatus: JetpackStatus
    ) : MultiLiveEvent.Event()
}
