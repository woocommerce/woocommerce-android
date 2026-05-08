package com.woocommerce.android.aiassistant.tools.products

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCProductModel

class ProductDetailResponseTest {
    @Test
    fun `given variable product, when detail response is built, then available lower vote fields are capped`() {
        val product = WCProductModel(
            remoteId = RemoteId(42L),
            name = "Hoodie",
            type = "variable",
            permalink = "https://example.com/product/hoodie",
            parentId = 9L,
            variations = (1..25).joinToString(prefix = "[", postfix = "]"),
            categories = """[{"id":1,"name":"Clothing","slug":"clothing"}]""",
            totalSales = 99L,
        )

        val response = product.toProductDetailResponse()

        assertThat(response.parentId).isEqualTo(9L)
        assertThat(response.permalink).isEqualTo("https://example.com/product/hoodie")
        assertThat(response.variationsCount).isEqualTo(25)
        assertThat(response.variationIds).hasSize(20)
        assertThat(response.variationIdsTruncated).isTrue
        assertThat(response.categories.map { it.name }).containsExactly("Clothing")
    }

    @Test
    fun `given all detail fields, when detail response is built, then every expanded field is projected`() {
        val product = WCProductModel(
            remoteId = RemoteId(42L),
            name = "Hoodie",
            type = "simple",
            description = "Long description",
            shortDescription = "Short description",
            attributes = """[{"id":1,"name":"Size","visible":true,"variation":true,"options":["M","L"]}]""",
            images = """[{"id":7,"src":"https://example.com/hoodie.jpg","alt":"Hoodie","name":"Front"}]""",
            length = "10",
            width = "5",
            height = "2",
            weight = "1.25",
            shippingClass = "shirts",
            crossSellIds = "[11,12]",
            upsellIds = "[21]",
            relatedIds = "[31,32]",
        )

        val response = product.toProductDetailResponse()

        assertThat(response.description).isEqualTo("Long description")
        assertThat(response.descriptionTruncated).isNull()
        assertThat(response.shortDescription).isEqualTo("Short description")
        assertThat(response.shortDescriptionTruncated).isNull()
        val attribute = requireNotNull(response.attributes).single()
        assertThat(attribute.name).isEqualTo("Size")
        assertThat(attribute.options).containsExactly("M", "L")
        val image = requireNotNull(response.images).single()
        assertThat(image.id).isEqualTo(7L)
        assertThat(image.src).isEqualTo("https://example.com/hoodie.jpg")
        assertThat(response.dimensions).isEqualTo(CompactProductDimensions(length = "10", width = "5", height = "2"))
        assertThat(response.weight).isEqualTo("1.25")
        assertThat(response.shippingClass).isEqualTo("shirts")
        assertThat(response.crossSellIds).containsExactly(11L, 12L)
        assertThat(response.upsellIds).containsExactly(21L)
        assertThat(response.relatedIds).containsExactly(31L, 32L)
    }

    @Test
    fun `given short product descriptions, when list row response is built, then truncation markers are omitted`() {
        val product = WCProductModel(
            remoteId = RemoteId(42L),
            name = "Hoodie",
            description = "Long description",
            shortDescription = "Short description",
        )

        val response = product.toProductListRowResponse()

        assertThat(response.descriptionTruncated).isNull()
        assertThat(response.shortDescriptionTruncated).isNull()
    }
}
