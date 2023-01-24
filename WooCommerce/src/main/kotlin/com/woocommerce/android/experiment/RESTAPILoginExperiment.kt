package com.woocommerce.android.experiment

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.analytics.ExperimentTracker
import com.woocommerce.android.config.RemoteConfigFetchStatus
import com.woocommerce.android.config.RemoteConfigRepository
import com.woocommerce.android.util.PackageUtils
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

class RESTAPILoginExperiment @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val experimentTracker: ExperimentTracker,
    private val remoteConfigRepository: RemoteConfigRepository,
) {
    companion object {
        private const val AB_TEST_MAX_WAIT_TIME = 5000L
    }

    suspend fun activate() {
        // Wait until fetch is done
        val fetchStatus = withTimeoutOrNull(AB_TEST_MAX_WAIT_TIME) {
            remoteConfigRepository.fetchStatus.filter { it != RemoteConfigFetchStatus.Pending }.first()
        }
        // Activate the AB test only if the fetch was successful
        if (fetchStatus != RemoteConfigFetchStatus.Success) return

        // Track Firebase's activation event for the A/B testing.
        experimentTracker.log(ExperimentTracker.REST_API_ELIGIBLE_EVENT)

        // Track used variant
        val variant = getCurrentVariant()
        analyticsTrackerWrapper.track(
            AnalyticsEvent.REST_API_LOGIN_EXPERIMENT,
            mapOf(Pair(AnalyticsTracker.KEY_EXPERIMENT_VARIANT, variant.name))
        )
    }

    fun trackSuccess() {
        experimentTracker.log(ExperimentTracker.MYSTORE_DISPLAYED_EVENT)
    }

    fun getCurrentVariant(): RESTAPILoginVariant = if (PackageUtils.isTesting()) {
        RESTAPILoginVariant.CONTROL
    } else {
        remoteConfigRepository.getRestAPILoginVariant()
    }

    enum class RESTAPILoginVariant {
        CONTROL,
        TREATMENT
    }
}
