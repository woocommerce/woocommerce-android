package com.woocommerce.android.experiment

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.analytics.ExperimentTracker
import com.woocommerce.android.config.RemoteConfigRepository
import com.woocommerce.android.experiment.SiteLoginExperiment.SiteLoginVariant.EMAIL_LOGIN
import com.woocommerce.android.experiment.SiteLoginExperiment.SiteLoginVariant.SITE_CREDENTIALS_LOGIN
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SiteLoginExperiment @Inject constructor(
    private val experimentTracker: ExperimentTracker,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    suspend fun run(
        siteAddress: String?,
        loginViaEmail: (String?) -> Unit,
        loginViaSiteCredentials: (String?) -> Unit
    ) {
        experimentTracker.log(ExperimentTracker.SITE_CREDENTIALS_EXPERIMENT_ELIGIBLE_EVENT)

        val loginVariant = remoteConfigRepository.observeSiteLoginVariant().first()

        // track the variant used
        analyticsTrackerWrapper.track(
            AnalyticsEvent.LOGIN_SITE_CREDENTIALS_EXPERIMENT,
            mapOf(Pair(AnalyticsTracker.KEY_EXPERIMENT_VARIANT, loginVariant.name))
        )

        when (loginVariant) {
            EMAIL_LOGIN -> loginViaEmail(siteAddress)
            SITE_CREDENTIALS_LOGIN -> loginViaSiteCredentials(siteAddress)
        }
    }

    enum class SiteLoginVariant {
        EMAIL_LOGIN,
        SITE_CREDENTIALS_LOGIN
    }
}
