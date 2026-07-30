package com.woocommerce.android.ui.woopos.common.data

import com.google.gson.Gson
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.VariantOption
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import com.woocommerce.android.ui.woopos.util.generateWooPosProduct
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosVariationEntity
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class WooPosVariationMapperTest : BaseUnitTest() {

    private lateinit var sut: WooPosVariationMapper
    private val gson = Gson()
    private val resourceProvider: ResourceProvider = mock()

    @Before
    fun setup() {
        sut = WooPosVariationMapper(gson)
        whenever(resourceProvider.getString(R.string.woopos_variations_any_variation, "Color"))
            .thenReturn("Any Color")
        whenever(resourceProvider.getString(R.string.woopos_variations_any_variation, "Size"))
            .thenReturn("Any Size")
    }

    @Test
    fun `given product variation with all fields, when mapping from ProductVariation, then all fields mapped correctly`() {
        // GIVEN
        val productVariation = ProductTestUtils.generateProductVariation(
            productId = 123L,
            variationId = 456L,
            amount = "19.99"
        )

        // WHEN
        val result = sut.fromProductVariation(productVariation)

        // THEN
        assertThat(result.remoteVariationId).isEqualTo(456L)
        assertThat(result.remoteProductId).isEqualTo(123L)
        assertThat(result.globalUniqueId).isEqualTo(productVariation.globalUniqueId)
        assertThat(result.price).isEqualTo(BigDecimal("19.99"))
        assertThat(result.isVisible).isEqualTo(productVariation.isVisible)
        assertThat(result.isDownloadable).isEqualTo(productVariation.isDownloadable)
    }

    @Test
    fun `given product variation with image, when mapping from ProductVariation, then image mapped correctly`() {
        // GIVEN
        val productVariation = ProductTestUtils.generateProductVariation()
            .copy(image = Product.Image(1L, "Test Image", null, "http://example.com/image.jpg", null, false))

        // WHEN
        val result = sut.fromProductVariation(productVariation)

        // THEN
        assertThat(result.image).isNotNull
        assertThat(result.image?.source).isEqualTo("http://example.com/image.jpg")
    }

    @Test
    fun `given product variation with no image, when mapping from ProductVariation, then image is null`() {
        // GIVEN
        val productVariation = ProductTestUtils.generateProductVariation()
            .copy(image = null)

        // WHEN
        val result = sut.fromProductVariation(productVariation)

        // THEN
        assertThat(result.image).isNull()
    }

    @Test
    fun `given product variation with attributes, when mapping from ProductVariation, then attributes mapped correctly`() {
        // GIVEN
        val attributes = arrayOf(
            VariantOption(
                id = 1L,
                name = "Color",
                option = "Blue"
            ),
            VariantOption(
                id = 2L,
                name = "Size",
                option = "Large"
            )
        )
        val productVariation = ProductTestUtils.generateProductVariation()
            .copy(attributes = attributes)

        // WHEN
        val result = sut.fromProductVariation(productVariation)

        // THEN
        assertThat(result.attributes).hasSize(2)
        assertThat(result.attributes[0].id).isEqualTo(1L)
        assertThat(result.attributes[0].name).isEqualTo("Color")
        assertThat(result.attributes[0].option).isEqualTo("Blue")
        assertThat(result.attributes[1].id).isEqualTo(2L)
        assertThat(result.attributes[1].name).isEqualTo("Size")
        assertThat(result.attributes[1].option).isEqualTo("Large")
    }

    @Test
    fun `given WC variation model with publish status, when mapping from WCProductVariationModel, then isVisible is true`() {
        // GIVEN
        val wcModel = createWCProductVariationModel(status = "publish")

        // WHEN
        val result = sut.fromWCProductVariationModel(wcModel)

        // THEN
        assertThat(result.isVisible).isTrue()
    }

    @Test
    fun `given WC variation model with draft status, when mapping from WCProductVariationModel, then isVisible is false`() {
        // GIVEN
        val wcModel = createWCProductVariationModel(status = "draft")

        // WHEN
        val result = sut.fromWCProductVariationModel(wcModel)

        // THEN
        assertThat(result.isVisible).isFalse()
    }

    @Test
    fun `given WC variation model with invalid price, when mapping from WCProductVariationModel, then price is null`() {
        // GIVEN
        val wcModel = createWCProductVariationModel(price = "invalid")

        // WHEN
        val result = sut.fromWCProductVariationModel(wcModel)

        // THEN
        assertThat(result.price).isNull()
    }

    @Test
    fun `given WC variation model with valid price, when mapping from WCProductVariationModel, then price is correct`() {
        // GIVEN
        val wcModel = createWCProductVariationModel(price = "25.50")

        // WHEN
        val result = sut.fromWCProductVariationModel(wcModel)

        // THEN
        assertThat(result.price).isEqualTo(BigDecimal("25.50"))
    }

    @Test
    fun `given WC variation model with empty image, when mapping from WCProductVariationModel, then image is null`() {
        // GIVEN
        val wcModel = createWCProductVariationModel(image = "")

        // WHEN
        val result = sut.fromWCProductVariationModel(wcModel)

        // THEN
        assertThat(result.image).isNull()
    }

    @Test
    fun `given WC variation model with valid image JSON, when mapping from WCProductVariationModel, then image mapped correctly`() {
        // GIVEN
        val imageJson = """{"src":"http://example.com/test.jpg","name":"Test Image"}"""
        val wcModel = createWCProductVariationModel(image = imageJson)

        // WHEN
        val result = sut.fromWCProductVariationModel(wcModel)

        // THEN
        assertThat(result.image?.source).isEqualTo("http://example.com/test.jpg")
    }

    @Test
    fun `given variation entity with valid attributes JSON, when mapping from WooPosVariationEntity, then attributes parsed correctly`() {
        // GIVEN
        val attributesJson = """[{"id":1,"name":"Color","option":"Red"},{"id":2,"name":"Size","option":"Medium"}]"""
        val entity = createWooPosVariationEntity(attributesJson = attributesJson)

        // WHEN
        val result = sut.fromWooPosVariationEntity(entity)

        // THEN
        assertThat(result.attributes).hasSize(2)
        assertThat(result.attributes[0].id).isEqualTo(1L)
        assertThat(result.attributes[0].name).isEqualTo("Color")
        assertThat(result.attributes[0].option).isEqualTo("Red")
        assertThat(result.attributes[1].id).isEqualTo(2L)
        assertThat(result.attributes[1].name).isEqualTo("Size")
        assertThat(result.attributes[1].option).isEqualTo("Medium")
    }

    @Test
    fun `given variation entity with empty attributes JSON, when mapping from WooPosVariationEntity, then attributes list is empty`() {
        // GIVEN
        val entity = createWooPosVariationEntity(attributesJson = "")

        // WHEN
        val result = sut.fromWooPosVariationEntity(entity)

        // THEN
        assertThat(result.attributes).isEmpty()
    }

    @Test
    fun `given variation entity with invalid attributes JSON, when mapping from WooPosVariationEntity, then attributes list is empty`() {
        // GIVEN
        val entity = createWooPosVariationEntity(attributesJson = "invalid json")

        // WHEN
        val result = sut.fromWooPosVariationEntity(entity)

        // THEN
        assertThat(result.attributes).isEmpty()
    }

    @Test
    fun `given variation entity with image URL, when mapping from WooPosVariationEntity, then image mapped correctly`() {
        // GIVEN
        val entity = createWooPosVariationEntity(imageUrl = "http://example.com/image.png")

        // WHEN
        val result = sut.fromWooPosVariationEntity(entity)

        // THEN
        assertThat(result.image?.source).isEqualTo("http://example.com/image.png")
    }

    @Test
    fun `given variation entity with empty image URL, when mapping from WooPosVariationEntity, then image is null`() {
        // GIVEN
        val entity = createWooPosVariationEntity(imageUrl = "")

        // WHEN
        val result = sut.fromWooPosVariationEntity(entity)

        // THEN
        assertThat(result.image).isNull()
    }

    @Test
    fun `given variation with attributes and parent product, when getting name for POS, then uses parent attributes order`() {
        // GIVEN
        val variation = createWooPosVariation(
            attributes = listOf(
                WooPosVariation.WooPosVariationAttribute(1L, "Color", "Blue"),
                WooPosVariation.WooPosVariationAttribute(2L, "Size", "Large")
            )
        )
        val parentProduct = generateWooPosProduct(
            attributes = listOf(
                WooPosProductModel.WooPosProductAttribute(1L, "Color", listOf("Red", "Blue", "Green"), true, true),
                WooPosProductModel.WooPosProductAttribute(2L, "Size", listOf("Small", "Medium", "Large"), true, true)
            )
        )

        // WHEN
        val result = sut.getNameForPOS(variation, parentProduct, resourceProvider)

        // THEN
        assertThat(result).isEqualTo("Color: Blue, Size: Large")
    }

    @Test
    fun `given variation with missing attribute option and parent product, when getting name for POS, then uses any variation text`() {
        // GIVEN
        val variation = createWooPosVariation(
            attributes = listOf(
                WooPosVariation.WooPosVariationAttribute(1L, "Color", "Blue"),
                WooPosVariation.WooPosVariationAttribute(2L, "Size", null)
            )
        )
        val parentProduct = generateWooPosProduct(
            attributes = listOf(
                WooPosProductModel.WooPosProductAttribute(1L, "Color", listOf("Red", "Blue"), true, true),
                WooPosProductModel.WooPosProductAttribute(2L, "Size", listOf("Small", "Large"), true, true)
            )
        )

        // WHEN
        val result = sut.getNameForPOS(variation, parentProduct, resourceProvider)

        // THEN
        assertThat(result).isEqualTo("Color: Blue, Any Size")
    }

    @Test
    fun `given variation without parent product, when getting name for POS, then uses variation attributes directly`() {
        // GIVEN
        val variation = createWooPosVariation(
            attributes = listOf(
                WooPosVariation.WooPosVariationAttribute(1L, "Color", "Blue"),
                WooPosVariation.WooPosVariationAttribute(2L, "Size", "Large")
            )
        )

        // WHEN
        val result = sut.getNameForPOS(variation, null, resourceProvider)

        // THEN
        assertThat(result).isEqualTo("Color: Blue, Size: Large")
    }

    @Test
    fun `given variation with attributes without name, when getting name for POS, then uses option only`() {
        // GIVEN
        val variation = createWooPosVariation(
            attributes = listOf(
                WooPosVariation.WooPosVariationAttribute(1L, null, "Blue"),
                WooPosVariation.WooPosVariationAttribute(2L, "Size", "Large")
            )
        )

        // WHEN
        val result = sut.getNameForPOS(variation, null, resourceProvider)

        // THEN
        assertThat(result).isEqualTo("Blue, Size: Large")
    }

    @Test
    fun `given variation with attributes without option, when getting name for POS, then uses any variation text`() {
        // GIVEN
        val variation = createWooPosVariation(
            attributes = listOf(
                WooPosVariation.WooPosVariationAttribute(1L, "Color", null),
                WooPosVariation.WooPosVariationAttribute(2L, "Size", "Large")
            )
        )

        // WHEN
        val result = sut.getNameForPOS(variation, null, resourceProvider)

        // THEN
        assertThat(result).isEqualTo("Any Color, Size: Large")
    }

    @Test
    fun `given variation with empty attributes, when getting name for POS, then returns empty string`() {
        // GIVEN
        val variation = createWooPosVariation(attributes = emptyList())

        // WHEN
        val result = sut.getNameForPOS(variation, null, resourceProvider)

        // THEN
        assertThat(result).isEmpty()
    }

    @Test
    fun `given variation with attributes and parent product, when getting name, then uses dash separator`() {
        // GIVEN
        val variation = createWooPosVariation(
            attributes = listOf(
                WooPosVariation.WooPosVariationAttribute(1L, "Color", "Blue"),
                WooPosVariation.WooPosVariationAttribute(2L, "Size", "Large")
            )
        )
        val parentProduct = generateWooPosProduct(
            attributes = listOf(
                WooPosProductModel.WooPosProductAttribute(1L, "Color", listOf("Red", "Blue"), true, true),
                WooPosProductModel.WooPosProductAttribute(2L, "Size", listOf("Small", "Large"), true, true)
            )
        )

        // WHEN
        val result = sut.getName(variation, parentProduct)

        // THEN
        assertThat(result).isEqualTo("Blue - Large")
    }

    @Test
    fun `given variation with missing attribute option and parent product, when getting name, then uses any prefix`() {
        // GIVEN
        val variation = createWooPosVariation(
            attributes = listOf(
                WooPosVariation.WooPosVariationAttribute(1L, "Color", "Blue"),
                WooPosVariation.WooPosVariationAttribute(2L, "Size", null)
            )
        )
        val parentProduct = generateWooPosProduct(
            attributes = listOf(
                WooPosProductModel.WooPosProductAttribute(1L, "Color", listOf("Red", "Blue"), true, true),
                WooPosProductModel.WooPosProductAttribute(2L, "Size", listOf("Small", "Large"), true, true)
            )
        )

        // WHEN
        val result = sut.getName(variation, parentProduct)

        // THEN
        assertThat(result).isEqualTo("Blue - Any Size")
    }

    @Test
    fun `given variation without parent product, when getting name, then uses only options with dash separator`() {
        // GIVEN
        val variation = createWooPosVariation(
            attributes = listOf(
                WooPosVariation.WooPosVariationAttribute(1L, "Color", "Blue"),
                WooPosVariation.WooPosVariationAttribute(2L, "Size", "Large"),
                WooPosVariation.WooPosVariationAttribute(3L, "Material", null)
            )
        )

        // WHEN
        val result = sut.getName(variation, null)

        // THEN
        assertThat(result).isEqualTo("Blue - Large")
    }

    @Test
    fun `given variation with no attribute options, when getting name, then returns empty string`() {
        // GIVEN
        val variation = createWooPosVariation(
            attributes = listOf(
                WooPosVariation.WooPosVariationAttribute(1L, "Color", null),
                WooPosVariation.WooPosVariationAttribute(2L, "Size", null)
            )
        )

        // WHEN
        val result = sut.getName(variation, null)

        // THEN
        assertThat(result).isEmpty()
    }

    @Test
    fun `given ProductVariation, when converting using extension function, then returns correct WooPosVariation`() {
        // GIVEN
        val productVariation = ProductTestUtils.generateProductVariation(
            productId = 123L,
            variationId = 456L
        )

        // WHEN
        val result = productVariation.toWooPosVariation(sut)

        // THEN
        assertThat(result.remoteVariationId).isEqualTo(456L)
        assertThat(result.remoteProductId).isEqualTo(123L)
    }

    @Test
    fun `given WCProductVariationModel, when converting using extension function, then returns correct WooPosVariation`() {
        // GIVEN
        val wcModel = createWCProductVariationModel()

        // WHEN
        val result = wcModel.toWooPosVariation(sut)

        // THEN
        assertThat(result.remoteVariationId).isEqualTo(456L)
        assertThat(result.remoteProductId).isEqualTo(123L)
    }

    @Test
    fun `given WooPosVariationEntity, when converting using extension function, then returns correct WooPosVariation`() {
        // GIVEN
        val entity = createWooPosVariationEntity()

        // WHEN
        val result = entity.toWooPosVariation(sut)

        // THEN
        assertThat(result.remoteVariationId).isEqualTo(456L)
        assertThat(result.remoteProductId).isEqualTo(123L)
    }

    @Test
    fun `given WooPosVariation, when getting name for POS using extension function, then returns correct name`() {
        // GIVEN
        val variation = createWooPosVariation(
            attributes = listOf(
                WooPosVariation.WooPosVariationAttribute(1L, "Color", "Blue")
            )
        )

        // WHEN
        val result = variation.getNameForPOS(sut, null, resourceProvider)

        // THEN
        assertThat(result).isEqualTo("Color: Blue")
    }

    @Test
    fun `given WooPosVariation, when getting name using extension function, then returns correct name`() {
        // GIVEN
        val variation = createWooPosVariation(
            attributes = listOf(
                WooPosVariation.WooPosVariationAttribute(1L, "Color", "Blue")
            )
        )

        // WHEN
        val result = variation.getName(sut, null)

        // THEN
        assertThat(result).isEqualTo("Blue")
    }

    private fun createWCProductVariationModel(
        status: String = "publish",
        price: String = "19.99",
        image: String = ""
    ): WCProductVariationModel {
        return WCProductVariationModel(LocalOrRemoteId.LocalId(1)).copy(
            remoteProductId = LocalOrRemoteId.RemoteId(123L),
            remoteVariationId = LocalOrRemoteId.RemoteId(456L),
            globalUniqueId = "test-global-id",
            price = price,
            image = image,
            status = status,
            downloadable = false,
            attributes = """[{"id":1,"name":"Color","option":"Blue"}]"""
        )
    }

    private fun createWooPosVariationEntity(
        attributesJson: String = "[]",
        imageUrl: String = "",
        status: String = "publish",
        price: String = "19.99",
        type: String = "variation"
    ): WooPosVariationEntity {
        return WooPosVariationEntity(
            localSiteId = LocalOrRemoteId.LocalId(1),
            remoteProductId = LocalOrRemoteId.RemoteId(123L),
            remoteVariationId = LocalOrRemoteId.RemoteId(456L),
            globalUniqueId = "test-global-id",
            price = price,
            imageUrl = imageUrl,
            attributesJson = attributesJson,
            status = status,
            downloadable = false,
            type = type
        )
    }

    private fun createWooPosVariation(
        attributes: List<WooPosVariation.WooPosVariationAttribute> = emptyList(),
        type: WooPosVariation.WooPosVariationType = WooPosVariation.WooPosVariationType.VARIATION
    ): WooPosVariation {
        return WooPosVariation(
            remoteVariationId = 456L,
            remoteProductId = 123L,
            globalUniqueId = "test-global-id",
            type = type,
            price = BigDecimal("19.99"),
            image = null,
            attributes = attributes,
            isVisible = true,
            isDownloadable = false
        )
    }

    @Test
    fun `given variation entity with variation type, when mapping, then type is VARIATION`() {
        // GIVEN
        val entity = createWooPosVariationEntity(type = "variation")

        // WHEN
        val result = sut.fromWooPosVariationEntity(entity)

        // THEN
        assertThat(result.type).isEqualTo(WooPosVariation.WooPosVariationType.VARIATION)
    }

    @Test
    fun `given variation entity with subscription_variation type, when mapping, then type is SUBSCRIPTION_VARIATION`() {
        // GIVEN
        val entity = createWooPosVariationEntity(type = "subscription_variation")

        // WHEN
        val result = sut.fromWooPosVariationEntity(entity)

        // THEN
        assertThat(result.type).isEqualTo(WooPosVariation.WooPosVariationType.SUBSCRIPTION_VARIATION)
    }

    @Test
    fun `given variation entity with unknown type, when mapping, then type is UNKNOWN`() {
        // GIVEN
        val entity = createWooPosVariationEntity(type = "some_unknown_type")

        // WHEN
        val result = sut.fromWooPosVariationEntity(entity)

        // THEN
        assertThat(result.type).isEqualTo(WooPosVariation.WooPosVariationType.UNKNOWN)
    }

    @Test
    fun `given ProductVariation, when mapping, then type defaults to VARIATION`() {
        // GIVEN
        val productVariation = ProductTestUtils.generateProductVariation()

        // WHEN
        val result = sut.fromProductVariation(productVariation)

        // THEN
        assertThat(result.type).isEqualTo(WooPosVariation.WooPosVariationType.VARIATION)
    }

    @Test
    fun `given WCProductVariationModel, when mapping, then type defaults to VARIATION`() {
        // GIVEN
        val wcModel = createWCProductVariationModel()

        // WHEN
        val result = sut.fromWCProductVariationModel(wcModel)

        // THEN
        assertThat(result.type).isEqualTo(WooPosVariation.WooPosVariationType.VARIATION)
    }
}
