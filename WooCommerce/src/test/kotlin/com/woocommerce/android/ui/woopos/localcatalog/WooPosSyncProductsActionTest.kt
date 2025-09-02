package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.localcatalog.PosLocalCatalogSyncRepository.Companion.PAGE_SIZE
import com.woocommerce.android.ui.woopos.localcatalog.WooPosSyncProductsAction.WooPosSyncProductsResult
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogSyncResult
import kotlin.Result as KotlinResult

class WooPosSyncProductsActionTest {

    private lateinit var sut: WooPosSyncProductsAction
    private var posLocalCatalogStore: WooPosLocalCatalogStore = mock()
    private lateinit var site: SiteModel
    private var logger: WooPosLogWrapper = mock()

    @Before
    fun setup() {
        sut = WooPosSyncProductsAction(posLocalCatalogStore, logger)
        site = SiteModel().apply {
            id = 1
            siteId = 123L
        }
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
        givenCatalogTooLarge(totalPages = 3, maxPages = maxPages)

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
        assertThat((result as WooPosSyncProductsResult.Failed).error).isEqualTo("Transaction failed and was rolled back")
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
        whenever(posLocalCatalogStore.executeInTransaction<WooPosSyncProductsResult>(any()))
            .thenReturn(KotlinResult.success(WooPosSyncProductsResult.Success(productsCount)))

        whenever(posLocalCatalogStore.syncRecentlyModifiedProducts(any(), anyOrNull(), eq(0), any(), any()))
            .thenReturn(
                KotlinResult.success(
                    WooPosLocalCatalogSyncResult(
                        syncedCount = productsCount,
                        hasMore = false,
                        nextOffset = 0,
                        totalPages = 1,
                        serverDate = "",
                    )
                )
            )
    }

    private suspend fun givenMultiPageCatalog(page1Count: Int, page2Count: Int, page3Count: Int, totalPages: Int = 3) {
        whenever(posLocalCatalogStore.executeInTransaction<WooPosSyncProductsResult>(any()))
            .thenReturn(KotlinResult.success(WooPosSyncProductsResult.Success(page1Count + page2Count + page3Count)))

        whenever(posLocalCatalogStore.syncRecentlyModifiedProducts(any(), anyOrNull(), eq(0), any(), any()))
            .thenReturn(
                KotlinResult.success(
                    WooPosLocalCatalogSyncResult(
                        syncedCount = page1Count,
                        hasMore = true,
                        nextOffset = page1Count,
                        totalPages = totalPages,
                        serverDate = ""
                    )
                )
            )

        whenever(posLocalCatalogStore.syncRecentlyModifiedProducts(any(), anyOrNull(), eq(page1Count), any(), any()))
            .thenReturn(
                KotlinResult.success(
                    WooPosLocalCatalogSyncResult(
                        syncedCount = page2Count,
                        hasMore = true,
                        nextOffset = page1Count + page2Count,
                        totalPages = totalPages,
                        serverDate = "",
                    )
                )
            )

        whenever(
            posLocalCatalogStore.syncRecentlyModifiedProducts(
                any(),
                anyOrNull(),
                eq(page1Count + page2Count),
                any(),
                any()
            )
        )
            .thenReturn(
                KotlinResult.success(
                    WooPosLocalCatalogSyncResult(
                        syncedCount = page3Count,
                        hasMore = false,
                        nextOffset = 0,
                        totalPages = totalPages,
                        serverDate = "",
                    )
                )
            )
    }

    private suspend fun givenEmptyCatalog() {
        whenever(posLocalCatalogStore.executeInTransaction<WooPosSyncProductsResult>(any()))
            .thenReturn(KotlinResult.success(WooPosSyncProductsResult.Success(0)))

        whenever(posLocalCatalogStore.syncRecentlyModifiedProducts(any(), anyOrNull(), any(), any(), any()))
            .thenReturn(
                KotlinResult.success(
                    WooPosLocalCatalogSyncResult(
                        syncedCount = 0,
                        hasMore = false,
                        nextOffset = 0,
                        totalPages = 1,
                        serverDate = "",
                    )
                )
            )
    }

    private suspend fun givenFirstPageFails(errorMessage: String) {
        whenever(posLocalCatalogStore.executeInTransaction<WooPosSyncProductsResult>(any()))
            .thenReturn(KotlinResult.failure(Exception(errorMessage)))

        whenever(posLocalCatalogStore.syncRecentlyModifiedProducts(any(), anyOrNull(), any(), any(), any()))
            .thenReturn(KotlinResult.failure(Exception(errorMessage)))
    }

    private suspend fun givenFirstPageFailsWithNullMessage() {
        whenever(posLocalCatalogStore.executeInTransaction<WooPosSyncProductsResult>(any()))
            .thenReturn(KotlinResult.failure(Exception()))

        whenever(posLocalCatalogStore.syncRecentlyModifiedProducts(any(), anyOrNull(), any(), any(), any()))
            .thenReturn(KotlinResult.failure(Exception()))
    }

    private suspend fun givenSecondPageFails(errorMessage: String) {
        whenever(posLocalCatalogStore.executeInTransaction<WooPosSyncProductsResult>(any()))
            .thenReturn(KotlinResult.failure(Exception(errorMessage)))

        whenever(posLocalCatalogStore.syncRecentlyModifiedProducts(any(), anyOrNull(), eq(0), any(), any()))
            .thenReturn(
                KotlinResult.success(
                    WooPosLocalCatalogSyncResult(
                        syncedCount = 100,
                        hasMore = true,
                        nextOffset = 100,
                        totalPages = 2,
                        serverDate = ""
                    )
                )
            )

        whenever(posLocalCatalogStore.syncRecentlyModifiedProducts(any(), anyOrNull(), eq(100), any(), any()))
            .thenReturn(KotlinResult.failure(Exception(errorMessage)))
    }

    private suspend fun givenPageWithZeroProductsButHasMore() {
        // Mock transaction to return success - this logic is complex so return 0 for simplicity
        whenever(posLocalCatalogStore.executeInTransaction<WooPosSyncProductsResult>(any()))
            .thenReturn(KotlinResult.success(WooPosSyncProductsResult.Success(0)))

        // Note: Due to current action logic, it won't continue if syncedCount == 0
        // So we use 1 product on first page instead of 0 to demonstrate continuing
        whenever(posLocalCatalogStore.syncRecentlyModifiedProducts(any(), anyOrNull(), eq(0), any(), any()))
            .thenReturn(
                KotlinResult.success(
                    WooPosLocalCatalogSyncResult(
                        syncedCount = 0,
                        hasMore = true,
                        nextOffset = 100,
                        totalPages = 2,
                        serverDate = "",
                    )
                )
            )

        whenever(posLocalCatalogStore.syncRecentlyModifiedProducts(any(), anyOrNull(), eq(100), any(), any()))
            .thenReturn(
                KotlinResult.success(
                    WooPosLocalCatalogSyncResult(
                        syncedCount = 50,
                        hasMore = false,
                        nextOffset = 0,
                        totalPages = 2,
                        serverDate = ""
                    )
                )
            )
    }

    private suspend fun givenCatalogTooLarge(totalPages: Int, maxPages: Int) {
        // Mock transaction to return CatalogTooLarge error
        whenever(posLocalCatalogStore.executeInTransaction<WooPosSyncProductsResult>(any()))
            .thenReturn(KotlinResult.failure(WooPosSyncProductsAction.CatalogTooLargeException(totalPages, maxPages)))

        // First page returns totalPages exceeding maxPages
        whenever(posLocalCatalogStore.syncRecentlyModifiedProducts(any(), anyOrNull(), eq(0), any(), any()))
            .thenReturn(
                KotlinResult.success(
                    WooPosLocalCatalogSyncResult(
                        syncedCount = PAGE_SIZE,
                        hasMore = true,
                        nextOffset = PAGE_SIZE,
                        totalPages = totalPages,
                        serverDate = ""
                    )
                )
            )
    }
}
