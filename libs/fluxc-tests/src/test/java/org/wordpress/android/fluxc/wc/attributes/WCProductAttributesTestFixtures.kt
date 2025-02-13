package org.wordpress.android.fluxc.wc.attributes

import org.wordpress.android.fluxc.JsonLoaderUtils.jsonFileAs
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.attribute.WCGlobalAttributeModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.AttributeApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.attributes.terms.AttributeTermApiResponse

object WCProductAttributesTestFixtures {
    val stubSite = SiteModel().apply { id = 321 }

    val attributeDeleteResponse by lazy {
        "wc/product-attribute-delete.json"
                .jsonFileAs(AttributeApiResponse::class.java)
    }

    val attributeCreateResponse by lazy {
        "wc/product-attribute-create.json"
                .jsonFileAs(AttributeApiResponse::class.java)
    }

    val attributeUpdateResponse by lazy {
        "wc/product-attribute-update.json"
                .jsonFileAs(AttributeApiResponse::class.java)
    }

    val attributesFullListResponse by lazy {
        "wc/product-attributes-all.json"
                .jsonFileAs(Array<AttributeApiResponse>::class.java)
    }

    val attributeTermsFullListResponse by lazy {
        "wc/attribute-terms-full-list.json"
                .jsonFileAs(Array<AttributeTermApiResponse>::class.java)
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

    val parsedCreateAttributeResponse by lazy {
        WCGlobalAttributeModel(
                1,
                321,
                "Color",
                "pa_color",
                "select",
                "menu_order",
                true
        )
    }

    val parsedDeleteAttributeResponse by lazy {
        WCGlobalAttributeModel(
                17,
                321,
                "Size",
                "pa_size",
                "select",
                "name",
                true
        )
    }

    val parsedUpdateAttributeResponse by lazy {
        WCGlobalAttributeModel(
                99,
                321,
                "test_name",
                "pa_test",
                "test_type",
                "test",
                false
        )
    }
}
