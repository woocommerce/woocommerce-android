package com.woocommerce.android.experiment

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.analytics.ExperimentTracker
import com.woocommerce.android.config.RemoteConfigRepository
import com.woocommerce.android.experiment.PrologueExperiment.PrologueVariant.CONTROL
import com.woocommerce.android.experiment.PrologueExperiment.PrologueVariant.SURVEY
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class PrologueExperiment @Inject constructor(
    private val experimentTracker: ExperimentTracker,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    suspend fun run(showOriginalPrologue: () -> Unit, showPrologueSurvey: () -> Unit) {
        experimentTracker.log(ExperimentTracker.PROLOGUE_EXPERIMENT_ELIGIBLE_EVENT)

        val prologueVariant = remoteConfigRepository.observePrologueVariant().first()

        // track the variant used
        analyticsTrackerWrapper.track(
            AnalyticsEvent.PROLOGUE_EXPERIMENT,
            mapOf(Pair(AnalyticsTracker.KEY_EXPERIMENT_VARIANT, prologueVariant.name))
        )

        when (prologueVariant) {
            CONTROL -> showOriginalPrologue()
            SURVEY -> showPrologueSurvey()
        }
    }

    enum class PrologueVariant {
        CONTROL,
        SURVEY
    }
}
