package org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes

import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.terms.AttributeTermApiResponse
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductAttributeRestClient @Inject constructor(private val wooNetwork: WooNetwork) {
    suspend fun fetchProductFullAttributesList(
        site: SiteModel
    ) = WOOCOMMERCE.products.attributes.pathV3
            .request<Array<AttributeApiResponse>>(site)

    suspend fun fetchSingleAttribute(
        site: SiteModel,
        attributeID: Long
    ) = WOOCOMMERCE.products.attributes.attribute(attributeID).pathV3
            .request<AttributeApiResponse>(site)

    suspend fun postNewAttribute(
        site: SiteModel,
        args: Map<String, String>
    ) = WOOCOMMERCE.products.attributes.pathV3
            .post<AttributeApiResponse>(site, args)

    suspend fun updateExistingAttribute(
        site: SiteModel,
        attributeID: Long,
        args: Map<String, String>
    ) = WOOCOMMERCE.products.attributes.attribute(attributeID).pathV3
            .put<AttributeApiResponse>(site, args)

    suspend fun deleteExistingAttribute(
        site: SiteModel,
        attributeID: Long
    ) = WOOCOMMERCE.products.attributes.attribute(attributeID).pathV3
            .delete<AttributeApiResponse>(site)

    suspend fun fetchAllAttributeTerms(
        site: SiteModel,
        attributeID: Long,
        page: Int,
        pageSize: Int
    ) = WOOCOMMERCE.products.attributes.attribute(attributeID).terms.pathV3
            .request<Array<AttributeTermApiResponse>>(
                site = site,
                params = mapOf(
                    "page" to page.toString(),
                    "per_page" to pageSize.toString()
                )
            )

    suspend fun postNewTerm(
        site: SiteModel,
        attributeID: Long,
        args: Map<String, String>
    ) = WOOCOMMERCE.products.attributes.attribute(attributeID).terms.pathV3
            .post<AttributeTermApiResponse>(site, args)

    suspend fun updateExistingTerm(
        site: SiteModel,
        args: Map<String, String>,
        attributeID: Long,
        termID: Long
    ) = WOOCOMMERCE.products.attributes.attribute(attributeID).terms.term(termID).pathV3
            .put<AttributeTermApiResponse>(site, args)

    suspend fun deleteExistingTerm(
        site: SiteModel,
        attributeID: Long,
        termID: Long
    ) = WOOCOMMERCE.products.attributes.attribute(attributeID).terms.term(termID).pathV3
            .delete<AttributeTermApiResponse>(site)

    private suspend inline fun <reified T : Any> String.request(
        site: SiteModel,
        params: Map<String, String> = emptyMap()
    ) = wooNetwork.executeGetGsonRequest(
        site = site,
        path = this,
        clazz = T::class.java,
        params = params
    ).toWooPayload()

    private suspend inline fun <reified T : Any> String.post(
        site: SiteModel,
        args: Map<String, String>
    ) = wooNetwork.executeGetGsonRequest(
        site = site,
        path = this,
        clazz = T::class.java,
        params = args
    ).toWooPayload()

    private suspend inline fun <reified T : Any> String.put(
        site: SiteModel,
        args: Map<String, String>
    ) = wooNetwork.executePostGsonRequest(
        site = site,
        path = this,
        clazz = T::class.java,
        body = args
    ).toWooPayload()

    private suspend inline fun <reified T : Any> String.delete(
        site: SiteModel
    ) = wooNetwork.executeDeleteGsonRequest(
        site = site,
        path = this,
        clazz = T::class.java
    ).toWooPayload()
}
