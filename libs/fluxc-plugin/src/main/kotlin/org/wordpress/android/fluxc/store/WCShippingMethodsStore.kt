package org.wordpress.android.fluxc.store

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCShippingMethod
import org.wordpress.android.fluxc.model.toAppModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_FOUND
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.EMPTY_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippingsmethods.ShippingMethodsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippingsmethods.toAppModel
import org.wordpress.android.fluxc.persistence.dao.ShippingMethodDao
import org.wordpress.android.fluxc.persistence.entity.toEntity
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCShippingMethodsStore @Inject constructor(
    private val restClient: ShippingMethodsRestClient,
    private val shippingMethodDao: ShippingMethodDao,
    private val coroutineEngine: CoroutineEngine
) {
    fun observeShippingMethods(site: SiteModel): Flow<List<WCShippingMethod>> {
        return shippingMethodDao.observeShippingMethods(site.localId()).map { list ->
            list.map { it.toAppModel() }
        }
    }

    suspend fun fetchShippingMethods(site: SiteModel): WooResult<List<WCShippingMethod>> {
        return coroutineEngine.withDefaultContext(
            AppLog.T.API,
            this, "fetchShippingMethods"
        ) {
            val response = restClient.fetchShippingMethods(site)
            return@withDefaultContext when {
                response.isError -> {
                    WooResult(response.error)
                }
                response.result != null -> {
                    val shippingMethods = response.result.map { it.toAppModel() }
                    WooResult(shippingMethods)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun updateShippingMethods(site: SiteModel, shippingMethods:List<WCShippingMethod>){
        val shippingMethodsEntity = shippingMethods
            .filter { it.id.isNotEmpty()}
            .map { it.toEntity(site.localId()) }
        shippingMethodDao.updateShippingMethods(shippingMethodsEntity)
    }

    suspend fun getShippingMethodById(site: SiteModel, id: String): WCShippingMethod? {
        return shippingMethodDao.getShippingMethodById(site.localId(), id)?.toAppModel()
    }

    suspend fun updateShippingMethod(site: SiteModel, shippingMethod: WCShippingMethod){
        if(shippingMethod.id.isEmpty()) return
        shippingMethodDao.insertShippingMethods(listOf(shippingMethod.toEntity(site.localId())))
    }

    suspend fun fetchShippingMethod(site: SiteModel, id: String): WooResult<WCShippingMethod>{
        return coroutineEngine.withDefaultContext(
            AppLog.T.API,
            this, "fetchShippingMethods"
        ) {
            val response = restClient.fetchShippingMethodsById(site, id)
            return@withDefaultContext when {
                response.isError -> {
                    WooResult(response.error)
                }
                response.result != null -> {
                    val shippingMethods = response.result.toAppModel()
                    WooResult(shippingMethods)
                }
                else -> WooResult(WooError(EMPTY_RESPONSE, NOT_FOUND))
            }
        }
    }
}
