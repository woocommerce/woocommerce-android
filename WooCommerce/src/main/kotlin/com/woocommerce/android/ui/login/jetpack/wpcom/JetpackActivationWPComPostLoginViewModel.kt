package com.woocommerce.android.ui.login.jetpack.wpcom

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel

open class JetpackActivationWPComPostLoginViewModel(
    savedStateHandle: SavedStateHandle,
    private val selectedSite: SelectedSite
) : ScopedViewModel(savedStateHandle) {
    protected suspend fun onLoginSuccess(jetpackStatus: JetpackStatus) {
        if (jetpackStatus.isJetpackConnected) {
            // TODO fetch sites then show main activity
        } else {
            triggerEvent(
                ShowJetpackActivationScreen(
                    isJetpackInstalled = jetpackStatus.isJetpackInstalled,
                    siteUrl = selectedSite.get().url
                )
            )
        }
    }

    data class ShowJetpackActivationScreen(
        val isJetpackInstalled: Boolean,
        val siteUrl: String
    ) : MultiLiveEvent.Event()
}
