package org.wordpress.android.fluxc.wc.attributes

import org.wordpress.android.fluxc.JsonLoaderUtils.jsonFileAs
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.attribute.WCGlobalAttributeModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.AttributeApiResponse

object WCProductAttributesTestFixtures {
    val stubSite = SiteModel().apply { id = 1 }

    val attributeCreateResponse by lazy {
        "wc/product-attribute-create.json"
            .jsonFileAs(AttributeApiResponse::class.java)
    }

    val attributesFullListResponse by lazy {
        "wc/product-attributes-all.json"
            .jsonFileAs(Array<AttributeApiResponse>::class.java)
    }

    val parsedAttributesList by lazy {
        listOf(
            WCGlobalAttributeModel(
                siteId = stubSite.localId(),
                remoteId = RemoteId(1),
                name = "Color",
                slug = "pa_color",
                type = "select",
                orderBy = "menu_order",
                hasArchives = true
            ),
            WCGlobalAttributeModel(
                siteId = stubSite.localId(),
                remoteId = RemoteId(2),
                name = "Size",
                slug = "pa_size",
                type = "select",
                orderBy = "menu_order",
                hasArchives = false
            )
        )
    }
}
