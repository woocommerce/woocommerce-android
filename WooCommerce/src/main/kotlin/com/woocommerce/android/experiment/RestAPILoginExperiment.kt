package com.woocommerce.android.experiment

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.analytics.ExperimentTracker
import com.woocommerce.android.config.RemoteConfigRepository
import com.woocommerce.android.util.PackageUtils
import javax.inject.Inject

class RestAPILoginExperiment @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val experimentTracker: ExperimentTracker,
    private val remoteConfigRepository: RemoteConfigRepository,
) {

    fun activate() {
        // Track Firebase's activation event for the A/B testing.
        experimentTracker.log(ExperimentTracker.REST_API_ELIGIBLE_EVENT)

        // Track used variant
        val variant = getCurrentVariant()
        analyticsTrackerWrapper.track(
            AnalyticsEvent.REST_API_LOGIN_EXPERIMENT,
            mapOf(Pair(AnalyticsTracker.KEY_EXPERIMENT_VARIANT, variant.name))
        )
    }

    fun getCurrentVariant(): RestAPILoginVariant = if (PackageUtils.isTesting()) {
        RestAPILoginVariant.CONTROL
    } else {
        remoteConfigRepository.getRestAPILoginVariant()
    }

    enum class RestAPILoginVariant {
        CONTROL,
        TREATMENT
    }
}
