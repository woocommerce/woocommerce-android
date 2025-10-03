package com.woocommerce.android.ui.woopos.home.items

import com.woocommerce.android.WooException
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsTypesFilterConfig
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel.WooPosPricing
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel.WooPosProductImage
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel.WooPosProductStatus
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel.WooPosProductType
import com.woocommerce.android.ui.woopos.common.data.models.WooPosWCProductToWooPosProductModelMapper
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsIndex
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.generateWooPosProduct
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
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCProductStore
import kotlin.test.Test

@ExperimentalCoroutinesApi
class WooPosProductsDataSourceTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val sampleProducts = listOf(
        generateProduct(
            productId = 1,
            productName = "Product 1",
            amount = "10.0",
            productType = WooPosProductType.SIMPLE,
            isDownloadable = false,
        ),
        generateProduct(
            productId = 2,
            productName = "Product 2",
            amount = "20.0",
            productType = WooPosProductType.SIMPLE,
            isDownloadable = false,
            images = listOf(WooPosProductImage(id = 1, url = "https://test.com", name = "", alt = "")),
        ),
        generateProduct(
            productId = 3,
            productName = "Product 3",
            amount = "20.0",
            productType = WooPosProductType.SIMPLE,
            isDownloadable = false,
            images = listOf(WooPosProductImage(id = 1, url = "https://test.com", name = "", alt = "")),
        )
    )

    private val additionalProducts = listOf(
        generateProduct(
            productId = 4,
            productName = "Product 4",
            amount = "10.0",
            productType = WooPosProductType.SIMPLE,
            isDownloadable = false,
        ),
        generateProduct(
            productId = 5,
            productName = "Product 5",
            amount = "20.0",
            productType = WooPosProductType.SIMPLE,
            isDownloadable = false,
            images = listOf(WooPosProductImage(id = 1, url = "https://test.com", name = "", alt = "")),
        )
    )

    private val productStore: WCProductStore = mock()
    private val siteModel: SiteModel = mock()
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(siteModel)
    }
    private val productsCache: WooPosProductsCache = mock {
        onBlocking { getAll() }.thenReturn(sampleProducts)
    }
    private val productsIndex: WooPosProductsIndex = mock()
    private val productsTypesFilterConfig = WooPosProductsTypesFilterConfig()
    private val productMapper: WooPosWCProductToWooPosProductModelMapper = mock()

    @Test
    fun `given cached products, when fetchFirstPage called, then should emit cached products first`() = runTest {
        // GIVEN
        whenever(productsCache.getAll()).thenReturn(sampleProducts)
        whenever(
            productStore.fetchProducts(
                site = eq(siteModel),
                offset = any(),
                pageSize = any(),
                sortType = any(),
                filterOptions = any(),
                includeTypes = any()
            )
        ).thenReturn(WooResult(listOf()))
        whenever(productsIndex.getProductList()).thenReturn(sampleProducts)
        val sut = WooPosProductsDataSource(
            productStore,
            selectedSite,
            productsCache,
            productsIndex,
            productsTypesFilterConfig,
            productMapper,
        )

        // WHEN
        val result = sut.fetchFirstPage(forceRefresh = false).first()

        // THEN
        assertThat(result).isInstanceOf(WooPosProductsDataSource.ProductsResult.Cached::class.java)
        val cachedResult = result as WooPosProductsDataSource.ProductsResult.Cached
        assertThat(cachedResult.products).containsExactlyElementsOf(sampleProducts)
    }

    @Test
    fun `given cached products, when fetchFirstPage called with forceRefresh, then should not emit cached products`() =
        runTest {
            // GIVEN
            whenever(productsCache.getAll()).thenReturn(sampleProducts)
            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = any(),
                    pageSize = any(),
                    sortType = any(),
                    filterOptions = any(),
                    includeTypes = any()
                )
            ).thenReturn(WooResult(listOf<WCProductModel>()))
            whenever(productsIndex.getProductList()).thenReturn(sampleProducts)
            val sut = WooPosProductsDataSource(
                productStore,
                selectedSite,
                productsCache,
                productsIndex,
                productsTypesFilterConfig,
                productMapper,
            )

            // WHEN
            val result = sut.fetchFirstPage(forceRefresh = true).first()

            // THEN
            assertThat(result).isInstanceOf(WooPosProductsDataSource.ProductsResult.Remote::class.java)
        }

    @Test
    fun `given no products in list cache, when fetchFirstPage called, then should return empty list`() = runTest {
        // GIVEN
        whenever(productsCache.getAll()).thenReturn(emptyList())
        whenever(
            productStore.fetchProducts(
                site = eq(siteModel),
                offset = any(),
                pageSize = any(),
                sortType = any(),
                filterOptions = any(),
                includeTypes = any()
            )
        ).thenReturn(WooResult(listOf()))
        whenever(productsIndex.getProductList()).thenReturn(emptyList())
        val sut = WooPosProductsDataSource(
            productStore,
            selectedSite,
            productsCache,
            productsIndex,
            productsTypesFilterConfig,
            productMapper,
        )

        // WHEN
        val result = sut.fetchFirstPage(forceRefresh = false).first()

        // THEN
        assertThat(result).isInstanceOf(WooPosProductsDataSource.ProductsResult.Cached::class.java)
        val cachedResult = result as WooPosProductsDataSource.ProductsResult.Cached
        assertThat(cachedResult.products).isEmpty()
    }

    @Test
    fun `given cached and remote products, when fetchFirstPage called, then should emit remote products after cached products`() =
        runTest {
            // GIVEN
            whenever(productsCache.getAll()).thenReturn(sampleProducts)
            whenever(productsIndex.getProductList()).thenReturn(sampleProducts)
            whenever(productMapper.map(any())).thenReturn(generateWooPosProduct())
            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = any(),
                    pageSize = any(),
                    sortType = any(),
                    filterOptions = any(),
                    includeTypes = any()
                )
            ).thenReturn(
                WooResult(
                    listOf(
                        WCProductModel().copy(
                            remoteId = RemoteId(1),
                            attributes = "[]",
                            status = "draft",
                        ),
                        WCProductModel().copy(
                            remoteId = RemoteId(2),
                            attributes = "[]",
                            status = "draft",
                        ),
                        WCProductModel().copy(
                            remoteId = RemoteId(3),
                            attributes = "[]",
                            status = "draft",
                        )
                    )
                )
            )
            val sut = WooPosProductsDataSource(
                productStore,
                selectedSite,
                productsCache,
                productsIndex,
                productsTypesFilterConfig,
                productMapper,
            )

            // WHEN
            val flow = sut.fetchFirstPage(forceRefresh = false).toList()

            // THEN
            val cachedResult = flow[0] as WooPosProductsDataSource.ProductsResult.Cached
            val remoteResult = flow[1] as WooPosProductsDataSource.ProductsResult.Remote

            assertThat(cachedResult.products.size).isEqualTo(3)
            assertThat(remoteResult.productsResult.isSuccess).isTrue()
            assertThat(remoteResult.productsResult.getOrNull()!!.size).isEqualTo(3)
            verify(productsCache).addAll(any())
        }

    @Test
    fun `given error, when loading products, then cached products are emitted first and error after`() =
        runTest {
            // GIVEN
            whenever(productsIndex.getProductList()).thenReturn(sampleProducts)
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
                    sortType = any(),
                    filterOptions = any<Map<WCProductStore.ProductFilterOption, String>>(),
                    includeTypes = eq(productsTypesFilterConfig.includeTypes),
                )
            ).thenReturn(WooResult(wooError))
            whenever(productsCache.getAll()).thenReturn(sampleProducts)

            val sut = WooPosProductsDataSource(
                productStore,
                selectedSite,
                productsCache,
                productsIndex,
                productsTypesFilterConfig,
                productMapper,
            )

            // WHEN
            val flow = sut.fetchFirstPage(forceRefresh = false).toList()

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
            whenever(productMapper.map(any())).thenReturn(generateWooPosProduct())
            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = any(),
                    pageSize = any(),
                    sortType = any(),
                    filterOptions = any(),
                    includeTypes = any()
                )
            ).thenReturn(
                WooResult(
                    List(25) {
                        WCProductModel().copy(
                            remoteId = RemoteId(it.toLong()),
                            attributes = "[]",
                            status = "draft",
                        )
                    }
                )
            )
            whenever(productsIndex.getProductList()).thenReturn(sampleProducts + additionalProducts)
            val sut = WooPosProductsDataSource(
                productStore,
                selectedSite,
                productsCache,
                productsIndex,
                productsTypesFilterConfig,
                productMapper,
            )
            sut.fetchFirstPage(forceRefresh = true).first()

            // WHEN
            val result = sut.loadMore()

            // THEN
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrNull()).containsExactlyElementsOf(sampleProducts + additionalProducts)
        }

    @Test
    fun `given failed loadMore, when loadMore called, then should return error and cache remains unchanged`() =
        runTest {
            // GIVEN
            whenever(productMapper.map(any())).thenReturn(generateWooPosProduct())
            whenever(productsIndex.getProductList())
                .thenReturn(
                    List(25) {
                        generateProduct(
                            productId = it.toLong(),
                            productName = "Product $it",
                            amount = "0",
                            productType = WooPosProductType.SIMPLE,
                            isDownloadable = false,
                        )
                    }
                )
            val wooError = WooError(
                type = WooErrorType.GENERIC_ERROR,
                original = GenericErrorType.UNKNOWN,
            )
            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = any(),
                    pageSize = any(),
                    sortType = any(),
                    filterOptions = any(),
                    includeTypes = any()
                )
            ).thenReturn(
                WooResult(
                    List(25) {
                        WCProductModel().copy(
                            remoteId = RemoteId(it.toLong()),
                            attributes = "[]",
                            status = "draft",
                        )
                    }
                ),
                WooResult(wooError)
            )

            val sut = WooPosProductsDataSource(
                productStore,
                selectedSite,
                productsCache,
                productsIndex,
                productsTypesFilterConfig,
                productMapper,
            )
            sut.fetchFirstPage(forceRefresh = true).first()

            // WHEN
            val result = sut.loadMore()

            // THEN
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(WooException::class.java)
        }

    @Test
    fun `given no cached products and remote load fails, when fetchFirstPage called, then should emit empty cache and then error`() =
        runTest {
            // GIVEN
            whenever(productsIndex.getProductList()).thenReturn(emptyList())
            whenever(productsCache.getAll()).thenReturn(emptyList())
            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = any(),
                    pageSize = any(),
                    sortType = any(),
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

            val sut = WooPosProductsDataSource(
                productStore,
                selectedSite,
                productsCache,
                productsIndex,
                productsTypesFilterConfig,
                productMapper,
            )

            // WHEN
            val flow = sut.fetchFirstPage(forceRefresh = false).toList()

            // THEN
            val cachedResult = flow[0] as WooPosProductsDataSource.ProductsResult.Cached
            val remoteResult = flow[1] as WooPosProductsDataSource.ProductsResult.Remote

            assertThat(cachedResult.products).isEmpty()
            assertThat(remoteResult.productsResult.isFailure).isTrue()
        }

    @Test
    fun `given empty product list in cache, when fetchFirstPage called, then should emit empty cache and empty remote result`() =
        runTest {
            // GIVEN
            whenever(productsCache.getAll()).thenReturn(emptyList())
            whenever(productsIndex.getProductList()).thenReturn(emptyList())
            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = any(),
                    pageSize = any(),
                    sortType = any(),
                    filterOptions = any(),
                    includeTypes = any(),
                )
            ).thenReturn(WooResult(emptyList()))
            val sut = WooPosProductsDataSource(
                productStore,
                selectedSite,
                productsCache,
                productsIndex,
                productsTypesFilterConfig,
                productMapper,
            )

            // WHEN
            val flow = sut.fetchFirstPage(forceRefresh = false).toList()

            // THEN
            val cachedResult = flow[0] as WooPosProductsDataSource.ProductsResult.Cached
            val remoteResult = flow[1] as WooPosProductsDataSource.ProductsResult.Remote

            assertThat(cachedResult.products).isEmpty()
            assertThat(remoteResult.productsResult.isSuccess).isTrue()
            assertThat(remoteResult.productsResult.getOrNull()).isEmpty()
            verify(productsCache).addAll(any())
            verify(productsIndex).storeProductList(emptyList())
        }

    @Test
    fun `when loading products, they should be sorted by name in ascending order`() = runTest {
        // GIVEN
        val mockProductC = generateProduct(productName = "C Product", productId = 3L)

        val mockProductA = generateProduct(productName = "A Product", productId = 1L)

        val mockProductB = generateProduct(productName = "B Product", productId = 2L)
        val mockProductab = generateProduct(productName = "ab Product", productId = 2L)

        val customUnsortedProducts = listOf(mockProductC, mockProductA, mockProductB, mockProductab)
        val sortedProducts = listOf(mockProductA, mockProductB, mockProductC)

        whenever(productsCache.getAll()).thenReturn(customUnsortedProducts)
        whenever(productsIndex.getProductList()).thenReturn(sortedProducts)
        whenever(
            productStore.fetchProducts(
                site = eq(siteModel),
                offset = any(),
                pageSize = any(),
                sortType = any(),
                filterOptions = any(),
                includeTypes = any()
            )
        ).thenReturn(WooResult(listOf()))

        val sut = WooPosProductsDataSource(
            productStore,
            selectedSite,
            productsCache,
            productsIndex,
            productsTypesFilterConfig,
            productMapper,
        )

        // WHEN
        val result = sut.fetchFirstPage(forceRefresh = false).first()

        // THEN
        assertThat(result).isInstanceOf(WooPosProductsDataSource.ProductsResult.Cached::class.java)
        val cachedResult = result as WooPosProductsDataSource.ProductsResult.Cached

        assertThat(cachedResult.products[0].name).isEqualTo("A Product")
        assertThat(cachedResult.products[1].name).isEqualTo("ab Product")
        assertThat(cachedResult.products[2].name).isEqualTo("B Product")
        assertThat(cachedResult.products[3].name).isEqualTo("C Product")
    }

    @Test
    fun `given successful fetch on both pages, when prepopulateProductsCache called, then add all products`() =
        runTest {
            // GIVEN
            val firstPageProducts = List(100) {
                WCProductModel().copy(
                    remoteId = RemoteId(it.toLong()),
                    attributes = "[]",
                    status = "draft"
                )
            }

            val secondPageProducts = List(100) {
                WCProductModel().copy(
                    remoteId = RemoteId((it + 100).toLong()),
                    attributes = "[]",
                    status = "draft"
                )
            }

            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = eq(0),
                    pageSize = eq(100),
                    sortType = any(),
                    filterOptions = any(),
                    includeTypes = any()
                )
            ).thenReturn(WooResult(firstPageProducts))

            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = eq(100),
                    pageSize = eq(100),
                    sortType = any(),
                    filterOptions = any(),
                    includeTypes = any()
                )
            ).thenReturn(WooResult(secondPageProducts))

            val sut = WooPosProductsDataSource(
                productStore,
                selectedSite,
                productsCache,
                productsIndex,
                productsTypesFilterConfig,
                productMapper,
            )

            // WHEN
            val result = sut.prepopulateProductsCache()

            // THEN
            verify(productsCache).clear()
            verify(productsCache).addAll(any())
            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun `given first fetch success but second fetch fails, when prepopulateProductsCache called, then should add first page products`() =
        runTest {
            // GIVEN
            val firstPageProducts = List(100) {
                WCProductModel().copy(
                    remoteId = RemoteId(it.toLong()),
                    attributes = "[]",
                    status = "draft"
                )
            }

            val wooError = WooError(
                WooErrorType.GENERIC_ERROR,
                GenericErrorType.UNKNOWN,
                "Failed to fetch products on second page"
            )

            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = eq(0),
                    pageSize = eq(100),
                    sortType = any(),
                    filterOptions = any(),
                    includeTypes = any()
                )
            ).thenReturn(WooResult(firstPageProducts))

            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = eq(100),
                    pageSize = eq(100),
                    sortType = any(),
                    filterOptions = any(),
                    includeTypes = any()
                )
            ).thenReturn(WooResult(wooError))

            val sut = WooPosProductsDataSource(
                productStore,
                selectedSite,
                productsCache,
                productsIndex,
                productsTypesFilterConfig,
                productMapper,
            )

            // WHEN
            val result = sut.prepopulateProductsCache()

            // THEN
            verify(productsCache).clear()
            verify(productsCache).addAll(any())
            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun `given first fetch fails, when prepopulateProductsCache called, then should return failure`() = runTest {
        // GIVEN
        val wooError = WooError(
            WooErrorType.GENERIC_ERROR,
            GenericErrorType.UNKNOWN,
            "Failed to fetch products on first page"
        )

        whenever(
            productStore.fetchProducts(
                site = eq(siteModel),
                offset = eq(0),
                pageSize = eq(100),
                sortType = any(),
                filterOptions = any(),
                includeTypes = any()
            )
        ).thenReturn(WooResult(wooError))

        val sut = WooPosProductsDataSource(
            productStore,
            selectedSite,
            productsCache,
            productsIndex,
            productsTypesFilterConfig,
            productMapper,
        )

        // WHEN
        val result = sut.prepopulateProductsCache()

        // THEN
        verify(productsCache).clear()
        verify(productsCache, never()).addAll(any())
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(WooException::class.java)
    }

    @Test
    fun `given both pages return products, when prepopulateProductsCache called, then should add all products to cache`() =
        runTest {
            // GIVEN
            val firstPageProducts = List(100) {
                WCProductModel().copy(
                    remoteId = RemoteId(it.toLong()),
                    attributes = "[]",
                    status = "draft"
                )
            }

            val secondPageProducts = List(50) {
                WCProductModel().copy(
                    remoteId = RemoteId((it + 100).toLong()),
                    attributes = "[]",
                    status = "draft"
                )
            }

            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = eq(0),
                    pageSize = eq(100),
                    sortType = any(),
                    filterOptions = any(),
                    includeTypes = any()
                )
            ).thenReturn(WooResult(firstPageProducts))

            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = eq(100),
                    pageSize = eq(100),
                    sortType = any(),
                    filterOptions = any(),
                    includeTypes = any()
                )
            ).thenReturn(WooResult(secondPageProducts))

            val sut = WooPosProductsDataSource(
                productStore,
                selectedSite,
                productsCache,
                productsIndex,
                productsTypesFilterConfig,
                productMapper,
            )

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
                sortType = any(),
                filterOptions = any(),
                includeTypes = any()
            )
        }

    private fun generateProduct(
        productId: Long = 1,
        productName: String = "Product 1",
        amount: String = "10.0",
        productType: WooPosProductType = WooPosProductType.SIMPLE,
        isDownloadable: Boolean = false,
        images: List<WooPosProductImage> = emptyList()
    ) = WooPosProductModel(
        remoteId = productId,
        name = productName,
        pricing = WooPosPricing.RegularPricing(amount.toBigDecimal()),
        type = productType,
        isDownloadable = isDownloadable,
        parentId = null,
        sku = "",
        globalUniqueId = "",
        status = WooPosProductStatus.PUBLISH,
        description = "",
        shortDescription = "",
        lastModified = "",
        images = images,
    )
}
