package com.woocommerce.android.ui.login.jetpack.wpcom

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.jetpack.GoToStore
import com.woocommerce.android.ui.login.jetpack.JetpackActivationRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel

open class JetpackActivationWPComPostLoginViewModel(
    savedStateHandle: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val jetpackActivationRepository: JetpackActivationRepository
) : ScopedViewModel(savedStateHandle) {
    protected suspend fun onLoginSuccess(jetpackStatus: JetpackStatus) {
        if (jetpackStatus.isJetpackConnected) {
            jetpackActivationRepository.fetchJetpackSite(selectedSite.get().url)
                .fold(
                    onSuccess = {
                        jetpackActivationRepository.setSelectedSiteAndCleanOldSites(it)
                        triggerEvent(GoToStore)
                    },
                    onFailure = {
                        triggerEvent(ShowSnackbar(R.string.error_generic))
                    }
                )
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
