package com.woocommerce.android.tracker

import kotlinx.coroutines.flow.first
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.tracker.TrackerStore
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import javax.inject.Inject

class SendTelemetry @Inject constructor(
    private val trackerStore: TrackerStore,
    private val trackerRepository: TrackerRepository,
    private val currentTimeProvider: CurrentTimeProvider,
) {
    suspend operator fun invoke(appVersion: String, siteModel: SiteModel) {
        trackerRepository.observeLastSendingDate(siteModel).first().let { lastUpdate ->
            val currentTime = currentTimeProvider.currentDate().time

            if (currentTime > lastUpdate + UPDATE_INTERVAL) {
                trackerStore.sendTelemetry(appVersion, siteModel)
                trackerRepository.updateLastSendingDate(siteModel, currentTime)
            }
        }
    }

    companion object {
        const val UPDATE_INTERVAL = 24 * 60 * 60 * 1000
    }
}
