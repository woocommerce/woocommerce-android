package com.woocommerce.android.ui.woopos.common.data.models

import com.google.gson.Gson
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCProductModel
import java.math.BigDecimal

class WCProductToWooPosProductModelMapperTest {
    private val logger: WooPosLogWrapper = mock()
    private val wooPosProductModelMapper = WooPosProductModelMapper(logger)
    private val sut: WooPosWCProductToWooPosProductModelMapper =
        WooPosWCProductToWooPosProductModelMapper(wooPosProductModelMapper, logger)

    @Test
    fun `when mapping WCProductModel, then all attributes are correctly mapped`() {
        val imagesJson = """[{"id": 1, "src": "https://example.com/image.jpg", "name": "Image", "alt": "Alt text"}]"""
            .trimIndent()
        val categoriesJson = """[{"id": 10, "name": "Electronics", "slug": "electronics"}]""".trimIndent()
        val tagsJson = """[{"id": 20, "name": "Featured", "slug": "featured"}]""".trimIndent()

        val attributes = arrayOf(
            WCProductModel.ProductAttribute(
                id = 100L,
                name = "Color",
                variation = true,
                visible = true,
                options = listOf("Red", "Blue")
            )
        )

        val wcProduct = createFullWCProductModel(imagesJson, attributes, categoriesJson, tagsJson)

        val result = sut.map(wcProduct)

        assertThat(result.remoteId).isEqualTo(123L)
        assertThat(result.parentId).isEqualTo(456L)
        assertThat(result.name).isEqualTo("Test Product")
        assertThat(result.sku).isEqualTo("TEST-SKU")
        assertThat(result.globalUniqueId).isEqualTo("global-123")
        assertThat(result.description).isEqualTo("Full description")
        assertThat(result.shortDescription).isEqualTo("Short description")
        assertThat(result.isDownloadable).isTrue()
        assertThat(result.lastModified).isEqualTo("2024-01-15T10:00:00Z")

        assertThat(result.type).isEqualTo(WooPosProductModel.WooPosProductType.Simple)
        assertThat(result.status).isEqualTo(WooPosProductModel.WooPosProductStatus.PUBLISH)

        assertThat(result.pricing).isInstanceOf(WooPosProductModel.WooPosPricing.SalePricing::class.java)
        val pricing = result.pricing as WooPosProductModel.WooPosPricing.SalePricing
        assertThat(pricing.regularPrice).isEqualTo(BigDecimal("24.99"))
        assertThat(pricing.salePrice).isEqualTo(BigDecimal("19.99"))

        assertThat(result.images).hasSize(1)
        assertThat(result.images[0].id).isEqualTo(1L)
        assertThat(result.images[0].url).isEqualTo("https://example.com/image.jpg")
        assertThat(result.images[0].name).isEqualTo("Image")
        assertThat(result.images[0].alt).isEqualTo("Alt text")

        assertThat(result.attributes).hasSize(1)
        assertThat(result.attributes[0].id).isEqualTo(100L)
        assertThat(result.attributes[0].name).isEqualTo("Color")
        assertThat(result.attributes[0].options).containsExactly("Red", "Blue")
        assertThat(result.attributes[0].isVisible).isTrue()
        assertThat(result.attributes[0].isVariation).isTrue()

        assertThat(result.categories).hasSize(1)
        assertThat(result.categories[0].id).isEqualTo(10L)
        assertThat(result.categories[0].name).isEqualTo("Electronics")
        assertThat(result.categories[0].slug).isEqualTo("electronics")

        assertThat(result.tags).hasSize(1)
        assertThat(result.tags[0].id).isEqualTo(20L)
        assertThat(result.tags[0].name).isEqualTo("Featured")
        assertThat(result.tags[0].slug).isEqualTo("featured")
    }

    @Test
    fun `when parent id is zero, then mapped parent id is null`() {
        val wcProduct = WCProductModel(
            localSiteId = LocalId(1),
            remoteId = RemoteId(123L),
            parentId = 0L
        )

        val result = sut.map(wcProduct)

        assertThat(result.parentId).isNull()
    }

    @Test
    fun `when parent id is non-zero, then mapped parent id has value`() {
        val wcProduct = WCProductModel(
            localSiteId = LocalId(1),
            remoteId = RemoteId(123L),
            parentId = 789L
        )

        val result = sut.map(wcProduct)

        assertThat(result.parentId).isEqualTo(789L)
    }

    @Test
    fun `when product has empty collections, then mapped collections are empty`() {
        val wcProduct = WCProductModel(
            localSiteId = LocalId(1),
            remoteId = RemoteId(123L),
            images = "",
            attributes = "[]",
            categories = "",
            tags = ""
        )

        val result = sut.map(wcProduct)

        assertThat(result.images).isEmpty()
        assertThat(result.attributes).isEmpty()
        assertThat(result.categories).isEmpty()
        assertThat(result.tags).isEmpty()
    }

    @Test
    fun `when product has no price, then pricing is NoPricing`() {
        val wcProduct = WCProductModel(
            localSiteId = LocalId(1),
            remoteId = RemoteId(123L),
            price = "",
            regularPrice = "",
            salePrice = "",
            onSale = false
        )

        val result = sut.map(wcProduct)

        assertThat(result.pricing).isEqualTo(WooPosProductModel.WooPosPricing.NoPricing)
    }

    @Test
    fun `when product has regular price only, then pricing is RegularPricing`() {
        val wcProduct = WCProductModel(
            localSiteId = LocalId(1),
            remoteId = RemoteId(123L),
            price = "",
            regularPrice = "29.99",
            salePrice = "",
            onSale = false
        )

        val result = sut.map(wcProduct)

        assertThat(result.pricing).isInstanceOf(WooPosProductModel.WooPosPricing.RegularPricing::class.java)
        val pricing = result.pricing as WooPosProductModel.WooPosPricing.RegularPricing
        assertThat(pricing.price).isEqualTo(BigDecimal("29.99"))
    }

    private fun createFullWCProductModel(
        imagesJson: String,
        attributes: Array<WCProductModel.ProductAttribute>,
        categoriesJson: String,
        tagsJson: String
    ): WCProductModel {
        val wcProduct = WCProductModel(
            localSiteId = LocalId(1),
            remoteId = RemoteId(123L),
            parentId = 456L,
            name = "Test Product",
            sku = "TEST-SKU",
            globalUniqueId = "global-123",
            type = "simple",
            status = "publish",
            price = "19.99",
            regularPrice = "24.99",
            salePrice = "19.99",
            onSale = true,
            description = "Full description",
            shortDescription = "Short description",
            downloadable = true,
            dateModified = "2024-01-15T10:00:00Z",
            images = imagesJson,
            attributes = Gson().toJson(attributes),
            categories = categoriesJson,
            tags = tagsJson
        )
        return wcProduct
    }
}
