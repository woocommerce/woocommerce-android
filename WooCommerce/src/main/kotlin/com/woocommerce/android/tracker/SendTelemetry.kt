package com.woocommerce.android.tracker

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.network.rest.wpcom.wc.tracker.TrackerStore
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class SendTelemetry @Inject constructor(
    private val trackerStore: TrackerStore,
    private val trackerRepository: TrackerRepository,
    private val currentTimeProvider: CurrentTimeProvider,
    private val selectedSite: SelectedSite,
    private val appsPrefsWrapper: AppPrefsWrapper
) {
    operator fun invoke(appVersion: String): Flow<Result> {
        return combine(
            selectedSite.observe(),
            ticker
        ) { siteModel, _ ->
            if (siteModel != null) {
                trackerRepository.observeLastSendingDate(siteModel).first().let { lastUpdate ->
                    val currentTime = currentTimeProvider.currentDate().time

                    if (lastUpdate == 0L || currentTime >= lastUpdate + UPDATE_INTERVAL) {
                        val formattedInstallationDate = appsPrefsWrapper.getAppInstallationDate()
                            ?.let { ISO8601_FORMAT.format(it) }
                        WooLog.d(
                            T.UTILS,
                            "Sending Telemetry: appVersion=$appVersion, site=${siteModel.siteId}, " +
                                "installationDate=$formattedInstallationDate"
                        )
                        trackerStore.sendTelemetry(
                            appVersion,
                            siteModel,
                            formattedInstallationDate
                        )
                        trackerRepository.updateLastSendingDate(siteModel, currentTime)
                        Result.SENT
                    } else {
                        Result.NOT_SENT
                    }
                }
            } else {
                Result.NOT_SENT
            }
        }
    }

    enum class Result {
        SENT, NOT_SENT
    }

    companion object {
        const val UPDATE_INTERVAL = 24 * 60 * 60 * 1000
        private val ticker = flow {
            while (true) {
                emit(Unit)
                delay(UPDATE_INTERVAL.toLong() / 2)
            }
        }
        private val ISO8601_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT)
            .apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
    }
}
