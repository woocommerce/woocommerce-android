package com.woocommerce.android.ui.login.jetpack.dispatcher

import androidx.lifecycle.SavedStateHandle
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
        when (selectedSite.connectionType) {
            SiteConnectionType.ApplicationPasswords -> {
                // Handle Jetpack activation for "Application Passwords" users
                TODO()
            }

            else -> {
                // Handle connecting a new site
                triggerEvent(
                    StartJetpackActivationForNewSite(
                        args.siteUrl,
                        args.jetpackStatus.isJetpackInstalled
                    )
                )
            }
        }
    }

    data class StartJetpackActivationForNewSite(
        val siteUrl: String,
        val isJetpackInstalled: Boolean
    ) : MultiLiveEvent.Event()
}
