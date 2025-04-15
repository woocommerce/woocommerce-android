package com.woocommerce.android.ui.woopos.home.items

import com.woocommerce.android.WooException
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsIndex
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCProductStore
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertFalse

@ExperimentalCoroutinesApi
class WooPosProductsDataSourceTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val sampleProducts = listOf(
        ProductTestUtils.generateProduct(
            productId = 1,
            productName = "Product 1",
            amount = "10.0",
            productType = "simple",
            isDownloadable = false,
        ),
        ProductTestUtils.generateProduct(
            productId = 2,
            productName = "Product 2",
            amount = "20.0",
            productType = "simple",
            isDownloadable = false,
        ).copy(firstImageUrl = "https://test.com"),
        ProductTestUtils.generateProduct(
            productId = 3,
            productName = "Product 3",
            amount = "20.0",
            productType = "simple",
            isDownloadable = false,
        ).copy(firstImageUrl = "https://test.com")
    )

    private val additionalProducts = listOf(
        ProductTestUtils.generateProduct(
            productId = 4,
            productName = "Product 4",
            amount = "10.0",
            productType = "simple",
            isDownloadable = false,
        ),
        ProductTestUtils.generateProduct(
            productId = 5,
            productName = "Product 5",
            amount = "20.0",
            productType = "simple",
            isDownloadable = false,
        ).copy(firstImageUrl = "https://test.com"),
    )

    private val productStore: WCProductStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val productsCache: WooPosProductsCache = mock()
    private val productList: WooPosProductsIndex = mock()
    private val siteModel: SiteModel = mock()
    private val includeTypes = listOf(WCProductStore.IncludeType.Simple, WCProductStore.IncludeType.Variable)

    @Test
    fun `given force refresh, when loadSimpleProducts called, then should clear cache`() = runTest {
        // GIVEN
        whenever(selectedSite.get()).thenReturn(siteModel)
        whenever(productList.getProductList()).thenReturn(emptyList(), sampleProducts)
        whenever(productsCache.getAll()).thenReturn(emptyList(), sampleProducts)
        whenever(
            productStore.fetchProducts(
                site = eq(siteModel),
                offset = any<Int>(),
                pageSize = any<Int>(),
                filterOptions = any<Map<WCProductStore.ProductFilterOption, String>>(),
                includeTypes = eq(includeTypes),
            )
        ).thenReturn(WooResult(listOf<WCProductModel>()))
        val sut = WooPosProductsDataSource(productStore, selectedSite, productsCache)

        // WHEN
        sut.loadProducts(forceRefreshProducts = true).first()

        // THEN
        verify(productsCache).clear()
        verify(productList).clearCache()
    }

    @Test
    fun `given cached products, when loadSimpleProducts called, then should emit cached products first`() = runTest {
        // GIVEN
        whenever(selectedSite.get()).thenReturn(siteModel)
        whenever(productsCache.getAll()).thenReturn(sampleProducts)
        whenever(
            productStore.fetchProducts(
                site = eq(siteModel),
                offset = any(),
                pageSize = any(),
                filterOptions = any(),
                includeTypes = any()
            )
        ).thenReturn(WooResult(listOf<WCProductModel>()))
        whenever(productList.getProductList()).thenReturn(sampleProducts)
        val sut = WooPosProductsDataSource(productStore, selectedSite, productsCache)

        // WHEN
        val result = sut.loadProducts(forceRefreshProducts = false).first()

        // THEN
        assertThat(result).isInstanceOf(WooPosProductsDataSource.ProductsResult.Cached::class.java)
        val cachedResult = result as WooPosProductsDataSource.ProductsResult.Cached
        assertThat(cachedResult.products).containsExactlyElementsOf(sampleProducts)
    }

    @Test
    fun `given no products in list cache, when loadSimpleProducts called, then should return empty list`() = runTest {
        // GIVEN
        whenever(handler.canLoadMore).thenReturn(AtomicBoolean(true))
        whenever(handler.productsFlow).thenReturn(flowOf(sampleProducts))
        whenever(
            handler.loadFromCacheAndFetch(any(), any(), any(), any(), any(), any())
        ).thenReturn(Result.success(Unit))
        whenever(productList.getProductList()).thenReturn(emptyList())
        val sut = WooPosProductsDataSource(handler, productsCache, productList)

        // WHEN
        val result = sut.loadSimpleProducts(forceRefreshProducts = false).first()

        // THEN
        assertThat(result).isInstanceOf(WooPosProductsDataSource.ProductsResult.Cached::class.java)
        val cachedResult = result as WooPosProductsDataSource.ProductsResult.Cached
        assertThat(cachedResult.products).isEmpty()
    }

    @Test
    fun `given cached and remote products, when loadSimpleProducts called, then should emit remote products after cached products`() =
        runTest {
            // GIVEN
            whenever(selectedSite.get()).thenReturn(siteModel)
            whenever(productsCache.getAll()).thenReturn(sampleProducts)
            whenever(productList.getProductList()).thenReturn(sampleProducts)
            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = any(),
                    pageSize = any(),
                    filterOptions = any(),
                    includeTypes = any()
                )
            ).thenReturn(WooResult(listOf<WCProductModel>()))
            val sut = WooPosProductsDataSource(productStore, selectedSite, productsCache)

            // WHEN
            val flow = sut.loadProducts(forceRefreshProducts = false).toList()

            // THEN
            val cachedResult = flow[0] as WooPosProductsDataSource.ProductsResult.Cached
            val remoteResult = flow[1] as WooPosProductsDataSource.ProductsResult.Remote

            assertThat(cachedResult.products).containsExactlyElementsOf(sampleProducts)
            assertThat(remoteResult.productsResult.isSuccess).isTrue()
            assertThat(remoteResult.productsResult.getOrNull()).containsExactlyElementsOf(sampleProducts)
            verify(productsCache).addAll(any())
            verify(productList).storeProductList(sampleProducts.map { it.remoteId })
        }

    @Test
    fun `given error condition when loading products, then cached products are emitted first and error after`() =
        runTest {
            // GIVEN
            whenever(selectedSite.get()).thenReturn(siteModel)
            whenever(productsCache.getAll()).thenReturn(sampleProducts)
            val wooError = WooError(
                WooErrorType.GENERIC_ERROR,
                GenericErrorType.UNKNOWN,
                "Some error message"
            )
            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = any<Int>(),
                    pageSize = any<Int>(),
                    filterOptions = any<Map<WCProductStore.ProductFilterOption, String>>(),
                    includeTypes = eq(includeTypes),
                )
            ).thenReturn(WooResult(wooError))

            val sut = WooPosProductsDataSource(productStore, selectedSite, productsCache, productList)

            // WHEN
            val flow = sut.loadProducts(forceRefreshProducts = false).toList()

            // THEN
            assertThat(flow.size).isEqualTo(2)
            assertThat(flow[0]).isInstanceOf(WooPosProductsDataSource.ProductsResult.Cached::class.java)
            val cachedResult = flow[0] as WooPosProductsDataSource.ProductsResult.Cached
            assertThat(cachedResult.products).isEqualTo(sampleProducts)
            assertThat(flow[1]).isInstanceOf(WooPosProductsDataSource.ProductsResult.Remote::class.java)
        }

    @Test
    fun `given successful loadMore, when loadMore called, then should add products to cache and return them`() =
        runTest {
            // GIVEN
            whenever(selectedSite.get()).thenReturn(siteModel)
            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = any(),
                    pageSize = any(),
                    filterOptions = any(),
                    includeTypes = any()
                )
            ).thenReturn(WooResult(listOf<WCProductModel>()))
            whenever(productList.getProductList()).thenReturn(sampleProducts + additionalProducts)
            val sut = WooPosProductsDataSource(productStore, selectedSite, productsCache, productList)
            sut::class.java.getDeclaredField("canLoadMore").apply {
                isAccessible = true
                set(sut, AtomicBoolean(true))
            }

            // WHEN
            val result = sut.loadMore()

            // THEN
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrNull()).containsExactlyElementsOf(sampleProducts + additionalProducts)
            verify(productsCache).addAll(any())
            verify(productList).storeProductList(additionalProducts.map { it.remoteId })
        }

    @Test
    fun `given failed loadMore, when loadMore called, then should return error and cache remains unchanged`() =
        runTest {
            // GIVEN
            whenever(selectedSite.get()).thenReturn(siteModel)
            val wooError = WooError(
                type = WooErrorType.GENERIC_ERROR,
                original = GenericErrorType.UNKNOWN,
            )
            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = any(),
                    pageSize = any(),
                    filterOptions = any(),
                    includeTypes = any()
                )
            ).thenReturn(WooResult(wooError))

            val sut = WooPosProductsDataSource(productStore, selectedSite, productsCache, productList)
            sut::class.java.getDeclaredField("canLoadMore").apply {
                isAccessible = true
                set(sut, AtomicBoolean(true))
            }

            // WHEN
            val result = sut.loadMore()

            // THEN
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(WooException::class.java)
        }

    @Test
    fun `given no cached products and remote load fails, when loadSimpleProducts called, then should emit empty cache and then error`() =
        runTest {
            // GIVEN
            whenever(selectedSite.get()).thenReturn(siteModel)
            whenever(productList.getProductList()).thenReturn(emptyList())
            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = any(),
                    pageSize = any(),
                    filterOptions = any(),
                    includeTypes = any()
                )
            ).thenReturn(
                WooResult(
                    WooError(
                        type = WooErrorType.GENERIC_ERROR,
                        original = GenericErrorType.UNKNOWN,
                    )
                )
            )

            val sut = WooPosProductsDataSource(productStore, selectedSite, productsCache, productList)

            // WHEN
            val flow = sut.loadProducts(forceRefreshProducts = false).toList()

            // THEN
            val cachedResult = flow[0] as WooPosProductsDataSource.ProductsResult.Cached
            val remoteResult = flow[1] as WooPosProductsDataSource.ProductsResult.Remote

            assertThat(cachedResult.products).isEmpty()
            assertThat(remoteResult.productsResult.isFailure).isTrue()
        }

    @Test
    fun `given empty product list from handler, when loadSimpleProducts called, then should emit empty cache and empty remote result`() =
        runTest {
            // GIVEN
            whenever(selectedSite.get()).thenReturn(siteModel)
            whenever(productList.getProductList()).thenReturn(emptyList())
            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = any(),
                    pageSize = any(),
                    filterOptions = any(),
                    includeTypes = any(),
                )
            ).thenReturn(WooResult(emptyList()))
            val sut = WooPosProductsDataSource(productStore, selectedSite, productsCache, productList)

            // WHEN
            val flow = sut.loadProducts(forceRefreshProducts = false).toList()

            // THEN
            val cachedResult = flow[0] as WooPosProductsDataSource.ProductsResult.Cached
            val remoteResult = flow[1] as WooPosProductsDataSource.ProductsResult.Remote

            assertThat(cachedResult.products).isEmpty()
            assertThat(remoteResult.productsResult.isSuccess).isTrue()
            assertThat(remoteResult.productsResult.getOrNull()).isEmpty()
            verify(productsCache).addAll(any())
            verify(productList).storeProductList(emptyList())
        }

    @Test
    fun `given cached products, when loadSimpleProducts called, then filter in only products that has price`() =
        runTest {
            // GIVEN
            val productsWithoutPrice = listOf(
                ProductTestUtils.generateProduct(
                    productId = 1,
                    productName = "Product 1",
                    amount = "0",
                    productType = "simple",
                    isDownloadable = false,
                ),
                ProductTestUtils.generateProduct(
                    productId = 2,
                    productName = "Product 2",
                    amount = "20.0",
                    productType = "simple",
                    isDownloadable = false
                ).copy(firstImageUrl = "https://test.com")
            )
            whenever(selectedSite.get()).thenReturn(siteModel)
            whenever(productList.getProductList()).thenReturn(productsWithoutPrice.filter { it.remoteId == 2L })
            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = any(),
                    pageSize = any(),
                    filterOptions = any(),
                    includeTypes = any()
                )
            ).thenReturn(WooResult(listOf<WCProductModel>()))
            val sut = WooPosProductsDataSource(productStore, selectedSite, productsCache, productList)

            // WHEN
            val flow = sut.loadProducts(forceRefreshProducts = false).toList()

            // THEN
            val cachedResult = flow[0] as WooPosProductsDataSource.ProductsResult.Cached

            assertFalse(cachedResult.products.any { it.remoteId == 1L })
        }

    @Test
    fun `given cached products, when loadSimpleProducts called, then filter out downloadable products`() =
        runTest {
            // GIVEN
            val mixedProducts = listOf(
                ProductTestUtils.generateProduct(
                    productId = 1,
                    productName = "Product 1",
                    amount = "0",
                    productType = "simple",
                    isDownloadable = true,
                ),
                ProductTestUtils.generateProduct(
                    productId = 2,
                    productName = "Product 2",
                    amount = "20.0",
                    productType = "simple",
                    isVirtual = false,
                    isDownloadable = false
                ).copy(firstImageUrl = "https://test.com")
            )
            whenever(selectedSite.get()).thenReturn(siteModel)
            whenever(productList.getProductList()).thenReturn(mixedProducts.filter { it.remoteId == 2L })
            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = any(),
                    pageSize = any(),
                    filterOptions = any(),
                    includeTypes = any()
                )
            ).thenReturn(WooResult(listOf<WCProductModel>()))
            val sut = WooPosProductsDataSource(productStore, selectedSite, productsCache, productList)

            // WHEN
            val flow = sut.loadProducts(forceRefreshProducts = false).toList()

            // THEN
            val cachedResult = flow[0] as WooPosProductsDataSource.ProductsResult.Cached

            assertFalse(cachedResult.products.any { it.remoteId == 1L })
        }

    @Test
    fun `given remote products, when loadSimpleProducts called, then do not filter out variable products even if price is null `() =
        runTest {
            // GIVEN
            val mixedProducts = listOf(
                ProductTestUtils.generateProduct(
                    productId = 1,
                    productName = "Product 1",
                    amount = "",
                    productType = "variable",
                ),
                ProductTestUtils.generateProduct(
                    productId = 2,
                    productName = "Product 2",
                    amount = "20.0",
                    productType = "simple",
                    isVirtual = false,
                    isDownloadable = false
                ).copy(firstImageUrl = "https://test.com")
            )
            whenever(selectedSite.get()).thenReturn(siteModel)
            whenever(productList.getProductList()).thenReturn(mixedProducts)
            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = any(),
                    pageSize = any(),
                    filterOptions = any(),
                    includeTypes = any()
                )
            ).thenReturn(WooResult(listOf<WCProductModel>()))
            val sut = WooPosProductsDataSource(productStore, selectedSite, productsCache, productList)

            // WHEN
            val flow = sut.loadProducts(forceRefreshProducts = false).toList()

            // THEN
            val remoteResult = flow[1] as WooPosProductsDataSource.ProductsResult.Remote

            assertThat(remoteResult.productsResult.getOrNull()).hasSize(2)
        }

    @Test
    fun `when loading products, they should be sorted by name in ascending order`() = runTest {
        // GIVEN
        val mockProductC = mock<Product>()
        whenever(mockProductC.name).thenReturn("C Product")

        val mockProductA = mock<Product>()
        whenever(mockProductA.name).thenReturn("A Product")

        val mockProductB = mock<Product>()
        whenever(mockProductB.name).thenReturn("B Product")

        val customUnsortedProducts = listOf(mockProductC, mockProductA, mockProductB)

        whenever(selectedSite.get()).thenReturn(siteModel)
        whenever(productList.getProductList()).thenReturn(customUnsortedProducts)
        whenever(
            productStore.fetchProducts(
                site = eq(siteModel),
                offset = any(),
                pageSize = any(),
                filterOptions = any(),
                includeTypes = any()
            )
        ).thenReturn(WooResult(listOf<WCProductModel>()))

        val sut = WooPosProductsDataSource(productStore, selectedSite, productsCache, productList)

        // WHEN
        val result = sut.loadProducts(forceRefreshProducts = false).first()

        // THEN
        assertThat(result).isInstanceOf(WooPosProductsDataSource.ProductsResult.Cached::class.java)
        val cachedResult = result as WooPosProductsDataSource.ProductsResult.Cached

        assertThat(cachedResult.products[0].name).isEqualTo("A Product")
        assertThat(cachedResult.products[1].name).isEqualTo("B Product")
        assertThat(cachedResult.products[2].name).isEqualTo("C Product")
    }

    @Test
    fun `when loading more products, they should be sorted by name in ascending order`() = runTest {
        // GIVEN
        val mockProductD = mock<Product>()
        whenever(mockProductD.name).thenReturn("D Product")

        val mockProductE = mock<Product>()
        whenever(mockProductE.name).thenReturn("E Product")

        val mockProductC = mock<Product>()
        whenever(mockProductC.name).thenReturn("C Product")

        val mockProductA = mock<Product>()
        whenever(mockProductA.name).thenReturn("A Product")

        val mockProductB = mock<Product>()
        whenever(mockProductB.name).thenReturn("B Product")

        val initialProducts = listOf(mockProductC, mockProductA, mockProductB)
        val additionalUnsortedProducts = listOf(mockProductE, mockProductD)
        val allProducts = initialProducts + additionalUnsortedProducts

        whenever(selectedSite.get()).thenReturn(siteModel)
        whenever(
            productStore.fetchProducts(
                site = eq(siteModel),
                offset = any(),
                pageSize = any(),
                filterOptions = any(),
                includeTypes = any()
            )
        ).thenReturn(WooResult(listOf<WCProductModel>()))
        whenever(productList.getProductList()).thenReturn(allProducts)

        val sut = WooPosProductsDataSource(productStore, selectedSite, productsCache, productList)
        sut::class.java.getDeclaredField("canLoadMore").apply {
            isAccessible = true
            set(sut, AtomicBoolean(true))
        }

        // WHEN
        val result = sut.loadMore()

        // THEN
        assertThat(result.isSuccess).isTrue()
        val sortedProducts = result.getOrNull()
        assertThat(sortedProducts).isNotNull

        assertThat(sortedProducts!![0].name).isEqualTo("A Product")
        assertThat(sortedProducts[1].name).isEqualTo("B Product")
        assertThat(sortedProducts[2].name).isEqualTo("C Product")
        assertThat(sortedProducts[3].name).isEqualTo("D Product")
        assertThat(sortedProducts[4].name).isEqualTo("E Product")

        verify(productsCache).addAll(any())
        verify(productList).storeProductList(additionalUnsortedProducts.map { it.remoteId })
    }

    @Test
    fun `given successful fetch, when prepopulateProductsCache called, then should clear cache and add products`() =
        runTest {
            // GIVEN
            whenever(selectedSite.get()).thenReturn(siteModel)
            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = eq(0),
                    pageSize = eq(100),
                    filterOptions = any(),
                    includeTypes = any()
                )
            ).thenReturn(WooResult(emptyList()))
            val sut = WooPosProductsDataSource(productStore, selectedSite, productsCache)

            // WHEN
            val result = sut.prepopulateProductsCache()

            // THEN
            verify(productsCache).clear()
            verify(productsCache).addAll(any())
            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun `given multiple pages of products, when prepopulateProductsCache called, then should fetch all pages up to limit`() =
        runTest {
            // GIVEN
            val firstPageWcProducts = List(100) {
                WCProductModel().apply {
                    attributes = "[]"
                    status = "draft"
                }
            }
            val secondPageWcProducts = List(50) {
                WCProductModel().apply {
                    attributes = "[]"
                    status = "draft"
                }
            }

            whenever(selectedSite.get()).thenReturn(siteModel)
            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = eq(0),
                    pageSize = eq(100),
                    filterOptions = any(),
                    includeTypes = any()
                )
            ).thenReturn(WooResult(firstPageWcProducts))

            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = eq(100),
                    pageSize = eq(100),
                    filterOptions = any(),
                    includeTypes = any()
                )
            ).thenReturn(WooResult(secondPageWcProducts))

            val sut = WooPosProductsDataSource(productStore, selectedSite, productsCache)

            // WHEN
            val result = sut.prepopulateProductsCache()

            // THEN
            verify(productsCache).clear()
            verify(productsCache).addAll(any())
            assertThat(result.isSuccess).isTrue()
            verify(productStore, times(2)).fetchProducts(
                site = any(),
                offset = any(),
                pageSize = any(),
                filterOptions = any(),
                includeTypes = any()
            )
        }

    @Test
    fun `given error when fetching products, when prepopulateProductsCache called, then should return failure`() =
        runTest {
            // GIVEN
            whenever(selectedSite.get()).thenReturn(siteModel)
            val wooError = WooError(
                WooErrorType.GENERIC_ERROR,
                GenericErrorType.UNKNOWN,
                "Failed to fetch products"
            )
            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = any(),
                    pageSize = any(),
                    filterOptions = any(),
                    includeTypes = any()
                )
            ).thenReturn(WooResult(wooError))

            val sut = WooPosProductsDataSource(productStore, selectedSite, productsCache)

            // WHEN
            val result = sut.prepopulateProductsCache()

            // THEN
            verify(productsCache).clear()
            verify(productsCache, never()).addAll(any())
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(WooException::class.java)
        }

    @Test
    fun `given full product list returned but under max pages, when prepopulateProductsCache called, then should stop fetching`() =
        runTest {
            // GIVEN
            val firstPageProducts = List(100) {
                WCProductModel().apply {
                    attributes = "[]"
                    status = "draft"
                }
            }
            val emptySecondPage = emptyList<WCProductModel>()

            whenever(selectedSite.get()).thenReturn(siteModel)
            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = eq(0),
                    pageSize = eq(100),
                    filterOptions = any(),
                    includeTypes = any()
                )
            ).thenReturn(WooResult(firstPageProducts))

            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = eq(100),
                    pageSize = eq(100),
                    filterOptions = any(),
                    includeTypes = any()
                )
            ).thenReturn(WooResult(emptySecondPage))

            val sut = WooPosProductsDataSource(productStore, selectedSite, productsCache)

            // WHEN
            val result = sut.prepopulateProductsCache()

            // THEN
            verify(productsCache).clear()
            verify(productsCache).addAll(any())
            assertThat(result.isSuccess).isTrue()
            verify(productStore, times(2)).fetchProducts(
                site = any(),
                offset = any(),
                pageSize = any(),
                filterOptions = any(),
                includeTypes = any()
            )
        }

    @Test
    fun `given max pages reached, when prepopulateProductsCache called, then should stop fetching and return success`() =
        runTest {
            // GIVEN
            val pageProducts = List(100) {
                WCProductModel().apply {
                    attributes = "[]"
                    status = "draft"
                }
            }

            whenever(selectedSite.get()).thenReturn(siteModel)
            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = any(),
                    pageSize = eq(100),
                    filterOptions = any(),
                    includeTypes = any()
                )
            ).thenReturn(WooResult(pageProducts))

            val sut = WooPosProductsDataSource(productStore, selectedSite, productsCache)

            // WHEN
            val result = sut.prepopulateProductsCache()

            // THEN
            verify(productsCache).clear()
            verify(productsCache).addAll(any())
            assertThat(result.isSuccess).isTrue()
            verify(productStore, times(2)).fetchProducts(
                site = any(),
                offset = any(),
                pageSize = any(),
                filterOptions = any(),
                includeTypes = any()
            )
        }
}
