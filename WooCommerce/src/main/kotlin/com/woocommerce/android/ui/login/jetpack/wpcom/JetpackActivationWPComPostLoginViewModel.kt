package com.woocommerce.android.ui.login.jetpack.wpcom

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.JETPACK_SETUP_LOGIN_COMPLETED
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
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
    private val jetpackActivationRepository: JetpackActivationRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {
    @Suppress("ReturnCount")
    protected suspend fun onLoginSuccess(jetpackStatus: JetpackStatus): Result<Unit> {
        analyticsTrackerWrapper.track(JETPACK_SETUP_LOGIN_COMPLETED)

        val siteUrl = selectedSite.get().url
        if (jetpackStatus.isJetpackConnected) {
            // Attempt returning the site from the DB if it exists, otherwise fetch it from API
            val jetpackSite = jetpackActivationRepository.getJetpackSiteByUrl(siteUrl)
                .takeIf { it?.hasWooCommerce == true }
                ?: jetpackActivationRepository.fetchJetpackSite(siteUrl)
                    .getOrElse {
                        triggerEvent(ShowSnackbar(R.string.error_generic))
                        return Result.failure(it)
                    }

            jetpackActivationRepository.setSelectedSiteAndCleanOldSites(jetpackSite)
            if (jetpackStatus.isJetpackInstalled) {
                triggerEvent(GoToStore)
            } else {
                triggerEvent(ShowJetpackCPInstallationScreen)
            }
            return Result.success(Unit)
        } else {
            triggerEvent(
                ShowJetpackActivationScreen(
                    isJetpackInstalled = jetpackStatus.isJetpackInstalled,
                    siteUrl = siteUrl
                )
            )
            return Result.success(Unit)
        }
    }

    data class ShowJetpackActivationScreen(
        val isJetpackInstalled: Boolean,
        val siteUrl: String
    ) : MultiLiveEvent.Event()

    object ShowJetpackCPInstallationScreen : MultiLiveEvent.Event()
}
