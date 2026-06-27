package org.wordpress.android.fluxc.store

import kotlinx.coroutines.runBlocking
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpapi.settings.WPSettingsRestClient
import org.wordpress.android.fluxc.persistence.dao.WPSiteSettingsDao
import org.wordpress.android.fluxc.persistence.entity.WPSiteSettingsModel
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WPSettingsStore @Inject constructor(
    private val restClient: WPSettingsRestClient,
    private val wpSiteSettingsDao: WPSiteSettingsDao
) {
    suspend fun fetchSiteSettings(site: SiteModel): FetchWPSiteSettingsPayload {
        return when (val response = restClient.fetchSiteSettings(site)) {
            is WPAPIResponse.Success -> {
                val settings = WPSiteSettingsModel(
                    localSiteId = site.localId(),
                    startOfWeek = response.data?.startOfWeek?.takeIf { it in VALID_START_OF_WEEK_RANGE },
                    updatedAt = System.currentTimeMillis()
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

    fun getStartOfWeek(site: SiteModel): Int? {
        return getSiteSettings(site)?.startOfWeek?.takeIf { it in VALID_START_OF_WEEK_RANGE }
    }

    class FetchWPSiteSettingsPayload(
        val model: WPSiteSettingsModel?
    ) : Payload<BaseNetworkError?>() {
        constructor(error: BaseNetworkError) : this(null) {
            this.error = error
        }
    }

    private companion object {
        val VALID_START_OF_WEEK_RANGE = 0..6
    }
}
