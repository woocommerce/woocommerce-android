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

    private suspend inline fun <reified T : Any> String.request(
        site: SiteModel,
        params: Map<String, String> = emptyMap()
    ) = wooNetwork.executeGetGsonRequest(
        site = site,
        path = this,
        clazz = T::class.java,
        params = params
    ).toWooPayload()
}
