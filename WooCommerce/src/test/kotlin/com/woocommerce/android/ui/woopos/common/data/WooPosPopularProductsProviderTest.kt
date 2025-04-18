package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting

class WooPosPopularProductsProviderTest {
    private val productStore: WCProductStore = mock()
    private val siteModel: SiteModel = mock()
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(siteModel)
    }
    private val productsCache: WooPosProductsCache = mock()
    private val productsTypesFilterConfig: WooPosProductsTypesFilterConfig = mock {
        on { filters }.thenReturn(emptyMap())
        on { includeTypes }.thenReturn(emptyList())
    }

    private val sampleProducts = listOf(
        WCProductModel().apply {
            remoteProductId = 1
            name = "Product 1"
            price = "10.0"
            attributes = "[]"
            status = "publish"
        },
        WCProductModel().apply {
            remoteProductId = 2
            name = "Product 2"
            price = "20.0"
            attributes = "[]"
            status = "publish"
        },
        WCProductModel().apply {
            remoteProductId = 3
            name = "Product 3"
            price = "30.0"
            attributes = "[]"
            status = "publish"
        }
    )

    @Test
    fun `given successful fetch, when fetchPopularProducts called, then should return success result`() = runTest {
        // GIVEN
        val successResult = WooResult(sampleProducts)

        whenever(
            productStore.fetchProducts(
                site = siteModel,
                offset = 0,
                pageSize = 3,
                filterOptions = emptyMap(),
                includeTypes = emptyList(),
                sortType = ProductSorting.POPULARITY_DESC
            )
        ).thenReturn(successResult)

        val sut = createSut()

        // WHEN
        val result = sut.fetchAndCachePopularProducts()

        // THEN
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `given successful fetch, when getPopularProducts called, then should return cached products`() = runTest {
        // GIVEN
        val successResult = WooResult(sampleProducts)

        whenever(
            productStore.fetchProducts(
                site = siteModel,
                offset = 0,
                pageSize = 3,
                filterOptions = emptyMap(),
                includeTypes = emptyList(),
                sortType = ProductSorting.POPULARITY_DESC
            )
        ).thenReturn(successResult)

        val sut = createSut()

        // WHEN
        sut.fetchAndCachePopularProducts()
        val popularProducts = sut.getPopularProducts()

        // THEN
        assertThat(popularProducts).hasSize(3)
        assertThat(popularProducts[0].remoteId).isEqualTo(1)
        assertThat(popularProducts[1].remoteId).isEqualTo(2)
        assertThat(popularProducts[2].remoteId).isEqualTo(3)
    }

    @Test
    fun `given failed fetch, when fetchPopularProducts called, then should return failure result`() = runTest {
        // GIVEN
        val wooError = WooError(
            type = WooErrorType.GENERIC_ERROR,
            original = GenericErrorType.UNKNOWN,
            message = "Error fetching products"
        )
        val errorResult = WooResult<List<WCProductModel>>(wooError)

        whenever(
            productStore.fetchProducts(
                site = siteModel,
                offset = 0,
                pageSize = 3,
                filterOptions = emptyMap(),
                includeTypes = emptyList(),
                sortType = ProductSorting.POPULARITY_DESC
            )
        ).thenReturn(errorResult)

        val sut = createSut()

        // WHEN
        val result = sut.fetchAndCachePopularProducts()

        // THEN
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Error fetching products")
    }

    @Test
    fun `given successful fetch, when fetchPopularProducts called, then should add products to both caches`() =
        runTest {
            // GIVEN
            val successResult = WooResult(sampleProducts)

            whenever(
                productStore.fetchProducts(
                    site = siteModel,
                    offset = 0,
                    pageSize = 3,
                    filterOptions = emptyMap(),
                    includeTypes = emptyList(),
                    sortType = ProductSorting.POPULARITY_DESC
                )
            ).thenReturn(successResult)

            val sut = createSut()

            // WHEN
            sut.fetchAndCachePopularProducts()

            // THEN
            verify(productsCache).addAll(any())
        }

    @Test
    fun `given empty result, when fetchPopularProducts called, then should return empty list from getPopularProducts`() =
        runTest {
            // GIVEN
            val emptyResult = WooResult(emptyList<WCProductModel>())

            whenever(
                productStore.fetchProducts(
                    site = siteModel,
                    offset = 0,
                    pageSize = 3,
                    filterOptions = emptyMap(),
                    includeTypes = emptyList(),
                    sortType = ProductSorting.POPULARITY_DESC
                )
            ).thenReturn(emptyResult)

            val sut = createSut()

            // WHEN
            sut.fetchAndCachePopularProducts()
            val popularProducts = sut.getPopularProducts()

            // THEN
            assertThat(popularProducts).isEmpty()
        }

    private fun createSut() = WooPosPopularProductsProvider(
        selectedSite = selectedSite,
        productStore = productStore,
        productsCache = productsCache,
        productsTypesFilterConfig = productsTypesFilterConfig
    )
}
