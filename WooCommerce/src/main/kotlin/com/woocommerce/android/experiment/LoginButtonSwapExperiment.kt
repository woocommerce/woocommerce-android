package com.woocommerce.android.experiment

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.analytics.ExperimentTracker
import com.woocommerce.android.config.RemoteConfigRepository
import com.woocommerce.android.experiment.LoginButtonSwapExperiment.LoginButtonSwapVariant.CONTROL
import com.woocommerce.android.experiment.LoginButtonSwapExperiment.LoginButtonSwapVariant.SWAPPED
import com.woocommerce.android.ui.login.LoginPrologueFragment
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class LoginButtonSwapExperiment @Inject constructor(
    private val experimentTracker: ExperimentTracker,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    suspend fun run(
        openOriginalLoginScreen: () -> LoginPrologueFragment,
        openSwappedLoginScreen: () -> LoginPrologueFragment
    ): LoginPrologueFragment {
        experimentTracker.log(ExperimentTracker.LOGIN_BUTTON_SWAP_EXPERIMENT_ELIGIBLE_EVENT)

        val loginButtonSwapVariant = remoteConfigRepository.observeLoginButtonsSwapVariant().first()

        // track the variant used
        analyticsTrackerWrapper.track(
            AnalyticsEvent.LOGIN_BUTTON_SWAP_EXPERIMENT,
            mapOf(Pair(AnalyticsTracker.KEY_EXPERIMENT_VARIANT, loginButtonSwapVariant.name))
        )

        return when (loginButtonSwapVariant) {
            CONTROL -> openOriginalLoginScreen()
            SWAPPED -> openSwappedLoginScreen()
        }
    }

    enum class LoginButtonSwapVariant {
        CONTROL,
        SWAPPED
    }
}
