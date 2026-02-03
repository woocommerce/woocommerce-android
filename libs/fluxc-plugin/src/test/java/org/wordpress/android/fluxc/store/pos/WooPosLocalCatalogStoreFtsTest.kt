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
    fun `when formatting multi-word query, then adds prefix wildcard to each word`() {
        val result = WooPosLocalCatalogStore.formatFtsQuery("blue shirt")
        assertThat(result).isEqualTo("blue* shirt*")
    }

    @Test
    fun `when formatting single word query, then adds prefix wildcard`() {
        val result = WooPosLocalCatalogStore.formatFtsQuery("shirt")
        assertThat(result).isEqualTo("shirt*")
    }

    @Test
    fun `when formatting hyphenated query, then preserves hyphen and adds wildcard`() {
        val result = WooPosLocalCatalogStore.formatFtsQuery("t-shirt")
        assertThat(result).isEqualTo("t-shirt*")
    }

    @Test
    fun `when formatting query with extra whitespace, then trims and normalizes`() {
        val result = WooPosLocalCatalogStore.formatFtsQuery("  shirt  blue  ")
        assertThat(result).isEqualTo("shirt* blue*")
    }

    @Test
    fun `when formatting empty query, then returns blank`() {
        val result = WooPosLocalCatalogStore.formatFtsQuery("")
        assertThat(result).isBlank()
    }

    @Test
    fun `when formatting whitespace only query, then returns blank`() {
        val result = WooPosLocalCatalogStore.formatFtsQuery("   ")
        assertThat(result).isBlank()
    }

    @Test
    fun `when formatting query with fts special characters, then strips them`() {
        val result = WooPosLocalCatalogStore.formatFtsQuery("shirt\"blue")
        assertThat(result).isEqualTo("shirtblue*")
    }

    @Test
    fun `when formatting query with asterisk, then preserves single wildcard`() {
        val result = WooPosLocalCatalogStore.formatFtsQuery("shirt*")
        assertThat(result).isEqualTo("shirt*")
    }

    @Test
    fun `when formatting sku-like query, then preserves format and adds wildcard`() {
        val result = WooPosLocalCatalogStore.formatFtsQuery("SHIRT-1234")
        assertThat(result).isEqualTo("SHIRT-1234*")
    }

    @Test
    fun `when formatting multi-word variation search, then adds wildcard to each word`() {
        val result = WooPosLocalCatalogStore.formatFtsQuery("t-shirt GANT blue m")
        assertThat(result).isEqualTo("t-shirt* GANT* blue* m*")
    }

    @Test
    fun `given blank query, when searching fts, then returns empty list`() = runTest {
        val result = store.searchProductsFts(siteId, "  ")
        assertThat(result.getOrThrow()).isEmpty()
    }

    @Test
    fun `given no matching results, when searching fts, then returns empty list`() = runTest {
        whenever(posFtsDao.search("123", "shirt*", 100, 0))
            .thenReturn(emptyList())

        val result = store.searchProductsFts(siteId, "shirt")

        assertThat(result.getOrThrow()).isEmpty()
    }

    @Test
    fun `given matching product, when searching fts, then returns hydrated product`() = runTest {
        val ftsEntity = createFtsEntity(itemId = "1", parentProductId = "")
        val productEntity = createProductEntity(remoteId = 1L)

        whenever(posFtsDao.search("123", "mug*", 100, 0))
            .thenReturn(listOf(ftsEntity))
        whenever(posProductsDao.getProductsByIds(siteId, listOf(RemoteId(1L))))
            .thenReturn(listOf(productEntity))

        val result = store.searchProductsFts(siteId, "mug")

        assertThat(result.getOrThrow()).hasSize(1)
        assertThat(result.getOrThrow()[0]).isInstanceOf(WooPosFtsSearchResult.Product::class.java)
        val product = result.getOrThrow()[0] as WooPosFtsSearchResult.Product
        assertThat(product.entity.remoteId.value).isEqualTo(1L)
    }

    @Test
    fun `given matching variation, when searching fts, then returns hydrated variation`() = runTest {
        val ftsEntity = createFtsEntity(itemId = "10", parentProductId = "1")
        val variationEntity = createVariationEntity(productId = 1L, variationId = 10L)

        whenever(posFtsDao.search("123", "blue*", 100, 0))
            .thenReturn(listOf(ftsEntity))
        whenever(posVariationsDao.getVariationsByIds(siteId, listOf(RemoteId(10L))))
            .thenReturn(listOf(variationEntity))

        val result = store.searchProductsFts(siteId, "blue")

        assertThat(result.getOrThrow()).hasSize(1)
        assertThat(result.getOrThrow()[0]).isInstanceOf(WooPosFtsSearchResult.Variation::class.java)
        val variation = result.getOrThrow()[0] as WooPosFtsSearchResult.Variation
        assertThat(variation.entity.remoteVariationId.value).isEqualTo(10L)
    }

    @Test
    fun `given mixed product and variation matches, when searching fts, then returns results in fts order`() = runTest {
        val productFts = createFtsEntity(itemId = "1", parentProductId = "")
        val variationFts = createFtsEntity(itemId = "10", parentProductId = "1")

        val productEntity = createProductEntity(remoteId = 1L)
        val variationEntity = createVariationEntity(productId = 1L, variationId = 10L)

        whenever(posFtsDao.search("123", "shirt*", 15, 0))
            .thenReturn(listOf(productFts, variationFts))
        whenever(posProductsDao.getProductsByIds(siteId, listOf(RemoteId(1L))))
            .thenReturn(listOf(productEntity))
        whenever(posVariationsDao.getVariationsByIds(siteId, listOf(RemoteId(10L))))
            .thenReturn(listOf(variationEntity))

        val result = store.searchProductsFts(siteId, "shirt", pageSize = 15)

        assertThat(result.getOrThrow()).hasSize(2)
        assertThat(result.getOrThrow()[0]).isInstanceOf(WooPosFtsSearchResult.Product::class.java)
        assertThat(result.getOrThrow()[1]).isInstanceOf(WooPosFtsSearchResult.Variation::class.java)
    }

    @Test
    fun `given entity not found in dao, when searching fts, then skips missing results`() = runTest {
        val ftsEntity = createFtsEntity(itemId = "999", parentProductId = "")

        whenever(posFtsDao.search("123", "missing*", 100, 0))
            .thenReturn(listOf(ftsEntity))
        whenever(posProductsDao.getProductsByIds(siteId, listOf(RemoteId(999L))))
            .thenReturn(emptyList())

        val result = store.searchProductsFts(siteId, "missing")

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
