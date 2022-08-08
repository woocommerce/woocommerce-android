package com.woocommerce.android.experiment

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.analytics.ExperimentTracker
import com.woocommerce.android.config.RemoteConfigRepository
import com.woocommerce.android.experiment.MagicLinkRequestExperiment.MagicLinkRequestVariant.AUTOMATIC
import com.woocommerce.android.experiment.MagicLinkRequestExperiment.MagicLinkRequestVariant.CONTROL
import com.woocommerce.android.experiment.MagicLinkRequestExperiment.MagicLinkRequestVariant.ENHANCED
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class MagicLinkRequestExperiment @Inject constructor(
    private val experimentTracker: ExperimentTracker,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    suspend fun run(
        openOriginalLoginScreen: () -> Unit,
        openAutomaticMagicLinkScreen: () -> Unit,
        openEnhancedMagicLinkScreen: () -> Unit
    ) {
        experimentTracker.log(ExperimentTracker.MAGIC_LINK_EXPERIMENT_ELIGIBLE_EVENT)

        val magicLinkRequestVariant = remoteConfigRepository.observeMagicLinkRequestVariant().first()

        // track the variant used
        analyticsTrackerWrapper.track(
            AnalyticsEvent.MAGIC_LINK_REQUEST_EXPERIMENT,
            mapOf(Pair(AnalyticsTracker.KEY_EXPERIMENT_VARIANT, magicLinkRequestVariant.name))
        )

        when (magicLinkRequestVariant) {
            CONTROL -> openOriginalLoginScreen()
            AUTOMATIC -> openAutomaticMagicLinkScreen()
            ENHANCED -> openEnhancedMagicLinkScreen()
        }
    }

    enum class MagicLinkRequestVariant {
        CONTROL,
        ENHANCED,
        AUTOMATIC
    }
}
