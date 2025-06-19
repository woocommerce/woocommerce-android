package org.wordpress.android.fluxc.store

import kotlinx.coroutines.runBlocking
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.data.WCCountryMapper
import org.wordpress.android.fluxc.model.data.WCLocationModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.data.WCDataRestClient
import org.wordpress.android.fluxc.persistence.dao.LocationsDao
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCDataStore @Inject internal constructor(
    private val restClient: WCDataRestClient,
    private val coroutineEngine: CoroutineEngine,
    private val mapper: WCCountryMapper,
    private val locationsDao: LocationsDao,
) {
    /**
     * Returns a list of countries
     */
    fun getCountries(): List<WCLocationModel> =
            runBlocking { locationsDao.getCountries() }

    /**
     * Returns a list of states
     */
    fun getStates(country: String): List<WCLocationModel> = if (country.isEmpty()) {
        emptyList()
    } else {
        runBlocking { locationsDao.getStates(country) }
    }

    suspend fun fetchCountriesAndStates(site: SiteModel): WooResult<List<WCLocationModel>> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchCountries") {
            val response = restClient.fetchCountries(site)
            return@withDefaultContext when {
                response.isError -> {
                    WooResult(response.error)
                }
                response.result != null -> {
                    val locationModels = response.result.flatMap {
                        mapper.map(it)
                    }

                    locationsDao.upsertLocations(locationModels)
                    WooResult(locationModels)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }
}
