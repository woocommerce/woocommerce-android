package org.wordpress.android.fluxc.wc.attributes

import org.wordpress.android.fluxc.JsonLoaderUtils.jsonFileAs
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.attribute.WCGlobalAttributeModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.AttributeApiResponse

object WCProductAttributesTestFixtures {
    val stubSite = SiteModel().apply { id = 321 }

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
                        1,
                        321,
                        "Color",
                        "pa_color",
                        "select",
                        "menu_order",
                        true
                ),
                WCGlobalAttributeModel(
                        2,
                        321,
                        "Size",
                        "pa_size",
                        "select",
                        "menu_order",
                        false
                )
        )
    }
}
