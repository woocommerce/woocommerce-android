package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.localcatalog.WooPosLocalCatalogSyncRepository.Companion.PAGE_SIZE
import com.woocommerce.android.ui.woopos.localcatalog.WooPosSyncProductsAction.WooPosSyncProductsResult
import com.woocommerce.android.util.InlineClassesAnswer
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.entity.pos.WCPosProductEntity
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogFetchProductsResult
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import kotlin.Result
import kotlin.Result as KotlinResult

class WooPosSyncProductsActionTest {

    private lateinit var sut: WooPosSyncProductsAction
    private var posLocalCatalogStore: WooPosLocalCatalogStore = mock()
    private lateinit var site: SiteModel
    private var logger: WooPosLogWrapper = mock()

    @Before
    fun setup() = runBlocking {
        sut = WooPosSyncProductsAction(posLocalCatalogStore, logger)
        site = SiteModel().apply {
            id = 1
            siteId = 123L
        }

        whenever(posLocalCatalogStore.upsertProducts(any()))
            .doAnswer(InlineClassesAnswer { KotlinResult.success(Unit) })
        Unit
    }

    @Test
    fun `when catalog has single page, then returns success with correct count`() = runTest {
        // GIVEN
        givenSinglePageCatalog(productsCount = 50)

        // WHEN
        val result = sut.execute(site, pageSize = 100, maxPages = 2)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncProductsResult.Success::class.java)
        assertThat((result as WooPosSyncProductsResult.Success).productsSynced).isEqualTo(50)
    }

    @Test
    fun `when catalog has multiple pages within limit, then returns success with total count`() = runTest {
        // GIVEN
        val maxPages = 10
        givenMultiPageCatalog(
            page1Count = 100,
            page2Count = 100,
            page3Count = 50
        )

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, pageSize = 100, maxPages = maxPages)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncProductsResult.Success::class.java)
        assertThat((result as WooPosSyncProductsResult.Success).productsSynced).isEqualTo(250)
    }

    @Test
    fun `when empty catalog, then returns success with zero count`() = runTest {
        // GIVEN
        val maxPages = 10
        givenEmptyCatalog()

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, pageSize = 100, maxPages = maxPages)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncProductsResult.Success::class.java)
        assertThat((result as WooPosSyncProductsResult.Success).productsSynced).isEqualTo(0)
    }

    @Test
    fun `when incremental sync with modifiedAfterGmt, then passes filter correctly`() = runTest {
        // GIVEN
        val modifiedAfter = "2024-01-01T00:00:00Z"
        val maxPages = 10
        givenSinglePageCatalog(productsCount = 25)

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = modifiedAfter, pageSize = 100, maxPages = maxPages)

        // THEN
        verify(posLocalCatalogStore).executeInTransaction<WooPosSyncProductsResult>(any())
        assertThat(result).isInstanceOf(WooPosSyncProductsResult.Success::class.java)
    }

    @Test
    fun `when catalog has exactly max pages, then returns success`() = runTest {
        // GIVEN
        val maxPages = 3
        givenMultiPageCatalog(
            page1Count = PAGE_SIZE,
            page2Count = PAGE_SIZE,
            page3Count = PAGE_SIZE
        )

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, pageSize = 100, maxPages = maxPages)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncProductsResult.Success::class.java)
        assertThat((result as WooPosSyncProductsResult.Success).productsSynced).isEqualTo(300)
    }

    @Test
    fun `when catalog has one page more than limit, then returns CatalogTooLarge`() = runTest {
        // GIVEN
        val maxPages = 2
        givenCatalogTooLarge(totalPages = 3)

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, pageSize = 100, maxPages = maxPages)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncProductsResult.Failed.CatalogTooLarge::class.java)
    }

    @Test
    fun `when first page fetch fails, then returns UnexpectedError`() = runTest {
        // GIVEN
        val errorMessage = "Network error"
        givenFirstPageFails(errorMessage)

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, pageSize = 100, maxPages = 10)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncProductsResult.Failed.UnexpectedError::class.java)
        assertThat((result as WooPosSyncProductsResult.Failed).error).contains(errorMessage)
    }

    @Test
    fun `when middle page fetch fails, then returns UnexpectedError`() = runTest {
        // GIVEN
        val maxPages = 10
        val errorMessage = "API error on page 2"
        givenSecondPageFails(errorMessage)

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, pageSize = 100, maxPages = maxPages)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncProductsResult.Failed.UnexpectedError::class.java)
        assertThat((result as WooPosSyncProductsResult.Failed).error).contains(errorMessage)
    }

    @Test
    fun `when API returns null error message, then returns generic error`() = runTest {
        // GIVEN
        val maxPages = 10
        givenFirstPageFailsWithNullMessage()

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, pageSize = 100, maxPages = maxPages)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncProductsResult.Failed.UnexpectedError::class.java)
    }

    @Test
    fun `when page has zero products but hasMore is true, then returns success`() = runTest {
        // GIVEN
        val maxPages = 10
        givenPageWithZeroProductsButHasMore()

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, pageSize = 100, maxPages = maxPages)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncProductsResult.Success::class.java)
        verify(posLocalCatalogStore, times(1))
            .executeInTransaction<WooPosSyncProductsResult>(any())
    }

    @Test
    fun `when hasMore is false, then stops fetching pages`() = runTest {
        // GIVEN
        val maxPages = 10
        givenSinglePageCatalog(productsCount = 10)

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, pageSize = 100, maxPages = maxPages)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncProductsResult.Success::class.java)
        verify(posLocalCatalogStore, times(1))
            .executeInTransaction<WooPosSyncProductsResult>(any())
    }

    @Test
    fun `when syncing multiple pages, then executes all operations in single transaction`() = runTest {
        // GIVEN
        givenMultiPageCatalog(
            page1Count = 100,
            page2Count = 100,
            page3Count = 50
        )

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, pageSize = 100, maxPages = 10)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncProductsResult.Success::class.java)
        assertThat((result as WooPosSyncProductsResult.Success).productsSynced).isEqualTo(250)

        verify(posLocalCatalogStore, times(1))
            .executeInTransaction<WooPosSyncProductsResult>(any())
    }

    private suspend fun givenSinglePageCatalog(productsCount: Int = PAGE_SIZE / 2) {
        val mockProducts = createMockProducts(1, productsCount)

        whenever(posLocalCatalogStore.executeInTransaction<WooPosSyncProductsResult>(any()))
            .doAnswer(InlineClassesAnswer { KotlinResult.success(Result.success(Unit)) })

        mockFetchRecentlyModifiedProductsSuccess(
            offset = 0,
            mockProducts = mockProducts,
            syncedCount = productsCount,
            hasMore = false,
            nextOffset = 0,
            totalPages = 1,
        )
    }

    private suspend fun givenMultiPageCatalog(page1Count: Int, page2Count: Int, page3Count: Int, totalPages: Int = 3) {
        val mockPage1Products = createMockProducts(1, page1Count)
        val mockPage2Products = createMockProducts(page1Count + 1, page1Count + page2Count)
        val mockPage3Products = createMockProducts(
            page1Count + page2Count + 1,
            page1Count + page2Count + page3Count
        )

        whenever(posLocalCatalogStore.executeInTransaction<WooPosSyncProductsResult>(any()))
            .doAnswer(InlineClassesAnswer { KotlinResult.success(Result.success(Unit)) })

        mockFetchRecentlyModifiedProductsSuccess(
            offset = 0,
            mockProducts = mockPage1Products,
            syncedCount = page1Count,
            hasMore = true,
            nextOffset = page1Count,
            totalPages = totalPages,
        )

        mockFetchRecentlyModifiedProductsSuccess(
            offset = page1Count,
            mockProducts = mockPage2Products,
            syncedCount = page2Count,
            hasMore = true,
            nextOffset = page1Count + page2Count,
            totalPages = totalPages,
        )

        mockFetchRecentlyModifiedProductsSuccess(
            offset = page1Count + page2Count,
            mockProducts = mockPage3Products,
            syncedCount = page3Count,
            hasMore = false,
            nextOffset = 0,
            totalPages = totalPages,
        )
    }

    private suspend fun givenEmptyCatalog() {
        whenever(posLocalCatalogStore.executeInTransaction<WooPosSyncProductsResult>(any()))
            .doAnswer(InlineClassesAnswer { KotlinResult.success(Result.success(Unit)) })

        mockFetchRecentlyModifiedProductsSuccess(
            offset = 0,
            mockProducts = emptyList(),
            syncedCount = 0,
            hasMore = false,
            nextOffset = 0,
            totalPages = 1,
        )
    }

    private suspend fun givenFirstPageFails(errorMessage: String) {
        whenever(posLocalCatalogStore.fetchRecentlyModifiedProducts(any(), anyOrNull(), any(), any())).doAnswer(
            InlineClassesAnswer<Result<WooPosLocalCatalogFetchProductsResult>> {
                KotlinResult.failure(Exception(errorMessage))
            }
        )
    }

    private suspend fun givenFirstPageFailsWithNullMessage() {
        whenever(posLocalCatalogStore.fetchRecentlyModifiedProducts(any(), anyOrNull(), any(), any())).doAnswer(
            InlineClassesAnswer<Result<WooPosLocalCatalogFetchProductsResult>> {
                KotlinResult.failure(Exception())
            }
        )
    }

    private suspend fun givenSecondPageFails(errorMessage: String) {
        val mockPage1Products = createMockProducts(1, 100)

        mockFetchRecentlyModifiedProductsSuccess(
            offset = 0,
            mockProducts = mockPage1Products,
            syncedCount = 100,
            hasMore = true,
            nextOffset = 100,
            totalPages = 2,
        )

        whenever(posLocalCatalogStore.fetchRecentlyModifiedProducts(any(), anyOrNull(), eq(100), any())).doAnswer(
            InlineClassesAnswer<Result<WooPosLocalCatalogFetchProductsResult>> {
                KotlinResult.failure(
                    Exception(errorMessage)
                )
            }
        )
    }

    private suspend fun givenPageWithZeroProductsButHasMore() {
        val mockPage2Products = createMockProducts(1, 50)

        whenever(posLocalCatalogStore.executeInTransaction<WooPosSyncProductsResult>(any()))
            .doAnswer(InlineClassesAnswer { KotlinResult.success(Result.success(Unit)) })

        mockFetchRecentlyModifiedProductsSuccess(
            offset = 0,
            mockProducts = emptyList(),
            syncedCount = 0,
            hasMore = true,
            nextOffset = 100,
            totalPages = 2,
        )

        mockFetchRecentlyModifiedProductsSuccess(
            offset = 100,
            mockProducts = mockPage2Products,
            syncedCount = 50,
            hasMore = true,
            nextOffset = 0,
            totalPages = 2,
        )
    }

    private suspend fun givenCatalogTooLarge(totalPages: Int) {
        val mockProducts = createMockProducts(1, PAGE_SIZE)

        mockFetchRecentlyModifiedProductsSuccess(
            offset = 0,
            mockProducts,
            totalPages,
            nextOffset = PAGE_SIZE,
            syncedCount = PAGE_SIZE,
            hasMore = true
        )
    }

    @Suppress("LongParameterList")
    private suspend fun mockFetchRecentlyModifiedProductsSuccess(
        offset: Int,
        mockProducts: List<WCPosProductEntity>,
        totalPages: Int,
        nextOffset: Int,
        syncedCount: Int,
        hasMore: Boolean
    ) {
        whenever(posLocalCatalogStore.fetchRecentlyModifiedProducts(any(), anyOrNull(), eq(offset), any()))
            .doAnswer(
                InlineClassesAnswer {
                    KotlinResult.success(
                        WooPosLocalCatalogFetchProductsResult(
                            products = mockProducts,
                            syncedCount = syncedCount,
                            hasMore = hasMore,
                            nextOffset = nextOffset,
                            totalPages = totalPages,
                            serverDate = ""
                        )
                    )
                }
            )
    }

    private fun createMockProducts(start: Int = 1, end: Int): List<WCPosProductEntity> {
        return (start..end).map {
            WCPosProductEntity(
                remoteId = LocalOrRemoteId.RemoteId(it.toLong()),
                name = "Product $it"
            )
        }
    }
}
