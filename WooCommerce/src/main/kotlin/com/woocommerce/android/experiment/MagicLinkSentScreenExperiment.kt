package com.woocommerce.android.experiment

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.analytics.ExperimentTracker
import com.woocommerce.android.config.RemoteConfigRepository
import com.woocommerce.android.experiment.MagicLinkSentScreenExperiment.MagicLinkSentScreenVariant.CONTROL
import com.woocommerce.android.experiment.MagicLinkSentScreenExperiment.MagicLinkSentScreenVariant.IMPROVED
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class MagicLinkSentScreenExperiment @Inject constructor(
    private val experimentTracker: ExperimentTracker,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    suspend fun run(
        openOriginalSentScreen: () -> Unit,
        openImprovedSentScreen: () -> Unit
    ) {
        experimentTracker.log(ExperimentTracker.MAGIC_LINK_SENT_EXPERIMENT_ELIGIBLE_EVENT)

        val sentScreenVariant = remoteConfigRepository.observeMagicLinkSentScreenVariant().first()

        // track the variant used
        analyticsTrackerWrapper.track(
            AnalyticsEvent.MAGIC_LINK_SENT_SCREEN_EXPERIMENT,
            mapOf(Pair(AnalyticsTracker.KEY_EXPERIMENT_VARIANT, sentScreenVariant.name))
        )

        when (sentScreenVariant) {
            CONTROL -> openOriginalSentScreen()
            IMPROVED -> openImprovedSentScreen()
        }
    }

    enum class MagicLinkSentScreenVariant {
        CONTROL,
        IMPROVED
    }
}
