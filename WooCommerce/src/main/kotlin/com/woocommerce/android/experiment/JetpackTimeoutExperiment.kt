package com.woocommerce.android.experiment

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.analytics.ExperimentTracker
import com.woocommerce.android.config.RemoteConfigRepository
import com.woocommerce.android.experiment.JetpackTimeoutExperiment.JetpackTimeoutPolicyVariant.CONTROL
import com.woocommerce.android.experiment.JetpackTimeoutExperiment.JetpackTimeoutPolicyVariant.NEW_TIMEOUT_POLICY
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class JetpackTimeoutExperiment @Inject constructor(
    private val experimentTracker: ExperimentTracker,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    suspend fun run(): Boolean {
        experimentTracker.log(ExperimentTracker.NEW_JETPACK_TIMEOUT_POLICY_ELIGIBLE_EVENT)

        val jetpackTimeoutPolicyVariant = remoteConfigRepository.observeJetpackTimeoutPolicyVariantVariant().first()

        // track the variant used
        analyticsTrackerWrapper.track(
            AnalyticsEvent.JETPACK_TIMEOUT_EXPERIMENT,
            mapOf(Pair(AnalyticsTracker.KEY_EXPERIMENT_VARIANT, jetpackTimeoutPolicyVariant.name))
        )

        return when (jetpackTimeoutPolicyVariant) {
            CONTROL -> false
            NEW_TIMEOUT_POLICY -> true
        }
    }

    enum class JetpackTimeoutPolicyVariant {
        CONTROL,
        NEW_TIMEOUT_POLICY
    }
}
