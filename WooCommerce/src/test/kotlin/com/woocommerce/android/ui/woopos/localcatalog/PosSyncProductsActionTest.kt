package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.localcatalog.PosSyncProductsAction.Result
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
import org.wordpress.android.fluxc.store.pos.PosLocalCatalogStore
import kotlin.Result as KotlinResult

class PosSyncProductsActionTest {

    private lateinit var sut: PosSyncProductsAction
    private lateinit var posLocalCatalogStore: PosLocalCatalogStore
    private lateinit var site: SiteModel

    @Before
    fun setup() {
        posLocalCatalogStore = mock()
        sut = PosSyncProductsAction(posLocalCatalogStore)
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
        val result = sut.execute(site, maxPages = 2)

        // THEN
        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).productsSynced).isEqualTo(50)
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
        val result = sut.execute(site, modifiedAfterGmt = null, maxPages = maxPages)

        // THEN
        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).productsSynced).isEqualTo(250)
    }

    @Test
    fun `when empty catalog, then returns success with zero count`() = runTest {
        // GIVEN
        val maxPages = 10
        givenEmptyCatalog()

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, maxPages = maxPages)

        // THEN
        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).productsSynced).isEqualTo(0)
    }

    @Test
    fun `when incremental sync with modifiedAfterGmt, then passes filter correctly`() = runTest {
        // GIVEN
        val modifiedAfter = "2024-01-01T00:00:00Z"
        val maxPages = 10
        givenSinglePageCatalog(productsCount = 25)

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = modifiedAfter, maxPages = maxPages)

        // THEN
        verify(posLocalCatalogStore).syncRecentlyModifiedProducts(
            site = eq(site),
            modifiedAfterGmt = eq(modifiedAfter),
            offset = eq(0),
            pageSize = eq(PosSyncProductsAction.PAGE_SIZE)
        )
        assertThat(result).isInstanceOf(Result.Success::class.java)
    }

    @Test
    fun `when catalog has exactly max pages, then returns success`() = runTest {
        // GIVEN
        val maxPages = 3
        givenMultiPageCatalog(
            page1Count = PosSyncProductsAction.PAGE_SIZE,
            page2Count = PosSyncProductsAction.PAGE_SIZE,
            page3Count = PosSyncProductsAction.PAGE_SIZE
        )

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, maxPages = maxPages)

        // THEN
        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).productsSynced).isEqualTo(300)
    }

    @Test
    fun `when catalog has one page more than limit, then returns CatalogTooLarge`() = runTest {
        // GIVEN
        val maxPages = 2
        givenMultiPageCatalog(
            page1Count = PosSyncProductsAction.PAGE_SIZE,
            page2Count = PosSyncProductsAction.PAGE_SIZE,
            page3Count = PosSyncProductsAction.PAGE_SIZE
        )

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, maxPages = maxPages)

        // THEN
        assertThat(result).isInstanceOf(Result.Failed.CatalogTooLarge::class.java)
    }

    @Test
    fun `when first page fetch fails, then returns UnexpectedError`() = runTest {
        // GIVEN
        val errorMessage = "Network error"
        givenFirstPageFails(errorMessage)

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, maxPages = 10)

        // THEN
        assertThat(result).isInstanceOf(Result.Failed.UnexpectedError::class.java)
        assertThat((result as Result.Failed).error).isEqualTo(errorMessage)
    }

    @Test
    fun `when middle page fetch fails, then returns UnexpectedError`() = runTest {
        // GIVEN
        val maxPages = 10
        val errorMessage = "API error on page 2"
        givenSecondPageFails(errorMessage)

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, maxPages = maxPages)

        // THEN
        assertThat(result).isInstanceOf(Result.Failed.UnexpectedError::class.java)
        assertThat((result as Result.Failed).error).isEqualTo(errorMessage)
    }

    @Test
    fun `when API returns null error message, then returns generic error`() = runTest {
        // GIVEN
        val maxPages = 10
        givenFirstPageFailsWithNullMessage()

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, maxPages = maxPages)

        // THEN
        assertThat(result).isInstanceOf(Result.Failed.UnexpectedError::class.java)
        assertThat((result as Result.Failed).error).isEqualTo("Unknown error")
    }

    @Test
    fun `when page has zero products but hasMore is true, then returns success`() = runTest {
        // GIVEN
        val maxPages = 10
        givenPageWithZeroProductsButHasMore()

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, maxPages = maxPages)

        // THEN
        assertThat(result).isInstanceOf(Result.Success::class.java)
        verify(posLocalCatalogStore, times(1))
            .syncRecentlyModifiedProducts(any(), anyOrNull(), any(), any())
    }

    @Test
    fun `when hasMore is false, then stops fetching pages`() = runTest {
        // GIVEN
        val maxPages = 10
        givenSinglePageCatalog(productsCount = 10)

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, maxPages = maxPages)

        // THEN
        assertThat(result).isInstanceOf(Result.Success::class.java)
        verify(posLocalCatalogStore, times(1))
            .syncRecentlyModifiedProducts(any(), anyOrNull(), any(), any())
    }

    private suspend fun givenSinglePageCatalog(productsCount: Int = PosSyncProductsAction.PAGE_SIZE / 2) {
        whenever(posLocalCatalogStore.syncRecentlyModifiedProducts(any(), anyOrNull(), eq(0), any()))
            .thenReturn(
                KotlinResult.success(
                    org.wordpress.android.fluxc.store.pos.PosLocalCatalogSyncResult(
                        syncedCount = productsCount,
                        hasMore = false,
                        nextOffset = 0,
                        totalPages = 1
                    )
                )
            )
    }

    private suspend fun givenMultiPageCatalog(page1Count: Int, page2Count: Int, page3Count: Int, totalPages: Int = 3) {
        whenever(posLocalCatalogStore.syncRecentlyModifiedProducts(any(), anyOrNull(), eq(0), any()))
            .thenReturn(
                KotlinResult.success(
                    org.wordpress.android.fluxc.store.pos.PosLocalCatalogSyncResult(
                        syncedCount = page1Count,
                        hasMore = true,
                        nextOffset = page1Count,
                        totalPages = totalPages
                    )
                )
            )

        whenever(posLocalCatalogStore.syncRecentlyModifiedProducts(any(), anyOrNull(), eq(page1Count), any()))
            .thenReturn(
                KotlinResult.success(
                    org.wordpress.android.fluxc.store.pos.PosLocalCatalogSyncResult(
                        syncedCount = page2Count,
                        hasMore = true,
                        nextOffset = page1Count + page2Count,
                        totalPages = totalPages
                    )
                )
            )

        whenever(
            posLocalCatalogStore.syncRecentlyModifiedProducts(
                any(),
                anyOrNull(),
                eq(page1Count + page2Count),
                any()
            )
        )
            .thenReturn(
                KotlinResult.success(
                    org.wordpress.android.fluxc.store.pos.PosLocalCatalogSyncResult(
                        syncedCount = page3Count,
                        hasMore = false,
                        nextOffset = 0,
                        totalPages = totalPages
                    )
                )
            )
    }

    private suspend fun givenEmptyCatalog() {
        whenever(posLocalCatalogStore.syncRecentlyModifiedProducts(any(), anyOrNull(), any(), any()))
            .thenReturn(
                KotlinResult.success(
                    org.wordpress.android.fluxc.store.pos.PosLocalCatalogSyncResult(
                        syncedCount = 0,
                        hasMore = false,
                        nextOffset = 0,
                        totalPages = 1
                    )
                )
            )
    }

    private suspend fun givenFirstPageFails(errorMessage: String) {
        whenever(posLocalCatalogStore.syncRecentlyModifiedProducts(any(), anyOrNull(), any(), any()))
            .thenReturn(KotlinResult.failure(Exception(errorMessage)))
    }

    private suspend fun givenFirstPageFailsWithNullMessage() {
        whenever(posLocalCatalogStore.syncRecentlyModifiedProducts(any(), anyOrNull(), any(), any()))
            .thenReturn(KotlinResult.failure(Exception()))
    }

    private suspend fun givenSecondPageFails(errorMessage: String) {
        whenever(posLocalCatalogStore.syncRecentlyModifiedProducts(any(), anyOrNull(), eq(0), any()))
            .thenReturn(
                KotlinResult.success(
                    org.wordpress.android.fluxc.store.pos.PosLocalCatalogSyncResult(
                        syncedCount = 100,
                        hasMore = true,
                        nextOffset = 100,
                        totalPages = 2
                    )
                )
            )

        whenever(posLocalCatalogStore.syncRecentlyModifiedProducts(any(), anyOrNull(), eq(100), any()))
            .thenReturn(KotlinResult.failure(Exception(errorMessage)))
    }

    private suspend fun givenPageWithZeroProductsButHasMore() {
        // Note: Due to current action logic, it won't continue if syncedCount == 0
        // So we use 1 product on first page instead of 0 to demonstrate continuing
        whenever(posLocalCatalogStore.syncRecentlyModifiedProducts(any(), anyOrNull(), eq(0), any()))
            .thenReturn(
                KotlinResult.success(
                    org.wordpress.android.fluxc.store.pos.PosLocalCatalogSyncResult(
                        syncedCount = 0,
                        hasMore = true,
                        nextOffset = 100,
                        totalPages = 2
                    )
                )
            )

        whenever(posLocalCatalogStore.syncRecentlyModifiedProducts(any(), anyOrNull(), eq(100), any()))
            .thenReturn(
                KotlinResult.success(
                    org.wordpress.android.fluxc.store.pos.PosLocalCatalogSyncResult(
                        syncedCount = 50,
                        hasMore = false,
                        nextOffset = 0,
                        totalPages = 2
                    )
                )
            )
    }
}
