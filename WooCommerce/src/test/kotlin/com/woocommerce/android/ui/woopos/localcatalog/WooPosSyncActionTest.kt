package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.localcatalog.WooPosLocalCatalogSyncRepository.Companion.PAGE_SIZE
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
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
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosVariationEntity
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogError
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosPaginatedFetchResult
import kotlin.Result as KotlinResult

class WooPosSyncActionTest {

    private lateinit var sut: WooPosSyncAction
    private var posLocalCatalogStore: WooPosLocalCatalogStore = mock()
    private lateinit var site: SiteModel
    private var logger: WooPosLogWrapper = mock()
    private var syncWithFts: WooPosLocalCatalogSyncWithFts = mock()
    private var analyticsTracker: WooPosAnalyticsTracker = mock()

    @Before
    fun setup() {
        sut = WooPosSyncAction(posLocalCatalogStore, logger, syncWithFts, analyticsTracker)
        site = SiteModel().apply {
            id = 1
            siteId = 123L
        }
        runBlocking {
            givenTransactionSuccess()
        }
    }

    // === PRODUCT SYNC TESTS ===

    @Test
    fun `when products have single page, then syncs correct product count`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(50), serverDate = "2024-01-01T12:00:00Z")
        mockFetchVariationsSuccess(pages = listOf(0), serverDate = "2024-01-01T12:00:00Z")

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        assertThat((result as WooPosSyncResult.Success).productsSynced).isEqualTo(50)
    }

    @Test
    fun `when products have multiple pages, then syncs all product pages`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(50, 30, 20), serverDate = "2024-01-01T12:00:00Z")
        mockFetchVariationsSuccess(pages = listOf(0), serverDate = "2024-01-01T12:00:00Z")

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        assertThat((result as WooPosSyncResult.Success).productsSynced).isEqualTo(100)
        verify(posLocalCatalogStore, times(3)).fetchRecentlyModifiedProducts(
            eq(site),
            any(),
            anyOrNull(),
            any(),
            any(),
            eq(null)
        )
    }

    @Test
    fun `when products have empty catalog, then returns zero product count`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(0), serverDate = "2024-01-01T12:00:00Z")
        mockFetchVariationsSuccess(pages = listOf(0), serverDate = "2024-01-01T12:00:00Z")

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        assertThat((result as WooPosSyncResult.Success).productsSynced).isEqualTo(0)
    }

    @Test
    fun `when product fetch fails on first page, then returns error`() = runTest {
        // GIVEN
        givenProductFetchFails(page = 1, "Network error")

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncResult.Failed.UnexpectedError::class.java)
    }

    @Test
    fun `when product fetch fails on middle page, then returns error`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(50, 30), serverDate = "2024-01-01T12:00:00Z")
        givenProductFetchFails(page = 2, "Network timeout")
        mockFetchVariationsSuccess(pages = listOf(0), serverDate = "2024-01-01T12:00:00Z")

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncResult.Failed.UnexpectedError::class.java)
    }

    @Test
    fun `when products exceed page limit, then returns CatalogTooLarge`() = runTest {
        // GIVEN
        val totalPages = 15
        val maxPages = 10
        givenProductCatalogTooLarge(totalPages)

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, maxPages)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncResult.Failed.CatalogTooLarge::class.java)
        val failure = result as WooPosSyncResult.Failed.CatalogTooLarge
        assertThat(failure.totalPages).isEqualTo(totalPages)
        assertThat(failure.maxPages).isEqualTo(maxPages)
    }

    @Test
    fun `when products have zero items but hasMore true, then stops fetching`() = runTest {
        // GIVEN
        givenProductPageWithZeroItemsButHasMore()
        mockFetchVariationsSuccess(pages = listOf(0), serverDate = "2024-01-01T12:00:00Z")

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        assertThat((result as WooPosSyncResult.Success).productsSynced).isEqualTo(0)
        verify(posLocalCatalogStore, times(1)).fetchRecentlyModifiedProducts(
            eq(site),
            any(),
            anyOrNull(),
            eq(1),
            any(),
            eq(null)
        )
    }

    // === VARIATIONS SYNC TESTS ===

    @Test
    fun `when variations have single page, then syncs correct variation count`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(0), serverDate = "2024-01-01T12:00:00Z")
        mockFetchVariationsSuccess(pages = listOf(25), serverDate = "2024-01-01T12:00:00Z")

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        assertThat((result as WooPosSyncResult.Success).variationsSynced).isEqualTo(25)
    }

    @Test
    fun `when variations have multiple pages, then syncs all variation pages`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(0), serverDate = "2024-01-01T12:00:00Z")
        mockFetchVariationsSuccess(pages = listOf(25, 15, 10), serverDate = "2024-01-01T12:00:00Z")

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        assertThat((result as WooPosSyncResult.Success).variationsSynced).isEqualTo(50)
        verify(
            posLocalCatalogStore,
            times(3)
        ).fetchRecentlyModifiedVariations(eq(site), any(), anyOrNull(), any(), any())
    }

    @Test
    fun `when variations have empty catalog, then returns zero variation count`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(0), serverDate = "2024-01-01T12:00:00Z")
        mockFetchVariationsSuccess(pages = listOf(0), serverDate = "2024-01-01T12:00:00Z")

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        assertThat((result as WooPosSyncResult.Success).variationsSynced).isEqualTo(0)
    }

    @Test
    fun `when variation fetch fails on first page, then returns error`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(10), serverDate = "2024-01-01T12:00:00Z")
        givenVariationFetchFails(page = 1, "Network error")

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncResult.Failed.UnexpectedError::class.java)
    }

    @Test
    fun `when variation fetch fails on middle page, then returns error`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(10), serverDate = "2024-01-01T12:00:00Z")
        mockFetchVariationsSuccess(pages = listOf(25, 15), serverDate = "2024-01-01T12:00:00Z")
        givenVariationFetchFails(page = 2, "Database error")

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncResult.Failed.UnexpectedError::class.java)
    }

    @Test
    fun `when variations exceed page limit, then returns CatalogTooLarge`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(10), serverDate = "2024-01-01T12:00:00Z")
        givenVariationCatalogTooLarge(totalPages = 12)

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncResult.Failed.CatalogTooLarge::class.java)
    }

    @Test
    fun `when variations have zero items but hasMore true, then stops fetching`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(10), serverDate = "2024-01-01T12:00:00Z")
        givenVariationPageWithZeroItemsButHasMore()

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        assertThat((result as WooPosSyncResult.Success).variationsSynced).isEqualTo(0)
        verify(
            posLocalCatalogStore,
            times(1)
        ).fetchRecentlyModifiedVariations(eq(site), any(), anyOrNull(), eq(1), any())
    }

    // === TRASH PRODUCTS SYNC TESTS ===

    @Test
    fun `when incremental sync, then passes modifiedAfterGmt to trash products fetch`() = runTest {
        // GIVEN
        val modifiedAfter = "2024-01-15T10:30:00Z"
        mockFetchProductsSuccess(pages = listOf(5), serverDate = "2024-01-15T10:30:00Z")
        mockFetchVariationsSuccess(pages = listOf(2), serverDate = "2024-01-15T10:30:00Z")
        givenTrashProducts(count = 1)

        // WHEN
        sut.syncCatalog(site, modifiedAfter, PAGE_SIZE, 10)

        // THEN
        verify(posLocalCatalogStore).fetchRecentlyModifiedProducts(
            eq(site),
            any(),
            eq(modifiedAfter),
            any(),
            any(),
            eq(listOf(CoreProductStatus.TRASH))
        )
    }

    @Test
    fun `when incremental sync, then fetches and includes trash products`() = runTest {
        // GIVEN
        val modifiedAfter = "2024-01-01T12:00:00Z"
        mockFetchProductsSuccess(pages = listOf(10), serverDate = "2024-01-01T12:00:00Z")
        mockFetchVariationsSuccess(pages = listOf(5), serverDate = "2024-01-01T12:00:00Z")
        givenTrashProducts(count = 3)

        // WHEN
        val result = sut.syncCatalog(site, modifiedAfter, PAGE_SIZE, 10)

        // THEN
        assertThat((result as WooPosSyncResult.Success).productsSynced).isEqualTo(13) // 10 + 3 trash
        verify(posLocalCatalogStore).fetchRecentlyModifiedProducts(
            eq(site),
            any(),
            eq(modifiedAfter),
            any(),
            any(),
            eq(listOf(CoreProductStatus.TRASH))
        )
    }

    @Test
    fun `when full sync, then does not fetch trash products`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(10), serverDate = "2024-01-01T12:00:00Z")
        mockFetchVariationsSuccess(pages = listOf(5), serverDate = "2024-01-01T12:00:00Z")

        // WHEN
        sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        verify(posLocalCatalogStore, never()).fetchRecentlyModifiedProducts(
            any(),
            any(),
            anyOrNull(),
            any(),
            any(),
            eq(listOf(CoreProductStatus.TRASH))
        )
    }

    @Test
    fun `when trash product fetch has multiple pages, then fetches all trash pages`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(10), serverDate = "2024-01-01T12:00:00Z")
        mockFetchVariationsSuccess(pages = listOf(5), serverDate = "2024-01-01T12:00:00Z")
        givenMultiPageTrashProducts(pages = listOf(5, 3))

        // WHEN
        val result = sut.syncCatalog(site, "2024-01-01T12:00:00Z", PAGE_SIZE, 10)

        // THEN
        assertThat((result as WooPosSyncResult.Success).productsSynced).isEqualTo(18) // 10 + 5 + 3 trash
        verify(posLocalCatalogStore, times(2)).fetchRecentlyModifiedProducts(
            eq(site),
            any(),
            anyOrNull(),
            any(),
            any(),
            eq(listOf(CoreProductStatus.TRASH))
        )
    }

    @Test
    fun `when trash product fetch fails, then returns error`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(10), serverDate = "2024-01-01T12:00:00Z")
        mockFetchVariationsSuccess(pages = listOf(5), serverDate = "2024-01-01T12:00:00Z")
        givenTrashProductFetchFails("Trash fetch error")

        // WHEN
        val result = sut.syncCatalog(site, "2024-01-01T12:00:00Z", PAGE_SIZE, 10)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncResult.Failed.UnexpectedError::class.java)
    }

    @Test
    fun `when trash products are empty, then includes zero trash products`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(10), serverDate = "2024-01-01T12:00:00Z")
        mockFetchVariationsSuccess(pages = listOf(5), serverDate = "2024-01-01T12:00:00Z")
        givenTrashProducts(count = 0)

        // WHEN
        val result = sut.syncCatalog(site, "2024-01-01T12:00:00Z", PAGE_SIZE, 10)

        // THEN
        assertThat((result as WooPosSyncResult.Success).productsSynced).isEqualTo(10) // no trash products
    }

    @Test
    fun `when incremental sync with trash products, then deletes trash products from FTS`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(5), serverDate = "2024-01-01T12:00:00Z")
        mockFetchVariationsSuccess(pages = listOf(0), serverDate = "2024-01-01T12:00:00Z")
        givenTrashProductsWithIds(listOf(100L, 101L))
        givenAllModifiedProductsMatchPosProducts(count = 5)

        // WHEN
        sut.syncCatalog(site, "2024-01-01T12:00:00Z", PAGE_SIZE, 10)

        // THEN
        verify(syncWithFts).syncFtsForIncrementalSync(
            eq("1"),
            any(),
            any(),
            eq(listOf(LocalOrRemoteId.RemoteId(100L), LocalOrRemoteId.RemoteId(101L)))
        )
    }

    // === TRANSACTION TESTS ===

    @Test
    fun `when syncing, then executes all operations in single transaction`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(10), serverDate = "2024-01-01T12:00:00Z")
        mockFetchVariationsSuccess(pages = listOf(5), serverDate = "2024-01-01T12:00:00Z")

        // WHEN
        sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        verify(posLocalCatalogStore).executeInTransaction(any<suspend () -> KotlinResult<Unit>>())
    }

    @Test
    fun `when transaction fails, then returns UnexpectedError`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(10), serverDate = "2024-01-01T12:00:00Z")
        mockFetchVariationsSuccess(pages = listOf(5), serverDate = "2024-01-01T12:00:00Z")
        givenTransactionFailure("Database error")

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncResult.Failed.UnexpectedError::class.java)
    }

    @Test
    fun `when full sync, then deletes both products and variations before insert`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(10), serverDate = "2024-01-01T12:00:00Z")
        mockFetchVariationsSuccess(pages = listOf(5), serverDate = "2024-01-01T12:00:00Z")

        // WHEN
        sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        verify(posLocalCatalogStore).deleteAllProducts(LocalOrRemoteId.LocalId(site.id))
        verify(posLocalCatalogStore).deleteAllVariations(LocalOrRemoteId.LocalId(site.id))
    }

    @Test
    fun `when incremental sync, then does not delete any data`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(10), serverDate = "2024-01-01T12:00:00Z")
        mockFetchVariationsSuccess(pages = listOf(5), serverDate = "2024-01-01T12:00:00Z")

        // WHEN
        sut.syncCatalog(site, "2024-01-01T12:00:00Z", PAGE_SIZE, 10)

        // THEN
        verify(posLocalCatalogStore, never()).deleteAllProducts(any())
        verify(posLocalCatalogStore, never()).deleteAllVariations(any())
    }

    @Test
    fun `when transaction succeeds, then commits all changes atomically`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(10), serverDate = "2024-01-01T12:00:00Z")
        mockFetchVariationsSuccess(pages = listOf(5), serverDate = "2024-01-01T12:00:00Z")

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncResult.Success::class.java)
        verify(posLocalCatalogStore).upsertProducts(any())
        verify(posLocalCatalogStore).upsertVariations(any())
    }

    // === SERVER DATE TESTS ===

    @Test
    fun `when sync succeeds, then returns both products and variations server dates`() = runTest {
        // GIVEN
        val productsDate = "2024-01-01T12:00:00Z"
        val variationsDate = "2024-01-01T13:00:00Z"
        mockFetchProductsSuccess(pages = listOf(10), serverDate = productsDate)
        mockFetchVariationsSuccess(pages = listOf(5), serverDate = variationsDate)

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        val success = result as WooPosSyncResult.Success
        assertThat(success.productsServerDate).isEqualTo(productsDate)
        assertThat(success.variationsServerDate).isEqualTo(variationsDate)
    }

    @Test
    fun `when products and variations have different dates, then returns both dates correctly`() = runTest {
        // GIVEN
        val productsDate = "2024-01-01T12:00:00Z"
        val variationsDate = "2024-01-02T15:30:00Z"
        mockFetchProductsSuccess(pages = listOf(5), serverDate = productsDate)
        mockFetchVariationsSuccess(pages = listOf(3), serverDate = variationsDate)

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        val success = result as WooPosSyncResult.Success
        assertThat(success.productsServerDate).isEqualTo(productsDate)
        assertThat(success.variationsServerDate).isEqualTo(variationsDate)
    }

    @Test
    fun `when multiple pages synced, then returns first page server date`() = runTest {
        // GIVEN
        val firstPageDate = "2024-01-01T12:00:00Z"
        mockFetchProductsSuccess(pages = listOf(50, 30), serverDate = firstPageDate)
        mockFetchVariationsSuccess(pages = listOf(25, 15), serverDate = firstPageDate)

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        val success = result as WooPosSyncResult.Success
        assertThat(success.productsServerDate).isEqualTo(firstPageDate)
        assertThat(success.variationsServerDate).isEqualTo(firstPageDate)
    }

    // === COMBINED INTEGRATION TESTS ===

    @Test
    fun `when catalog has both products and variations, then returns success with correct counts`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(50), serverDate = "2024-01-01T12:00:00Z")
        mockFetchVariationsSuccess(pages = listOf(30), serverDate = "2024-01-01T12:00:00Z")

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        val success = result as WooPosSyncResult.Success
        assertThat(success.productsSynced).isEqualTo(50)
        assertThat(success.variationsSynced).isEqualTo(30)
    }

    @Test
    fun `when catalog has multiple pages of both types, then syncs all pages correctly`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(50, 30, 20), serverDate = "2024-01-01T12:00:00Z")
        mockFetchVariationsSuccess(pages = listOf(25, 15, 10), serverDate = "2024-01-01T12:00:00Z")

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        val success = result as WooPosSyncResult.Success
        assertThat(success.productsSynced).isEqualTo(100)
        assertThat(success.variationsSynced).isEqualTo(50)
    }

    @Test
    fun `when empty catalog for both types, then returns success with zero counts`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(0), serverDate = "2024-01-01T12:00:00Z")
        mockFetchVariationsSuccess(pages = listOf(0), serverDate = "2024-01-01T12:00:00Z")

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        val success = result as WooPosSyncResult.Success
        assertThat(success.productsSynced).isEqualTo(0)
        assertThat(success.variationsSynced).isEqualTo(0)
    }

    @Test
    fun `when sync with incremental mode includes trash products, then combines all correctly`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(20), serverDate = "2024-01-01T12:00:00Z")
        mockFetchVariationsSuccess(pages = listOf(10), serverDate = "2024-01-01T12:00:00Z")
        givenTrashProducts(count = 5)

        // WHEN
        val result = sut.syncCatalog(site, "2024-01-01T12:00:00Z", PAGE_SIZE, 10)

        // THEN
        val success = result as WooPosSyncResult.Success
        assertThat(success.productsSynced).isEqualTo(25) // 20 + 5 trash
        assertThat(success.variationsSynced).isEqualTo(10)
    }

    // === ERROR HANDLING TESTS ===

    @Test
    fun `when API returns null error message, then returns generic error`() = runTest {
        // GIVEN
        givenProductFetchFails(page = 1, null)

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncResult.Failed.UnexpectedError::class.java)
    }

    @Test
    fun `when network timeout occurs, then returns UnexpectedError`() = runTest {
        // GIVEN
        givenProductFetchFails(page = 1, "Network timeout")

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncResult.Failed.UnexpectedError::class.java)
    }

    @Test
    fun `when catalog size check fails before sync, then returns appropriate error`() = runTest {
        // GIVEN
        givenProductCatalogTooLarge(totalPages = 20)

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncResult.Failed.CatalogTooLarge::class.java)
    }

    @Test(expected = CancellationException::class)
    fun `when fetch is cancelled, then CancellationException propagates`() = runTest {
        // GIVEN
        givenProductFetchThrowsCancellation()
        mockFetchVariationsSuccess(pages = listOf(0), serverDate = "2024-01-01T12:00:00Z")

        // WHEN
        sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN — expected CancellationException
    }

    @Test(expected = CancellationException::class)
    fun `when transaction is cancelled, then CancellationException propagates`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(10), serverDate = "2024-01-01T12:00:00Z")
        mockFetchVariationsSuccess(pages = listOf(5), serverDate = "2024-01-01T12:00:00Z")
        givenTransactionThrowsCancellation()

        // WHEN
        sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN — expected CancellationException
    }

    // === EDGE CASE TESTS ===

    @Test
    fun `when products succeed but variations fail, then returns error`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(50), serverDate = "2024-01-01T12:00:00Z")
        givenVariationFetchFails(page = 1, "Variation error")

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncResult.Failed.UnexpectedError::class.java)
    }

    @Test
    fun `when variations succeed but products fail, then returns error`() = runTest {
        // GIVEN
        givenProductFetchFails(page = 1, "Product error")

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncResult.Failed.UnexpectedError::class.java)
    }

    @Test
    fun `when both fetches succeed but transaction fails, then returns transaction error`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(10), serverDate = "2024-01-01T12:00:00Z")
        mockFetchVariationsSuccess(pages = listOf(5), serverDate = "2024-01-01T12:00:00Z")
        givenTransactionFailure("Transaction rollback")

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncResult.Failed.UnexpectedError::class.java)
    }

    @Test
    fun `when page has zero items and hasMore false, then completes successfully`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(0), serverDate = "2024-01-01T12:00:00Z")
        mockFetchVariationsSuccess(pages = listOf(0), serverDate = "2024-01-01T12:00:00Z")

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncResult.Success::class.java)
        val success = result as WooPosSyncResult.Success
        assertThat(success.productsSynced).isEqualTo(0)
        assertThat(success.variationsSynced).isEqualTo(0)
    }

    // === ERROR TYPE TESTS ===

    @Test
    fun `given network error on product fetch, when sync catalog, then returns NetworkError result`() = runTest {
        // GIVEN
        givenProductFetchFailsWithNetworkError(page = 1, "Network timeout")

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncResult.Failed.NetworkError::class.java)
        val networkError = result as WooPosSyncResult.Failed.NetworkError
        assertThat(networkError.errorMessage).isEqualTo("Network timeout")
    }

    @Test
    fun `given database error on product fetch, when sync catalog, then returns DatabaseError result`() = runTest {
        // GIVEN
        givenProductFetchFailsWithDatabaseError(page = 1, "Database constraint violation")

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncResult.Failed.DatabaseError::class.java)
        val databaseError = result as WooPosSyncResult.Failed.DatabaseError
        assertThat(databaseError.errorMessage).isEqualTo("Database constraint violation")
    }

    @Test
    fun `given invalid response on product fetch, when sync catalog, then returns InvalidResponse result`() = runTest {
        // GIVEN
        givenProductFetchFailsWithInvalidResponse(page = 1, "Missing required header: X-WP-Total")

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncResult.Failed.InvalidResponse::class.java)
        val invalidResponse = result as WooPosSyncResult.Failed.InvalidResponse
        assertThat(invalidResponse.errorMessage).isEqualTo("Missing required header: X-WP-Total")
    }

    @Test
    fun `given empty response on product fetch, when sync catalog, then returns InvalidResponse result`() = runTest {
        // GIVEN
        givenProductFetchFailsWithEmptyResponse(page = 1)

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncResult.Failed.InvalidResponse::class.java)
    }

    @Test
    fun `given network error on variation fetch, when sync catalog, then returns NetworkError result`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(10), serverDate = "2024-01-01T12:00:00Z")
        givenVariationFetchFailsWithNetworkError(page = 1, "No network connection")

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncResult.Failed.NetworkError::class.java)
        val networkError = result as WooPosSyncResult.Failed.NetworkError
        assertThat(networkError.errorMessage).isEqualTo("No network connection")
    }

    @Test
    fun `given database error on transaction, when sync catalog, then returns DatabaseError result`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(10), serverDate = "2024-01-01T12:00:00Z")
        mockFetchVariationsSuccess(pages = listOf(5), serverDate = "2024-01-01T12:00:00Z")
        givenTransactionFailsWithDatabaseError("Transaction rollback due to constraint violation")

        // WHEN
        val result = sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        assertThat(result).isInstanceOf(WooPosSyncResult.Failed.DatabaseError::class.java)
        val databaseError = result as WooPosSyncResult.Failed.DatabaseError
        assertThat(databaseError.errorMessage).isEqualTo("Transaction rollback due to constraint violation")
    }

    @Test
    fun `when full sync, then deleteProducts is not called`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(10), serverDate = "2024-01-01T12:00:00Z")
        mockFetchVariationsSuccess(pages = listOf(5), serverDate = "2024-01-01T12:00:00Z")

        // WHEN
        sut.syncCatalog(site, null, PAGE_SIZE, 10)

        // THEN
        verify(posLocalCatalogStore, never()).deleteProducts(any(), any())
    }

    @Test
    fun `when incremental sync with no products to remove, then deleteProducts is not called`() = runTest {
        // GIVEN
        mockFetchProductsSuccess(pages = listOf(10), serverDate = "2024-01-01T12:00:00Z")
        mockFetchVariationsSuccess(pages = listOf(5), serverDate = "2024-01-01T12:00:00Z")
        givenTrashProducts(count = 0)
        givenAllModifiedProductsMatchPosProducts(count = 10)

        // WHEN
        sut.syncCatalog(site, "2024-01-01T12:00:00Z", PAGE_SIZE, 10)

        // THEN
        verify(posLocalCatalogStore, never()).deleteProducts(any(), any())
    }

    @Test
    fun `when incremental sync with products to remove, then deleteProducts is called`() = runTest {
        // GIVEN
        givenPosOnlyProductsFetch(
            posProductIds = listOf(1L, 2L, 3L),
            serverDate = "2024-01-01T12:00:00Z"
        )
        givenAllProductsFetch(
            allProductIds = listOf(1L, 2L, 3L, 4L, 5L),
            serverDate = "2024-01-01T12:00:00Z"
        )
        mockFetchVariationsSuccess(pages = listOf(0), serverDate = "2024-01-01T12:00:00Z")
        givenTrashProducts(count = 0)

        // WHEN
        sut.syncCatalog(site, "2024-01-01T12:00:00Z", PAGE_SIZE, 10)

        // THEN
        verify(posLocalCatalogStore).deleteProducts(
            eq(LocalOrRemoteId.LocalId(site.id)),
            eq(listOf(LocalOrRemoteId.RemoteId(4L), LocalOrRemoteId.RemoteId(5L)))
        )
    }

    @Test
    fun `when incremental sync with all products now non-POS, then deleteProducts removes all`() = runTest {
        // GIVEN
        givenPosOnlyProductsFetch(
            posProductIds = emptyList(),
            serverDate = "2024-01-01T12:00:00Z"
        )
        givenAllProductsFetch(
            allProductIds = listOf(1L, 2L, 3L),
            serverDate = "2024-01-01T12:00:00Z"
        )
        mockFetchVariationsSuccess(pages = listOf(0), serverDate = "2024-01-01T12:00:00Z")
        givenTrashProducts(count = 0)

        // WHEN
        sut.syncCatalog(site, "2024-01-01T12:00:00Z", PAGE_SIZE, 10)

        // THEN
        verify(posLocalCatalogStore).deleteProducts(
            eq(LocalOrRemoteId.LocalId(site.id)),
            eq(
                listOf(
                    LocalOrRemoteId.RemoteId(1L),
                    LocalOrRemoteId.RemoteId(2L),
                    LocalOrRemoteId.RemoteId(3L)
                )
            )
        )
    }

    // === HELPER METHODS ===

    private fun mockFetchProductsSuccess(pages: List<Int>, serverDate: String) = runBlocking {
        pages.forEachIndexed { index, count ->
            val pageNumber = index + 1
            whenever(
                posLocalCatalogStore.fetchRecentlyModifiedProducts(
                    eq(site),
                    any(),
                    anyOrNull(),
                    eq(pageNumber),
                    any(),
                    eq(null)
                )
            ).thenReturn(
                KotlinResult.success(
                    WooPosPaginatedFetchResult(
                        items = generateProducts(count),
                        totalPages = pages.size,
                        hasMore = pageNumber < pages.size,
                        nextPage = pageNumber + 1,
                        syncedCount = count,
                        serverDate = serverDate
                    )
                )
            )
        }
    }

    private fun mockFetchVariationsSuccess(pages: List<Int>, serverDate: String) = runBlocking {
        pages.forEachIndexed { index, count ->
            val pageNumber = index + 1
            whenever(
                posLocalCatalogStore.fetchRecentlyModifiedVariations(
                    eq(site),
                    any(),
                    anyOrNull(),
                    eq(pageNumber),
                    any()
                )
            ).thenReturn(
                KotlinResult.success(
                    WooPosPaginatedFetchResult(
                        items = generateVariations(count),
                        totalPages = pages.size,
                        hasMore = pageNumber < pages.size,
                        nextPage = pageNumber + 1,
                        syncedCount = count,
                        serverDate = serverDate
                    )
                )
            )
        }
    }

    private fun givenTrashProducts(count: Int) = runBlocking {
        whenever(
            posLocalCatalogStore.fetchRecentlyModifiedProducts(
                eq(site),
                any(),
                anyOrNull(),
                any(),
                any(),
                eq(listOf(CoreProductStatus.TRASH))
            )
        ).thenReturn(
            KotlinResult.success(
                WooPosPaginatedFetchResult(
                    items = generateProducts(count),
                    totalPages = 1,
                    hasMore = false,
                    nextPage = 1,
                    syncedCount = count,
                    serverDate = "2024-01-01T12:00:00Z"
                )
            )
        )
    }

    private fun givenTrashProductsWithIds(ids: List<Long>) = runBlocking {
        val products = ids.map { id ->
            WooPosProductEntity(
                remoteId = LocalOrRemoteId.RemoteId(id),
                name = "Trash Product $id"
            )
        }
        whenever(
            posLocalCatalogStore.fetchRecentlyModifiedProducts(
                eq(site),
                any(),
                anyOrNull(),
                any(),
                any(),
                eq(listOf(CoreProductStatus.TRASH))
            )
        ).thenReturn(
            KotlinResult.success(
                WooPosPaginatedFetchResult(
                    items = products,
                    totalPages = 1,
                    hasMore = false,
                    nextPage = 1,
                    syncedCount = products.size,
                    serverDate = "2024-01-01T12:00:00Z"
                )
            )
        )
    }

    private fun givenMultiPageTrashProducts(pages: List<Int>) = runBlocking {
        pages.forEachIndexed { index, count ->
            val pageNumber = index + 1
            whenever(
                posLocalCatalogStore.fetchRecentlyModifiedProducts(
                    eq(site),
                    any(),
                    anyOrNull(),
                    eq(pageNumber),
                    any(),
                    eq(listOf(CoreProductStatus.TRASH))
                )
            ).thenReturn(
                KotlinResult.success(
                    WooPosPaginatedFetchResult(
                        items = generateProducts(count),
                        totalPages = pages.size,
                        hasMore = pageNumber < pages.size,
                        nextPage = pageNumber + 1,
                        syncedCount = count,
                        serverDate = "2024-01-01T12:00:00Z"
                    )
                )
            )
        }
    }

    private fun givenTrashProductFetchFails(errorMessage: String) = runBlocking {
        whenever(
            posLocalCatalogStore.fetchRecentlyModifiedProducts(
                eq(site),
                any(),
                anyOrNull(),
                any(),
                any(),
                eq(listOf(CoreProductStatus.TRASH))
            )
        ).thenReturn(KotlinResult.failure(Exception(errorMessage)))
    }

    private fun givenProductFetchFails(page: Int, errorMessage: String?) = runBlocking {
        whenever(
            posLocalCatalogStore.fetchRecentlyModifiedProducts(
                eq(site),
                any(),
                anyOrNull(),
                eq(page),
                any(),
                eq(null)
            )
        ).thenReturn(KotlinResult.failure(Exception(errorMessage ?: "Generic error")))
    }

    private fun givenVariationFetchFails(page: Int, errorMessage: String?) = runBlocking {
        whenever(
            posLocalCatalogStore.fetchRecentlyModifiedVariations(eq(site), any(), anyOrNull(), eq(page), any())
        ).thenReturn(KotlinResult.failure(Exception(errorMessage ?: "Generic error")))
    }

    private fun givenProductFetchThrowsCancellation() = runBlocking {
        whenever(
            posLocalCatalogStore.fetchRecentlyModifiedProducts(
                eq(site),
                any(),
                anyOrNull(),
                eq(1),
                any(),
                eq(null)
            )
        ).thenReturn(KotlinResult.failure(CancellationException("Worker cancelled")))
    }

    private fun givenProductCatalogTooLarge(totalPages: Int) = runBlocking {
        whenever(
            posLocalCatalogStore.fetchRecentlyModifiedProducts(eq(site), any(), anyOrNull(), eq(1), any(), eq(null))
        ).thenReturn(
            KotlinResult.success(
                WooPosPaginatedFetchResult(
                    items = generateProducts(50),
                    totalPages = totalPages,
                    hasMore = true,
                    nextPage = 2,
                    syncedCount = 50,
                    serverDate = "2024-01-01T12:00:00Z"
                )
            )
        )
    }

    private fun givenVariationCatalogTooLarge(totalPages: Int) = runBlocking {
        whenever(
            posLocalCatalogStore.fetchRecentlyModifiedVariations(eq(site), any(), anyOrNull(), eq(1), any())
        ).thenReturn(
            KotlinResult.success(
                WooPosPaginatedFetchResult(
                    items = generateVariations(50),
                    totalPages = totalPages,
                    hasMore = true,
                    nextPage = 2,
                    syncedCount = 50,
                    serverDate = "2024-01-01T12:00:00Z"
                )
            )
        )
    }

    private fun givenProductPageWithZeroItemsButHasMore() = runBlocking {
        whenever(
            posLocalCatalogStore.fetchRecentlyModifiedProducts(eq(site), any(), anyOrNull(), eq(1), any(), eq(null))
        ).thenReturn(
            KotlinResult.success(
                WooPosPaginatedFetchResult(
                    items = emptyList(),
                    totalPages = 1,
                    hasMore = true,
                    nextPage = 2,
                    syncedCount = 0,
                    serverDate = "2024-01-01T12:00:00Z"
                )
            )
        )
    }

    private fun givenVariationPageWithZeroItemsButHasMore() = runBlocking {
        whenever(
            posLocalCatalogStore.fetchRecentlyModifiedVariations(eq(site), any(), anyOrNull(), eq(1), any())
        ).thenReturn(
            KotlinResult.success(
                WooPosPaginatedFetchResult(
                    items = emptyList(),
                    totalPages = 1,
                    hasMore = true,
                    nextPage = 2,
                    syncedCount = 0,
                    serverDate = "2024-01-01T12:00:00Z"
                )
            )
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun givenTransactionSuccess() = runBlocking {
        whenever(
            posLocalCatalogStore.executeInTransaction(any<suspend () -> KotlinResult<Unit>>())
        ).thenAnswer { invocation ->
            val block = invocation.arguments[0] as suspend () -> KotlinResult<Unit>
            runBlocking { block.invoke() }
        }
        whenever(posLocalCatalogStore.deleteAllProducts(any())).thenReturn(KotlinResult.success(Unit))
        whenever(posLocalCatalogStore.deleteAllVariations(any())).thenReturn(KotlinResult.success(Unit))
        whenever(posLocalCatalogStore.deleteProducts(any(), any())).thenReturn(KotlinResult.success(Unit))
        whenever(posLocalCatalogStore.upsertProducts(any())).thenReturn(KotlinResult.success(Unit))
        whenever(posLocalCatalogStore.upsertVariations(any())).thenReturn(KotlinResult.success(Unit))
    }

    @Suppress("UNCHECKED_CAST")
    private fun givenTransactionFailure(errorMessage: String) = runBlocking {
        whenever(
            posLocalCatalogStore.executeInTransaction(any<suspend () -> KotlinResult<Unit>>())
        ).thenReturn(KotlinResult.failure(Exception(errorMessage)))
    }

    @Suppress("UNCHECKED_CAST")
    private fun givenTransactionThrowsCancellation() = runBlocking {
        whenever(
            posLocalCatalogStore.executeInTransaction(any<suspend () -> KotlinResult<Unit>>())
        ).thenAnswer { throw CancellationException("Worker cancelled") }
    }

    private fun generateProducts(count: Int): List<WooPosProductEntity> {
        return (1..count).map { index ->
            WooPosProductEntity(
                remoteId = LocalOrRemoteId.RemoteId(index.toLong()),
                name = "Product $index"
            )
        }
    }

    private fun generateVariations(count: Int): List<WooPosVariationEntity> {
        return (1..count).map { index ->
            WooPosVariationEntity(
                localSiteId = LocalOrRemoteId.LocalId(site.id),
                remoteProductId = LocalOrRemoteId.RemoteId(100),
                remoteVariationId = LocalOrRemoteId.RemoteId(index.toLong()),
                dateModified = "2024-01-15T10:00:00Z",
                sku = "VAR-$index",
                globalUniqueId = "var-$index",
                variationName = "Variation $index",
                price = "10.00",
                regularPrice = "10.00",
                salePrice = "",
                description = "Test variation $index",
                stockQuantity = 1.0
            )
        }
    }

    private fun givenProductFetchFailsWithNetworkError(page: Int, errorMessage: String) = runBlocking {
        whenever(
            posLocalCatalogStore.fetchRecentlyModifiedProducts(eq(site), any(), anyOrNull(), eq(page), any(), eq(null))
        ).thenReturn(
            KotlinResult.failure(
                WooPosLocalCatalogError.NetworkError(errorMessage = errorMessage)
            )
        )
    }

    private fun givenProductFetchFailsWithDatabaseError(page: Int, errorMessage: String) = runBlocking {
        whenever(
            posLocalCatalogStore.fetchRecentlyModifiedProducts(eq(site), any(), anyOrNull(), eq(page), any(), eq(null))
        ).thenReturn(
            KotlinResult.failure(
                WooPosLocalCatalogError.DatabaseError(errorMessage = errorMessage)
            )
        )
    }

    private fun givenProductFetchFailsWithInvalidResponse(page: Int, errorMessage: String) = runBlocking {
        whenever(
            posLocalCatalogStore.fetchRecentlyModifiedProducts(eq(site), any(), anyOrNull(), eq(page), any(), eq(null))
        ).thenReturn(
            KotlinResult.failure(
                WooPosLocalCatalogError.InvalidResponse(errorMessage = errorMessage)
            )
        )
    }

    private fun givenProductFetchFailsWithEmptyResponse(page: Int) = runBlocking {
        whenever(
            posLocalCatalogStore.fetchRecentlyModifiedProducts(eq(site), any(), anyOrNull(), eq(page), any(), eq(null))
        ).thenReturn(
            KotlinResult.failure(WooPosLocalCatalogError.EmptyResponse)
        )
    }

    private fun givenVariationFetchFailsWithNetworkError(
        page: Int,
        errorMessage: String
    ) = runBlocking {
        whenever(
            posLocalCatalogStore.fetchRecentlyModifiedVariations(eq(site), any(), anyOrNull(), eq(page), any())
        ).thenReturn(
            KotlinResult.failure(
                WooPosLocalCatalogError.NetworkError(errorMessage = errorMessage)
            )
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun givenTransactionFailsWithDatabaseError(errorMessage: String) = runBlocking {
        whenever(
            posLocalCatalogStore.executeInTransaction(any<suspend () -> KotlinResult<Unit>>())
        ).thenReturn(
            KotlinResult.failure(WooPosLocalCatalogError.DatabaseError(errorMessage = errorMessage))
        )
    }

    private fun givenAllModifiedProductsMatchPosProducts(count: Int) = runBlocking {
        whenever(
            posLocalCatalogStore.fetchRecentlyModifiedProducts(
                eq(site),
                eq(false),
                anyOrNull(),
                eq(1),
                any(),
                eq(null)
            )
        ).thenReturn(
            KotlinResult.success(
                WooPosPaginatedFetchResult(
                    items = generateProducts(count),
                    totalPages = 1,
                    hasMore = false,
                    nextPage = 1,
                    syncedCount = count,
                    serverDate = "2024-01-01T12:00:00Z"
                )
            )
        )
    }

    private fun givenPosOnlyProductsFetch(posProductIds: List<Long>, serverDate: String) = runBlocking {
        val products = posProductIds.map { id ->
            WooPosProductEntity(
                remoteId = LocalOrRemoteId.RemoteId(id),
                name = "Product $id"
            )
        }
        whenever(
            posLocalCatalogStore.fetchRecentlyModifiedProducts(
                eq(site),
                eq(true),
                anyOrNull(),
                eq(1),
                any(),
                eq(null)
            )
        ).thenReturn(
            KotlinResult.success(
                WooPosPaginatedFetchResult(
                    items = products,
                    totalPages = 1,
                    hasMore = false,
                    nextPage = 1,
                    syncedCount = products.size,
                    serverDate = serverDate
                )
            )
        )
    }

    private fun givenAllProductsFetch(allProductIds: List<Long>, serverDate: String) = runBlocking {
        val products = allProductIds.map { id ->
            WooPosProductEntity(
                remoteId = LocalOrRemoteId.RemoteId(id),
                name = "Product $id"
            )
        }
        whenever(
            posLocalCatalogStore.fetchRecentlyModifiedProducts(
                eq(site),
                eq(false),
                anyOrNull(),
                eq(1),
                any(),
                eq(null)
            )
        ).thenReturn(
            KotlinResult.success(
                WooPosPaginatedFetchResult(
                    items = products,
                    totalPages = 1,
                    hasMore = false,
                    nextPage = 1,
                    syncedCount = products.size,
                    serverDate = serverDate
                )
            )
        )
    }
}
