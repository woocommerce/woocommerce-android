package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.attribute.WCGlobalAttributeMapper
import org.wordpress.android.fluxc.model.attribute.WCGlobalAttributeModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.ProductAttributeRestClient
import org.wordpress.android.fluxc.persistence.dao.GlobalAttributesDao
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCGlobalAttributeStore @Inject internal constructor(
    private val restClient: ProductAttributeRestClient,
    private val globalAttributesDao: GlobalAttributesDao,
    private val mapper: WCGlobalAttributeMapper,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchStoreAttributes(
        site: SiteModel
    ): WooResult<List<WCGlobalAttributeModel>> =
            coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchStoreAttributes") {
                restClient.fetchProductFullAttributesList(site)
                        .asWooResult()
                        .model
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { mapper.responseToAttributeModelList(it, site) }
                        ?.also { globalAttributesDao.replaceAllForSite(site.localId(), it) }
                        ?.let { globalAttributesDao.getAttributesForSite(site.localId()) }
                        ?.let { WooResult(it) }
                        ?: globalAttributesDao.getAttributesForSite(site.localId())
                                .takeIf { it.isNotEmpty() }
                                ?.let { WooResult(it) }
                        ?: WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }

    suspend fun loadCachedStoreAttributes(
        site: SiteModel
    ) = WooResult(globalAttributesDao.getAttributesForSite(site.localId()))

    suspend fun fetchAttributeTerms(
        site: SiteModel,
        attributeID: Long,
        page: Int = DEFAULT_PAGE_INDEX,
        pageSize: Int = DEFAULT_PAGE_SIZE
    ) = restClient.fetchAllAttributeTerms(site, attributeID, page, pageSize)
            .result?.map { mapper.responseToAttributeTermModel(it, attributeID.toInt(), site) }
            ?.let { WooResult(it) }

    companion object {
        const val DEFAULT_PAGE_SIZE = 100
        const val DEFAULT_PAGE_INDEX = 1
    }
}
