package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.attribute.WCGlobalAttributeMapper
import org.wordpress.android.fluxc.model.attribute.WCGlobalAttributeModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.ProductAttributeRestClient
import org.wordpress.android.fluxc.persistence.WCGlobalAttributeSqlUtils.deleteSingleStoredAttribute
import org.wordpress.android.fluxc.persistence.WCGlobalAttributeSqlUtils.fetchSingleStoredAttribute
import org.wordpress.android.fluxc.persistence.WCGlobalAttributeSqlUtils.getCurrentAttributes
import org.wordpress.android.fluxc.persistence.WCGlobalAttributeSqlUtils.insertAttributeTermsFromScratch
import org.wordpress.android.fluxc.persistence.WCGlobalAttributeSqlUtils.insertFromScratchCompleteAttributesList
import org.wordpress.android.fluxc.persistence.WCGlobalAttributeSqlUtils.insertOrUpdateSingleAttribute
import org.wordpress.android.fluxc.persistence.WCGlobalAttributeSqlUtils.insertSingleAttribute
import org.wordpress.android.fluxc.persistence.WCGlobalAttributeSqlUtils.updateSingleAttributeTermsMapping
import org.wordpress.android.fluxc.persistence.WCGlobalAttributeSqlUtils.updateSingleStoredAttribute
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCGlobalAttributeStore @Inject constructor(
    private val restClient: ProductAttributeRestClient,
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
                        ?.let {
                            insertFromScratchCompleteAttributesList(it, site.id)
                            getCurrentAttributes(site.id)
                        }
                        ?.let { WooResult(it) }
                        ?: getCurrentAttributes(site.id)
                                .takeIf { it.isNotEmpty() }
                                ?.let { WooResult(it) }
                        ?: WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }

    fun loadCachedStoreAttributes(
        site: SiteModel
    ) = WooResult(getCurrentAttributes(site.id))

    suspend fun fetchAttributeTerms(
        site: SiteModel,
        attributeID: Long,
        page: Int = DEFAULT_PAGE_INDEX,
        pageSize: Int = DEFAULT_PAGE_SIZE
    ) = restClient.fetchAllAttributeTerms(site, attributeID, page, pageSize)
            .result?.map { mapper.responseToAttributeTermModel(it, attributeID.toInt(), site) }
            ?.apply {
                insertAttributeTermsFromScratch(attributeID.toInt(), site.id, this)
                map { it.remoteId.toString() }
                        .takeIf { it.isNotEmpty() }
                        ?.reduce { total, new -> "$total;$new" }
                        ?.let { termsId ->
                            updateSingleAttributeTermsMapping(attributeID.toInt(), termsId, site.id)
                                    ?: handleMissingAttribute(site, attributeID, termsId)
                        }
            }
            ?.let { WooResult(it) }

    suspend fun fetchAttribute(
        site: SiteModel,
        attributeID: Long,
        withTerms: Boolean = false
    ): WooResult<WCGlobalAttributeModel> =
            coroutineEngine.withDefaultContext(AppLog.T.API, this, "createStoreAttributes") {
                restClient.fetchSingleAttribute(site, attributeID)
                        .asWooResult()
                        .model
                        ?.let { mapper.responseToAttributeModel(it, site) }
                        ?.let { insertOrUpdateSingleAttribute(it, site.id) }
                        ?.apply { takeIf { withTerms }?.let { fetchAttributeTerms(site, attributeID) } }
                        ?.let { fetchSingleStoredAttribute(attributeID.toInt(), site.id) }
                        ?.let { WooResult(it) }
                        ?: WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }

    suspend fun createAttribute(
        site: SiteModel,
        name: String,
        slug: String = name,
        type: String = "select",
        orderBy: String = "menu_order",
        hasArchives: Boolean = false
    ): WooResult<WCGlobalAttributeModel> =
            coroutineEngine.withDefaultContext(AppLog.T.API, this, "createStoreAttributes") {
                restClient.postNewAttribute(
                        site, mapOf(
                        "name" to name,
                        "slug" to slug,
                        "type" to type,
                        "order_by" to orderBy,
                        "has_archives" to hasArchives.toString()
                )
                )
                        .asWooResult()
                        .model
                        ?.let { mapper.responseToAttributeModel(it, site) }
                        ?.let { insertSingleAttribute(it) }
                        ?.let { WooResult(it) }
                        ?: WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }

    suspend fun updateAttribute(
        site: SiteModel,
        attributeID: Long,
        name: String,
        slug: String = name,
        type: String = "select",
        orderBy: String = "menu_order",
        hasArchives: Boolean = false
    ): WooResult<WCGlobalAttributeModel> =
            coroutineEngine.withDefaultContext(AppLog.T.API, this, "updateStoreAttributes") {
                restClient.updateExistingAttribute(
                        site, attributeID, mapOf(
                        "id" to attributeID.toString(),
                        "name" to name,
                        "slug" to slug,
                        "type" to type,
                        "order_by" to orderBy,
                        "has_archives" to hasArchives.toString()
                )
                )
                        .asWooResult()
                        .model
                        ?.let { mapper.responseToAttributeModel(it, site) }
                        ?.let { updateSingleStoredAttribute(it, site.id) }
                        ?.let { WooResult(it) }
                        ?: WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }

    suspend fun deleteAttribute(
        site: SiteModel,
        attributeID: Long
    ): WooResult<WCGlobalAttributeModel> =
            coroutineEngine.withDefaultContext(AppLog.T.API, this, "deleteStoreAttributes") {
                restClient.deleteExistingAttribute(site, attributeID)
                        .asWooResult()
                        .model
                        ?.let { mapper.responseToAttributeModel(it, site) }
                        ?.let { deleteSingleStoredAttribute(it, site.id) }
                        ?.let { WooResult(it) }
                        ?: WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }

    suspend fun createOptionValueForAttribute(
        site: SiteModel,
        attributeID: Long,
        term: String
    ): WooResult<WCGlobalAttributeModel> =
            coroutineEngine.withDefaultContext(AppLog.T.API, this, "createAttributeTerm") {
                restClient.postNewTerm(
                        site, attributeID,
                        mapOf("name" to term)
                )
                        .asWooResult()
                        .model
                        ?.let { fetchAttribute(site, attributeID) }
                        ?: WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }

    suspend fun deleteOptionValueFromAttribute(
        site: SiteModel,
        attributeID: Long,
        termID: Long
    ): WooResult<WCGlobalAttributeModel> =
            coroutineEngine.withDefaultContext(AppLog.T.API, this, "deleteAttributeTerm") {
                restClient.deleteExistingTerm(site, attributeID, termID)
                        .asWooResult()
                        .model
                        ?.let { fetchAttribute(site, attributeID) }
                        ?: WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }

    private suspend fun handleMissingAttribute(
        site: SiteModel,
        attributeID: Long,
        termsId: String
    ) {
        fetchAttribute(site, attributeID)
                .model
                ?.let { updateSingleAttributeTermsMapping(attributeID.toInt(), termsId, site.id) }
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 100
        const val DEFAULT_PAGE_INDEX = 1
    }
}
