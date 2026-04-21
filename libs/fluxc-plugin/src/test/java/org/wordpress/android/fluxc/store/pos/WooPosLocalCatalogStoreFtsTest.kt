package org.wordpress.android.fluxc.store.pos

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.pos.WooPosProductRestClient
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.persistence.dao.pos.WooPosProductsDao
import org.wordpress.android.fluxc.persistence.dao.pos.WooPosSearchableFtsDao
import org.wordpress.android.fluxc.persistence.dao.pos.WooPosVariationsDao
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosProductEntity
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosSearchableFtsEntity
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosVariationEntity
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosFtsSearchResult
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import org.wordpress.android.fluxc.utils.HeadersParser
import org.wordpress.android.fluxc.utils.initCoroutineEngine

@RunWith(MockitoJUnitRunner::class)
class WooPosLocalCatalogStoreFtsTest {
    private val posProductRestClient: WooPosProductRestClient = mock()
    private val posProductsDao: WooPosProductsDao = mock()
    private val posVariationsDao: WooPosVariationsDao = mock()
    private val posFtsDao: WooPosSearchableFtsDao = mock()
    private val headersParser: HeadersParser = mock()
    private val database: WCAndroidDatabase = mock()

    private lateinit var store: WooPosLocalCatalogStore

    private val siteId = LocalId(123)

    @Before
    fun setUp() {
        store = WooPosLocalCatalogStore(
            posProductRestClient = posProductRestClient,
            coroutineEngine = initCoroutineEngine(),
            posProductDao = posProductsDao,
            posVariationsDao = posVariationsDao,
            posFtsDao = posFtsDao,
            headersParser = headersParser,
            database = database,
        )
    }

    @Test
    fun `when formatting query, then splits on unicode61 separators and adds prefix wildcards`() {
        // WHEN & THEN
        assertThat(WooPosLocalCatalogStore.formatFtsQuery("shirt")).isEqualTo("shirt*")
        assertThat(WooPosLocalCatalogStore.formatFtsQuery("blue shirt")).isEqualTo("blue* shirt*")
        assertThat(WooPosLocalCatalogStore.formatFtsQuery("t-shirt")).isEqualTo("t* shirt*")
        assertThat(WooPosLocalCatalogStore.formatFtsQuery("SHIRT-1234")).isEqualTo("SHIRT* 1234*")
        assertThat(WooPosLocalCatalogStore.formatFtsQuery("t-shirt GANT blue m")).isEqualTo("t* shirt* GANT* blue* m*")
        assertThat(WooPosLocalCatalogStore.formatFtsQuery("  shirt  blue  ")).isEqualTo("shirt* blue*")
        assertThat(WooPosLocalCatalogStore.formatFtsQuery("shirt\"blue")).isEqualTo("shirt* blue*")
    }

    @Test
    fun `when formatting empty or whitespace query, then returns blank`() {
        // WHEN & THEN
        assertThat(WooPosLocalCatalogStore.formatFtsQuery("")).isBlank()
        assertThat(WooPosLocalCatalogStore.formatFtsQuery("   ")).isBlank()
    }

    @Test
    fun `given blank query, when searching fts, then returns empty list`() = runTest {
        // WHEN
        val result = store.searchProductsFts(siteId, "  ")

        // THEN
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEmpty()
    }

    @Test
    fun `given symbol-only query, when searching fts, then returns empty list`() = runTest {
        // WHEN
        val result = store.searchProductsFts(siteId, "-!@#$")

        // THEN
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEmpty()
    }

    @Test
    fun `given no matching results, when searching fts, then returns empty list`() = runTest {
        // GIVEN
        whenever(posFtsDao.search("123", "shirt*", 100, 0))
            .thenReturn(emptyList())

        // WHEN
        val result = store.searchProductsFts(siteId, "shirt")

        // THEN
        assertThat(result.getOrThrow()).isEmpty()
    }

    @Test
    fun `given mixed fts matches, when searching, then returns hydrated results in fts order`() = runTest {
        // GIVEN
        val productFts = createFtsEntity(itemId = "1", parentProductId = "")
        val variationFts = createFtsEntity(itemId = "10", parentProductId = "1")
        val productEntity = createProductEntity(remoteId = 1L)
        val variationEntity = createVariationEntity(productId = 1L, variationId = 10L)

        whenever(posFtsDao.search("123", "shirt*", 100, 0))
            .thenReturn(listOf(productFts, variationFts))
        whenever(posProductsDao.getProductsByIds(siteId, listOf(RemoteId(1L))))
            .thenReturn(listOf(productEntity))
        whenever(posVariationsDao.getVariationsByIds(siteId, listOf(RemoteId(10L))))
            .thenReturn(listOf(variationEntity))

        // WHEN
        val result = store.searchProductsFts(siteId, "shirt")

        // THEN
        val results = result.getOrThrow()
        assertThat(results).hasSize(2)
        assertThat(results[0]).isInstanceOf(WooPosFtsSearchResult.Product::class.java)
        assertThat(results[1]).isInstanceOf(WooPosFtsSearchResult.Variation::class.java)
        assertThat((results[0] as WooPosFtsSearchResult.Product).entity.remoteId.value).isEqualTo(1L)
        assertThat((results[1] as WooPosFtsSearchResult.Variation).entity.remoteVariationId.value).isEqualTo(10L)
    }

    @Test
    fun `given entity not found in dao, when searching fts, then skips missing results`() = runTest {
        // GIVEN
        val ftsEntity = createFtsEntity(itemId = "999", parentProductId = "")
        whenever(posFtsDao.search("123", "missing*", 100, 0))
            .thenReturn(listOf(ftsEntity))
        whenever(posProductsDao.getProductsByIds(siteId, listOf(RemoteId(999L))))
            .thenReturn(emptyList())

        // WHEN
        val result = store.searchProductsFts(siteId, "missing")

        // THEN
        assertThat(result.getOrThrow()).isEmpty()
    }

    private fun createFtsEntity(
        itemId: String,
        parentProductId: String,
        name: String = "Test"
    ) = WooPosSearchableFtsEntity(
        localSiteId = "123",
        itemId = itemId,
        parentProductId = parentProductId,
        name = name,
        sku = "",
        barcode = "",
        attributeValues = ""
    )

    private fun createProductEntity(remoteId: Long) = WooPosProductEntity(
        localSiteId = siteId,
        remoteId = RemoteId(remoteId),
        name = "Test Product",
        price = "10.00",
    )

    private fun createVariationEntity(productId: Long, variationId: Long) = WooPosVariationEntity(
        localSiteId = siteId,
        remoteProductId = RemoteId(productId),
        remoteVariationId = RemoteId(variationId),
        price = "10.00",
    )
}
