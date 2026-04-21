package com.woocommerce.android.ui.woopos.home.items

import com.woocommerce.android.WooException
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.variations.selector.VariationListHandler
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsTypesFilterConfig
import com.woocommerce.android.ui.woopos.common.data.WooPosVariation
import com.woocommerce.android.ui.woopos.common.data.WooPosVariationMapper
import com.woocommerce.android.ui.woopos.common.data.WooPosVariationsTypesFilterConfig
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel.WooPosPricing
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel.WooPosProductImage
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel.WooPosProductStatus
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel.WooPosProductType
import com.woocommerce.android.ui.woopos.common.data.models.WooPosWCProductToWooPosProductModelMapper
import com.woocommerce.android.ui.woopos.common.data.toWooPosVariation
import com.woocommerce.android.ui.woopos.home.items.products.SearchProductsResult
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsIndex
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsRemoteDataSource
import com.woocommerce.android.ui.woopos.home.items.search.WooPosSearchProductsDataSource
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsLRUCache
import com.woocommerce.android.ui.woopos.localcatalog.ProductsResult
import com.woocommerce.android.ui.woopos.localcatalog.VariationsResult
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.generateWooPosProduct
import junit.framework.TestCase.assertFalse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import org.wordpress.android.fluxc.store.WCProductStore

class WooPosProductsRemoteDataSourceTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val sampleProducts = listOf(
        generateProduct(
            productId = 1,
            productName = "Product 1",
            amount = "10.0",
            productType = WooPosProductType.Simple,
            isDownloadable = false,
        ),
        generateProduct(
            productId = 2,
            productName = "Product 2",
            amount = "20.0",
            productType = WooPosProductType.Simple,
            isDownloadable = false,
            images = listOf(WooPosProductImage(id = 1, url = "https://test.com", name = "", alt = "")),
        ),
        generateProduct(
            productId = 3,
            productName = "Product 3",
            amount = "20.0",
            productType = WooPosProductType.Simple,
            isDownloadable = false,
            images = listOf(WooPosProductImage(id = 1, url = "https://test.com", name = "", alt = "")),
        )
    )

    private val additionalProducts = listOf(
        generateProduct(
            productId = 4,
            productName = "Product 4",
            amount = "10.0",
            productType = WooPosProductType.Simple,
            isDownloadable = false,
        ),
        generateProduct(
            productId = 5,
            productName = "Product 5",
            amount = "20.0",
            productType = WooPosProductType.Simple,
            isDownloadable = false,
            images = listOf(WooPosProductImage(id = 1, url = "https://test.com", name = "", alt = "")),
        )
    )

    private val productStore: WCProductStore = mock()
    private val searchProductsDataSource: WooPosSearchProductsDataSource = mock()
    private val siteModel: SiteModel = mock()
    private val productRestClient: ProductRestClient = mock()
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(siteModel)
    }
    private val productsCache: WooPosProductsCache = mock {
        on { getAll() }.thenReturn(sampleProducts)
    }
    private val productsIndex: WooPosProductsIndex = mock()
    private val productsTypesFilterConfig = WooPosProductsTypesFilterConfig()
    private val productMapper: WooPosWCProductToWooPosProductModelMapper = mock()

    private val variationMapper: WooPosVariationMapper = mock {
        on { fromProductVariation(any()) } doAnswer { invocation ->
            val productVariation = invocation.arguments[0] as com.woocommerce.android.model.ProductVariation
            WooPosVariation(
                remoteVariationId = productVariation.remoteVariationId,
                remoteProductId = productVariation.remoteProductId,
                globalUniqueId = productVariation.globalUniqueId,
                type = WooPosVariation.WooPosVariationType.VARIATION,
                price = productVariation.price,
                image = productVariation.image?.let { WooPosVariation.WooPosVariationImage(it.source) },
                attributes = productVariation.attributes.map {
                    WooPosVariation.WooPosVariationAttribute(
                        id = it.id,
                        name = it.name,
                        option = it.option
                    )
                },
                isVisible = productVariation.isVisible,
                isDownloadable = productVariation.isDownloadable
            )
        }
    }

    private val variationsSampleProductVariations = listOf(
        ProductTestUtils.generateProductVariation(
            productId = 1,
            variationId = 2,
            amount = "10.0",
            isDownloadable = false,
        ),
        ProductTestUtils.generateProductVariation(
            productId = 2,
            variationId = 3,
            amount = "20.0",
            isDownloadable = false,
        ),
        ProductTestUtils.generateProductVariation(
            productId = 3,
            variationId = 4,
            amount = "20.0",
            isDownloadable = false,
        )
    )

    private val variationsSampleProducts =
        variationsSampleProductVariations.map { it.toWooPosVariation(variationMapper) }

    private val variationsAdditionalProductVariations = listOf(
        ProductTestUtils.generateProductVariation(
            productId = 4,
            variationId = 5,
            amount = "10.0",
            isDownloadable = false,
        ),
        ProductTestUtils.generateProductVariation(
            productId = 5,
            variationId = 6,
            amount = "20.0",
            isDownloadable = false,
        ),
    )

    private val variationsAdditionalProducts =
        variationsAdditionalProductVariations.map { it.toWooPosVariation(variationMapper) }

    private val variationsHandler: VariationListHandler = mock()
    private val variationsCache: WooPosVariationsLRUCache = mock()

    @Test
    fun `given cached products, when fetchFirstProductsPage called, then should emit cached products first`() =
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
                    includeTypes = any(),
                    posProductsOnly = any()
                )
            ).thenReturn(WooResult(listOf()))
            whenever(productsIndex.getProductList()).thenReturn(sampleProducts)
            val sut = createSUT()

            // WHEN
            val result = sut.fetchFirstProductsPage(forceRefresh = false).first()

            // THEN
            assertThat(result).isInstanceOf(ProductsResult.Cached::class.java)
            val cachedResult = result as ProductsResult.Cached
            assertThat(cachedResult.products).containsExactlyElementsOf(sampleProducts)
        }

    @Test
    fun `given cached products, when fetchFirstProductsPage called with forceRefresh, then should not emit cached products`() =
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
                    includeTypes = any(),
                    posProductsOnly = any()
                )
            ).thenReturn(WooResult(listOf<WCProductModel>()))
            whenever(productsIndex.getProductList()).thenReturn(sampleProducts)
            val sut = createSUT()

            // WHEN
            val result = sut.fetchFirstProductsPage(forceRefresh = true).first()

            // THEN
            assertThat(result).isInstanceOf(ProductsResult.Remote::class.java)
        }

    @Test
    fun `given no products in list cache, when fetchFirstProductsPage called, then should return empty list`() =
        runTest {
            // GIVEN
            whenever(productsCache.getAll()).thenReturn(emptyList())
            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = any(),
                    pageSize = any(),
                    sortType = any(),
                    filterOptions = any(),
                    includeTypes = any(),
                    posProductsOnly = any()
                )
            ).thenReturn(WooResult(listOf()))
            whenever(productsIndex.getProductList()).thenReturn(emptyList())
            val sut = createSUT()

            // WHEN
            val result = sut.fetchFirstProductsPage(forceRefresh = false).first()

            // THEN
            assertThat(result).isInstanceOf(ProductsResult.Cached::class.java)
            val cachedResult = result as ProductsResult.Cached
            assertThat(cachedResult.products).isEmpty()
        }

    @Test
    fun `given cached and remote products, when fetchFirstProductsPage called, then should emit remote products after cached products`() =
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
                    includeTypes = any(),
                    posProductsOnly = any()
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
            val sut = createSUT()

            // WHEN
            val flow = sut.fetchFirstProductsPage(forceRefresh = false).toList()

            // THEN
            val cachedResult = flow[0] as ProductsResult.Cached
            val remoteResult = flow[1] as ProductsResult.Remote

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
                    posProductsOnly = any()
                )
            ).thenReturn(WooResult(wooError))
            whenever(productsCache.getAll()).thenReturn(sampleProducts)

            val sut = createSUT()

            // WHEN
            val flow = sut.fetchFirstProductsPage(forceRefresh = false).toList()

            // THEN
            assertThat(flow.size).isEqualTo(2)
            assertThat(flow[0]).isInstanceOf(ProductsResult.Cached::class.java)
            val cachedResult = flow[0] as ProductsResult.Cached
            assertThat(cachedResult.products).isEqualTo(sampleProducts)
            assertThat(flow[1]).isInstanceOf(ProductsResult.Remote::class.java)
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
                    includeTypes = any(),
                    posProductsOnly = any()
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
            val sut = createSUT()
            sut.fetchFirstProductsPage(forceRefresh = true).first()

            // WHEN
            val result = sut.loadMoreProducts()

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
                            productType = WooPosProductType.Simple,
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
                    includeTypes = any(),
                    posProductsOnly = any()
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

            val sut = createSUT()
            sut.fetchFirstProductsPage(forceRefresh = true).first()

            // WHEN
            val result = sut.loadMoreProducts()

            // THEN
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(WooException::class.java)
        }

    @Test
    fun `given no cached products and remote load fails, when fetchFirstProductsPage called, then should emit empty cache and then error`() =
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
                    includeTypes = any(),
                    posProductsOnly = any()
                )
            ).thenReturn(
                WooResult(
                    WooError(
                        type = WooErrorType.GENERIC_ERROR,
                        original = GenericErrorType.UNKNOWN,
                    )
                )
            )

            val sut = createSUT()

            // WHEN
            val flow = sut.fetchFirstProductsPage(forceRefresh = false).toList()

            // THEN
            val cachedResult = flow[0] as ProductsResult.Cached
            val remoteResult = flow[1] as ProductsResult.Remote

            assertThat(cachedResult.products).isEmpty()
            assertThat(remoteResult.productsResult.isFailure).isTrue()
        }

    @Test
    fun `given empty product list in cache, when fetchFirstProductsPage called, then should emit empty cache and empty remote result`() =
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
                    posProductsOnly = any()
                )
            ).thenReturn(WooResult(emptyList()))
            val sut = createSUT()

            // WHEN
            val flow = sut.fetchFirstProductsPage(forceRefresh = false).toList()

            // THEN
            val cachedResult = flow[0] as ProductsResult.Cached
            val remoteResult = flow[1] as ProductsResult.Remote

            assertThat(cachedResult.products).isEmpty()
            assertThat(remoteResult.productsResult.isSuccess).isTrue()
            assertThat(remoteResult.productsResult.getOrNull()).isEmpty()
            verify(productsCache).addAll(any())
            verify(productsIndex).storeProductList(emptyList())
        }

    @Test
    fun `when loading products, then should be sorted by name in ascending order`() = runTest {
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
                includeTypes = any(),
                posProductsOnly = any()
            )
        ).thenReturn(WooResult(listOf()))

        val sut = createSUT()

        // WHEN
        val result = sut.fetchFirstProductsPage(forceRefresh = false).first()

        // THEN
        assertThat(result).isInstanceOf(ProductsResult.Cached::class.java)
        val cachedResult = result as ProductsResult.Cached

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
                    includeTypes = any(),
                    posProductsOnly = any()
                )
            ).thenReturn(WooResult(firstPageProducts))

            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = eq(100),
                    pageSize = eq(100),
                    sortType = any(),
                    filterOptions = any(),
                    includeTypes = any(),
                    posProductsOnly = any()
                )
            ).thenReturn(WooResult(secondPageProducts))

            val sut = createSUT()

            // WHEN
            val result = sut.prepopulateCache()

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
                    includeTypes = any(),
                    posProductsOnly = any()
                )
            ).thenReturn(WooResult(firstPageProducts))

            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = eq(100),
                    pageSize = eq(100),
                    sortType = any(),
                    filterOptions = any(),
                    includeTypes = any(),
                    posProductsOnly = any()
                )
            ).thenReturn(WooResult(wooError))

            val sut = createSUT()

            // WHEN
            val result = sut.prepopulateCache()

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
                includeTypes = any(),
                posProductsOnly = any()
            )
        ).thenReturn(WooResult(wooError))

        val sut = createSUT()

        // WHEN
        val result = sut.prepopulateCache()

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
                    includeTypes = any(),
                    posProductsOnly = any()
                )
            ).thenReturn(WooResult(firstPageProducts))

            whenever(
                productStore.fetchProducts(
                    site = eq(siteModel),
                    offset = eq(100),
                    pageSize = eq(100),
                    sortType = any(),
                    filterOptions = any(),
                    includeTypes = any(),
                    posProductsOnly = any()
                )
            ).thenReturn(WooResult(secondPageProducts))

            val sut = createSUT()

            // WHEN
            val result = sut.prepopulateCache()

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
                includeTypes = any(),
                posProductsOnly = any()
            )
        }

    @Test
    fun `given force refresh, when fetchFirstVariationsPage called, then should clear cache`() = runTest {
        // GIVEN
        val productId = 1L
        whenever(variationsHandler.canLoadMore(5)).thenReturn(true)
        whenever(variationsHandler.getVariationsFlow(productId)).thenReturn(flowOf(variationsSampleProductVariations))
        whenever(variationsCache.get(productId)).thenReturn(variationsSampleProducts)
        val sut = createSUT()

        sut.fetchFirstVariationsPage(productId, forceRefresh = true).first()
        assertThat(
            sut.fetchFirstVariationsPage(productId, forceRefresh = true).first()
        ).isInstanceOf(VariationsResult.Cached::class.java)

        // WHEN
        sut.fetchFirstVariationsPage(productId, forceRefresh = true).first()

        // THEN
        // Ensure the cache is cleared (by checking that the cache was reloaded)
        val result = sut.fetchFirstVariationsPage(productId, forceRefresh = false).first()
        assertThat(result).isInstanceOf(VariationsResult.Cached::class.java)
        val cachedResult = result as VariationsResult.Cached
        assertThat(cachedResult.data).containsExactlyElementsOf(variationsSampleProducts)
    }

    @Test
    fun `given cached products, when fetchFirstVariationsPage called, then should emit cached products first`() =
        runTest {
            // GIVEN
            val productId = 1L
            whenever(variationsHandler.canLoadMore(5)).thenReturn(true)
            whenever(variationsHandler.getVariationsFlow(productId))
                .thenReturn(flowOf(variationsSampleProductVariations))
            whenever(variationsCache.get(productId)).thenReturn(variationsSampleProducts)
            val sut = createSUT()

            sut.fetchFirstVariationsPage(productId, forceRefresh = true).first()

            // WHEN
            val result = sut.fetchFirstVariationsPage(productId, forceRefresh = false).first()

            // THEN
            // Ensure the result is from cache
            assertThat(result).isInstanceOf(VariationsResult.Cached::class.java)
            val cachedResult = result as VariationsResult.Cached
            assertThat(cachedResult.data).containsExactlyElementsOf(variationsSampleProducts)
        }

    @Test
    fun `given cached and remote variations, when fetchFirstVariationsPage called, then emits remote after cached`() =
        runTest {
            // GIVEN
            val productId = 1L
            whenever(variationsHandler.canLoadMore(5)).thenReturn(true)
            whenever(variationsHandler.getVariationsFlow(productId))
                .thenReturn(flowOf(variationsSampleProductVariations))
            whenever(variationsCache.get(productId)).thenReturn(variationsSampleProducts)
            val sut = createSUT()

            sut.fetchFirstVariationsPage(productId, forceRefresh = true).first()

            // WHEN
            val flow = sut.fetchFirstVariationsPage(productId, forceRefresh = false).toList()

            // THEN
            val cachedResult = flow[0] as VariationsResult.Cached
            assertThat(cachedResult.data).containsExactlyElementsOf(variationsSampleProducts)

            // Validate remote result
            val remoteResult = flow[1] as VariationsResult.Remote
            assertThat(remoteResult.result.getOrNull()).isNotNull
            assertThat(remoteResult.result.getOrNull()).containsExactlyElementsOf(variationsSampleProducts)
        }

    @Test
    fun `given remote load fails, when fetchFirstVariationsPage called, then should emit cached variations and then error`() =
        runTest {
            // GIVEN
            val productId = 1L
            whenever(variationsHandler.canLoadMore(5)).thenReturn(true)
            whenever(variationsHandler.getVariationsFlow(productId))
                .thenReturn(flowOf(variationsSampleProductVariations))
            whenever(variationsCache.get(productId)).thenReturn(variationsSampleProducts)
            val exception = Exception("Remote load failed")
            val sut = createSUT()

            sut.fetchFirstVariationsPage(productId, forceRefresh = true).first()

            whenever(
                variationsHandler.fetchVariations(
                    productId,
                    forceRefresh = true,
                    mapOf(
                        WCProductStore.VariationFilterOption.STATUS to "publish",
                        WCProductStore.VariationFilterOption.DOWNLOADABLE to "false"
                    )
                )
            )
                .thenReturn(Result.failure(exception))

            // WHEN
            val flow = sut.fetchFirstVariationsPage(productId, forceRefresh = false).toList()

            // THEN
            val cachedResult = flow[0] as VariationsResult.Cached
            assertThat(cachedResult.data).containsExactlyElementsOf(variationsSampleProducts)

            val remoteResult = flow[1] as VariationsResult.Remote
            assertThat(remoteResult.result.getOrNull()).isNull()
            assertThat(remoteResult.result.exceptionOrNull()).isEqualTo(exception)
        }

    @Test
    fun `given successful loadMoreVariations, when loadMoreVariations called, then should add variations to cache and return them`() =
        runTest {
            // GIVEN
            val productId = 1L
            whenever(variationsHandler.canLoadMore(5)).thenReturn(true)
            whenever(variationsHandler.getVariationsFlow(productId)).thenReturn(
                flowOf(variationsSampleProductVariations),
                flowOf(variationsSampleProductVariations + variationsAdditionalProductVariations)
            )
            whenever(variationsCache.get(productId)).thenReturn(variationsSampleProducts + variationsAdditionalProducts)
            whenever(variationsHandler.loadMore(productId)).thenReturn(Result.success(Unit))
            val sut = createSUT()

            sut.fetchFirstVariationsPage(productId, forceRefresh = false).first()

            // WHEN
            val result = sut.loadMoreVariations(productId)

            // THEN
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrNull())
                .containsExactlyElementsOf(variationsSampleProducts + variationsAdditionalProducts)

            val cachedResult = sut.fetchFirstVariationsPage(productId, forceRefresh = false).first()
            assertThat(cachedResult).isInstanceOf(VariationsResult.Cached::class.java)
            val cachedVariations = (cachedResult as VariationsResult.Cached).data
            assertThat(cachedVariations)
                .containsExactlyElementsOf(variationsSampleProducts + variationsAdditionalProducts)
        }

    @Test
    fun `when loadMoreVariations fails, then should return error and cache remains unchanged`() =
        runTest {
            // GIVEN
            val productId = 1L
            whenever(variationsHandler.canLoadMore(5)).thenReturn(true)
            whenever(variationsHandler.getVariationsFlow(productId))
                .thenReturn(flowOf(variationsSampleProductVariations))
            val exception = Exception("Load more failed")
            whenever(
                variationsHandler.loadMore(
                    productId,
                    mapOf(
                        WCProductStore.VariationFilterOption.STATUS to "publish",
                        WCProductStore.VariationFilterOption.DOWNLOADABLE to "false"
                    )
                ),
            ).thenReturn(Result.failure(exception))
            whenever(variationsCache.get(productId)).thenReturn(variationsSampleProducts)
            val sut = createSUT()

            sut.fetchFirstVariationsPage(productId, forceRefresh = false).first()

            // WHEN
            val result = sut.loadMoreVariations(productId)

            // THEN
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isEqualTo(exception)

            val cachedResult = sut.fetchFirstVariationsPage(productId, forceRefresh = false).first()
            assertThat(cachedResult).isInstanceOf(VariationsResult.Cached::class.java)
            val cachedVariations = (cachedResult as VariationsResult.Cached).data
            assertThat(cachedVariations).containsExactlyElementsOf(variationsSampleProducts)
        }

    @Test
    fun `given no cached variations and remote load fails, when fetchFirstVariationsPage called, then should emit empty cache and then error`() =
        runTest {
            // GIVEN
            val productId = 1L
            whenever(variationsHandler.canLoadMore(5)).thenReturn(true)
            whenever(variationsHandler.getVariationsFlow(productId)).thenReturn(flowOf(emptyList()))
            val exception = Exception("Remote load failed")
            whenever(
                variationsHandler.fetchVariations(
                    productId,
                    forceRefresh = true,
                    mapOf(
                        WCProductStore.VariationFilterOption.STATUS to "publish",
                        WCProductStore.VariationFilterOption.DOWNLOADABLE to "false"
                    )
                )
            ).thenReturn(Result.failure(exception))
            whenever(variationsCache.get(productId)).thenReturn(emptyList())

            val sut = createSUT()

            // WHEN
            val flow = sut.fetchFirstVariationsPage(productId, forceRefresh = false).toList()

            // THEN
            val remoteResult = flow[0] as VariationsResult.Remote
            assertThat(remoteResult.result.getOrNull()).isNull()
            assertThat(remoteResult.result.exceptionOrNull()).isEqualTo(exception)
        }

    @Test
    fun `given empty variations list from handler, when fetchFirstVariationsPage called, then should emit empty remote result`() =
        runTest {
            // GIVEN
            val productId = 1L
            whenever(variationsHandler.canLoadMore(5)).thenReturn(true)
            whenever(variationsHandler.getVariationsFlow(productId)).thenReturn(flowOf(emptyList()))
            whenever(
                variationsHandler.fetchVariations(
                    productId,
                    forceRefresh = false
                )
            ).thenReturn(Result.success(Unit))
            whenever(variationsCache.get(productId)).thenReturn(emptyList())
            val sut = createSUT()

            // WHEN
            val flow = sut.fetchFirstVariationsPage(productId, forceRefresh = false).toList()

            // THEN
            val remoteResult = flow[0] as VariationsResult.Remote
            assertThat(remoteResult.result.getOrNull()).isNotNull
            assertThat(remoteResult.result.getOrNull()).isEmpty()
        }

    @Test
    fun `given cached variations, when fetchFirstVariationsPage called, then filter in only variations that have price`() =
        runTest {
            // GIVEN
            val productId = 1L
            whenever(variationsHandler.canLoadMore(5)).thenReturn(true)
            whenever(variationsHandler.getVariationsFlow(productId)).thenReturn(
                flowOf(
                    listOf(
                        ProductTestUtils.generateProductVariation(
                            variationId = 1,
                            amount = "0",
                        ),
                        ProductTestUtils.generateProductVariation(
                            variationId = 2,
                            amount = "20.0",
                        )
                    )
                )
            )
            whenever(variationsHandler.fetchVariations(productId, forceRefresh = true)).thenReturn(Result.success(Unit))
            whenever(variationsCache.get(productId)).thenReturn(variationsSampleProducts)
            val sut = createSUT()

            // WHEN
            val flow = sut.fetchFirstVariationsPage(productId, forceRefresh = false).toList()

            // THEN
            val cachedResult = flow[0] as VariationsResult.Cached

            assertFalse(cachedResult.data.any { it.remoteVariationId == 1L })
        }

    @Test
    fun `given cached variations, when fetchFirstVariationsPage called, then filter out virtual variations`() =
        runTest {
            // GIVEN
            val productId = 1L
            whenever(variationsHandler.canLoadMore(5)).thenReturn(true)
            whenever(variationsHandler.getVariationsFlow(productId)).thenReturn(
                flowOf(
                    listOf(
                        ProductTestUtils.generateProductVariation(
                            variationId = 1,
                            amount = "0",
                            isVirtual = true
                        ),
                        ProductTestUtils.generateProductVariation(
                            variationId = 2,
                            amount = "20.0",
                            isVirtual = false
                        )
                    )
                )
            )
            whenever(variationsHandler.fetchVariations(productId, forceRefresh = true)).thenReturn(Result.success(Unit))
            whenever(variationsCache.get(productId)).thenReturn(variationsSampleProducts)
            val sut = createSUT()

            // WHEN
            val flow = sut.fetchFirstVariationsPage(productId, forceRefresh = false).toList()

            // THEN
            val cachedResult = flow[0] as VariationsResult.Cached

            assertFalse(cachedResult.data.any { it.remoteVariationId == 1L })
        }

    @Test
    fun `when local products exist, then searchProducts emits Local then Remote success`() = runTest {
        // GIVEN
        whenever(searchProductsDataSource.searchLocalProducts("query"))
            .thenReturn(sampleProducts)
        whenever(searchProductsDataSource.searchRemoteProducts("query"))
            .thenReturn(Result.success(sampleProducts + additionalProducts))
        val sut = createSUT()

        // WHEN
        val results = sut.searchProducts("query").toList()

        // THEN
        assertThat(results[0]).isInstanceOf(SearchProductsResult.Local::class.java)
        assertThat(results[1]).isInstanceOf(SearchProductsResult.Remote::class.java)
    }

    @Test
    fun `when no local products, then searchProducts emits only Remote`() = runTest {
        // GIVEN
        whenever(searchProductsDataSource.searchLocalProducts("query"))
            .thenReturn(emptyList())
        whenever(searchProductsDataSource.searchRemoteProducts("query"))
            .thenReturn(Result.success(sampleProducts))
        val sut = createSUT()

        // WHEN
        val results = sut.searchProducts("query").toList()

        // THEN
        assertThat(results[0]).isInstanceOf(SearchProductsResult.Remote::class.java)
    }

    @Test
    fun `when remote fails, then searchProducts emits Remote failure`() = runTest {
        // GIVEN
        val exception = Exception("Network error")
        whenever(searchProductsDataSource.searchLocalProducts("query"))
            .thenReturn(sampleProducts)
        whenever(searchProductsDataSource.searchRemoteProducts("query"))
            .thenReturn(Result.failure(exception))
        val sut = createSUT()

        // WHEN
        val results = sut.searchProducts("query").toList()

        // THEN
        val remoteResult = results[1] as SearchProductsResult.Remote
        assertThat(remoteResult.productsResult.isFailure).isTrue()
        assertThat(remoteResult.productsResult.exceptionOrNull()).isEqualTo(exception)
    }

    private fun generateProduct(
        productId: Long = 1,
        productName: String = "Product 1",
        amount: String = "10.0",
        productType: WooPosProductType = WooPosProductType.Simple,
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

    @Test
    fun `given variation in cache, when getVariationById called, then returns cached variation without REST call`() =
        runTest {
            // GIVEN
            val productId = 1L
            val variationId = 2L
            val cachedVariation = variationsSampleProducts.find { it.remoteVariationId == variationId }
            whenever(variationsCache.get(productId)).thenReturn(variationsSampleProducts)
            val sut = createSUT()

            // WHEN
            val result = sut.getVariationById(productId, variationId)

            // THEN
            assertThat(result).isNotNull
            assertThat(result).isEqualTo(cachedVariation)
            verify(productRestClient, never()).fetchProductVariations(any(), any(), any(), any(), any())
        }

    @Test
    fun `given variation not in cache and cache has variations, when getVariationById called, then fetches all variations and updates cache`() =
        runTest {
            // GIVEN
            val productId = 1L
            val variationId = 10L
            val wcVariation = WCProductVariationModel(LocalId(1)).copy(
                remoteVariationId = RemoteId(variationId),
                remoteProductId = RemoteId(productId)
            )
            val expectedVariation = WooPosVariation(
                remoteVariationId = variationId,
                remoteProductId = productId,
                globalUniqueId = "test-global-id",
                type = WooPosVariation.WooPosVariationType.VARIATION,
                price = java.math.BigDecimal("10.0"),
                image = null,
                attributes = emptyList(),
                isVisible = true,
                isDownloadable = false
            )
            val remotePayload = WCProductStore.RemoteProductVariationsPayload(
                site = siteModel,
                remoteProductId = productId,
                variations = listOf(wcVariation)
            )

            whenever(variationsCache.get(productId)).thenReturn(variationsSampleProducts)
            whenever(productRestClient.fetchProductVariations(eq(siteModel), eq(productId), any(), any(), any()))
                .thenReturn(remotePayload)
            whenever(variationMapper.fromWCProductVariationModel(wcVariation)).thenReturn(expectedVariation)
            whenever(selectedSite.get()).thenReturn(siteModel)
            val sut = createSUT()

            // WHEN
            val result = sut.getVariationById(productId, variationId)

            // THEN
            assertThat(result).isNotNull
            assertThat(result?.remoteVariationId).isEqualTo(variationId)
            verify(productRestClient).fetchProductVariations(eq(siteModel), eq(productId), any(), any(), any())
            verify(variationsCache).put(eq(productId), any())
        }

    @Test
    fun `given empty cache, when getVariationById called, then returns null without fetching`() =
        runTest {
            // GIVEN
            val productId = 1L
            val variationId = 2L

            whenever(variationsCache.get(productId)).thenReturn(emptyList())
            val sut = createSUT()

            // WHEN
            val result = sut.getVariationById(productId, variationId)

            // THEN
            assertThat(result).isNull()
            verify(productRestClient, never()).fetchProductVariations(any(), any(), any(), any(), any())
            verify(variationsCache, never()).put(any(), any())
        }

    @Test
    fun `given REST fetch fails, when getVariationById called, then returns null without updating cache`() =
        runTest {
            // GIVEN
            val productId = 1L
            val variationId = 999L
            val errorPayload = WCProductStore.RemoteProductVariationsPayload(
                error = WCProductStore.ProductError(WCProductStore.ProductErrorType.GENERIC_ERROR),
                site = siteModel,
                remoteProductId = productId
            )

            whenever(variationsCache.get(productId)).thenReturn(variationsSampleProducts)
            whenever(productRestClient.fetchProductVariations(eq(siteModel), eq(productId), any(), any(), any()))
                .thenReturn(errorPayload)
            whenever(selectedSite.get()).thenReturn(siteModel)
            val sut = createSUT()

            // WHEN
            val result = sut.getVariationById(productId, variationId)

            // THEN
            assertThat(result).isNull()
            verify(productRestClient).fetchProductVariations(eq(siteModel), eq(productId), any(), any(), any())
            verify(variationsCache, never()).put(any(), any())
        }

    @Test
    fun `given variation not in cache but others are, when getVariationById called, then refreshes entire cache and returns variation`() =
        runTest {
            // GIVEN
            val productId = 1L
            val variationId = 99L
            val wcVariation = WCProductVariationModel(LocalId(1)).copy(
                remoteVariationId = RemoteId(variationId),
                remoteProductId = RemoteId(productId)
            )
            val expectedVariation = WooPosVariation(
                remoteVariationId = variationId,
                remoteProductId = productId,
                globalUniqueId = "test-global-id",
                type = WooPosVariation.WooPosVariationType.VARIATION,
                price = java.math.BigDecimal("99.99"),
                image = null,
                attributes = emptyList(),
                isVisible = true,
                isDownloadable = false
            )
            val remotePayload = WCProductStore.RemoteProductVariationsPayload(
                site = siteModel,
                remoteProductId = productId,
                variations = listOf(wcVariation)
            )

            whenever(variationsCache.get(productId)).thenReturn(variationsSampleProducts)
            whenever(productRestClient.fetchProductVariations(eq(siteModel), eq(productId), any(), any(), any()))
                .thenReturn(remotePayload)
            whenever(variationMapper.fromWCProductVariationModel(wcVariation)).thenReturn(expectedVariation)
            whenever(selectedSite.get()).thenReturn(siteModel)
            val sut = createSUT()

            // WHEN
            val result = sut.getVariationById(productId, variationId)

            // THEN
            assertThat(result).isNotNull
            assertThat(result?.remoteVariationId).isEqualTo(variationId)
            verify(productRestClient).fetchProductVariations(eq(siteModel), eq(productId), any(), any(), any())
            verify(variationsCache).put(eq(productId), any())
        }

    private fun createSUT(): WooPosProductsRemoteDataSource {
        val sut = WooPosProductsRemoteDataSource(
            productStore,
            productRestClient,
            selectedSite,
            productsCache,
            productsIndex,
            productsTypesFilterConfig,
            productMapper,
            variationsHandler,
            variationsCache,
            WooPosVariationsTypesFilterConfig(),
            variationMapper,
            searchProductsDataSource
        )
        return sut
    }
}
