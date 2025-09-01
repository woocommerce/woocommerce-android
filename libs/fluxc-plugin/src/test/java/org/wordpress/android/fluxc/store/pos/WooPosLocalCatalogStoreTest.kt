package org.wordpress.android.fluxc.store.pos

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.pos.WooPosCatalogStatusResponse
import org.wordpress.android.fluxc.model.pos.WooPosGenerateCatalogResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.pos.WooPosProductRestClient
import org.wordpress.android.fluxc.persistence.dao.pos.WooPosProductsDao
import org.wordpress.android.fluxc.persistence.dao.pos.WooPosVariationsDao
import org.wordpress.android.fluxc.persistence.entity.pos.WCPosProductModel
import org.wordpress.android.fluxc.persistence.entity.pos.WCPosVariationModel
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogError
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import org.wordpress.android.fluxc.utils.HeadersParser
import org.wordpress.android.fluxc.utils.initCoroutineEngine

@RunWith(MockitoJUnitRunner::class)
class WooPosLocalCatalogStoreTest {
    private val posProductRestClient: WooPosProductRestClient = mock()

    private val posProductsDao: WooPosProductsDao =  mock()

    private val posVariationsDao: WooPosVariationsDao = mock()

    private val headersParser: HeadersParser = mock()

    private lateinit var store: WooPosLocalCatalogStore

    companion object Companion {
        private val SITE_ID = LocalId(123)
        private val PRODUCT_ID = RemoteId(456)
        private val SITE = SiteModel().apply { id = 123 }
        private const val SYNC_DATE = "2024-01-01T00:00:00Z"
    }

    private val testSiteId = SITE_ID
    private val testRemoteId = PRODUCT_ID
    private val testSite = SITE
    private val validDateString = SYNC_DATE

    private val sampleProduct1 = ProductTestData.coffeMug(testSiteId)
    private val sampleProduct2 = ProductTestData.laptopStand(testSiteId)
    private val sampleProducts = listOf(sampleProduct1, sampleProduct2)

    @Before
    fun setUp() {
        store = WooPosLocalCatalogStore(
            posProductRestClient = posProductRestClient,
            coroutineEngine = initCoroutineEngine(),
            posProductDao = posProductsDao,
            posVariationsDao = posVariationsDao,
            headersParser = headersParser
        )

        // Set up default successful responses for common operations
        whenever(headersParser.getServerDate<Any>(any()))
            .thenReturn("2024-01-01T00:00:00Z")
        whenever(headersParser.getTotalPages<Any>(any()))
            .thenReturn(1)
    }

    @Test
    fun `when observing products for site, then emits current product catalog`() = runTest {
        // GIVEN
        whenever(posProductsDao.observeAllProducts(testSiteId))
            .thenReturn(flowOf(sampleProducts))

        // WHEN
        val catalog = store.observeProducts(testSiteId).first().getOrThrow()

        // THEN
        assertThat(catalog).hasSize(2)
        assertThat(catalog).containsExactlyInAnyOrder(sampleProduct1, sampleProduct2)
    }

    @Test
    fun `when observing products for empty site, then emits empty catalog`() = runTest {
        // GIVEN
        whenever(posProductsDao.observeAllProducts(testSiteId))
            .thenReturn(flowOf(emptyList()))

        // WHEN
        val catalog = store.observeProducts(testSiteId).first().getOrThrow()

        // THEN
        assertThat(catalog).isEmpty()
    }

    @Test
    fun `when getting existing product, then returns product details`() = runTest {
        // GIVEN
        val expectedProduct = createTestProduct(remoteId = testRemoteId.value, name = "Wireless Mouse")
        whenever(posProductsDao.getProduct(testSiteId, testRemoteId))
            .thenReturn(expectedProduct)

        // WHEN
        val product = store.getProduct(testSiteId, testRemoteId).getOrThrow()

        // THEN
        assertThat(product).isNotNull()
        assertThat(product!!.name).isEqualTo("Wireless Mouse")
        assertThat(product.remoteId).isEqualTo(testRemoteId)
    }

    @Test
    fun `when getting non-existing product, then returns null`() = runTest {
        // GIVEN
        val nonExistentProductId = RemoteId(999L)
        whenever(posProductsDao.getProduct(testSiteId, nonExistentProductId))
            .thenReturn(null)

        // WHEN
        val product = store.getProduct(testSiteId, nonExistentProductId).getOrThrow()

        // THEN
        assertThat(product).isNull()
    }

    @Test
    fun `when syncing products, then saves products to local catalog and returns sync summary`() = runTest {
        // GIVEN
        val remoteProducts = arrayOf(
            createTestApiResponse(id = 1L, name = "Coffee Mug"),
            createTestApiResponse(id = 2L, name = "Laptop Stand")
        )
        whenever(posProductRestClient.fetchProducts(testSite, validDateString, 0, 100))
            .thenReturn(WooResult(remoteProducts))

        // WHEN
        val syncResult = store.syncRecentlyModifiedProducts(testSite, validDateString, 0, 100).getOrThrow()

        // THEN
        verify(posProductsDao).upsertProducts(any())
        assertThat(syncResult.syncedCount).isEqualTo(2)
        assertThat(syncResult.hasMore).isFalse()
        assertThat(syncResult.nextOffset).isEqualTo(0)
    }

    @Test
    fun `given full page of products, when syncing, then indicates more products available`() = runTest {
        // GIVEN
        val fullPageOfProducts = Array(100) { index ->
            createTestApiResponse(id = index.toLong() + 1, name = "Product ${index + 1}")
        }
        whenever(posProductRestClient.fetchProducts(testSite, validDateString, 0, 100))
            .thenReturn(WooResult(fullPageOfProducts))

        // WHEN
        val syncResult = store.syncRecentlyModifiedProducts(testSite, validDateString, 0, 100).getOrThrow()

        // THEN
        assertThat(syncResult.hasMore).isTrue()
        assertThat(syncResult.nextOffset).isEqualTo(100)
        assertThat(syncResult.syncedCount).isEqualTo(100)
    }

    @Test
    fun `given no remote products, when syncing, then completes without saving anything`() = runTest {
        // GIVEN
        whenever(posProductRestClient.fetchProducts(testSite, validDateString, 0, 100))
            .thenReturn(WooResult(emptyArray()))

        // WHEN
        val syncResult = store.syncRecentlyModifiedProducts(testSite, validDateString, 0, 100).getOrThrow()

        // THEN
        verifyNoInteractions(posProductsDao)
        assertThat(syncResult.syncedCount).isEqualTo(0)
        assertThat(syncResult.hasMore).isFalse()
        assertThat(syncResult.nextOffset).isEqualTo(0)
    }

    @Test
    fun `given network connectivity issues, when syncing, then throws network error`() = runTest {
        // GIVEN
        val connectivityError = WooError(
            type = WooErrorType.NO_CONNECTION,
            original = org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NO_CONNECTION,
            message = "Connection failed"
        )
        whenever(posProductRestClient.fetchProducts(testSite, validDateString, 0, 100))
            .thenReturn(WooResult(connectivityError))

        // WHEN
        val result = store.syncRecentlyModifiedProducts(testSite, validDateString, 0, 100)

        // THEN
        assertThat(result.isFailure).isTrue()
        val error = result.exceptionOrNull() as WooPosLocalCatalogError.NetworkError
        assertThat(error.errorMessage).contains("Connection failed")
        verifyNoInteractions(posProductsDao)
    }

    @Test
    fun `given database storage fails, when syncing, then throws database error`() = runTest {
        // GIVEN
        val remoteProducts = arrayOf(createTestApiResponse(id = 1L, name = "Product 1"))
        val storageException = RuntimeException("Disk full")
        whenever(posProductRestClient.fetchProducts(testSite, validDateString, 0, 100))
            .thenReturn(WooResult(remoteProducts))
        whenever(posProductsDao.upsertProducts(any()))
            .thenThrow(storageException)

        // WHEN
        val result = store.syncRecentlyModifiedProducts(testSite, validDateString, 0, 100)

        // THEN
        assertThat(result.isFailure).isTrue()
        val error = result.exceptionOrNull() as WooPosLocalCatalogError.DatabaseError
        assertThat(error.errorMessage).contains("Failed to save products to database")
        assertThat(error.throwable).isEqualTo(storageException)
    }

    @Test
    fun `given oversized page request, when syncing, then constrains to maximum page size`() = runTest {
        // GIVEN
        val remoteProducts = arrayOf(createTestApiResponse(id = 1L, name = "Product 1"))
        whenever(posProductRestClient.fetchProducts(testSite, validDateString, 0, 100))
            .thenReturn(WooResult(remoteProducts))

        // WHEN
        val result = store.syncRecentlyModifiedProducts(testSite, validDateString, 0, 500)

        // THEN
        assertThat(result.isSuccess).isTrue()
        verify(posProductRestClient).fetchProducts(testSite, validDateString, 0, 100)
    }

    @Test
    fun `given partial page of products, when syncing, then indicates no more products available`() = runTest {
        // GIVEN
        val partialPageProducts = Array(50) { index ->
            createTestApiResponse(id = index.toLong() + 1, name = "Product ${index + 1}")
        }
        whenever(posProductRestClient.fetchProducts(testSite, validDateString, 0, 100))
            .thenReturn(WooResult(partialPageProducts))

        // WHEN
        val syncResult = store.syncRecentlyModifiedProducts(testSite, validDateString, 0, 100).getOrThrow()

        // THEN
        assertThat(syncResult.hasMore).isFalse()
        assertThat(syncResult.nextOffset).isEqualTo(0)
        assertThat(syncResult.syncedCount).isEqualTo(50)
    }

    @Test
    fun `given negative page size, when syncing, then constrains to minimum page size`() = runTest {
        // GIVEN
        val remoteProducts = arrayOf(createTestApiResponse(id = 1L, name = "Product 1"))
        whenever(posProductRestClient.fetchProducts(testSite, validDateString, 0, 1))
            .thenReturn(WooResult(remoteProducts))

        // WHEN
        val result = store.syncRecentlyModifiedProducts(testSite, validDateString, 0, -5)

        // THEN
        assertThat(result.isSuccess).isTrue()
        verify(posProductRestClient).fetchProducts(testSite, validDateString, 0, 1)
    }

    @Test
    fun `given API timeout error, when syncing, then throws timeout specific network error`() = runTest {
        // GIVEN
        val timeoutError = WooError(
            type = WooErrorType.TIMEOUT,
            original = org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.TIMEOUT,
            message = "Request timeout"
        )
        whenever(posProductRestClient.fetchProducts(testSite, validDateString, 0, 100))
            .thenReturn(WooResult(timeoutError))

        // WHEN
        val result = store.syncRecentlyModifiedProducts(testSite, validDateString, 0, 100)

        // THEN
        assertThat(result.isFailure).isTrue()
        val error = result.exceptionOrNull() as WooPosLocalCatalogError.NetworkError
        assertThat(error.errorMessage).contains("Request timed out")
        assertThat(error.code).isEqualTo("TIMEOUT")
    }

    @Test
    fun `given multiple site IDs, when observing products, then returns site-specific catalog`() = runTest {
        // GIVEN
        val site1 = LocalId(111)
        val site2 = LocalId(222)
        val site1Products = listOf(createTestProduct(localSiteId = site1.value, remoteId = 1L, name = "Site1 Product"))
        val site2Products = listOf(createTestProduct(localSiteId = site2.value, remoteId = 2L, name = "Site2 Product"))

        whenever(posProductsDao.observeAllProducts(site1)).thenReturn(flowOf(site1Products))
        whenever(posProductsDao.observeAllProducts(site2)).thenReturn(flowOf(site2Products))

        // WHEN
        val catalog1 = store.observeProducts(site1).first().getOrThrow()
        val catalog2 = store.observeProducts(site2).first().getOrThrow()

        // THEN
        assertThat(catalog1).hasSize(1)
        assertThat(catalog1.first().name).isEqualTo("Site1 Product")
        assertThat(catalog2).hasSize(1)
        assertThat(catalog2.first().name).isEqualTo("Site2 Product")
    }

    // Variation-specific tests
    @Test
    fun `when observing variations for product, then emits current variations`() = runTest {
        // GIVEN
        val variation1 = createTestVariation(variationId = 1L, name = "Small")
        val variation2 = createTestVariation(variationId = 2L, name = "Large")
        val variations = listOf(variation1, variation2)

        whenever(posVariationsDao.observeVariationsForProduct(testSiteId, testRemoteId))
            .thenReturn(flowOf(variations))

        // WHEN
        val result = store.observeVariationsForProduct(testSiteId, testRemoteId).first().getOrThrow()

        // THEN
        assertThat(result).hasSize(2)
        assertThat(result).containsExactlyInAnyOrder(variation1, variation2)
    }

    @Test
    fun `when getting existing variation, then returns variation details`() = runTest {
        // GIVEN
        val variationId = RemoteId(789L)
        val expectedVariation = createTestVariation(variationId = variationId.value, name = "Blue Large")
        whenever(posVariationsDao.getVariation(testSiteId, testRemoteId, variationId))
            .thenReturn(expectedVariation)

        // WHEN
        val variation = store.getVariation(testSiteId, testRemoteId, variationId).getOrThrow()

        // THEN
        assertThat(variation).isNotNull()
        assertThat(variation!!.variationName).isEqualTo("Blue Large")
        assertThat(variation.remoteVariationId).isEqualTo(variationId)
    }

    @Test
    fun `given no remote variations, when syncing, then completes without saving`() = runTest {
        // GIVEN
        whenever(posProductRestClient.fetchVariations(testSite, validDateString, 1, 100))
            .thenReturn(WooResult(emptyArray()))

        // WHEN
        val syncResult = store.syncRecentlyModifiedVariations(testSite, validDateString, 1, 100).getOrThrow()

        // THEN
        verifyNoInteractions(posVariationsDao)
        assertThat(syncResult.syncedCount).isEqualTo(0)
        assertThat(syncResult.hasMore).isFalse()
        assertThat(syncResult.nextPage).isEqualTo(1)
    }

    @Test
    fun `given network error, when syncing variations, then returns failure`() = runTest {
        // GIVEN
        val networkError = WooError(
            type = WooErrorType.API_ERROR,
            original = org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR,
            message = "Network error"
        )
        whenever(posProductRestClient.fetchVariations(testSite, validDateString, 1, 100))
            .thenReturn(WooResult(networkError))

        // WHEN
        val result = store.syncRecentlyModifiedVariations(testSite, validDateString, 1, 100)

        // THEN
        assertThat(result.isFailure).isTrue()
        val error = result.exceptionOrNull() as WooPosLocalCatalogError.NetworkError
        assertThat(error.errorMessage).contains("Network error")
        verifyNoInteractions(posVariationsDao)
    }

    @Test
    fun `given valid variation API response, when sync variations called, then variations saved to database`() = runTest {
        // GIVEN
        val variations = arrayOf(createTestVariationApiResponse())
        whenever(posProductRestClient.fetchVariations(testSite, validDateString, 1, 100))
            .thenReturn(WooResult(variations))

        // WHEN
        val result = store.syncRecentlyModifiedVariations(testSite, validDateString, 1, 100)

        // THEN
        assertThat(result.isSuccess).isTrue()
        result.getOrNull()?.let { syncResult ->
            assertThat(syncResult.syncedCount).isEqualTo(1)
            assertThat(syncResult.hasMore).isFalse()
            assertThat(syncResult.nextPage).isEqualTo(1)
        }
        verify(posVariationsDao).upsertVariations(any())
    }

    @Test
    fun `given database error during variation sync, when sync called, then database error returned`() = runTest {
        // GIVEN
        val variations = arrayOf(createTestVariationApiResponse())
        whenever(posProductRestClient.fetchVariations(testSite, validDateString, 1, 100))
            .thenReturn(WooResult(variations))
        whenever(posVariationsDao.upsertVariations(any()))
            .thenThrow(RuntimeException("Database error"))

        // WHEN
        val result = store.syncRecentlyModifiedVariations(testSite, validDateString, 1, 100)

        // THEN
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(WooPosLocalCatalogError.DatabaseError::class.java)
    }

    @Test
    fun `given full page of variations, when sync called, then pagination indicates more pages`() = runTest {
        // GIVEN
        val variations = Array(100) { createTestVariationApiResponse(id = it.toLong()) }
        whenever(posProductRestClient.fetchVariations(testSite, validDateString, 1, 100))
            .thenReturn(WooResult(variations))

        // WHEN
        val result = store.syncRecentlyModifiedVariations(testSite, validDateString, 1, 100)

        // THEN
        assertThat(result.isSuccess).isTrue()
        result.getOrNull()?.let { syncResult ->
            assertThat(syncResult.syncedCount).isEqualTo(100)
            assertThat(syncResult.hasMore).isTrue()
            assertThat(syncResult.nextPage).isEqualTo(2)
        }
    }

    @Test
    fun `given variation not in database, when get variation called, then null returned`() = runTest {
        // GIVEN
        val variationId = RemoteId(999L)
        whenever(posVariationsDao.getVariation(testSiteId, testRemoteId, variationId))
            .thenReturn(null)

        // WHEN
        val variation = store.getVariation(testSiteId, testRemoteId, variationId).getOrThrow()

        // THEN
        assertThat(variation).isNull()
    }

    @Test
    fun `given page size over limit for variations, when sync called, then max page size enforced`() = runTest {
        // GIVEN
        val variations = Array(100) { createTestVariationApiResponse() }
        whenever(posProductRestClient.fetchVariations(testSite, validDateString, 1, 100))
            .thenReturn(WooResult(variations))

        // WHEN
        store.syncRecentlyModifiedVariations(testSite, validDateString, 1, 200)

        // THEN
        verify(posProductRestClient).fetchVariations(testSite, validDateString, 1, 100)
    }

    @Test
    fun `when generating catalog successfully, then returns job ID`() = runTest {
        // GIVEN
        val expectedJobId = 12345L
        val response = WooPosGenerateCatalogResponse(jobId = expectedJobId)
        whenever(posProductRestClient.postGenerateCatalog(testSite))
            .thenReturn(WooResult(response))

        // WHEN
        val result = store.generateCatalog(testSite).getOrThrow()

        // THEN
        assertThat(result.jobId).isEqualTo(expectedJobId.toString())
        verify(posProductRestClient).postGenerateCatalog(testSite)
    }

    @Test
    fun `when generating catalog returns null response, then returns empty response error`() = runTest {
        // GIVEN
        whenever(posProductRestClient.postGenerateCatalog(testSite))
            .thenReturn(WooResult(null))

        // WHEN
        val result = store.generateCatalog(testSite)

        // THEN
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(WooPosLocalCatalogError.EmptyResponse::class.java)
    }

    @Test
    fun `when generating catalog fails with network error, then returns network error`() = runTest {
        // GIVEN
        val networkError = WooError(
            type = WooErrorType.API_ERROR,
            original = org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR,
            message = "Network request failed"
        )
        whenever(posProductRestClient.postGenerateCatalog(testSite))
            .thenReturn(WooResult(networkError))

        // WHEN
        val result = store.generateCatalog(testSite)

        // THEN
        assertThat(result.isFailure).isTrue()
        val error = result.exceptionOrNull() as WooPosLocalCatalogError.NetworkError
        assertThat(error.errorMessage).contains("Network request failed")
        assertThat(error.code).isEqualTo("API_ERROR")
    }

    @Test
    fun `when fetching catalog status successfully, then returns status and download URL`() = runTest {
        // GIVEN
        val jobId = "12345"
        val expectedStatus = "completed"
        val expectedDownloadUrl = "https://example.com/catalog.zip"
        val response = WooPosCatalogStatusResponse(
            status = expectedStatus,
            downloadUrl = expectedDownloadUrl
        )
        whenever(posProductRestClient.getCatalogStatus(testSite, jobId))
            .thenReturn(WooResult(response))

        // WHEN
        val result = store.fetchCatalogStatus(testSite, jobId).getOrThrow()

        // THEN
        assertThat(result.status).isEqualTo(expectedStatus)
        assertThat(result.downloadUrl).isEqualTo(expectedDownloadUrl)
        verify(posProductRestClient).getCatalogStatus(testSite, jobId)
    }

    @Test
    fun `when fetching catalog status returns null response, then returns empty response error`() = runTest {
        // GIVEN
        val jobId = "12345"
        whenever(posProductRestClient.getCatalogStatus(testSite, jobId))
            .thenReturn(WooResult(null))

        // WHEN
        val result = store.fetchCatalogStatus(testSite, jobId)

        // THEN
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(WooPosLocalCatalogError.EmptyResponse::class.java)
    }

    @Test
    fun `when fetching catalog status fails with timeout, then returns timeout error`() = runTest {
        // GIVEN
        val jobId = "12345"
        val timeoutError = WooError(
            type = WooErrorType.TIMEOUT,
            original = org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.TIMEOUT,
            message = "Request timed out"
        )
        whenever(posProductRestClient.getCatalogStatus(testSite, jobId))
            .thenReturn(WooResult(timeoutError))

        // WHEN
        val result = store.fetchCatalogStatus(testSite, jobId)

        // THEN
        assertThat(result.isFailure).isTrue()
        val error = result.exceptionOrNull() as WooPosLocalCatalogError.NetworkError
        assertThat(error.errorMessage).contains("Request timed out")
        assertThat(error.code).isEqualTo("TIMEOUT")
    }

    @Test
    fun `when fetching catalog status for in-progress job, then returns processing status`() = runTest {
        // GIVEN
        val jobId = "12345"
        val response = WooPosCatalogStatusResponse(
            status = "processing",
            downloadUrl = null
        )
        whenever(posProductRestClient.getCatalogStatus(testSite, jobId))
            .thenReturn(WooResult(response))

        // WHEN
        val result = store.fetchCatalogStatus(testSite, jobId).getOrThrow()

        // THEN
        assertThat(result.status).isEqualTo("processing")
        assertThat(result.downloadUrl).isNull()
    }

    @Test
    fun `when generating catalog returns null job ID, then returns invalid response error`() = runTest {
        // GIVEN
        val response = WooPosGenerateCatalogResponse(jobId = null)
        whenever(posProductRestClient.postGenerateCatalog(testSite))
            .thenReturn(WooResult(response))

        // WHEN
        val result = store.generateCatalog(testSite)

        // THEN
        assertThat(result.isFailure).isTrue()
        val error = result.exceptionOrNull() as WooPosLocalCatalogError.InvalidResponse
        assertThat(error.message).isEqualTo("Missing job ID in response")
    }

    @Test
    fun `when fetching catalog status with null status field, then returns invalid response error`() = runTest {
        // GIVEN
        val jobId = "12345"
        val response = WooPosCatalogStatusResponse(
            status = null,
            downloadUrl = "https://example.com/catalog.zip"
        )
        whenever(posProductRestClient.getCatalogStatus(testSite, jobId))
            .thenReturn(WooResult(response))

        // WHEN
        val result = store.fetchCatalogStatus(testSite, jobId)

        // THEN
        assertThat(result.isFailure).isTrue()
        val error = result.exceptionOrNull() as WooPosLocalCatalogError.InvalidResponse
        assertThat(error.message).isEqualTo("Missing job ID in response")
    }

    @Test
    fun `when fetching catalog status with empty status field, then returns invalid response error`() = runTest {
        // GIVEN
        val jobId = "12345"
        val response = WooPosCatalogStatusResponse(
            status = "",
            downloadUrl = "https://example.com/catalog.zip"
        )
        whenever(posProductRestClient.getCatalogStatus(testSite, jobId))
            .thenReturn(WooResult(response))

        // WHEN
        val result = store.fetchCatalogStatus(testSite, jobId)

        // THEN
        assertThat(result.isFailure).isTrue()
        val error = result.exceptionOrNull() as WooPosLocalCatalogError.InvalidResponse
        assertThat(error.message).isEqualTo("Missing job ID in response")
    }

    @Test
    fun `when server date header is missing, then sync returns invalid response error`() = runTest {
        // GIVEN
        val remoteProducts = arrayOf(createTestApiResponse(id = 1L, name = "Product"))
        val response = WooResult(remoteProducts)
        whenever(posProductRestClient.fetchProducts(testSite, validDateString, 0, 100))
            .thenReturn(response)
        whenever(headersParser.getServerDate(response))
            .thenReturn(null) // Missing server date

        // WHEN
        val result = store.syncRecentlyModifiedProducts(testSite, validDateString, 0, 100)

        // THEN
        assertThat(result.isFailure).isTrue()
        val error = result.exceptionOrNull() as WooPosLocalCatalogError.InvalidResponse
        assertThat(error.message).contains("Missing required header in response, Server Date")
    }

    @Test
    fun `when total pages header is missing, then sync returns invalid response error`() = runTest {
        // GIVEN
        val remoteProducts = arrayOf(createTestApiResponse(id = 1L, name = "Product"))
        val response = WooResult(remoteProducts)
        whenever(posProductRestClient.fetchProducts(testSite, validDateString, 0, 100))
            .thenReturn(response)
        whenever(headersParser.getServerDate(response))
            .thenReturn("2024-01-01T00:00:00Z")
        whenever(headersParser.getTotalPages(response))
            .thenReturn(null) // Missing total pages

        // WHEN
        val result = store.syncRecentlyModifiedProducts(testSite, validDateString, 0, 100)

        // THEN
        assertThat(result.isFailure).isTrue()
        val error = result.exceptionOrNull() as WooPosLocalCatalogError.InvalidResponse
        assertThat(error.message).contains("Missing required header in response, Total Pages")
    }

    @Test
    fun `when headers are present, then sync includes them in result`() = runTest {
        // GIVEN
        val remoteProducts = arrayOf(createTestApiResponse(id = 1L, name = "Product"))
        val response = WooResult(remoteProducts)
        val expectedServerDate = "2024-03-15T10:30:00Z"
        val expectedTotalPages = 3

        whenever(posProductRestClient.fetchProducts(testSite, validDateString, 0, 100))
            .thenReturn(response)
        whenever(headersParser.getServerDate(response))
            .thenReturn(expectedServerDate)
        whenever(headersParser.getTotalPages(response))
            .thenReturn(expectedTotalPages)

        // WHEN
        val syncResult = store.syncRecentlyModifiedProducts(testSite, validDateString, 0, 100).getOrThrow()

        // THEN
        assertThat(syncResult.serverDate).isEqualTo(expectedServerDate)
        assertThat(syncResult.totalPages).isEqualTo(expectedTotalPages)
    }

    @Test
    fun `when storeInDb is false, then products are not saved to database`() = runTest {
        // GIVEN
        val remoteProducts = arrayOf(
            createTestApiResponse(id = 1L, name = "Product 1"),
            createTestApiResponse(id = 2L, name = "Product 2")
        )
        val response = WooResult(remoteProducts)
        whenever(posProductRestClient.fetchProducts(testSite, validDateString, 0, 100))
            .thenReturn(response)

        // WHEN
        val syncResult = store.syncRecentlyModifiedProducts(
            site = testSite,
            modifiedAfterGmt = validDateString,
            offset = 0,
            pageSize = 100,
            storeInDb = false
        ).getOrThrow()

        // THEN
        verifyNoInteractions(posProductsDao)
        assertThat(syncResult.syncedCount).isEqualTo(2)
    }

    @Test
    fun `when storeInDb is true, then products are saved to database`() = runTest {
        // GIVEN
        val remoteProducts = arrayOf(
            createTestApiResponse(id = 1L, name = "Product 1"),
            createTestApiResponse(id = 2L, name = "Product 2")
        )
        val response = WooResult(remoteProducts)
        whenever(posProductRestClient.fetchProducts(testSite, validDateString, 0, 100))
            .thenReturn(response)

        // WHEN
        val syncResult = store.syncRecentlyModifiedProducts(
            site = testSite,
            modifiedAfterGmt = validDateString,
            offset = 0,
            pageSize = 100,
            storeInDb = true
        ).getOrThrow()

        // THEN
        verify(posProductsDao).upsertProducts(any())
        assertThat(syncResult.syncedCount).isEqualTo(2)
    }

    @Test
    fun `when storeInDb is not specified, then defaults to storing in database`() = runTest {
        // GIVEN
        val remoteProducts = arrayOf(createTestApiResponse(id = 1L, name = "Product"))
        val response = WooResult(remoteProducts)
        whenever(posProductRestClient.fetchProducts(testSite, validDateString, 0, 100))
            .thenReturn(response)

        // WHEN
        store.syncRecentlyModifiedProducts(testSite, validDateString, 0, 100).getOrThrow()

        // THEN
        verify(posProductsDao).upsertProducts(any())
    }

    private fun createTestVariationApiResponse(
        id: Long = 200L,
        productId: Long = 100L
    ) = org.wordpress.android.fluxc.model.pos.WooPosVariationApiResponse(
        id = id,
        productId = productId,
        sku = "VAR-SKU-$id",
        name = "Test Variation $id",
        price = "25.00",
        regularPrice = "30.00",
        salePrice = "25.00",
        stockQuantity = 10.0,
        stockStatus = "instock",
        manageStock = true,
        status = "publish",
        description = "Test variation description",
        dateModified = "2024-01-01T00:00:00Z"
    )

    private fun createTestVariation(
        variationId: Long,
        name: String,
        price: String = "19.99"
    ) = WCPosVariationModel(
        localSiteId = testSiteId,
        remoteProductId = testRemoteId,
        remoteVariationId = RemoteId(variationId),
        variationName = name,
        price = price,
        sku = "VAR-$variationId"
    )

    object ProductTestData {
        fun coffeMug(siteId: LocalId) = WCPosProductModel(
            localSiteId = siteId,
            remoteId = RemoteId(1L),
            name = "Coffee Mug",
            description = "Ceramic coffee mug with handle",
            price = "12.99",
            images = ""
        )

        fun laptopStand(siteId: LocalId) = WCPosProductModel(
            localSiteId = siteId,
            remoteId = RemoteId(2L),
            name = "Laptop Stand",
            description = "Adjustable aluminum laptop stand",
            price = "49.99",
            images = ""
        )

        fun createProduct(
            siteId: LocalId = SITE_ID,
            remoteId: Long,
            name: String,
            price: String = "29.99"
        ) = WCPosProductModel(
            localSiteId = siteId,
            remoteId = RemoteId(remoteId),
            name = name,
            description = "High-quality product for your store",
            price = price,
            images = ""
        )

        fun createApiResponse(
            id: Long,
            name: String,
            price: String = "29.99"
        ) = ProductApiResponse(
            id = id,
            name = name,
            description = "High-quality product for your store",
            price = price
        )
    }

    private fun createTestProduct(
        localSiteId: Int = testSiteId.value,
        remoteId: Long,
        name: String
    ) = ProductTestData.createProduct(LocalId(localSiteId), remoteId, name)

    private fun createTestApiResponse(
        id: Long,
        name: String
    ) = ProductTestData.createApiResponse(id, name)
}
