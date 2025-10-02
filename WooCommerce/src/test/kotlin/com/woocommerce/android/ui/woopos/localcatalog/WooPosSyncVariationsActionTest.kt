package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.localcatalog.WooPosLocalCatalogSyncRepository.Companion.PAGE_SIZE
import com.woocommerce.android.ui.woopos.localcatalog.WooPosSyncVariationsAction.WooPosSyncVariationsResult
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
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosVariationEntity
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosVariationsFetchResult
import kotlin.Result as KotlinResult

class WooPosSyncVariationsActionTest {

    private lateinit var sut: WooPosSyncVariationsAction
    private var posLocalCatalogStore: WooPosLocalCatalogStore = mock()
    private lateinit var site: SiteModel
    private var logger: WooPosLogWrapper = mock()

    @Before
    fun setup() = runBlocking {
        sut = WooPosSyncVariationsAction(posLocalCatalogStore, logger)
        site = SiteModel().apply {
            id = 1
            siteId = 123L
        }
        givenTransactionSuccess()
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
            verify(posLocalCatalogStore).fetchRecentlyModifiedVariations(
                site = eq(site),
                modifiedAfterGmt = eq(modifiedAfter),
                page = eq(1),
                pageSize = eq(100)
            )
            assertThat(result).isInstanceOf(WooPosSyncVariationsResult.Success::class.java)
        }

    @Test
    fun `given null modifiedAfterGmt, when sync variations called, then passes null as filter`() = runTest {
        // GIVEN
        val maxPages = 10
        givenSinglePageCatalog(variationsCount = 30)

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, pageSize = 100, maxPages = maxPages)

        // THEN
        verify(posLocalCatalogStore).fetchRecentlyModifiedVariations(
            site = eq(site),
            modifiedAfterGmt = eq(null),
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
        assertThat((result as WooPosSyncVariationsResult.Failed).error).isEqualTo("Failed to sync variations")
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
                .fetchRecentlyModifiedVariations(any(), anyOrNull(), any(), any())
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
            .fetchRecentlyModifiedVariations(any(), anyOrNull(), any(), any())
    }

    @Test
    fun `given multiple pages synced, when sync variations called, then returns first server date`() = runTest {
        // GIVEN
        val maxPages = 10
        val firstServerDate = "2024-01-15T15:45:00Z"
        givenMultiPageCatalog(
            page1Count = 100,
            page2Count = 50,
            page3Count = 0,
            serverDate = firstServerDate
        )

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, pageSize = 100, maxPages = maxPages)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncVariationsResult.Success::class.java)
        assertThat((result as WooPosSyncVariationsResult.Success).serverDate).isEqualTo(firstServerDate)
    }

    @Test
    fun `given pagination uses offsets, when sync variations called, then increments offset correctly`() = runTest {
        // GIVEN
        val maxPages = 10
        givenThreePageCatalogWithOffsets()

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, pageSize = 100, maxPages = maxPages)

        // THEN
        verify(posLocalCatalogStore).fetchRecentlyModifiedVariations(any(), anyOrNull(), eq(1), any())
        verify(posLocalCatalogStore).fetchRecentlyModifiedVariations(any(), anyOrNull(), eq(2), any())
        verify(posLocalCatalogStore).fetchRecentlyModifiedVariations(any(), anyOrNull(), eq(3), any())
        assertThat(result).isInstanceOf(WooPosSyncVariationsResult.Success::class.java)
    }

    // Helper functions
    private suspend fun givenSinglePageCatalog(
        variationsCount: Int = PAGE_SIZE / 2,
        serverDate: String = "2024-01-15T10:00:00Z"
    ) {
        val variations = createMockVariations(1, variationsCount)
        whenever(posLocalCatalogStore.fetchRecentlyModifiedVariations(any(), anyOrNull(), eq(1), any()))
            .doAnswer(
                InlineClassesAnswer {
                    KotlinResult.success(
                        WooPosVariationsFetchResult(
                            variations = variations,
                            syncedCount = variationsCount,
                            hasMore = false,
                            nextPage = 2,
                            totalPages = 1,
                            serverDate = serverDate
                        )
                    )
                }
            )
    }

    private suspend fun givenMultiPageCatalog(
        page1Count: Int,
        page2Count: Int,
        page3Count: Int,
        totalPages: Int = 3,
        serverDate: String = "2024-01-15T12:00:00Z"
    ) {
        val variations1 = createMockVariations(1, page1Count)
        val variations2 = createMockVariations(page1Count + 1, page2Count)
        val variations3 = createMockVariations(page1Count + page2Count + 1, page3Count)

        whenever(posLocalCatalogStore.fetchRecentlyModifiedVariations(any(), anyOrNull(), eq(1), any()))
            .doAnswer(
                InlineClassesAnswer {
                    KotlinResult.success(
                        WooPosVariationsFetchResult(
                            variations = variations1,
                            syncedCount = page1Count,
                            hasMore = true,
                            nextPage = 2,
                            totalPages = totalPages,
                            serverDate = serverDate
                        )
                    )
                }
            )

        whenever(posLocalCatalogStore.fetchRecentlyModifiedVariations(any(), anyOrNull(), eq(2), any()))
            .doAnswer(
                InlineClassesAnswer {
                    KotlinResult.success(
                        WooPosVariationsFetchResult(
                            variations = variations2,
                            syncedCount = page2Count,
                            hasMore = page3Count > 0,
                            nextPage = 3,
                            totalPages = totalPages,
                            serverDate = "2024-01-15T11:00:00Z",
                        )
                    )
                }
            )

        whenever(
            posLocalCatalogStore.fetchRecentlyModifiedVariations(
                any(),
                anyOrNull(),
                eq(3),
                any()
            )
        )
            .doAnswer(
                InlineClassesAnswer {
                    KotlinResult.success(
                        WooPosVariationsFetchResult(
                            variations = variations3,
                            syncedCount = page3Count,
                            hasMore = false,
                            nextPage = 4,
                            totalPages = totalPages,
                            serverDate = "2024-01-15T11:00:00Z",
                        )
                    )
                }
            )
    }

    private suspend fun givenEmptyCatalog() {
        whenever(posLocalCatalogStore.fetchRecentlyModifiedVariations(any(), anyOrNull(), any(), any()))
            .doAnswer(
                InlineClassesAnswer {
                    KotlinResult.success(
                        WooPosVariationsFetchResult(
                            variations = emptyList(),
                            syncedCount = 0,
                            hasMore = false,
                            nextPage = 1,
                            totalPages = 0,
                            serverDate = "2024-01-15T10:00:00Z"
                        )
                    )
                }
            )
    }

    private suspend fun givenFirstPageFails(errorMessage: String) {
        whenever(posLocalCatalogStore.fetchRecentlyModifiedVariations(any(), anyOrNull(), any(), any()))
            .thenReturn(KotlinResult.failure(Exception(errorMessage)))
    }

    private suspend fun givenFirstPageFailsWithNullMessage() {
        whenever(posLocalCatalogStore.fetchRecentlyModifiedVariations(any(), anyOrNull(), any(), any()))
            .thenReturn(KotlinResult.failure(Exception()))
    }

    private suspend fun givenSecondPageFails(errorMessage: String) {
        val variations1 = createMockVariations(1, 100)
        whenever(posLocalCatalogStore.fetchRecentlyModifiedVariations(any(), anyOrNull(), eq(1), any()))
            .doAnswer(
                InlineClassesAnswer {
                    KotlinResult.success(
                        WooPosVariationsFetchResult(
                            variations = variations1,
                            syncedCount = 100,
                            hasMore = true,
                            nextPage = 2,
                            totalPages = 2,
                            serverDate = "2024-01-15T10:00:00Z"
                        )
                    )
                }
            )

        whenever(posLocalCatalogStore.fetchRecentlyModifiedVariations(any(), anyOrNull(), eq(2), any()))
            .thenReturn(KotlinResult.failure(Exception(errorMessage)))
    }

    private suspend fun givenPageWithZeroVariationsButHasMore() {
        whenever(posLocalCatalogStore.fetchRecentlyModifiedVariations(any(), anyOrNull(), eq(1), any()))
            .doAnswer(
                InlineClassesAnswer {
                    KotlinResult.success(
                        WooPosVariationsFetchResult(
                            variations = emptyList(),
                            syncedCount = 0,
                            hasMore = false, // Changed to false so it stops fetching after zero items
                            nextPage = 1,
                            totalPages = 1,
                            serverDate = "2024-01-15T10:00:00Z"
                        )
                    )
                }
            )
    }

    private suspend fun givenThreePageCatalogWithOffsets() {
        val variations1 = createMockVariations(1, 100)
        val variations2 = createMockVariations(101, 100)
        val variations3 = createMockVariations(201, 50)

        whenever(posLocalCatalogStore.fetchRecentlyModifiedVariations(any(), anyOrNull(), eq(1), any()))
            .doAnswer(
                InlineClassesAnswer {
                    KotlinResult.success(
                        WooPosVariationsFetchResult(
                            variations = variations1,
                            syncedCount = 100,
                            hasMore = true,
                            nextPage = 2,
                            totalPages = 3,
                            serverDate = "2024-01-15T10:00:00Z"
                        )
                    )
                }
            )

        whenever(posLocalCatalogStore.fetchRecentlyModifiedVariations(any(), anyOrNull(), eq(2), any()))
            .doAnswer(
                InlineClassesAnswer {
                    KotlinResult.success(
                        WooPosVariationsFetchResult(
                            variations = variations2,
                            syncedCount = 100,
                            hasMore = true,
                            nextPage = 3,
                            totalPages = 3,
                            serverDate = "2024-01-15T11:00:00Z"
                        )
                    )
                }
            )

        whenever(posLocalCatalogStore.fetchRecentlyModifiedVariations(any(), anyOrNull(), eq(3), any()))
            .doAnswer(
                InlineClassesAnswer {
                    KotlinResult.success(
                        WooPosVariationsFetchResult(
                            variations = variations3,
                            syncedCount = 50,
                            hasMore = false,
                            nextPage = 4,
                            totalPages = 3,
                            serverDate = "2024-01-15T12:00:00Z"
                        )
                    )
                }
            )
    }

    private fun createMockVariations(startId: Int, count: Int): List<WooPosVariationEntity> {
        return (startId until startId + count).map { id ->
            WooPosVariationEntity(
                localSiteId = LocalId(1),
                remoteProductId = RemoteId(100),
                remoteVariationId = RemoteId(id.toLong()),
                dateModified = "2024-01-15T10:00:00Z",
                sku = "VAR-$id",
                globalUniqueId = "var-$id",
                variationName = "Variation $id",
                price = "10.00",
                regularPrice = "10.00",
                salePrice = "",
                description = "Test variation $id",
                stockQuantity = 1.0,
                stockStatus = "instock",
                manageStock = false,
                backordered = false,
                attributesJson = "{}",
                imageUrl = "",
                status = "publish",
                lastUpdated = "2024-01-15T10:00:00Z",
                downloadable = false
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun givenTransactionSuccess() {
        whenever(
            posLocalCatalogStore
                .executeInTransaction(any<suspend () -> KotlinResult<Unit>>())
        ).thenAnswer { invocation ->
            val block = invocation.arguments[0] as suspend () -> KotlinResult<Unit>
            runBlocking { block.invoke() }
        }
        whenever(posLocalCatalogStore.upsertVariations(any())).thenReturn(KotlinResult.success(Unit))
    }
}
