package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.JsonLoaderUtils.jsonFileAs
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemotePriceType.FlatFee
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteRestrictionsType.AnyText
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto.RemoteType.Checkbox
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.model.metadata.get
import kotlin.test.fail

class ProductDtoMapperTest {
    private val productDtoMapper = ProductDtoMapper(
        stripProductMetaData = mock {
            on { invoke(any()) } doAnswer {
                @Suppress("UNCHECKED_CAST")
                it.arguments[0] as List<WCMetaData>
            }
        }
    )

    @Test
    fun `Product addons should be serialized correctly`() {
        val productMetaData =
            "wc/product-with-addons.json"
                .jsonFileAs(ProductApiResponse::class.java)
                ?.let { productDtoMapper.mapToModel(LocalOrRemoteId.LocalId(0), it) }
                ?.metaData

        val addons = productMetaData?.get(WCMetaData.AddOnsMetadataKeys.ADDONS_METADATA_KEY)
            ?.let { RemoteAddonDto.fromMetaDataValue(it.value) }

        assertThat(addons).isNotNull
        assertThat(addons).isNotEmpty
        assertThat(addons?.size).isEqualTo(3)
    }

    @Test
    fun `Product addons should be serialized with enum values correctly`() {
        val productMetaData =
            "wc/product-with-addons.json"
                .jsonFileAs(ProductApiResponse::class.java)
                ?.let { productDtoMapper.mapToModel(LocalOrRemoteId.LocalId(0), it) }
                ?.metaData

        val addons = productMetaData?.get(WCMetaData.AddOnsMetadataKeys.ADDONS_METADATA_KEY)
            ?.let { RemoteAddonDto.fromMetaDataValue(it.value) }

        assertThat(addons).isNotEmpty

        addons?.first()?.let {
            assertThat(it.priceType).isEqualTo(FlatFee)
            assertThat(it.restrictionsType).isEqualTo(AnyText)
            assertThat(it.type).isEqualTo(Checkbox)
        } ?: fail("Addons list shouldn't be empty")
    }

    @Test
    fun `Product addons should contain Addon options serialized correctly`() {
        val addonOptions = "wc/product-with-addons.json"
            .jsonFileAs(ProductApiResponse::class.java)
            ?.let { productDtoMapper.mapToModel(LocalOrRemoteId.LocalId(0), it) }
            ?.metaData
            ?.get(WCMetaData.AddOnsMetadataKeys.ADDONS_METADATA_KEY)
            ?.let { RemoteAddonDto.fromMetaDataValue(it.value) }
            ?.takeIf { it.isNotEmpty() }
            ?.first()
            ?.options

        assertThat(addonOptions).isNotNull
        assertThat(addonOptions).isNotEmpty
    }

    @Test
    fun `Bundled product with max size is serialized correctly`() {
        val product = "wc/product-bundle-with-max-quantity.json"
            .jsonFileAs(ProductApiResponse::class.java)
            ?.let { productDtoMapper.mapToModel(LocalOrRemoteId.LocalId(0), it) }
            ?.product

        assertThat(product?.bundleMaxSize).isEqualTo(5f)
    }

    @Test
    fun `Bundled product with min size is serialized correctly`() {
        val product = "wc/product-bundle-with-min-quantity.json"
            .jsonFileAs(ProductApiResponse::class.java)
            ?.let { productDtoMapper.mapToModel(LocalOrRemoteId.LocalId(0), it) }
            ?.product

        assertThat(product?.bundleMinSize).isEqualTo(5f)
    }

    @Test
    fun `Bundled product with quantity rules is serialized correctly`() {
        val product = "wc/product-bundle-with-quantity-rules.json"
            .jsonFileAs(ProductApiResponse::class.java)
            ?.let { productDtoMapper.mapToModel(LocalOrRemoteId.LocalId(0), it) }
            ?.product

        assertThat(product?.bundleMinSize).isEqualTo(5f)
        assertThat(product?.bundleMaxSize).isEqualTo(5f)
    }

    @Test
    fun `Bundled product with special stock status is serialized correctly`() {
        val product = "wc/product-bundle-with-max-quantity.json"
            .jsonFileAs(ProductApiResponse::class.java)
            ?.let { productDtoMapper.mapToModel(LocalOrRemoteId.LocalId(0), it) }
            ?.product

        assertThat(product?.specialStockStatus).isEqualTo("insufficientstock")
    }
}
