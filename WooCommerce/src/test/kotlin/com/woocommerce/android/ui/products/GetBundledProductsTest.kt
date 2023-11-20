package com.woocommerce.android.ui.products

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCBundledProduct
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.store.WCProductStore

@OptIn(ExperimentalCoroutinesApi::class)
class GetBundledProductsTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn SiteModel()
    }
    private val productStore: WCProductStore = mock()

    lateinit var sut: GetBundledProducts

    @Before
    fun setUp() {
        sut = GetBundledProducts(
            selectedSite,
            productStore,
            coroutinesTestRule.testDispatchers
        )
    }

    @Test
    fun `should process bundled products successfully`() = testBlocking {
        // given
        val remoteProductId = 5L
        whenever(productStore.observeBundledProducts(any(), eq(remoteProductId))).doReturn(flowOf(bundledProducts))
        whenever(productStore.getProductsByRemoteIds(any(), any())).doReturn(products)

        // when
        val result = sut.invoke(remoteProductId).first()

        // then
        assertThat(result.size).isEqualTo(bundledProducts.size)
        val resultWithIndex = result.associateBy { bundledProduct -> bundledProduct.id }

        val bundleWithImageAndSKU = resultWithIndex.getValue(1)
        assertThat(bundleWithImageAndSKU.imageUrl).isNotEmpty
        assertThat(bundleWithImageAndSKU.sku).isNotEmpty
        assertThat(bundleWithImageAndSKU.stockStatus).isEqualTo(ProductStockStatus.InStock)

        val bundleWithSKU = resultWithIndex.getValue(2)
        assertThat(bundleWithSKU.imageUrl).isNull()
        assertThat(bundleWithSKU.sku).isNotEmpty
        assertThat(bundleWithSKU.stockStatus).isEqualTo(ProductStockStatus.OutOfStock)

        val bundleWithoutImageOrSKU = resultWithIndex.getValue(3)
        assertThat(bundleWithoutImageOrSKU.imageUrl).isNull()
        assertThat(bundleWithoutImageOrSKU.sku).isNull()
        assertThat(bundleWithoutImageOrSKU.stockStatus).isEqualTo(ProductStockStatus.InStock)
    }

    @Test
    fun `when there is no cached info about the products, then display the bundled info without image or SKU`() =
        testBlocking {
            // given
            val remoteProductId = 5L
            whenever(productStore.observeBundledProducts(any(), eq(remoteProductId))).doReturn(flowOf(bundledProducts))
            whenever(productStore.getProductsByRemoteIds(any(), any())).doReturn(emptyList())

            // when
            val result = sut.invoke(remoteProductId).first()

            // then
            assertThat(result.size).isEqualTo(bundledProducts.size)
            val resultWithIndex = result.associateBy { bundledProduct -> bundledProduct.id }

            val bundleWithImageAndSKU = resultWithIndex.getValue(1)
            assertThat(bundleWithImageAndSKU.imageUrl).isNull()
            assertThat(bundleWithImageAndSKU.sku).isNull()
            assertThat(bundleWithImageAndSKU.stockStatus).isEqualTo(ProductStockStatus.InStock)

            val bundleWithSKU = resultWithIndex.getValue(2)
            assertThat(bundleWithSKU.imageUrl).isNull()
            assertThat(bundleWithSKU.sku).isNull()
            assertThat(bundleWithSKU.stockStatus).isEqualTo(ProductStockStatus.OutOfStock)

            val bundleWithoutImageOrSKU = resultWithIndex.getValue(3)
            assertThat(bundleWithoutImageOrSKU.imageUrl).isNull()
            assertThat(bundleWithoutImageOrSKU.sku).isNull()
            assertThat(bundleWithoutImageOrSKU.stockStatus).isEqualTo(ProductStockStatus.InStock)
        }

    private val products = listOf(
        WCProductModel().apply {
            remoteProductId = 25
            sku = "bundled_product_with_image"
            images = "[{\n" +
                "  \"id\": 60,\n" +
                "  \"date_created\": \"2023-03-30T07:29:35\",\n" +
                "  \"date_created_gmt\": \"2023-03-30T19:29:35\",\n" +
                "  \"date_modified\": \"2023-03-30T07:29:35\",\n" +
                "  \"date_modified_gmt\": \"2023-03-30T19:29:35\",\n" +
                "  \"src\": \"https://woo-dutifully-impossible-collector/sample.png\",\n" +
                "  \"name\": \"Placeholder Image\",\n" +
                "  \"alt\": \"\"\n" +
                "}]"
        },
        WCProductModel().apply {
            remoteProductId = 26
            sku = "bundled_product_no_image"
        }
    )

    private val bundledProducts = listOf(
        WCBundledProduct(
            id = 1,
            bundledProductId = 25,
            menuOrder = 1,
            title = "Bundled product",
            stockStatus = "in_stock",
            quantityMin = null,
            quantityMax = null,
            quantityDefault = null,
            isOptional = false,
            attributesDefault = null,
            variationIds = null
        ),
        WCBundledProduct(
            id = 2,
            bundledProductId = 26,
            menuOrder = 2,
            title = "Another bundled product",
            stockStatus = "out_of_stock",
            quantityMin = null,
            quantityMax = null,
            quantityDefault = null,
            isOptional = false,
            attributesDefault = null,
            variationIds = null
        ),
        WCBundledProduct(
            id = 3,
            bundledProductId = 27,
            menuOrder = 3,
            title = "Awesome bundled product",
            stockStatus = "in_stock____",
            quantityMin = null,
            quantityMax = null,
            quantityDefault = null,
            isOptional = false,
            attributesDefault = null,
            variationIds = null
        )
    )
}
