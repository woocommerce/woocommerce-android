package com.woocommerce.android.ui.woopos.common.data.models

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.mock
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosProductEntity
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class WooPosProductModelMapperTest : BaseUnitTest() {

    private val logger: WooPosLogWrapper = mock()
    private val mapper: WooPosProductModelMapper = WooPosProductModelMapper(logger)

    @Test
    fun `given complete entity, when mapping to model, then all fields are mapped correctly`() {
        val entity = createCompleteEntity()

        val result = mapper.fromEntity(entity)

        assertThat(result.remoteId).isEqualTo(123L)
        assertThat(result.parentId).isEqualTo(456L)
        assertThat(result.name).isEqualTo("Test Product")
        assertThat(result.sku).isEqualTo("TEST-SKU")
        assertThat(result.globalUniqueId).isEqualTo("global-123")
        assertThat(result.type).isEqualTo(WooPosProductModel.WooPosProductType.Simple)
        assertThat(result.status).isEqualTo(WooPosProductModel.WooPosProductStatus.PUBLISH)
        assertThat(result.pricing).isInstanceOf(WooPosProductModel.WooPosPricing.SalePricing::class.java)
        val salePricing = result.pricing as WooPosProductModel.WooPosPricing.SalePricing
        assertThat(salePricing.regularPrice).isEqualTo(BigDecimal("29.99"))
        assertThat(salePricing.salePrice).isEqualTo(BigDecimal("19.99"))
        assertThat(result.pricing.isOnSale).isTrue()
        assertThat(result.pricing.displayPrice).isEqualTo(BigDecimal("19.99"))
        assertThat(result.description).isEqualTo("Test description")
        assertThat(result.shortDescription).isEqualTo("Short desc")
        assertThat(result.isDownloadable).isFalse()
        assertThat(result.lastModified).isEqualTo("2023-01-01T00:00:00")
    }

    @Test
    fun `given entity with pricing information, when mapping to model, then pricing is handled correctly`() {
        // Regular pricing only
        var entity = WooPosProductEntity(
            remoteId = LocalOrRemoteId.RemoteId(123),
            regularPrice = "25.99",
            onSale = false
        )
        var result = mapper.fromEntity(entity)
        assertThat(result.pricing).isInstanceOf(WooPosProductModel.WooPosPricing.RegularPricing::class.java)
        val regularPricing = result.pricing as WooPosProductModel.WooPosPricing.RegularPricing
        assertThat(regularPricing.price).isEqualTo(BigDecimal("25.99"))

        // Sale pricing
        entity = WooPosProductEntity(
            remoteId = LocalOrRemoteId.RemoteId(123),
            regularPrice = "29.99",
            salePrice = "19.99",
            onSale = true
        )
        result = mapper.fromEntity(entity)
        assertThat(result.pricing).isInstanceOf(WooPosProductModel.WooPosPricing.SalePricing::class.java)
        val salePricing = result.pricing as WooPosProductModel.WooPosPricing.SalePricing
        assertThat(salePricing.regularPrice).isEqualTo(BigDecimal("29.99"))
        assertThat(salePricing.salePrice).isEqualTo(BigDecimal("19.99"))

        // No pricing
        entity = WooPosProductEntity(
            remoteId = LocalOrRemoteId.RemoteId(123),
            price = "",
            regularPrice = "",
            salePrice = ""
        )
        result = mapper.fromEntity(entity)
        assertThat(result.pricing).isInstanceOf(WooPosProductModel.WooPosPricing.NoPricing::class.java)
        assertThat(result.pricing.displayPrice).isNull()
        assertThat(result.pricing.hasPrice).isFalse()
    }

    @Test
    fun `given entity with invalid price format, when mapping to model, then pricing defaults to NoPricing`() {
        val entity = WooPosProductEntity(
            remoteId = LocalOrRemoteId.RemoteId(123),
            price = "invalid",
            regularPrice = "not-a-number",
            salePrice = "abc"
        )

        val result = mapper.fromEntity(entity)

        assertThat(result.pricing).isInstanceOf(WooPosProductModel.WooPosPricing.NoPricing::class.java)
        assertThat(result.pricing.displayPrice).isNull()
        assertThat(result.pricing.hasPrice).isFalse()
    }

    @Test
    fun `given entity with JSON images, when mapping to model, then images are parsed correctly`() {
        val imagesJson = """[{"id": 1, "src": "https://example.com/image.jpg", "name": "Image 1", "alt": "Alt text"}]"""
        val entity = WooPosProductEntity(
            remoteId = LocalOrRemoteId.RemoteId(123),
            images = imagesJson
        )

        val result = mapper.fromEntity(entity)
        val images = result.images

        assertThat(images).hasSize(1)
        val image = images.first()
        assertThat(image.id).isEqualTo(1L)
        assertThat(image.url).isEqualTo("https://example.com/image.jpg")
        assertThat(image.name).isEqualTo("Image 1")
        assertThat(image.alt).isEqualTo("Alt text")
    }

    @Test
    fun `given entity with malformed JSON, when mapping to model, then returns empty collections gracefully`() {
        val entity = WooPosProductEntity(
            remoteId = LocalOrRemoteId.RemoteId(123),
            images = "malformed json",
            attributes = "invalid json",
            categories = "not json",
            tags = "bad format"
        )

        val result = mapper.fromEntity(entity)

        assertThat(result.images).isEmpty()
        assertThat(result.attributes).isEmpty()
        assertThat(result.categories).isEmpty()
        assertThat(result.tags).isEmpty()
    }

    @Test
    fun `given entity with empty JSON collections, when mapping to model, then returns empty lists`() {
        val entity = WooPosProductEntity(
            remoteId = LocalOrRemoteId.RemoteId(123),
            images = "",
            attributes = "",
            categories = "",
            tags = ""
        )

        val result = mapper.fromEntity(entity)

        assertThat(result.images).isEmpty()
        assertThat(result.attributes).isEmpty()
        assertThat(result.categories).isEmpty()
        assertThat(result.tags).isEmpty()
    }

    @Test
    fun `given entity with valid JSON attributes, when mapping to model, then attributes are parsed correctly`() {
        val attributesJson =
            """[{"id": 1, "name": "Color", "options": ["Red", "Blue"], "visible": true, "variation": false}]"""
        val entity = WooPosProductEntity(
            remoteId = LocalOrRemoteId.RemoteId(123),
            attributes = attributesJson
        )

        val result = mapper.fromEntity(entity)
        val attributes = result.attributes

        assertThat(attributes).hasSize(1)
        val attribute = attributes.first()
        assertThat(attribute.id).isEqualTo(1L)
        assertThat(attribute.name).isEqualTo("Color")
        assertThat(attribute.options).containsExactly("Red", "Blue")
        assertThat(attribute.isVisible).isTrue()
        assertThat(attribute.isVariation).isFalse()
    }

    @Test
    fun `given product model with JSON collections, when accessing multiple times, then returns same parsed data`() {
        val entity = WooPosProductEntity(
            remoteId = LocalOrRemoteId.RemoteId(123),
            images = """[{"id": 1, "src": "test.jpg"}]""",
            attributes = """[{"id": 2, "name": "Size"}]"""
        )

        val result = mapper.fromEntity(entity)

        // First access
        val images1 = result.images
        val attributes1 = result.attributes

        // Second access - should return same data
        val images2 = result.images
        val attributes2 = result.attributes

        // Should be the same instances (part of the data class)
        assertThat(images1).isSameAs(images2)
        assertThat(attributes1).isSameAs(attributes2)

        // Content should be correct
        assertThat(images1).hasSize(1)
        assertThat(attributes1).hasSize(1)
    }

    @Test
    fun `given list of entities, when mapping to models, then all entities are mapped correctly`() {
        val entities = listOf(
            createMinimalEntity(1),
            createMinimalEntity(2),
            createCompleteEntity()
        )

        val result = mapper.fromEntities(entities)

        assertThat(result).hasSize(3)
        assertThat(result[0].remoteId).isEqualTo(1L)
        assertThat(result[1].remoteId).isEqualTo(2L)
        assertThat(result[2].remoteId).isEqualTo(123L)
        assertThat(result[2].name).isEqualTo("Test Product")
    }

    @Test
    fun `given different product types, when mapping to model, then types are parsed correctly`() {
        val testCases = mapOf(
            "simple" to WooPosProductModel.WooPosProductType.Simple,
            "variable" to WooPosProductModel.WooPosProductType.Variable,
            "grouped" to WooPosProductModel.WooPosProductType.Grouped,
            "external" to WooPosProductModel.WooPosProductType.External,
            "subscription" to WooPosProductModel.WooPosProductType.Subscription,
            "variable-subscription" to WooPosProductModel.WooPosProductType.VariableSubscription,
            "bundle" to WooPosProductModel.WooPosProductType.Bundle,
            "composite" to WooPosProductModel.WooPosProductType.Composite,
            "unknown" to WooPosProductModel.WooPosProductType.Custom
        )

        testCases.forEach { (typeString, expectedType) ->
            val entity = WooPosProductEntity(
                remoteId = LocalOrRemoteId.RemoteId(123),
                type = typeString
            )

            val result = mapper.fromEntity(entity)

            assertThat(result.type).isEqualTo(expectedType)
        }
    }

    @Test
    fun `given different product statuses, when mapping to model, then statuses are parsed correctly`() {
        val testCases = mapOf(
            "publish" to WooPosProductModel.WooPosProductStatus.PUBLISH,
            "draft" to WooPosProductModel.WooPosProductStatus.DRAFT,
            "pending" to WooPosProductModel.WooPosProductStatus.PENDING,
            "private" to WooPosProductModel.WooPosProductStatus.PRIVATE,
            "trash" to WooPosProductModel.WooPosProductStatus.TRASH,
            "unknown" to WooPosProductModel.WooPosProductStatus.UNKNOWN
        )

        testCases.forEach { (statusString, expectedStatus) ->
            val entity = WooPosProductEntity(
                remoteId = LocalOrRemoteId.RemoteId(123),
                status = statusString
            )

            val result = mapper.fromEntity(entity)

            assertThat(result.status).isEqualTo(expectedStatus)
        }
    }

    @Test
    fun `when mapPricing is called with various inputs, then returns correct pricing types`() {
        // Test sale pricing
        var pricing = mapper.mapPricing(
            price = null,
            regularPrice = BigDecimal("29.99"),
            salePrice = BigDecimal("19.99"),
            isOnSale = true
        )
        assertThat(pricing).isInstanceOf(WooPosProductModel.WooPosPricing.SalePricing::class.java)
        val salePricing = pricing as WooPosProductModel.WooPosPricing.SalePricing
        assertThat(salePricing.regularPrice).isEqualTo(BigDecimal("29.99"))
        assertThat(salePricing.salePrice).isEqualTo(BigDecimal("19.99"))

        // Test regular pricing
        pricing = mapper.mapPricing(
            price = null,
            regularPrice = BigDecimal("25.99"),
            salePrice = null,
            isOnSale = false
        )
        assertThat(pricing).isInstanceOf(WooPosProductModel.WooPosPricing.RegularPricing::class.java)
        val regularPricing = pricing as WooPosProductModel.WooPosPricing.RegularPricing
        assertThat(regularPricing.price).isEqualTo(BigDecimal("25.99"))

        // Test no pricing
        pricing = mapper.mapPricing(
            price = null,
            regularPrice = null,
            salePrice = null,
            isOnSale = false
        )
        assertThat(pricing).isInstanceOf(WooPosProductModel.WooPosPricing.NoPricing::class.java)
    }

    @Test
    fun `when mapProductType is called with type strings, then returns correct product types`() {
        assertThat(mapper.mapProductType("simple")).isEqualTo(WooPosProductModel.WooPosProductType.Simple)
        assertThat(mapper.mapProductType("SIMPLE")).isEqualTo(WooPosProductModel.WooPosProductType.Simple)
        assertThat(mapper.mapProductType("variable")).isEqualTo(WooPosProductModel.WooPosProductType.Variable)
        assertThat(mapper.mapProductType("unknown")).isEqualTo(WooPosProductModel.WooPosProductType.Custom)
    }

    @Test
    fun `when mapProductStatus is called with status strings, then returns correct product statuses`() {
        assertThat(mapper.mapProductStatus("publish")).isEqualTo(WooPosProductModel.WooPosProductStatus.PUBLISH)
        assertThat(mapper.mapProductStatus("PUBLISH")).isEqualTo(WooPosProductModel.WooPosProductStatus.PUBLISH)
        assertThat(mapper.mapProductStatus("draft")).isEqualTo(WooPosProductModel.WooPosProductStatus.DRAFT)
        assertThat(mapper.mapProductStatus("unknown")).isEqualTo(WooPosProductModel.WooPosProductStatus.UNKNOWN)
    }

    private fun createCompleteEntity() = WooPosProductEntity(
        localSiteId = LocalOrRemoteId.LocalId(1),
        remoteId = LocalOrRemoteId.RemoteId(123),
        name = "Test Product",
        sku = "TEST-SKU",
        globalUniqueId = "global-123",
        type = "simple",
        price = "19.99",
        downloadable = false,
        images = """[{"id": 1, "src": "https://example.com/image.jpg"}]""",
        attributes = """[{"id": 1, "name": "Color", "options": ["Red", "Blue"]}]""",
        parentId = 456L,
        status = "publish",
        regularPrice = "29.99",
        salePrice = "19.99",
        onSale = true,
        description = "Test description",
        shortDescription = "Short desc",
        stockQuantity = 10.5,
        stockStatus = "instock",
        categories = """[{"id": 1, "name": "Category 1", "slug": "category-1"}]""",
        tags = """[{"id": 1, "name": "Tag 1", "slug": "tag-1"}]""",
        dateModified = "2023-01-01T00:00:00"
    )

    private fun createMinimalEntity(id: Long) = WooPosProductEntity(
        remoteId = LocalOrRemoteId.RemoteId(id)
    )
}
