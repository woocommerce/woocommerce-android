package org.wordpress.android.fluxc.model.attribute

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.attribute.terms.WCAttributeTermModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.AttributeApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.terms.AttributeTermApiResponse
import javax.inject.Inject

class WCGlobalAttributeMapper @Inject constructor() {
    fun responseToAttributeModel(
        response: AttributeApiResponse,
        site: SiteModel
    ) = response.run {
        WCGlobalAttributeModel(
                remoteId = id?.toIntOrNull() ?: 0,
                localSiteId = site.id,
                name = name.orEmpty(),
                slug = slug.orEmpty(),
                type = type.orEmpty(),
                orderBy = orderBy.orEmpty(),
                hasArchives = hasArchives ?: false
        )
    }

    fun responseToAttributeModelList(
        response: Array<AttributeApiResponse>,
        site: SiteModel
    ) = response.map { responseToAttributeModel(it, site) }
            .toList()

    fun responseToAttributeTermModel(
        response: AttributeTermApiResponse,
        attributeID: Int,
        site: SiteModel
    ) = with(response) {
        WCAttributeTermModel(
                remoteId = id?.toIntOrNull() ?: 0,
                localSiteId = site.id,
                attributeId = attributeID,
                name = name.orEmpty(),
                slug = slug.orEmpty(),
                description = description.orEmpty(),
                count = count?.toIntOrNull() ?: 0,
                menuOrder = menuOrder?.toIntOrNull() ?: 0
        )
    }
}
