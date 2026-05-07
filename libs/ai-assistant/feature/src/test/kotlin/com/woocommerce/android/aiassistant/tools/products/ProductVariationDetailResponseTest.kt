package com.woocommerce.android.aiassistant.tools.products

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCProductVariationModel

class ProductVariationDetailResponseTest {
    @Test
    fun `given variation has attributes, when detail response is built, then attributes are compact`() {
        val variation = WCProductVariationModel(
            remoteProductId = RemoteId(100L),
            remoteVariationId = RemoteId(10L),
            sku = "SKU-10",
            price = "19.99",
            stockStatus = "instock",
            attributes = """[{"id":1,"name":"Size","option":"M"},{"id":2,"name":"Color","option":"Red"}]""",
        )

        val response = variation.toProductVariationDetailResponse()

        assertThat(response.attributes.map { it.name }).containsExactly("Size", "Color")
        assertThat(response.attributes.map { it.option }).containsExactly("M", "Red")
    }

    @Test
    fun `given long variation description is requested, when detail response is built, then it is capped`() {
        val longDescription = "x".repeat(PRODUCT_TEXT_FIELD_LIMIT + 1)
        val variation = WCProductVariationModel(
            remoteProductId = RemoteId(100L),
            remoteVariationId = RemoteId(10L),
            description = longDescription,
        )

        val response = variation.toProductVariationDetailResponse(extraFields = setOf("description"))

        assertThat(response.description).hasSize(PRODUCT_TEXT_FIELD_LIMIT)
        assertThat(response.description).isEqualTo(longDescription.take(PRODUCT_TEXT_FIELD_LIMIT))
        assertThat(response.descriptionTruncated).isTrue
    }
}
