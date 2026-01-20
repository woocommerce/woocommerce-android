package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosProductEntity
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosVariationEntity
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosGenerateCatalogResult
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosGenerateCatalogState
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import java.io.File

class WooPosFileBasedSyncActionTest {

    private lateinit var sut: WooPosFileBasedSyncAction
    private val posLocalCatalogStore: WooPosLocalCatalogStore = mock()
    private val catalogFileDownloader: WooPosCatalogFileDownloader = mock()
    private val catalogFileParser: WooPosCatalogFileParser = mock()
    private val logger: WooPosLogWrapper = mock()
    private lateinit var site: SiteModel

    private val defaultUrl = "https://example.com/catalog.json"
    private val defaultFile = File.createTempFile("test_catalog", ".json")
    private val defaultProducts = listOf(
        WooPosProductEntity(
            remoteId = LocalOrRemoteId.RemoteId(1L),
            name = "Product 1"
        )
    )
    private val defaultVariations = listOf(
        WooPosVariationEntity(
            localSiteId = LocalOrRemoteId.LocalId(1),
            remoteProductId = LocalOrRemoteId.RemoteId(1L),
            remoteVariationId = LocalOrRemoteId.RemoteId(100L),
            dateModified = "2024-01-01T12:00:00Z",
            sku = "VAR-1",
            globalUniqueId = "var-1",
            variationName = "Variation 1",
            price = "10.00",
            regularPrice = "10.00",
            salePrice = "",
            description = "Test variation",
            stockQuantity = 1.0
        )
    )
    private val defaultParsedData = WooPosCatalogFileParser.ParsedCatalogData(
        products = defaultProducts,
        variations = defaultVariations
    )

    @Before
    fun setup() = runTest {
        sut = WooPosFileBasedSyncAction(
            posLocalCatalogStore = posLocalCatalogStore,
            catalogFileDownloader = catalogFileDownloader,
            catalogFileParser = catalogFileParser,
            logger = logger
        )
        site = SiteModel().apply {
            id = 1
            siteId = 123L
        }

        givenCatalogGenerationCompleted()
        givenFileDownloadSuccess()
        givenFileParseSuccess()
        givenStoreCatalogDataSuccess()
    }

    @Test
    fun `given happy path, when syncCatalog, then returns success with correct data`() = runTest {
        // WHEN
        val result = sut.syncCatalog(site)

        // THEN
        assertThat(result).isInstanceOf(WooPosFileBasedSyncAction.WooPosFileBasedSyncResult.Success::class.java)
        val syncResult = (result as WooPosFileBasedSyncAction.WooPosFileBasedSyncResult.Success).result
        assertThat(syncResult.productsSynced).isEqualTo(1)
        assertThat(syncResult.variationsSynced).isEqualTo(1)
    }

    @Test
    fun `given happy path, when syncCatalog, then downloads file from correct URL`() = runTest {
        // WHEN
        sut.syncCatalog(site)

        // THEN
        verify(catalogFileDownloader).downloadCatalogFile(eq(defaultUrl), any())
    }

    @Test
    fun `given happy path, when syncCatalog, then parses downloaded file`() = runTest {
        // WHEN
        sut.syncCatalog(site)

        // THEN
        verify(catalogFileParser).parseCatalogFile(any(), any())
    }

    @Test
    fun `given happy path, when syncCatalog, then stores parsed data`() = runTest {
        // WHEN
        sut.syncCatalog(site)

        // THEN
        verify(posLocalCatalogStore).storeCatalogData(
            localSiteId = site.localId(),
            products = defaultProducts,
            variations = defaultVariations
        )
    }

    @Test
    fun `given happy path, when syncCatalog, then cleans up old catalog files`() = runTest {
        // WHEN
        sut.syncCatalog(site)

        // THEN
        verify(catalogFileDownloader).cleanupOldCatalogFiles(keepLatest = defaultFile)
    }

    @Test
    fun `given catalog in progress, when syncCatalog, then polls until completed`() = runTest {
        // GIVEN
        givenCatalogGenerationInProgressThenCompleted(inProgressAttempts = 2)

        // WHEN
        val result = sut.syncCatalog(site)

        // THEN
        assertThat(result).isInstanceOf(WooPosFileBasedSyncAction.WooPosFileBasedSyncResult.Success::class.java)
        verify(posLocalCatalogStore, times(3)).generateCatalogOrGetStatus(site)
    }

    @Test
    fun `given catalog scheduled, when syncCatalog, then polls until completed`() = runTest {
        // GIVEN
        givenCatalogGenerationScheduledThenCompleted(scheduledAttempts = 1)

        // WHEN
        val result = sut.syncCatalog(site)

        // THEN
        assertThat(result).isInstanceOf(WooPosFileBasedSyncAction.WooPosFileBasedSyncResult.Success::class.java)
        verify(posLocalCatalogStore, times(2)).generateCatalogOrGetStatus(site)
    }

    @Test
    fun `given single API failure, when syncCatalog, then continues polling`() = runTest {
        // GIVEN
        givenCatalogGenerationFailsOnceThenCompleted()

        // WHEN
        val result = sut.syncCatalog(site)

        // THEN
        assertThat(result).isInstanceOf(WooPosFileBasedSyncAction.WooPosFileBasedSyncResult.Success::class.java)
    }

    @Test
    fun `given consecutive API failures, when syncCatalog, then returns failure`() = runTest {
        // GIVEN
        givenCatalogGenerationFailsConsecutively()

        // WHEN
        val result = sut.syncCatalog(site)

        // THEN
        assertThat(result).isInstanceOf(WooPosFileBasedSyncAction.WooPosFileBasedSyncResult.Failure::class.java)
    }

    @Test
    fun `given catalog never completes, when syncCatalog, then returns timeout failure`() = runTest {
        // GIVEN
        givenCatalogGenerationNeverCompletes()

        // WHEN
        val result = sut.syncCatalog(site)

        // THEN
        assertThat(result).isInstanceOf(WooPosFileBasedSyncAction.WooPosFileBasedSyncResult.Failure::class.java)
        val failure = (result as WooPosFileBasedSyncAction.WooPosFileBasedSyncResult.Failure).result
        assertThat(failure).isInstanceOf(PosLocalCatalogSyncResult.Failure.CatalogGenerationTimeout::class.java)
    }

    @Test
    fun `given completed without URL, when syncCatalog, then returns failure`() = runTest {
        // GIVEN
        givenCatalogGenerationCompletedWithoutUrl()

        // WHEN
        val result = sut.syncCatalog(site)

        // THEN
        assertThat(result).isInstanceOf(WooPosFileBasedSyncAction.WooPosFileBasedSyncResult.Failure::class.java)
        val failure = (result as WooPosFileBasedSyncAction.WooPosFileBasedSyncResult.Failure).result
        assertThat(failure).isInstanceOf(PosLocalCatalogSyncResult.Failure.InvalidResponse::class.java)
    }

    @Test
    fun `given file download fails, when syncCatalog, then returns failure`() = runTest {
        // GIVEN
        givenFileDownloadFails("Download error")

        // WHEN
        val result = sut.syncCatalog(site)

        // THEN
        assertThat(result).isInstanceOf(WooPosFileBasedSyncAction.WooPosFileBasedSyncResult.Failure::class.java)
    }

    @Test
    fun `given file download fails, when syncCatalog, then does not attempt to parse`() = runTest {
        // GIVEN
        givenFileDownloadFails("Download error")

        // WHEN
        sut.syncCatalog(site)

        // THEN
        verify(catalogFileParser, never()).parseCatalogFile(any(), any())
    }

    @Test
    fun `given file parsing fails, when syncCatalog, then returns failure`() = runTest {
        // GIVEN
        givenFileParseFails("Parse error")

        // WHEN
        val result = sut.syncCatalog(site)

        // THEN
        assertThat(result).isInstanceOf(WooPosFileBasedSyncAction.WooPosFileBasedSyncResult.Failure::class.java)
    }

    @Test
    fun `given file parsing fails, when syncCatalog, then does not attempt to store`() = runTest {
        // GIVEN
        givenFileParseFails("Parse error")

        // WHEN
        sut.syncCatalog(site)

        // THEN
        verify(posLocalCatalogStore, never()).storeCatalogData(any(), any(), any())
    }

    @Test
    fun `given storing data fails, when syncCatalog, then returns failure`() = runTest {
        // GIVEN
        givenStoreCatalogDataFails("Storage error")

        // WHEN
        val result = sut.syncCatalog(site)

        // THEN
        assertThat(result).isInstanceOf(WooPosFileBasedSyncAction.WooPosFileBasedSyncResult.Failure::class.java)
    }

    @Test
    fun `given storing data fails, when syncCatalog, then does not cleanup files`() = runTest {
        // GIVEN
        givenStoreCatalogDataFails("Storage error")

        // WHEN
        sut.syncCatalog(site)

        // THEN
        verify(catalogFileDownloader, never()).cleanupOldCatalogFiles(any())
    }

    @Test
    fun `given empty catalog, when syncCatalog, then returns success with zero counts`() = runTest {
        // GIVEN
        givenFileParseSuccessWithData(
            WooPosCatalogFileParser.ParsedCatalogData(
                products = emptyList(),
                variations = emptyList()
            )
        )

        // WHEN
        val result = sut.syncCatalog(site)

        // THEN
        assertThat(result).isInstanceOf(WooPosFileBasedSyncAction.WooPosFileBasedSyncResult.Success::class.java)
        val syncResult = (result as WooPosFileBasedSyncAction.WooPosFileBasedSyncResult.Success).result
        assertThat(syncResult.productsSynced).isEqualTo(0)
        assertThat(syncResult.variationsSynced).isEqualTo(0)
    }

    @Test
    fun `given catalog with only products, when syncCatalog, then returns correct counts`() = runTest {
        // GIVEN
        givenFileParseSuccessWithData(
            WooPosCatalogFileParser.ParsedCatalogData(
                products = defaultProducts,
                variations = emptyList()
            )
        )

        // WHEN
        val result = sut.syncCatalog(site)

        // THEN
        assertThat(result).isInstanceOf(WooPosFileBasedSyncAction.WooPosFileBasedSyncResult.Success::class.java)
        val syncResult = (result as WooPosFileBasedSyncAction.WooPosFileBasedSyncResult.Success).result
        assertThat(syncResult.productsSynced).isEqualTo(1)
        assertThat(syncResult.variationsSynced).isEqualTo(0)
    }

    @Test
    fun `given catalog with only variations, when syncCatalog, then returns correct counts`() = runTest {
        // GIVEN
        givenFileParseSuccessWithData(
            WooPosCatalogFileParser.ParsedCatalogData(
                products = emptyList(),
                variations = defaultVariations
            )
        )

        // WHEN
        val result = sut.syncCatalog(site)

        // THEN
        assertThat(result).isInstanceOf(WooPosFileBasedSyncAction.WooPosFileBasedSyncResult.Success::class.java)
        val syncResult = (result as WooPosFileBasedSyncAction.WooPosFileBasedSyncResult.Success).result
        assertThat(syncResult.productsSynced).isEqualTo(0)
        assertThat(syncResult.variationsSynced).isEqualTo(1)
    }

    @Test
    fun `given two failures followed by success, when syncCatalog, then returns success`() = runTest {
        // GIVEN
        givenCatalogGenerationFailsTwiceThenCompleted()

        // WHEN
        val result = sut.syncCatalog(site)

        // THEN
        assertThat(result).isInstanceOf(WooPosFileBasedSyncAction.WooPosFileBasedSyncResult.Success::class.java)
    }

    private suspend fun givenCatalogGenerationCompleted() {
        whenever(posLocalCatalogStore.generateCatalogOrGetStatus(site))
            .thenReturn(
                Result.success(
                    WooPosGenerateCatalogResult(
                        state = WooPosGenerateCatalogState.COMPLETED,
                        url = defaultUrl,
                        total = 10,
                        completedAt = "2024-01-01T12:00:00Z"
                    )
                )
            )
    }

    private suspend fun givenCatalogGenerationCompletedWithoutUrl() {
        whenever(posLocalCatalogStore.generateCatalogOrGetStatus(site))
            .thenReturn(
                Result.success(
                    WooPosGenerateCatalogResult(
                        state = WooPosGenerateCatalogState.COMPLETED,
                        url = null
                    )
                )
            )
    }

    private suspend fun givenCatalogGenerationInProgressThenCompleted(inProgressAttempts: Int) {
        val inProgressResult = Result.success(
            WooPosGenerateCatalogResult(
                state = WooPosGenerateCatalogState.IN_PROGRESS,
                progress = 50f,
                total = 100
            )
        )
        val completedResult = Result.success(
            WooPosGenerateCatalogResult(
                state = WooPosGenerateCatalogState.COMPLETED,
                url = defaultUrl
            )
        )
        var stub = whenever(posLocalCatalogStore.generateCatalogOrGetStatus(site))
            .thenReturn(inProgressResult)
        repeat(inProgressAttempts - 1) {
            stub = stub.thenReturn(inProgressResult)
        }
        stub.thenReturn(completedResult)
    }

    private suspend fun givenCatalogGenerationScheduledThenCompleted(scheduledAttempts: Int) {
        val scheduledResult = Result.success(
            WooPosGenerateCatalogResult(
                state = WooPosGenerateCatalogState.SCHEDULED,
                scheduledAt = "2024-01-01T12:00:00Z"
            )
        )
        val completedResult = Result.success(
            WooPosGenerateCatalogResult(
                state = WooPosGenerateCatalogState.COMPLETED,
                url = defaultUrl
            )
        )
        var stub = whenever(posLocalCatalogStore.generateCatalogOrGetStatus(site))
            .thenReturn(scheduledResult)
        repeat(scheduledAttempts - 1) {
            stub = stub.thenReturn(scheduledResult)
        }
        stub.thenReturn(completedResult)
    }

    private suspend fun givenCatalogGenerationFailsOnceThenCompleted() {
        val completedResult = Result.success(
            WooPosGenerateCatalogResult(
                state = WooPosGenerateCatalogState.COMPLETED,
                url = defaultUrl
            )
        )
        whenever(posLocalCatalogStore.generateCatalogOrGetStatus(site))
            .thenReturn(Result.failure(Exception("Temporary error")))
            .thenReturn(completedResult)
    }

    private suspend fun givenCatalogGenerationFailsTwiceThenCompleted() {
        val completedResult = Result.success(
            WooPosGenerateCatalogResult(
                state = WooPosGenerateCatalogState.COMPLETED,
                url = defaultUrl
            )
        )
        whenever(posLocalCatalogStore.generateCatalogOrGetStatus(site))
            .thenReturn(Result.failure(Exception("Temporary error")))
            .thenReturn(Result.failure(Exception("Temporary error")))
            .thenReturn(completedResult)
    }

    private suspend fun givenCatalogGenerationFailsConsecutively() {
        whenever(posLocalCatalogStore.generateCatalogOrGetStatus(site))
            .thenReturn(Result.failure(Exception("Persistent error")))
    }

    private suspend fun givenCatalogGenerationNeverCompletes() {
        whenever(posLocalCatalogStore.generateCatalogOrGetStatus(site))
            .thenReturn(
                Result.success(
                    WooPosGenerateCatalogResult(
                        state = WooPosGenerateCatalogState.IN_PROGRESS,
                        progress = 50f,
                        total = 100
                    )
                )
            )
    }

    private suspend fun givenFileDownloadSuccess() {
        whenever(catalogFileDownloader.downloadCatalogFile(any(), any()))
            .thenReturn(Result.success(defaultFile))
    }

    private suspend fun givenFileDownloadFails(errorMessage: String) {
        whenever(catalogFileDownloader.downloadCatalogFile(any(), any()))
            .thenReturn(Result.failure(Exception(errorMessage)))
    }

    private suspend fun givenFileParseSuccess() {
        whenever(catalogFileParser.parseCatalogFile(any(), any()))
            .thenReturn(Result.success(defaultParsedData))
    }

    private suspend fun givenFileParseSuccessWithData(data: WooPosCatalogFileParser.ParsedCatalogData) {
        whenever(catalogFileParser.parseCatalogFile(any(), any()))
            .thenReturn(Result.success(data))
    }

    private suspend fun givenFileParseFails(errorMessage: String) {
        whenever(catalogFileParser.parseCatalogFile(any(), any()))
            .thenReturn(Result.failure(Exception(errorMessage)))
    }

    private suspend fun givenStoreCatalogDataSuccess() {
        whenever(posLocalCatalogStore.storeCatalogData(any(), any(), any()))
            .thenReturn(Result.success(Unit))
    }

    private suspend fun givenStoreCatalogDataFails(errorMessage: String) {
        whenever(posLocalCatalogStore.storeCatalogData(any(), any(), any()))
            .thenReturn(Result.failure(Exception(errorMessage)))
    }
}
