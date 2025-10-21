package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.localcatalog.WooPosCheckCatalogSizeAction.WooPosCheckCatalogSizeResult
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import kotlin.Result as KotlinResult

class WooPosCheckCatalogSizeActionTest {

    private lateinit var sut: WooPosCheckCatalogSizeAction
    private var posLocalCatalogStore: WooPosLocalCatalogStore = mock()
    private lateinit var site: SiteModel
    private var logger: WooPosLogWrapper = mock()

    @Before
    fun setup() {
        sut = WooPosCheckCatalogSizeAction(posLocalCatalogStore, logger)
        site = SiteModel().apply {
            id = 1
            siteId = 123L
        }
    }

    @Test
    fun `given catalog within limit, when execute, then returns SizeAcceptable`() = runTest {
        // GIVEN
        val maxTotalItems = 1000
        givenProductsCount(300)
        givenVariationsCount(400)

        // WHEN
        val result = sut.execute(site, maxTotalItems = maxTotalItems)

        // THEN
        assertThat(result).isInstanceOf(WooPosCheckCatalogSizeResult.SizeAcceptable::class.java)
    }

    @Test
    fun `given catalog exactly at limit, when execute, then returns SizeAcceptable`() = runTest {
        // GIVEN
        val maxTotalItems = 1000
        givenProductsCount(600)
        givenVariationsCount(400)

        // WHEN
        val result = sut.execute(site, maxTotalItems = maxTotalItems)

        // THEN
        assertThat(result).isInstanceOf(WooPosCheckCatalogSizeResult.SizeAcceptable::class.java)
    }

    @Test
    fun `given catalog one item over limit, when execute, then returns CatalogTooLarge`() = runTest {
        // GIVEN
        val maxTotalItems = 1000
        givenProductsCount(600)
        givenVariationsCount(401)

        // WHEN
        val result = sut.execute(site, maxTotalItems = maxTotalItems)

        // THEN
        assertThat(result).isInstanceOf(WooPosCheckCatalogSizeResult.CatalogTooLarge::class.java)
        val catalogTooLarge = result as WooPosCheckCatalogSizeResult.CatalogTooLarge
        assertThat(catalogTooLarge.error).contains("1001 items")
        assertThat(catalogTooLarge.error).contains("products: 600")
        assertThat(catalogTooLarge.error).contains("variations: 401")
        assertThat(catalogTooLarge.error).contains("exceed maximum of 1000 items")
    }

    @Test
    fun `given catalog exceeds limit, when execute, then returns CatalogTooLarge`() = runTest {
        // GIVEN
        val maxTotalItems = 1000
        givenProductsCount(800)
        givenVariationsCount(500)

        // WHEN
        val result = sut.execute(site, maxTotalItems = maxTotalItems)

        // THEN
        assertThat(result).isInstanceOf(WooPosCheckCatalogSizeResult.CatalogTooLarge::class.java)
        val catalogTooLarge = result as WooPosCheckCatalogSizeResult.CatalogTooLarge
        assertThat(catalogTooLarge.error).contains("1300 items")
        assertThat(catalogTooLarge.error).contains("products: 800")
        assertThat(catalogTooLarge.error).contains("variations: 500")
        assertThat(catalogTooLarge.error).contains("exceed maximum of 1000 items")
    }

    @Test
    fun `given products count fetch fails, when execute, then returns SizeUnknown`() = runTest {
        // GIVEN
        val maxTotalItems = 1000
        givenProductsCountFails("Network error")
        givenVariationsCount(400)

        // WHEN
        val result = sut.execute(site, maxTotalItems = maxTotalItems)

        // THEN
        assertThat(result).isInstanceOf(WooPosCheckCatalogSizeResult.SizeUnknown::class.java)
    }

    @Test
    fun `given variations count fetch fails, when execute, then returns SizeUnknown`() = runTest {
        // GIVEN
        val maxTotalItems = 1000
        givenProductsCount(300)
        givenVariationsCountFails("API error")

        // WHEN
        val result = sut.execute(site, maxTotalItems = maxTotalItems)

        // THEN
        assertThat(result).isInstanceOf(WooPosCheckCatalogSizeResult.SizeUnknown::class.java)
    }

    @Test
    fun `given both counts fetch fail, when execute, then returns SizeUnknown`() = runTest {
        // GIVEN
        val maxTotalItems = 1000
        givenProductsCountFails("Network error")
        givenVariationsCountFails("Network error")

        // WHEN
        val result = sut.execute(site, maxTotalItems = maxTotalItems)

        // THEN
        assertThat(result).isInstanceOf(WooPosCheckCatalogSizeResult.SizeUnknown::class.java)
    }

    @Test
    fun `given empty catalog, when execute, then returns SizeAcceptable`() = runTest {
        // GIVEN
        val maxTotalItems = 1000
        givenProductsCount(0)
        givenVariationsCount(0)

        // WHEN
        val result = sut.execute(site, maxTotalItems = maxTotalItems)

        // THEN
        assertThat(result).isInstanceOf(WooPosCheckCatalogSizeResult.SizeAcceptable::class.java)
    }

    @Test
    fun `given only products no variations, when execute, then returns SizeAcceptable`() = runTest {
        // GIVEN
        val maxTotalItems = 1000
        givenProductsCount(500)
        givenVariationsCount(0)

        // WHEN
        val result = sut.execute(site, maxTotalItems = maxTotalItems)

        // THEN
        assertThat(result).isInstanceOf(WooPosCheckCatalogSizeResult.SizeAcceptable::class.java)
    }

    @Test
    fun `given only variations no products, when execute, then returns SizeAcceptable`() = runTest {
        // GIVEN
        val maxTotalItems = 1000
        givenProductsCount(0)
        givenVariationsCount(500)

        // WHEN
        val result = sut.execute(site, maxTotalItems = maxTotalItems)

        // THEN
        assertThat(result).isInstanceOf(WooPosCheckCatalogSizeResult.SizeAcceptable::class.java)
    }

    @Test
    fun `given modifiedAfterGmt filter, when execute, then passes filter to store`() = runTest {
        // GIVEN
        val maxTotalItems = 300
        val modifiedAfterGmt = "2024-01-01T00:00:00Z"
        givenProductsCount(100, modifiedAfterGmt)
        givenVariationsCount(50, modifiedAfterGmt)

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = modifiedAfterGmt, maxTotalItems = maxTotalItems)

        // THEN
        assertThat(result).isInstanceOf(WooPosCheckCatalogSizeResult.SizeAcceptable::class.java)
    }

    @Test
    fun `given incremental sync exceeds limit, when execute, then returns CatalogTooLarge`() = runTest {
        // GIVEN
        val maxTotalItems = 300
        val modifiedAfterGmt = "2024-01-01T00:00:00Z"
        givenProductsCount(200, modifiedAfterGmt)
        givenVariationsCount(150, modifiedAfterGmt)

        // WHEN
        val result = sut.execute(site, modifiedAfterGmt = modifiedAfterGmt, maxTotalItems = maxTotalItems)

        // THEN
        assertThat(result).isInstanceOf(WooPosCheckCatalogSizeResult.CatalogTooLarge::class.java)
        val catalogTooLarge = result as WooPosCheckCatalogSizeResult.CatalogTooLarge
        assertThat(catalogTooLarge.error).contains("350 items")
        assertThat(catalogTooLarge.error).contains("exceed maximum of 300 items")
    }

    private suspend fun givenProductsCount(count: Int, modifiedAfterGmt: String? = null) {
        whenever(posLocalCatalogStore.fetchProductsCount(any(), anyOrNull()))
            .thenReturn(KotlinResult.success(count))
    }

    private suspend fun givenVariationsCount(count: Int, modifiedAfterGmt: String? = null) {
        whenever(posLocalCatalogStore.fetchVariationsCount(any(), anyOrNull()))
            .thenReturn(KotlinResult.success(count))
    }

    private suspend fun givenProductsCountFails(errorMessage: String) {
        whenever(posLocalCatalogStore.fetchProductsCount(any(), anyOrNull()))
            .thenReturn(KotlinResult.failure(Exception(errorMessage)))
    }

    private suspend fun givenVariationsCountFails(errorMessage: String) {
        whenever(posLocalCatalogStore.fetchVariationsCount(any(), anyOrNull()))
            .thenReturn(KotlinResult.failure(Exception(errorMessage)))
    }
}
