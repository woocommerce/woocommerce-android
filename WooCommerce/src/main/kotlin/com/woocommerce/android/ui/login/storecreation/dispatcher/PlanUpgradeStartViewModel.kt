package com.woocommerce.android.ui.login.storecreation.dispatcher

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_SOURCE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_BANNER
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_NOTIFICATION
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_UPGRADES_SCREEN
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewViewModel
import com.woocommerce.android.ui.login.storecreation.dispatcher.PlanUpgradeStartFragment.PlanUpgradeStartSource.BANNER
import com.woocommerce.android.ui.login.storecreation.dispatcher.PlanUpgradeStartFragment.PlanUpgradeStartSource.NOTIFICATION
import com.woocommerce.android.ui.login.storecreation.dispatcher.PlanUpgradeStartFragment.PlanUpgradeStartSource.UPGRADES_SCREEN
import com.woocommerce.android.ui.plans.domain.SitePlan
import com.woocommerce.android.ui.plans.repository.SitePlanRepository
import com.woocommerce.android.ui.sitepicker.SitePickerRepository
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.UserAgent
import javax.inject.Inject

@HiltViewModel
class PlanUpgradeStartViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val wpComWebViewAuthenticator: WPComWebViewAuthenticator,
    val userAgent: UserAgent,
    private val selectedSite: SelectedSite,
    private val tracks: AnalyticsTrackerWrapper,
    private val sitePlanRepository: SitePlanRepository,
    private val sitePickerRepository: SitePickerRepository
) : ScopedViewModel(savedStateHandle) {

    private val navArgs: PlanUpgradeStartFragmentArgs by savedStateHandle.navArgs()
    private val tracksProperties =
        mapOf(
            KEY_SOURCE to when (navArgs.source) {
                BANNER -> VALUE_BANNER
                UPGRADES_SCREEN -> VALUE_UPGRADES_SCREEN
                NOTIFICATION -> VALUE_NOTIFICATION
            }
        )

    private companion object {
        private const val URL_TO_TRIGGER_EXIT = "my-plan/trial-upgraded"
    }

    init {
        launch {
            val currentPlan = sitePlanRepository.fetchCurrentPlanDetails(selectedSite.get())

            if (currentPlan?.type != SitePlan.Type.FREE_TRIAL) {
                triggerEvent(MultiLiveEvent.Event.Exit)
            }
        }
    }

    val viewState =
        WPComWebViewViewModel.ViewState(
            urlToLoad = "https://wordpress.com/plans/${selectedSite.get().siteId}",
            title = null,
            displayMode = WPComWebViewViewModel.DisplayMode.MODAL,
            captureBackButton = true
        )

    fun onUrlLoaded(url: String) {
        if (url.contains(URL_TO_TRIGGER_EXIT, ignoreCase = true)) {
            tracks.track(AnalyticsEvent.PLAN_UPGRADE_SUCCESS, tracksProperties)
            launch {
                sitePickerRepository.fetchWooCommerceSite(selectedSite.get())
                    .fold(
                        onFailure = {
                            WooLog.d(
                                T.WOO_TRIAL,
                                "Failed to refresh site data after upgrading from trial to eCommerce plan"
                            )
                        },
                        onSuccess = { selectedSite.set(it) }
                    )
                triggerEvent(MultiLiveEvent.Event.ExitWithResult(Unit))
            }
        }
    }

    fun onClose() {
        tracks.track(AnalyticsEvent.PLAN_UPGRADE_ABANDONED, tracksProperties)
        triggerEvent(MultiLiveEvent.Event.Exit)
    }
}
