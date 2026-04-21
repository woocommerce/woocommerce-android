package com.woocommerce.android.ui.woopos.localcatalog

import com.google.gson.Gson
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsTypesFilterConfig
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.dao.pos.WooPosProductsDao
import org.wordpress.android.fluxc.persistence.dao.pos.WooPosSearchableFtsDao
import org.wordpress.android.fluxc.persistence.dao.pos.WooPosVariationsDao
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosProductEntity
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosSearchableFtsEntity
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosVariationEntity

@ExperimentalCoroutinesApi
class WooPosLocalCatalogSyncWithFtsTest : BaseUnitTest() {
    private lateinit var sut: WooPosLocalCatalogSyncWithFts

    private val ftsDao: WooPosSearchableFtsDao = mock()
    private val productsDao: WooPosProductsDao = mock()
    private val variationsDao: WooPosVariationsDao = mock()
    private val filterConfig = WooPosProductsTypesFilterConfig()
    private val gson = Gson()
    private val logger: WooPosLogWrapper = mock()

    private lateinit var site: SiteModel

    @Before
    fun setup() {
        sut = WooPosLocalCatalogSyncWithFts(
            ftsDao = ftsDao,
            productsDao = productsDao,
            variationsDao = variationsDao,
            filterConfig = filterConfig,
            gson = gson,
            logger = logger
        )

        site = SiteModel().apply {
            id = 1
            siteId = 123L
        }
    }

    @Test
    fun `given products, when syncFtsForFullSync, then clears and populates FTS`() =
        testBlocking {
            // GIVEN
            val products = listOf(createProduct(1, "Blue Shirt", "SKU-001", "BARCODE-001"))

            // WHEN
            sut.syncFtsForFullSync("1", products, emptyList())

            // THEN
            verify(ftsDao).deleteAllForSite("1")
            verify(ftsDao).insertAll(
                listOf(
                    WooPosSearchableFtsEntity(
                        localSiteId = "1",
                        itemId = "1",
                        parentProductId = "",
                        name = "Blue Shirt",
                        sku = "SKU-001",
                        barcode = "BARCODE-001",
                        attributeValues = ""
                    )
                )
            )
        }

    @Test
    fun `given products with variations, when syncFtsForFullSync, then includes parent name and attributes`() =
        testBlocking {
            // GIVEN
            val products = listOf(createProduct(1, "Gant T-Shirt", "SKU-001", "", type = "variable"))
            val variations = listOf(
                createVariation(
                    variationId = 10,
                    productId = 1,
                    sku = "VAR-SKU",
                    barcode = "VAR-BARCODE",
                    attributesJson = """[{"id":1,"name":"Color","option":"Blue"},""" +
                        """{"id":2,"name":"Size","option":"Medium"}]"""
                )
            )

            // WHEN
            sut.syncFtsForFullSync("1", products, variations)

            // THEN
            verify(ftsDao).insertAll(
                listOf(
                    WooPosSearchableFtsEntity(
                        localSiteId = "1",
                        itemId = "1",
                        parentProductId = "",
                        name = "Gant T-Shirt",
                        sku = "SKU-001",
                        barcode = "",
                        attributeValues = ""
                    ),
                    WooPosSearchableFtsEntity(
                        localSiteId = "1",
                        itemId = "10",
                        parentProductId = "1",
                        name = "Gant T-Shirt",
                        sku = "VAR-SKU",
                        barcode = "VAR-BARCODE",
                        attributeValues = "Blue Medium"
                    )
                )
            )
        }

    @Test
    fun `given no items to update, when syncFtsForIncrementalSync, then does nothing`() = testBlocking {
        // WHEN
        sut.syncFtsForIncrementalSync("1", emptyList(), emptyList(), emptyList())

        // THEN
        verify(ftsDao, never()).deleteProducts(any(), any())
        verify(ftsDao, never()).insertAll(any())
    }

    @Test
    fun `given products to update, when syncFtsForIncrementalSync, then deletes old and inserts new`() =
        testBlocking {
            // GIVEN
            val products = listOf(createProduct(1, "Updated Product", "SKU-001", "BARCODE"))

            // WHEN
            sut.syncFtsForIncrementalSync("1", products, emptyList(), emptyList())

            // THEN
            verify(ftsDao).deleteProducts("1", listOf("1"))
            verify(ftsDao).insertAll(
                listOf(
                    WooPosSearchableFtsEntity(
                        localSiteId = "1",
                        itemId = "1",
                        parentProductId = "",
                        name = "Updated Product",
                        sku = "SKU-001",
                        barcode = "BARCODE",
                        attributeValues = ""
                    )
                )
            )
        }

    @Test
    fun `given products to remove, when syncFtsForIncrementalSync, then deletes products and their variations from FTS`() =
        testBlocking {
            // GIVEN
            val productsToRemove = listOf(RemoteId(5), RemoteId(10))

            // WHEN
            sut.syncFtsForIncrementalSync("1", emptyList(), emptyList(), productsToRemove)

            // THEN
            verify(ftsDao).deleteProducts("1", listOf("5", "10"))
            verify(ftsDao).deleteVariationsByParentProductIds("1", listOf("5", "10"))
            verify(ftsDao, never()).insertAll(any())
        }

    @Test
    fun `given FTS empty with existing products, when ensureFtsPopulated, then populates FTS`() =
        testBlocking {
            // GIVEN
            whenever(productsDao.getProductCount(LocalId(1))).thenReturn(1)
            whenever(ftsDao.countAllForSite("1")).thenReturn(0)
            whenever(productsDao.getAllProducts(LocalId(1))).thenReturn(
                listOf(createProduct(1, "Test Product", "SKU", "BARCODE"))
            )
            whenever(variationsDao.getAllVariations(LocalId(1))).thenReturn(emptyList())

            // WHEN
            sut.ensureFtsPopulated(site)

            // THEN
            verify(ftsDao).insertAll(any())
        }

    @Test
    fun `given FTS not empty, when ensureFtsPopulated, then does not repopulate`() = testBlocking {
        // GIVEN
        whenever(productsDao.getProductCount(LocalId(1))).thenReturn(10)
        whenever(ftsDao.countAllForSite("1")).thenReturn(10)

        // WHEN
        sut.ensureFtsPopulated(site)

        // THEN
        verify(ftsDao, never()).insertAll(any())
    }

    @Test
    fun `given no products, when ensureFtsPopulated, then does not populate`() = testBlocking {
        // GIVEN
        whenever(productsDao.getProductCount(LocalId(1))).thenReturn(0)

        // WHEN
        sut.ensureFtsPopulated(site)

        // THEN
        verify(ftsDao, never()).countAllForSite(any())
        verify(ftsDao, never()).insertAll(any())
    }

    @Test
    fun `given variation with missing parent, when syncFtsForFullSync, then skips variation and logs warning`() =
        testBlocking {
            // GIVEN
            val products = listOf(createProduct(1, "Product A", "SKU-A", ""))
            val variations = listOf(
                createVariation(10, 1, "VAR-SKU-1", "", "[]"),
                createVariation(20, 999, "VAR-SKU-2", "", "[]")
            )

            // WHEN
            sut.syncFtsForFullSync("1", products, variations)

            // THEN
            verify(logger).w("Skipping variation 20: parent product 999 not found")
            verify(ftsDao).insertAll(
                listOf(
                    WooPosSearchableFtsEntity(
                        localSiteId = "1",
                        itemId = "1",
                        parentProductId = "",
                        name = "Product A",
                        sku = "SKU-A",
                        barcode = "",
                        attributeValues = ""
                    ),
                    WooPosSearchableFtsEntity(
                        localSiteId = "1",
                        itemId = "10",
                        parentProductId = "1",
                        name = "Product A",
                        sku = "VAR-SKU-1",
                        barcode = "",
                        attributeValues = ""
                    )
                )
            )
        }

    @Test
    fun `given variation with missing parent, when syncFtsForIncrementalSync, then skips variation and logs warning`() =
        testBlocking {
            // GIVEN
            val products = listOf(createProduct(1, "Product A", "SKU-A", ""))
            val variations = listOf(
                createVariation(10, 1, "VAR-SKU-1", "", "[]"),
                createVariation(20, 999, "VAR-SKU-2", "", "[]")
            )
            whenever(productsDao.getProductsByIds(LocalId(1), listOf(RemoteId(999))))
                .thenReturn(emptyList())

            // WHEN
            sut.syncFtsForIncrementalSync("1", products, variations, emptyList())

            // THEN
            verify(logger).w("Skipping variation 20: parent product 999 not found")
            verify(ftsDao).insertAll(
                listOf(
                    WooPosSearchableFtsEntity(
                        localSiteId = "1",
                        itemId = "1",
                        parentProductId = "",
                        name = "Product A",
                        sku = "SKU-A",
                        barcode = "",
                        attributeValues = ""
                    ),
                    WooPosSearchableFtsEntity(
                        localSiteId = "1",
                        itemId = "10",
                        parentProductId = "1",
                        name = "Product A",
                        sku = "VAR-SKU-1",
                        barcode = "",
                        attributeValues = ""
                    )
                )
            )
        }

    @Test
    fun `given variation with empty attributes, when building FTS entity, then attributeValues is empty`() =
        testBlocking {
            // GIVEN
            val products = listOf(createProduct(1, "Product", "", ""))
            val variations = listOf(createVariation(10, 1, "", "", "[]"))

            // WHEN
            sut.syncFtsForFullSync("1", products, variations)

            // THEN
            verify(ftsDao).insertAll(
                listOf(
                    WooPosSearchableFtsEntity(
                        localSiteId = "1",
                        itemId = "1",
                        parentProductId = "",
                        name = "Product",
                        sku = "",
                        barcode = "",
                        attributeValues = ""
                    ),
                    WooPosSearchableFtsEntity(
                        localSiteId = "1",
                        itemId = "10",
                        parentProductId = "1",
                        name = "Product",
                        sku = "",
                        barcode = "",
                        attributeValues = ""
                    )
                )
            )
        }

    @Test
    fun `given variation with malformed JSON, when building FTS entity, then attributeValues is empty`() =
        testBlocking {
            // GIVEN
            val products = listOf(createProduct(1, "Product", "", ""))
            val variations = listOf(createVariation(10, 1, "", "", "not valid json"))

            // WHEN
            sut.syncFtsForFullSync("1", products, variations)

            // THEN
            verify(ftsDao).insertAll(
                listOf(
                    WooPosSearchableFtsEntity(
                        localSiteId = "1",
                        itemId = "1",
                        parentProductId = "",
                        name = "Product",
                        sku = "",
                        barcode = "",
                        attributeValues = ""
                    ),
                    WooPosSearchableFtsEntity(
                        localSiteId = "1",
                        itemId = "10",
                        parentProductId = "1",
                        name = "Product",
                        sku = "",
                        barcode = "",
                        attributeValues = ""
                    )
                )
            )
        }

    private fun createProduct(
        id: Long,
        name: String,
        sku: String,
        barcode: String,
        type: String = "simple",
        status: String = "publish"
    ) = WooPosProductEntity(
        localSiteId = LocalId(1),
        remoteId = RemoteId(id),
        name = name,
        sku = sku,
        globalUniqueId = barcode,
        type = type,
        status = status
    )

    private fun createVariation(
        variationId: Long,
        productId: Long,
        sku: String,
        barcode: String,
        attributesJson: String,
        status: String = "publish"
    ) = WooPosVariationEntity(
        localSiteId = LocalId(1),
        remoteProductId = RemoteId(productId),
        remoteVariationId = RemoteId(variationId),
        sku = sku,
        globalUniqueId = barcode,
        attributesJson = attributesJson,
        status = status
    )
}
