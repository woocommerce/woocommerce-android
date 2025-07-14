package com.woocommerce.android.ui.onboarding.payments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flowOf
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import javax.inject.Inject

@HiltViewModel
class GetPaidViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    selectedSite: SelectedSite,
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val SUCCESS_FLAG = "wcpay-connection-success"
        private const val CANCELLATION_FLAG = "wcpay-connection-error"
    }

    private val args: GetPaidFragmentArgs by savedStateHandle.navArgs()
    private val isWooPaymentsSetup = args.taskId == OnboardingTaskType.WC_PAYMENTS.id
    private var isDismissed = false

    private val setupUrl = selectedSite.get().url.slashJoin("/wp-admin/admin.php?page=wc-admin&task=${args.taskId}")

    val viewState = flowOf(
        ViewState(
            url = setupUrl,
            shouldAuthenticate = selectedSite.get().isWPComAtomic
        )
    ).asLiveData()

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onUrlLoaded(url: String) {
        if (isDismissed || !isWooPaymentsSetup) return

        when {
            url.contains(SUCCESS_FLAG) -> {
                WooLog.d(WooLog.T.ONBOARDING, "WooPayments setup completed successfully")
                analyticsTracker.track(
                    stat = AnalyticsEvent.STORE_ONBOARDING_TASK_COMPLETED,
                    properties = mapOf(AnalyticsTracker.ONBOARDING_TASK_KEY to AnalyticsTracker.VALUE_WOO_PAYMENTS)
                )
                triggerEvent(ShowWooPaymentsSetupSuccess)
                isDismissed = true
            }

            url.contains(CANCELLATION_FLAG) -> {
                WooLog.d(WooLog.T.ONBOARDING, "WooPayments setup dismissed")
                triggerEvent(MultiLiveEvent.Event.Exit)
                isDismissed = true
            }
        }
    }

    data class ViewState(
        val url: String,
        val shouldAuthenticate: Boolean
    )

    object ShowWooPaymentsSetupSuccess : MultiLiveEvent.Event()
}
