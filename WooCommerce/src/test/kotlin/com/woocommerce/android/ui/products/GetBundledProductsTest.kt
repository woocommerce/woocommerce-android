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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
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

    @Test
    fun `given bundled products, when the products are processed, then the product type is resolved`() =
        testBlocking {
            // given
            val remoteProductId = 5L
            whenever(productStore.observeBundledProducts(any(), eq(remoteProductId))).doReturn(flowOf(bundledProducts))
            whenever(productStore.getProductsByRemoteIds(any(), any())).doReturn(
                listOf(
                    WCProductModel().copy(remoteId = RemoteId(25), type = "variable"),
                    WCProductModel().copy(remoteId = RemoteId(26), type = "variable-subscription"),
                    WCProductModel().copy(remoteId = RemoteId(27), type = "simple")
                )
            )

            // when
            val result = sut.invoke(remoteProductId).first().associateBy { it.id }

            // then
            assertThat(result.getValue(1).productType).isEqualTo(ProductType.VARIABLE)
            assertThat(result.getValue(2).productType).isEqualTo(ProductType.VARIABLE_SUBSCRIPTION)
            assertThat(result.getValue(3).productType).isEqualTo(ProductType.SIMPLE)

            assertThat(result.getValue(1).isVariable).isTrue
            assertThat(result.getValue(2).isVariable).isFalse
            assertThat(result.getValue(3).isVariable).isFalse
        }

    @Test
    fun `given products missing from the cache, when the products are processed, then the missing ones are fetched`() =
        testBlocking {
            // given
            val remoteProductId = 5L
            val missingProduct = WCProductModel().copy(remoteId = RemoteId(27), type = "variable")
            whenever(productStore.observeBundledProducts(any(), eq(remoteProductId))).doReturn(flowOf(bundledProducts))
            whenever(productStore.getProductsByRemoteIds(any(), any())).doReturn(products)
            whenever(productStore.fetchProductListSynced(any(), any())).doReturn(listOf(missingProduct))

            // when
            val result = sut.invoke(remoteProductId).first().associateBy { it.id }

            // then
            verify(productStore).fetchProductListSynced(any(), eq(listOf(27L)))
            assertThat(result.getValue(3).isVariable).isTrue
        }

    @Test
    fun `given more missing products than fit in a page, when they are processed, then every one is fetched`() =
        testBlocking {
            // given
            val remoteProductId = 5L
            val manyBundledProducts = generateBundledProducts(WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE + 1)
            whenever(productStore.observeBundledProducts(any(), eq(remoteProductId)))
                .doReturn(flowOf(manyBundledProducts))
            whenever(productStore.getProductsByRemoteIds(any(), any())).doReturn(emptyList())

            // when
            sut.invoke(remoteProductId).first()

            // then
            val requestedIds = argumentCaptor<List<Long>>()
            verify(productStore, times(2)).fetchProductListSynced(any(), requestedIds.capture())
            assertThat(requestedIds.allValues.flatten())
                .containsExactlyElementsOf(manyBundledProducts.map { it.bundledProductId })
        }

    private fun generateBundledProducts(count: Int) = List(count) { index ->
        WCBundledProduct(
            id = index.toLong(),
            bundledProductId = 100L + index,
            menuOrder = index,
            title = "Bundled product $index",
            stockStatus = "in_stock",
            quantityMin = null,
            quantityMax = null,
            quantityDefault = null,
            isOptional = false,
            attributesDefault = null,
            variationIds = null
        )
    }

    @Test
    fun `given every product is cached, when the products are processed, then no product is fetched`() = testBlocking {
        // given
        val remoteProductId = 5L
        whenever(productStore.observeBundledProducts(any(), eq(remoteProductId))).doReturn(flowOf(bundledProducts))
        whenever(productStore.getProductsByRemoteIds(any(), any())).doReturn(
            products + WCProductModel().copy(remoteId = RemoteId(27))
        )

        // when
        sut.invoke(remoteProductId).first()

        // then
        verify(productStore, never()).fetchProductListSynced(any(), any())
    }

    private val products = listOf(
        WCProductModel().copy(
            remoteId = RemoteId(25),
            sku = "bundled_product_with_image",
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
        ),
        WCProductModel().copy(
            remoteId = RemoteId(26),
            sku = "bundled_product_no_image"
        )
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
