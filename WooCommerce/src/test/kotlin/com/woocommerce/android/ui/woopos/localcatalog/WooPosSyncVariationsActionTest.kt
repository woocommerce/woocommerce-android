package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.localcatalog.PosLocalCatalogSyncRepository.Companion.PAGE_SIZE
import com.woocommerce.android.ui.woopos.localcatalog.WooPosSyncVariationsAction.WooPosSyncVariationsResult
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
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosVariationsSyncResult
import kotlin.Result as KotlinResult

class WooPosSyncVariationsActionTest {

    private lateinit var sut: WooPosSyncVariationsAction
    private var posLocalCatalogStore: WooPosLocalCatalogStore = mock()
    private lateinit var site: SiteModel
    private var logger: WooPosLogWrapper = mock()

    @Before
    fun setup() {
        sut = WooPosSyncVariationsAction(posLocalCatalogStore, logger)
        site = SiteModel().apply {
            id = 1
            siteId = 123L
        }
    }

    @Test
    fun `given single page catalog, when sync variations called, then returns success with correct count`() = runTest {
        // GIVEN
        givenSinglePageCatalog(variationsCount = 50)

        // WHEN
        val result = sut.execute(site, pageSize = 100, maxPages = 2)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncVariationsResult.Success::class.java)
        assertThat((result as WooPosSyncVariationsResult.Success).variationsSynced).isEqualTo(50)
    }

    @Test
    fun `given single page catalog, when sync variations called, then returns server date`() = runTest {
        // GIVEN
        val expectedServerDate = "2024-01-15T10:30:00Z"
        givenSinglePageCatalog(variationsCount = 25, serverDate = expectedServerDate)

        // WHEN
        val result = sut.execute(site, pageSize = 100, maxPages = 2)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncVariationsResult.Success::class.java)
        assertThat((result as WooPosSyncVariationsResult.Success).serverDate).isEqualTo(expectedServerDate)
    }

    @Test
    fun `given multiple pages within limit, when sync variations called, then returns success with total count`() =
        runTest {
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
            assertThat(result).isInstanceOf(WooPosSyncVariationsResult.Success::class.java)
            assertThat((result as WooPosSyncVariationsResult.Success).variationsSynced).isEqualTo(250)
        }

    @Test
    fun `given empty catalog, when sync variations called, then returns success with zero count`() = runTest {
        // GIVEN
        val maxPages = 10
        givenEmptyCatalog()

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, pageSize = 100, maxPages = maxPages)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncVariationsResult.Success::class.java)
        assertThat((result as WooPosSyncVariationsResult.Success).variationsSynced).isEqualTo(0)
    }

    @Test
    fun `given incremental sync with modifiedAfterGmt, when sync variations called, then passes filter correctly`() =
        runTest {
            // GIVEN
            val modifiedAfter = "2024-01-01T00:00:00Z"
            val maxPages = 10
            givenSinglePageCatalog(variationsCount = 25)

            // WHEN
            val result = sut.execute(site, modifiedAfterGmt = modifiedAfter, pageSize = 100, maxPages = maxPages)

            // THEN
            verify(posLocalCatalogStore).syncRecentlyModifiedVariations(
                site = eq(site),
                modifiedAfterGmt = eq(modifiedAfter),
                page = eq(1),
                pageSize = eq(100)
            )
            assertThat(result).isInstanceOf(WooPosSyncVariationsResult.Success::class.java)
        }

    @Test
    fun `given null modifiedAfterGmt, when sync variations called, then passes empty string as filter`() = runTest {
        // GIVEN
        val maxPages = 10
        givenSinglePageCatalog(variationsCount = 30)

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, pageSize = 100, maxPages = maxPages)

        // THEN
        verify(posLocalCatalogStore).syncRecentlyModifiedVariations(
            site = eq(site),
            modifiedAfterGmt = eq(""),
            page = eq(1),
            pageSize = eq(100)
        )
        assertThat(result).isInstanceOf(WooPosSyncVariationsResult.Success::class.java)
    }

    @Test
    fun `given catalog has exactly max pages, when sync variations called, then returns success`() = runTest {
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
        assertThat(result).isInstanceOf(WooPosSyncVariationsResult.Success::class.java)
        assertThat((result as WooPosSyncVariationsResult.Success).variationsSynced).isEqualTo(300)
    }

    @Test
    fun `given catalog has more pages than limit, when sync variations called, then returns CatalogTooLarge`() =
        runTest {
            // GIVEN
            val maxPages = 2
            givenMultiPageCatalog(
                page1Count = PAGE_SIZE,
                page2Count = PAGE_SIZE,
                page3Count = PAGE_SIZE,
                totalPages = 3
            )

            // WHEN
            val result = sut.execute(site, modifiedAfterGmt = null, pageSize = 100, maxPages = maxPages)

            // THEN
            assertThat(result).isInstanceOf(WooPosSyncVariationsResult.Failed.CatalogTooLarge::class.java)
            val failureResult = result as WooPosSyncVariationsResult.Failed.CatalogTooLarge
            assertThat(failureResult.totalPages).isEqualTo(3)
            assertThat(failureResult.maxPages).isEqualTo(maxPages)
        }

    @Test
    fun `given first page fetch fails, when sync variations called, then returns UnexpectedError`() = runTest {
        // GIVEN
        val errorMessage = "Network error"
        givenFirstPageFails(errorMessage)

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, pageSize = 100, maxPages = 10)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncVariationsResult.Failed.UnexpectedError::class.java)
        assertThat((result as WooPosSyncVariationsResult.Failed).error).isEqualTo(errorMessage)
    }

    @Test
    fun `given middle page fetch fails, when sync variations called, then returns UnexpectedError`() = runTest {
        // GIVEN
        val maxPages = 10
        val errorMessage = "API error on page 2"
        givenSecondPageFails(errorMessage)

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, pageSize = 100, maxPages = maxPages)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncVariationsResult.Failed.UnexpectedError::class.java)
        assertThat((result as WooPosSyncVariationsResult.Failed).error).isEqualTo(errorMessage)
    }

    @Test
    fun `given API returns null error message, when sync variations called, then returns generic error`() = runTest {
        // GIVEN
        val maxPages = 10
        givenFirstPageFailsWithNullMessage()

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, pageSize = 100, maxPages = maxPages)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncVariationsResult.Failed.UnexpectedError::class.java)
        assertThat((result as WooPosSyncVariationsResult.Failed).error).isEqualTo("Unknown error")
    }

    @Test
    fun `given page has zero variations but hasMore is true, when sync variations called, then stops fetching`() =
        runTest {
            // GIVEN
            val maxPages = 10
            givenPageWithZeroVariationsButHasMore()

            // WHEN
            val result = sut.execute(site, modifiedAfterGmt = null, pageSize = 100, maxPages = maxPages)

            // THEN
            assertThat(result).isInstanceOf(WooPosSyncVariationsResult.Success::class.java)
            verify(posLocalCatalogStore, times(1))
                .syncRecentlyModifiedVariations(any(), anyOrNull(), any(), any())
        }

    @Test
    fun `given hasMore is false, when sync variations called, then stops fetching pages`() = runTest {
        // GIVEN
        val maxPages = 10
        givenSinglePageCatalog(variationsCount = 10)

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, pageSize = 100, maxPages = maxPages)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncVariationsResult.Success::class.java)
        verify(posLocalCatalogStore, times(1))
            .syncRecentlyModifiedVariations(any(), anyOrNull(), any(), any())
    }

    @Test
    fun `given multiple pages synced, when sync variations called, then returns last server date`() = runTest {
        // GIVEN
        val maxPages = 10
        val lastServerDate = "2024-01-15T15:45:00Z"
        givenMultiPageCatalog(
            page1Count = 100,
            page2Count = 50,
            page3Count = 0,
            serverDate3 = lastServerDate
        )

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, pageSize = 100, maxPages = maxPages)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncVariationsResult.Success::class.java)
        assertThat((result as WooPosSyncVariationsResult.Success).serverDate).isEqualTo(lastServerDate)
    }

    @Test
    fun `given pagination uses page numbers, when sync variations called, then increments page correctly`() = runTest {
        // GIVEN
        val maxPages = 10
        givenThreePageCatalogWithPageNumbers()

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, pageSize = 100, maxPages = maxPages)

        // THEN
        verify(posLocalCatalogStore).syncRecentlyModifiedVariations(any(), anyOrNull(), eq(1), any())
        verify(posLocalCatalogStore).syncRecentlyModifiedVariations(any(), anyOrNull(), eq(2), any())
        verify(posLocalCatalogStore).syncRecentlyModifiedVariations(any(), anyOrNull(), eq(3), any())
        assertThat(result).isInstanceOf(WooPosSyncVariationsResult.Success::class.java)
    }

    // Helper functions
    private suspend fun givenSinglePageCatalog(
        variationsCount: Int = PAGE_SIZE / 2,
        serverDate: String = "2024-01-15T10:00:00Z"
    ) {
        whenever(posLocalCatalogStore.syncRecentlyModifiedVariations(any(), anyOrNull(), eq(1), any()))
            .thenReturn(
                KotlinResult.success(
                    WooPosVariationsSyncResult(
                        syncedCount = variationsCount,
                        hasMore = false,
                        nextPage = 1,
                        totalPages = 1,
                        serverDate = serverDate
                    )
                )
            )
    }

    private suspend fun givenMultiPageCatalog(
        page1Count: Int,
        page2Count: Int,
        page3Count: Int,
        totalPages: Int = 3,
        serverDate3: String = "2024-01-15T12:00:00Z"
    ) {
        whenever(posLocalCatalogStore.syncRecentlyModifiedVariations(any(), anyOrNull(), eq(1), any()))
            .thenReturn(
                KotlinResult.success(
                    WooPosVariationsSyncResult(
                        syncedCount = page1Count,
                        hasMore = true,
                        nextPage = 2,
                        totalPages = totalPages,
                        serverDate = "2024-01-15T10:00:00Z"
                    )
                )
            )

        whenever(posLocalCatalogStore.syncRecentlyModifiedVariations(any(), anyOrNull(), eq(2), any()))
            .thenReturn(
                KotlinResult.success(
                    WooPosVariationsSyncResult(
                        syncedCount = page2Count,
                        hasMore = page3Count > 0,
                        nextPage = 3,
                        totalPages = totalPages,
                        serverDate = "2024-01-15T11:00:00Z"
                    )
                )
            )

        whenever(posLocalCatalogStore.syncRecentlyModifiedVariations(any(), anyOrNull(), eq(3), any()))
            .thenReturn(
                KotlinResult.success(
                    WooPosVariationsSyncResult(
                        syncedCount = page3Count,
                        hasMore = false,
                        nextPage = 3,
                        totalPages = totalPages,
                        serverDate = serverDate3
                    )
                )
            )
    }

    private suspend fun givenEmptyCatalog() {
        whenever(posLocalCatalogStore.syncRecentlyModifiedVariations(any(), anyOrNull(), any(), any()))
            .thenReturn(
                KotlinResult.success(
                    WooPosVariationsSyncResult(
                        syncedCount = 0,
                        hasMore = false,
                        nextPage = 1,
                        totalPages = 0,
                        serverDate = "2024-01-15T10:00:00Z"
                    )
                )
            )
    }

    private suspend fun givenFirstPageFails(errorMessage: String) {
        whenever(posLocalCatalogStore.syncRecentlyModifiedVariations(any(), anyOrNull(), any(), any()))
            .thenReturn(KotlinResult.failure(Exception(errorMessage)))
    }

    private suspend fun givenFirstPageFailsWithNullMessage() {
        whenever(posLocalCatalogStore.syncRecentlyModifiedVariations(any(), anyOrNull(), any(), any()))
            .thenReturn(KotlinResult.failure(Exception()))
    }

    private suspend fun givenSecondPageFails(errorMessage: String) {
        whenever(posLocalCatalogStore.syncRecentlyModifiedVariations(any(), anyOrNull(), eq(1), any()))
            .thenReturn(
                KotlinResult.success(
                    WooPosVariationsSyncResult(
                        syncedCount = 100,
                        hasMore = true,
                        nextPage = 2,
                        totalPages = 2,
                        serverDate = "2024-01-15T10:00:00Z"
                    )
                )
            )

        whenever(posLocalCatalogStore.syncRecentlyModifiedVariations(any(), anyOrNull(), eq(2), any()))
            .thenReturn(KotlinResult.failure(Exception(errorMessage)))
    }

    private suspend fun givenPageWithZeroVariationsButHasMore() {
        whenever(posLocalCatalogStore.syncRecentlyModifiedVariations(any(), anyOrNull(), eq(1), any()))
            .thenReturn(
                KotlinResult.success(
                    WooPosVariationsSyncResult(
                        syncedCount = 0,
                        hasMore = true,
                        nextPage = 2,
                        totalPages = 2,
                        serverDate = "2024-01-15T10:00:00Z"
                    )
                )
            )

        whenever(posLocalCatalogStore.syncRecentlyModifiedVariations(any(), anyOrNull(), eq(2), any()))
            .thenReturn(
                KotlinResult.success(
                    WooPosVariationsSyncResult(
                        syncedCount = 50,
                        hasMore = false,
                        nextPage = 2,
                        totalPages = 2,
                        serverDate = "2024-01-15T11:00:00Z"
                    )
                )
            )
    }

    private suspend fun givenThreePageCatalogWithPageNumbers() {
        whenever(posLocalCatalogStore.syncRecentlyModifiedVariations(any(), anyOrNull(), eq(1), any()))
            .thenReturn(
                KotlinResult.success(
                    WooPosVariationsSyncResult(
                        syncedCount = 100,
                        hasMore = true,
                        nextPage = 2,
                        totalPages = 3,
                        serverDate = "2024-01-15T10:00:00Z"
                    )
                )
            )

        whenever(posLocalCatalogStore.syncRecentlyModifiedVariations(any(), anyOrNull(), eq(2), any()))
            .thenReturn(
                KotlinResult.success(
                    WooPosVariationsSyncResult(
                        syncedCount = 100,
                        hasMore = true,
                        nextPage = 3,
                        totalPages = 3,
                        serverDate = "2024-01-15T11:00:00Z"
                    )
                )
            )

        whenever(posLocalCatalogStore.syncRecentlyModifiedVariations(any(), anyOrNull(), eq(3), any()))
            .thenReturn(
                KotlinResult.success(
                    WooPosVariationsSyncResult(
                        syncedCount = 50,
                        hasMore = false,
                        nextPage = 3,
                        totalPages = 3,
                        serverDate = "2024-01-15T12:00:00Z"
                    )
                )
            )
    }
}
