package com.woocommerce.android.experiment

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.analytics.ExperimentTracker
import com.woocommerce.android.config.RemoteConfigRepository
import com.woocommerce.android.experiment.AutomaticMagicLinkRequestExperiment.AutomaticMagicLinkRequestVariant.AUTOMATIC
import com.woocommerce.android.experiment.AutomaticMagicLinkRequestExperiment.AutomaticMagicLinkRequestVariant.CONTROL
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AutomaticMagicLinkRequestExperiment @Inject constructor(
    private val experimentTracker: ExperimentTracker,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    suspend fun run(
        openOriginalLoginScreen: () -> Unit,
        openAutomaticMagicLinkScreen: () -> Unit
    ) {
        experimentTracker.log(ExperimentTracker.AUTOMATIC_MAGIC_LINK_EXPERIMENT_ELIGIBLE_EVENT)

        val magicLinkRequestVariant = remoteConfigRepository.observeAutomaticMagicLinkRequestVariant().first()

        // track the variant used
        analyticsTrackerWrapper.track(
            AnalyticsEvent.AUTOMATIC_MAGIC_LINK_REQUEST_EXPERIMENT,
            mapOf(Pair(AnalyticsTracker.KEY_EXPERIMENT_VARIANT, magicLinkRequestVariant))
        )

        when (magicLinkRequestVariant) {
            CONTROL -> openOriginalLoginScreen()
            AUTOMATIC -> openAutomaticMagicLinkScreen()
        }
    }

    enum class AutomaticMagicLinkRequestVariant {
        CONTROL,
        AUTOMATIC
    }
}
