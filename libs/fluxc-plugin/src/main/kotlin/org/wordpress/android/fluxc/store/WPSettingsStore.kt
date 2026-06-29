package org.wordpress.android.fluxc.store

import kotlinx.coroutines.runBlocking
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.settings.WPSettingsRestClient
import org.wordpress.android.fluxc.persistence.converters.WPStartOfWeekConverter
import org.wordpress.android.fluxc.persistence.dao.WPSiteSettingsDao
import org.wordpress.android.fluxc.persistence.entity.WPSiteSettingsModel
import org.wordpress.android.util.AppLog
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WPSettingsStore @Inject constructor(
    private val restClient: WPSettingsRestClient,
    private val wpSiteSettingsDao: WPSiteSettingsDao
) {
    private val startOfWeekConverter = WPStartOfWeekConverter()

    suspend fun fetchSiteSettings(site: SiteModel): FetchWPSiteSettingsPayload {
        return when (val response = restClient.fetchSiteSettings(site)) {
            is WPAPIResponse.Success -> {
                val settings = WPSiteSettingsModel(
                    localSiteId = site.localId(),
                    startOfWeek = startOfWeekConverter.fromWPStartOfWeek(response.data?.startOfWeek)
                )
                wpSiteSettingsDao.upsertSiteSettings(settings)
                FetchWPSiteSettingsPayload(settings)
            }

            is WPAPIResponse.Error -> {
                AppLog.w(AppLog.T.SETTINGS, "Error fetching WordPress site settings: ${response.error.message}")
                FetchWPSiteSettingsPayload(response.error)
            }
        }
    }

    fun getSiteSettings(site: SiteModel): WPSiteSettingsModel? = runBlocking {
        getSiteSettingsAsync(site)
    }

    suspend fun getSiteSettingsAsync(site: SiteModel): WPSiteSettingsModel? {
        return wpSiteSettingsDao.getSiteSettings(site.localId())
    }

    fun getStartOfWeek(site: SiteModel): DayOfWeek? {
        return getSiteSettings(site)?.startOfWeek
    }

    class FetchWPSiteSettingsPayload(
        val model: WPSiteSettingsModel?
    ) : Payload<BaseNetworkError?>() {
        constructor(error: BaseNetworkError) : this(null) {
            this.error = error
        }
    }
}
