package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosGenerateCatalogResult
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosGenerateCatalogState
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore

@ExperimentalCoroutinesApi
class WooPosFileBasedSyncActionTest : BaseUnitTest() {
    private val posLocalCatalogStore: WooPosLocalCatalogStore = mock()
    private val logger: WooPosLogWrapper = mock()

    private lateinit var sut: WooPosFileBasedSyncAction
    private val site = SiteModel().apply { id = 123 }

    @Before
    fun setup() {
        sut = WooPosFileBasedSyncAction(
            posLocalCatalogStore = posLocalCatalogStore,
            logger = logger
        )
    }

    @Test
    fun `given catalog is already generated, when generateCatalogWithPolling, then returns success immediately`() =
        runTest {
            // GIVEN
            val initialResult = WooPosGenerateCatalogResult(
                url = "url",
                state = WooPosGenerateCatalogState.COMPLETED,
            )
            whenever(posLocalCatalogStore.generateCatalog(site)).thenReturn(Result.success(initialResult))

            // WHEN
            val result = sut.generateCatalogWithPolling(site)

            // THEN
            assertThat(result.isSuccess).isTrue()
            verify(posLocalCatalogStore, times(1)).generateCatalog(site)
        }

    @Test
    fun `when polling completes, then returns success`() = runTest {
        // GIVEN
        val initialResult = WooPosGenerateCatalogResult(
            state = WooPosGenerateCatalogState.SCHEDULED,
        )
        val progressResult = WooPosGenerateCatalogResult(
            state = WooPosGenerateCatalogState.IN_PROGRESS,
        )
        val completedResult = WooPosGenerateCatalogResult(
            state = WooPosGenerateCatalogState.COMPLETED,
            url = "url",
        )

        whenever(posLocalCatalogStore.generateCatalog(site))
            .thenReturn(Result.success(initialResult))
            .thenReturn(Result.success(progressResult))
            .thenReturn(Result.success(completedResult))

        // WHEN
        val result = sut.generateCatalogWithPolling(site)

        // THEN
        advanceUntilIdle()
        assertThat(result.isSuccess).isTrue()
        verify(posLocalCatalogStore, times(3)).generateCatalog(site)
    }

    @Test
    fun `when polling completes without URL, then returns failure`() = runTest {
        // GIVEN
        val initialResult = WooPosGenerateCatalogResult(
            state = WooPosGenerateCatalogState.SCHEDULED
        )
        val completedWithoutUrl = WooPosGenerateCatalogResult(
            state = WooPosGenerateCatalogState.COMPLETED,
            url = null
        )

        whenever(posLocalCatalogStore.generateCatalog(site))
            .thenReturn(Result.success(initialResult))
            .thenReturn(Result.success(completedWithoutUrl))

        // WHEN
        val result = sut.generateCatalogWithPolling(site)

        // THEN
        advanceUntilIdle()
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `given initial request fails, when generateCatalogWithPolling, then retries and returns success`() = runTest {
        // GIVEN
        val error = Exception("Network error")
        val completed = WooPosGenerateCatalogResult(
            state = WooPosGenerateCatalogState.COMPLETED,
            url = "url"
        )
        whenever(posLocalCatalogStore.generateCatalog(site))
            .thenReturn(Result.failure(error))
            .thenReturn(Result.success(completed))

        // WHEN
        val result = sut.generateCatalogWithPolling(site)

        // THEN
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `given two requests fails, when generateCatalogWithPolling, then continues until success`() =
        runTest {
            // GIVEN
            val initialResult = WooPosGenerateCatalogResult(state = WooPosGenerateCatalogState.SCHEDULED)
            val networkError = Exception("Network error")
            val completedResult = WooPosGenerateCatalogResult(
                state = WooPosGenerateCatalogState.COMPLETED,
                url = "https://example.com/catalog.json"
            )

            whenever(posLocalCatalogStore.generateCatalog(site))
                .thenReturn(Result.success(initialResult))
                .thenReturn(Result.failure(networkError))
                .thenReturn(Result.failure(networkError))
                .thenReturn(Result.success(initialResult))
                .thenReturn(Result.success(completedResult))

            // WHEN
            val result = sut.generateCatalogWithPolling(site)

            // THEN
            assertThat(result.isSuccess).isTrue()
            verify(posLocalCatalogStore, times(5)).generateCatalog(site)
        }

    @Test
    fun `given 3 consecutive requests fail, when generateCatalogWithPolling, then returns failure`() = runTest {
        // GIVEN
        val error = Exception("Network error")
        val completed = WooPosGenerateCatalogResult(
            state = WooPosGenerateCatalogState.COMPLETED,
            url = "url"
        )
        whenever(posLocalCatalogStore.generateCatalog(site))
            .thenReturn(Result.failure(error))
            .thenReturn(Result.failure(error))
            .thenReturn(Result.failure(error))
            .thenReturn(Result.success(completed)) // unreachable

        // WHEN
        val result = sut.generateCatalogWithPolling(site)

        // THEN
        assertThat(result.isFailure).isFalse
    }

    @Test
    fun `when polling max attempts reached, then returns timeout failure`() = runTest {
        // GIVEN
        val inProgressResult = WooPosGenerateCatalogResult(
            state = WooPosGenerateCatalogState.IN_PROGRESS,
            progress = 50,
            total = 100
        )
        whenever(posLocalCatalogStore.generateCatalog(site))
            .thenReturn(Result.success(inProgressResult))

        // WHEN
        val result = sut.generateCatalogWithPolling(site)

        // THEN
        assertThat(result.isFailure).isTrue()
        verify(posLocalCatalogStore, times(20)).generateCatalog(site)
    }
}
