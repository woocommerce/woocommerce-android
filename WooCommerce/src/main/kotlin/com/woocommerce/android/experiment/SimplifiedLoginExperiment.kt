package com.woocommerce.android.experiment

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.analytics.ExperimentTracker
import com.woocommerce.android.config.RemoteConfigRepository
import javax.inject.Inject

class SimplifiedLoginExperiment @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val experimentTracker: ExperimentTracker,
    private val remoteConfigRepository: RemoteConfigRepository,
) {
    companion object {
        private const val VARIANT_CONTROL = "control"
        private const val VARIANT_SIMPLIFIED = "simplified_login_i1"
    }

    fun run() {
        // Firebase's activation event for A/B testing. Make sure to only call it once!
        experimentTracker.log(ExperimentTracker.SIMPLIFIED_LOGIN_ELIGIBLE_EVENT)

        // Track used variant
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SIMPLIFIED_LOGIN_EXPERIMENT,
            mapOf(Pair(AnalyticsTracker.KEY_EXPERIMENT_VARIANT, getCurrentVariant().name))
        )
    }

    fun getCurrentVariant(): LoginVariant {
        return when (remoteConfigRepository.getSimplifiedLoginVariant()) {
            VARIANT_CONTROL -> LoginVariant.STANDARD
            VARIANT_SIMPLIFIED -> LoginVariant.SIMPLIFIED
            else -> LoginVariant.STANDARD
        }
    }

    enum class LoginVariant {
        STANDARD,
        SIMPLIFIED
    }
}
