package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.localcatalog.WooPosLocalCatalogSyncRepository.Companion.PAGE_SIZE
import com.woocommerce.android.ui.woopos.localcatalog.WooPosSyncProductsAction.WooPosSyncProductsResult
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStatus
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosProductEntity
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogFetchProductsResult
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
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
        givenSinglePageCatalog(productsCount = 10)
        givenTransactionSuccess()
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
        verify(posLocalCatalogStore).executeInTransaction<Unit>(any())
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
            .executeInTransaction<Unit>(any())
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
            .executeInTransaction<Unit>(any())
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
            .executeInTransaction<Unit>(any())
    }

    @Test
    fun `when sync with no modifiedAfterGmt, then deletes and reinserts products`() = runTest {
        // GIVEN
        givenSinglePageCatalog(productsCount = 50)

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, pageSize = 100, maxPages = 10)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncProductsResult.Success::class.java)
        verify(posLocalCatalogStore).deleteAllProducts(eq(site.localId()))
        verify(posLocalCatalogStore).upsertProducts(any())
    }

    @Test
    fun `when sync with modifiedAfterGmt, then does not delete products`() = runTest {
        // GIVEN
        val modifiedAfter = "2024-01-01T00:00:00Z"
        givenSinglePageCatalog(productsCount = 25)

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = modifiedAfter, pageSize = 100, maxPages = 10)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncProductsResult.Success::class.java)
        verify(posLocalCatalogStore, times(0)).deleteAllProducts(any())
        verify(posLocalCatalogStore).upsertProducts(any())
    }

    @Test
    fun `when transaction fails, then returns UnexpectedError`() = runTest {
        // GIVEN
        val errorMessage = "Database transaction failed"
        givenSinglePageCatalog(productsCount = 50)
        whenever(posLocalCatalogStore.upsertProducts(any()))
            .thenReturn(KotlinResult.failure(Exception(errorMessage)))
        whenever(posLocalCatalogStore.executeInTransaction<Unit>(any()))
            .thenReturn(KotlinResult.failure(Exception(errorMessage)))

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = null, pageSize = 100, maxPages = 10)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncProductsResult.Failed.UnexpectedError::class.java)
        assertThat((result as WooPosSyncProductsResult.Failed).error).contains(errorMessage)
    }

    @Test
    fun `given incremental sync, when sync products called, then fetches trash products`() = runTest {
        // GIVEN
        val modifiedAfter = "2024-01-01T00:00:00Z"
        givenSinglePageCatalog(productsCount = 10)
        givenSinglePageTrashCatalog(productsCount = 5)

        // WHEN
        sut.execute(site, modifiedAfterGmt = modifiedAfter, pageSize = 100, maxPages = 2)

        // THEN
        verify(posLocalCatalogStore, times(1)).fetchRecentlyModifiedProducts(
            any(),
            anyOrNull(),
            any(),
            any(),
            includeStatus = argThat { this.contains(CoreProductStatus.TRASH) }
        )
    }

    @Test
    fun `given incremental sync with multiple trash pages, when sync products called, then fetches all trash pages`() =
        runTest {
            // GIVEN
            val modifiedAfter = "2024-01-01T00:00:00Z"
            givenSinglePageCatalog(productsCount = 10)
            givenMultiPageTrashCatalog()

            // WHEN
            sut.execute(site, modifiedAfterGmt = modifiedAfter, pageSize = 100, maxPages = 2)

            // THEN
            verify(posLocalCatalogStore, times(3)).fetchRecentlyModifiedProducts(
                any(),
                anyOrNull(),
                any(),
                any(),
                includeStatus = argThat { this.contains(CoreProductStatus.TRASH) }
            )
        }

    @Test
    fun `given full sync, when sync products called, then does not fetch trash products`() = runTest {
        // GIVEN
        givenSinglePageCatalog(productsCount = 50)

        // WHEN
        sut.execute(site, modifiedAfterGmt = null, pageSize = 100, maxPages = 2)

        // THEN
        verify(posLocalCatalogStore, never()).fetchRecentlyModifiedProducts(
            any(),
            anyOrNull(),
            any(),
            any(),
            includeStatus = argThat { this.contains(CoreProductStatus.TRASH) }
        )
    }

    private suspend fun givenSinglePageTrashCatalog(productsCount: Int) {
        val trashProducts = createMockProducts(101, 101 + productsCount - 1)
        whenever(
            posLocalCatalogStore.fetchRecentlyModifiedProducts(
                site = any(),
                modifiedAfterGmt = eq(null),
                page = eq(1),
                pageSize = any(),
                includeStatus = argThat { this.contains(CoreProductStatus.TRASH) }
            )
        ).thenReturn(
            KotlinResult.success(
                WooPosLocalCatalogFetchProductsResult(
                    products = trashProducts,
                    syncedCount = productsCount,
                    hasMore = false,
                    nextPage = 1,
                    totalPages = 1,
                    serverDate = ""
                )
            )
        )
    }

    @Suppress("LongMethod")
    private suspend fun givenMultiPageTrashCatalog() {
        val trashPage1 = createMockProducts(101, 110)
        val trashPage2 = createMockProducts(111, 120)
        val trashPage3 = createMockProducts(121, 125)

        whenever(
            posLocalCatalogStore.fetchRecentlyModifiedProducts(
                site = any(),
                modifiedAfterGmt = eq(null),
                page = eq(1),
                pageSize = any(),
                includeStatus = argThat { this.contains(CoreProductStatus.TRASH) }
            )
        ).thenReturn(
            KotlinResult.success(
                WooPosLocalCatalogFetchProductsResult(
                    products = trashPage1,
                    syncedCount = 10,
                    hasMore = true,
                    nextPage = 2,
                    totalPages = 3,
                    serverDate = ""
                )
            )
        )

        whenever(
            posLocalCatalogStore.fetchRecentlyModifiedProducts(
                site = any(),
                modifiedAfterGmt = eq(null),
                page = eq(2),
                pageSize = any(),
                includeStatus = argThat { this.contains(CoreProductStatus.TRASH) }
            )
        ).thenReturn(
            KotlinResult.success(
                WooPosLocalCatalogFetchProductsResult(
                    products = trashPage2,
                    syncedCount = 10,
                    hasMore = true,
                    nextPage = 3,
                    totalPages = 3,
                    serverDate = ""
                )
            )
        )

        whenever(
            posLocalCatalogStore.fetchRecentlyModifiedProducts(
                site = any(),
                modifiedAfterGmt = eq(null),
                page = eq(3),
                pageSize = any(),
                includeStatus = argThat { this.contains(CoreProductStatus.TRASH) }
            )
        ).thenReturn(
            KotlinResult.success(
                WooPosLocalCatalogFetchProductsResult(
                    products = trashPage3,
                    syncedCount = 5,
                    hasMore = false,
                    nextPage = 3,
                    totalPages = 3,
                    serverDate = ""
                )
            )
        )
    }

    private suspend fun givenSinglePageCatalog(productsCount: Int = PAGE_SIZE / 2) {
        val mockProducts = createMockProducts(1, productsCount)

        mockFetchRecentlyModifiedProductsSuccess(
            page = 1,
            mockProducts = mockProducts,
            syncedCount = productsCount,
            hasMore = false,
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

        mockFetchRecentlyModifiedProductsSuccess(
            page = 1,
            mockProducts = mockPage1Products,
            syncedCount = page1Count,
            hasMore = true,
            totalPages = totalPages,
        )

        mockFetchRecentlyModifiedProductsSuccess(
            page = 2,
            mockProducts = mockPage2Products,
            syncedCount = page2Count,
            hasMore = true,
            totalPages = totalPages,
        )

        mockFetchRecentlyModifiedProductsSuccess(
            page = 3,
            mockProducts = mockPage3Products,
            syncedCount = page3Count,
            hasMore = false,
            totalPages = totalPages,
        )
    }

    private suspend fun givenEmptyCatalog() {
        mockFetchRecentlyModifiedProductsSuccess(
            page = 1,
            mockProducts = emptyList(),
            syncedCount = 0,
            hasMore = false,
            totalPages = 1,
        )
    }

    private suspend fun givenFirstPageFails(errorMessage: String) {
        whenever(posLocalCatalogStore.fetchRecentlyModifiedProducts(any(), anyOrNull(), any(), any(), eq(null)))
            .thenReturn(KotlinResult.failure(Exception(errorMessage)))
    }

    private suspend fun givenFirstPageFailsWithNullMessage() {
        whenever(posLocalCatalogStore.fetchRecentlyModifiedProducts(any(), anyOrNull(), any(), any(), eq(null)))
            .thenReturn(KotlinResult.failure(Exception()))
    }

    private suspend fun givenSecondPageFails(errorMessage: String) {
        val mockPage1Products = createMockProducts(1, 100)

        mockFetchRecentlyModifiedProductsSuccess(
            page = 1,
            mockProducts = mockPage1Products,
            syncedCount = 100,
            hasMore = true,
            totalPages = 2,
        )

        whenever(posLocalCatalogStore.fetchRecentlyModifiedProducts(any(), anyOrNull(), eq(2), any(), eq(null)))
            .thenReturn(KotlinResult.failure(Exception(errorMessage)))
    }

    private suspend fun givenPageWithZeroProductsButHasMore() {
        val mockPage2Products = createMockProducts(1, 50)

        mockFetchRecentlyModifiedProductsSuccess(
            page = 1,
            mockProducts = emptyList(),
            syncedCount = 0,
            hasMore = true,
            totalPages = 2,
        )

        mockFetchRecentlyModifiedProductsSuccess(
            page = 2,
            mockProducts = mockPage2Products,
            syncedCount = 50,
            hasMore = true,
            totalPages = 2,
        )
    }

    private suspend fun givenCatalogTooLarge(totalPages: Int) {
        val mockProducts = createMockProducts(1, PAGE_SIZE)

        mockFetchRecentlyModifiedProductsSuccess(
            page = 1,
            mockProducts,
            totalPages,
            syncedCount = PAGE_SIZE,
            hasMore = true
        )
    }

    @Suppress("LongParameterList")
    private suspend fun mockFetchRecentlyModifiedProductsSuccess(
        page: Int,
        mockProducts: List<WooPosProductEntity>,
        totalPages: Int,
        syncedCount: Int,
        hasMore: Boolean
    ) {
        whenever(
            posLocalCatalogStore.fetchRecentlyModifiedProducts(
                site = any(),
                modifiedAfterGmt = anyOrNull(),
                page = eq(page),
                pageSize = any(),
                includeStatus = eq(null)
            )
        )
            .thenReturn(
                KotlinResult.success(
                    WooPosLocalCatalogFetchProductsResult(
                        products = mockProducts,
                        syncedCount = syncedCount,
                        hasMore = hasMore,
                        nextPage = if (hasMore) page + 1 else page,
                        totalPages = totalPages,
                        serverDate = ""
                    )
                )
            )

        whenever(
            posLocalCatalogStore.fetchRecentlyModifiedProducts(
                site = any(),
                modifiedAfterGmt = anyOrNull(),
                page = eq(page),
                pageSize = any(),
                includeStatus = argThat { this.contains(CoreProductStatus.TRASH) }
            )
        ).thenReturn(
            KotlinResult.success(
                WooPosLocalCatalogFetchProductsResult(
                    products = mockProducts,
                    syncedCount = syncedCount,
                    hasMore = hasMore,
                    nextPage = if (hasMore) page + 1 else page,
                    totalPages = totalPages,
                    serverDate = ""
                )
            )
        )
    }

    private fun createMockProducts(start: Int = 1, end: Int): List<WooPosProductEntity> {
        return (start..end).map {
            WooPosProductEntity(
                remoteId = LocalOrRemoteId.RemoteId(it.toLong()),
                name = "Product $it"
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
        whenever(posLocalCatalogStore.upsertProducts(any())).thenReturn(KotlinResult.success(Unit))
        whenever(posLocalCatalogStore.deleteAllProducts(any())).thenReturn(KotlinResult.success(Unit))
    }
}
