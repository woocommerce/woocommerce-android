package org.wordpress.android.fluxc.network.rest.wpcom.wc.product.pos

import com.google.gson.JsonParser
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.pos.WooPosVariationApiResponse

class WooPosVariationMapperTest {

    @Test
    fun `given complete API response, when mapper called, then all fields mapped correctly`() {
        // Given
        val response = WooPosVariationApiResponse(
            id = 123L,
            productId = 456L,
            name = "Blue Large T-Shirt",
            description = "Cotton T-Shirt in Blue, Size Large",
            sku = "TSHIRT-BLUE-L",
            globalUniqueId = "gid://woocommerce/ProductVariation/123",
            status = "publish",
            price = "19.99",
            regularPrice = "24.99",
            salePrice = "19.99",
            dateModified = "2024-01-15T10:30:00Z",
            stockQuantity = 25.5,
            stockStatus = "instock",
            manageStock = true,
            backordered = false,
            attributes = JsonParser.parseString("""[{"id":1,"name":"Color","option":"Blue"},{"id":2,"name":"Size","option":"Large"}]"""),
            image = WooPosVariationApiResponse.VariationImage(
                id = 789,
                src = "https://example.com/image.jpg",
                name = "product-image",
                alt = "Product Image"
            ),
            downloadable = false
        )

        val siteId = LocalOrRemoteId.LocalId(1)

        // When
        val model = response.mapToPosVariationModel(siteId)

        // Then
        assertThat(model.localSiteId).isEqualTo(siteId)
        assertThat(model.remoteProductId.value).isEqualTo(456L)
        assertThat(model.remoteVariationId.value).isEqualTo(123L)
        assertThat(model.variationName).isEqualTo("Blue Large T-Shirt")
        assertThat(model.description).isEqualTo("Cotton T-Shirt in Blue, Size Large")
        assertThat(model.sku).isEqualTo("TSHIRT-BLUE-L")
        assertThat(model.globalUniqueId).isEqualTo("gid://woocommerce/ProductVariation/123")
        assertThat(model.status).isEqualTo("publish")
        assertThat(model.price).isEqualTo("19.99")
        assertThat(model.regularPrice).isEqualTo("24.99")
        assertThat(model.salePrice).isEqualTo("19.99")
        assertThat(model.dateModified).isEqualTo("2024-01-15T10:30:00Z")
        assertThat(model.stockQuantity).isEqualTo(25.5)
        assertThat(model.stockStatus).isEqualTo("instock")
        assertThat(model.manageStock).isTrue()
        assertThat(model.backordered).isFalse()
        assertThat(model.attributesJson).contains("Color")
        assertThat(model.attributesJson).contains("Blue")
        assertThat(model.imageUrl).isEqualTo("https://example.com/image.jpg")
        assertThat(model.downloadable).isFalse()
    }

    @Test
    fun `given null stock quantity, when mapper called, then defaults to zero`() {
        // Given
        val response = WooPosVariationApiResponse(
            id = 123L,
            productId = 456L,
            stockQuantity = null
        )

        // When
        val model = response.mapToPosVariationModel(LocalOrRemoteId.LocalId(1))

        // Then
        assertThat(model.stockQuantity).isEqualTo(0.0)
    }

    @Test
    fun `given null image, when mapper called, then image URL is empty`() {
        // Given
        val response = WooPosVariationApiResponse(
            id = 123L,
            productId = 456L,
            image = null
        )

        // When
        val model = response.mapToPosVariationModel(LocalOrRemoteId.LocalId(1))

        // Then
        assertThat(model.imageUrl).isEmpty()
    }

    @Test
    fun `given empty attributes, when mapper called, then JSON is empty array`() {
        // Given
        val response = WooPosVariationApiResponse(
            id = 123L,
            productId = 456L,
            attributes = JsonParser.parseString("[]")
        )

        // When
        val model = response.mapToPosVariationModel(LocalOrRemoteId.LocalId(1))

        // Then
        assertThat(model.attributesJson).isEqualTo("[]")
    }

    @Test
    fun `given multiple attributes, when mapper called, then JSON contains all attributes`() {
        // Given
        val response = WooPosVariationApiResponse(
            id = 123L,
            productId = 456L,
            attributes = JsonParser.parseString("""[{"id":1,"name":"Color","option":"Red"},{"id":2,"name":"Size","option":"Medium"},{"id":3,"name":"Material","option":"Cotton"}]""")
        )

        // When
        val model = response.mapToPosVariationModel(LocalOrRemoteId.LocalId(1))

        // Then
        assertThat(model.attributesJson).contains("Color")
        assertThat(model.attributesJson).contains("Red")
        assertThat(model.attributesJson).contains("Size")
        assertThat(model.attributesJson).contains("Medium")
        assertThat(model.attributesJson).contains("Material")
        assertThat(model.attributesJson).contains("Cotton")
    }

    @Test
    fun `given special characters in fields, when mapper called, then characters preserved`() {
        // Given
        val response = WooPosVariationApiResponse(
            id = 123L,
            productId = 456L,
            name = "Product with \"quotes\" & special <chars>",
            description = "Description with\nnewlines\tand\ttabs",
            sku = "SKU-WITH-SPECIAL-#123"
        )

        // When
        val model = response.mapToPosVariationModel(LocalOrRemoteId.LocalId(1))

        // Then
        assertThat(model.variationName).isEqualTo("Product with \"quotes\" & special <chars>")
        assertThat(model.description).contains("newlines")
        assertThat(model.sku).isEqualTo("SKU-WITH-SPECIAL-#123")
    }

    @Test
    fun `given edge case numeric values, when mapper called, then values handled correctly`() {
        // Given
        val response = WooPosVariationApiResponse(
            id = Long.MAX_VALUE,
            productId = 0L,
            price = "0.01",
            regularPrice = "999999.99",
            salePrice = "",
            stockQuantity = Double.MAX_VALUE
        )

        // When
        val model = response.mapToPosVariationModel(LocalOrRemoteId.LocalId(Int.MAX_VALUE))

        // Then
        assertThat(model.remoteVariationId.value).isEqualTo(Long.MAX_VALUE)
        assertThat(model.remoteProductId.value).isEqualTo(0L)
        assertThat(model.price).isEqualTo("0.01")
        assertThat(model.regularPrice).isEqualTo("999999.99")
        assertThat(model.salePrice).isEmpty()
        assertThat(model.stockQuantity).isEqualTo(Double.MAX_VALUE)
    }

    @Test
    fun `given minimal API response, when mapper called, then defaults applied correctly`() {
        // Given - only required fields
        val response = WooPosVariationApiResponse(
            id = 1L,
            productId = 2L
        )

        // When
        val model = response.mapToPosVariationModel(LocalOrRemoteId.LocalId(1))

        // Then
        assertThat(model.remoteVariationId.value).isEqualTo(1L)
        assertThat(model.remoteProductId.value).isEqualTo(2L)
        assertThat(model.variationName).isEmpty()
        assertThat(model.sku).isEmpty()
        assertThat(model.price).isEmpty()
        assertThat(model.stockQuantity).isEqualTo(0.0)
        assertThat(model.manageStock).isFalse()
        assertThat(model.downloadable).isFalse()
    }
}
