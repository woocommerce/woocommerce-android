package com.woocommerce.android.experiment

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.analytics.ExperimentTracker
import com.woocommerce.android.config.RemoteConfigRepository
import javax.inject.Inject

class JetpackInstallationExperiment @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val experimentTracker: ExperimentTracker,
    private val remoteConfigRepository: RemoteConfigRepository,
) {

    fun activate() {
        // Track Firebase's activation event for the A/B testing.
        experimentTracker.log(ExperimentTracker.JETPACK_INSTALLATION_ELIGIBLE_EVENT)

        // Track used variant
        val variant = getCurrentVariant()
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SIMPLIFIED_LOGIN_EXPERIMENT,
            mapOf(Pair(AnalyticsTracker.KEY_EXPERIMENT_VARIANT, variant.name))
        )
    }

    fun getCurrentVariant() = remoteConfigRepository.getJetpackInstallationVariant()

    enum class JetpackInstallationVariant {
        CONTROL,
        NATIVE
    }
}
