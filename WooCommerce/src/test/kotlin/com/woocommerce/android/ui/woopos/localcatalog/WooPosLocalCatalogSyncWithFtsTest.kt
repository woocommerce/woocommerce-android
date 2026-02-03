package com.woocommerce.android.ui.woopos.localcatalog

import com.google.gson.Gson
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.featureflags.IsPosProductsFtsEnabled
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
    private val isFtsEnabled: IsPosProductsFtsEnabled = mock()
    private val gson = Gson()
    private val logger: WooPosLogWrapper = mock()

    private lateinit var site: SiteModel

    @Before
    fun setup() {
        sut = WooPosLocalCatalogSyncWithFts(
            ftsDao = ftsDao,
            productsDao = productsDao,
            variationsDao = variationsDao,
            isFtsEnabled = isFtsEnabled,
            gson = gson,
            logger = logger
        )

        site = SiteModel().apply {
            id = 1
            siteId = 123L
        }
    }

    @Test
    fun `given FTS disabled, when syncFtsForFullSync, then does nothing`() = testBlocking {
        whenever(isFtsEnabled()).thenReturn(false)

        sut.syncFtsForFullSync("1", emptyList(), emptyList())

        verify(ftsDao, never()).deleteAllForSite(any())
        verify(ftsDao, never()).insertAll(any())
    }

    @Test
    fun `given FTS enabled with products, when syncFtsForFullSync, then clears and populates FTS`() =
        testBlocking {
            whenever(isFtsEnabled()).thenReturn(true)
            val products = listOf(createProduct(1, "Blue Shirt", "SKU-001", "BARCODE-001"))

            sut.syncFtsForFullSync("1", products, emptyList())

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
    fun `given FTS enabled with variations, when syncFtsForFullSync, then includes parent name and attributes`() =
        testBlocking {
            whenever(isFtsEnabled()).thenReturn(true)
            val products = listOf(createProduct(1, "Gant T-Shirt", "SKU-001", ""))
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

            sut.syncFtsForFullSync("1", products, variations)

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
    fun `given FTS disabled, when syncFtsForIncrementalSync, then does nothing`() = testBlocking {
        whenever(isFtsEnabled()).thenReturn(false)
        val products = listOf(createProduct(1, "Product", "SKU", ""))

        sut.syncFtsForIncrementalSync("1", products, emptyList(), emptyList())

        verify(ftsDao, never()).deleteProducts(any(), any())
        verify(ftsDao, never()).insertAll(any())
    }

    @Test
    fun `given no items to update, when syncFtsForIncrementalSync, then does nothing`() = testBlocking {
        whenever(isFtsEnabled()).thenReturn(true)

        sut.syncFtsForIncrementalSync("1", emptyList(), emptyList(), emptyList())

        verify(ftsDao, never()).deleteProducts(any(), any())
        verify(ftsDao, never()).insertAll(any())
    }

    @Test
    fun `given products to update, when syncFtsForIncrementalSync, then deletes old and inserts new`() =
        testBlocking {
            whenever(isFtsEnabled()).thenReturn(true)
            val products = listOf(createProduct(1, "Updated Product", "SKU-001", "BARCODE"))

            sut.syncFtsForIncrementalSync("1", products, emptyList(), emptyList())

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
            whenever(isFtsEnabled()).thenReturn(true)
            val productsToRemove = listOf(RemoteId(5), RemoteId(10))

            // WHEN
            sut.syncFtsForIncrementalSync("1", emptyList(), emptyList(), productsToRemove)

            // THEN
            verify(ftsDao).deleteProducts("1", listOf("5", "10"))
            verify(ftsDao).deleteVariationsByParentProductIds("1", listOf("5", "10"))
            verify(ftsDao, never()).insertAll(any())
        }

    @Test
    fun `given FTS disabled, when ensureFtsPopulated, then does nothing`() = testBlocking {
        whenever(isFtsEnabled()).thenReturn(false)

        sut.ensureFtsPopulated(site)

        verify(ftsDao, never()).countAllForSite(any())
        verify(productsDao, never()).getProductCount(any())
    }

    @Test
    fun `given FTS enabled and FTS empty with existing products, when ensureFtsPopulated, then populates FTS`() =
        testBlocking {
            whenever(isFtsEnabled()).thenReturn(true)
            whenever(productsDao.getProductCount(LocalId(1))).thenReturn(1)
            whenever(ftsDao.countAllForSite("1")).thenReturn(0)
            whenever(productsDao.getAllProducts(LocalId(1))).thenReturn(
                listOf(createProduct(1, "Test Product", "SKU", "BARCODE"))
            )
            whenever(variationsDao.getAllVariations(LocalId(1))).thenReturn(emptyList())

            sut.ensureFtsPopulated(site)

            verify(ftsDao).insertAll(any())
        }

    @Test
    fun `given FTS enabled and FTS not empty, when ensureFtsPopulated, then does not repopulate`() = testBlocking {
        whenever(isFtsEnabled()).thenReturn(true)
        whenever(productsDao.getProductCount(LocalId(1))).thenReturn(10)
        whenever(ftsDao.countAllForSite("1")).thenReturn(10)

        sut.ensureFtsPopulated(site)

        verify(ftsDao, never()).insertAll(any())
    }

    @Test
    fun `given FTS enabled and no products, when ensureFtsPopulated, then does not populate`() = testBlocking {
        whenever(isFtsEnabled()).thenReturn(true)
        whenever(productsDao.getProductCount(LocalId(1))).thenReturn(0)

        sut.ensureFtsPopulated(site)

        verify(ftsDao, never()).countAllForSite(any())
        verify(ftsDao, never()).insertAll(any())
    }

    @Test
    fun `given variation with empty attributes, when building FTS entity, then attributeValues is empty`() =
        testBlocking {
            whenever(isFtsEnabled()).thenReturn(true)
            val products = listOf(createProduct(1, "Product", "", ""))
            val variations = listOf(createVariation(10, 1, "", "", "[]"))

            sut.syncFtsForFullSync("1", products, variations)

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
            whenever(isFtsEnabled()).thenReturn(true)
            val products = listOf(createProduct(1, "Product", "", ""))
            val variations = listOf(createVariation(10, 1, "", "", "not valid json"))

            sut.syncFtsForFullSync("1", products, variations)

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
        barcode: String
    ) = WooPosProductEntity(
        localSiteId = LocalId(1),
        remoteId = RemoteId(id),
        name = name,
        sku = sku,
        globalUniqueId = barcode
    )

    private fun createVariation(
        variationId: Long,
        productId: Long,
        sku: String,
        barcode: String,
        attributesJson: String
    ) = WooPosVariationEntity(
        localSiteId = LocalId(1),
        remoteProductId = RemoteId(productId),
        remoteVariationId = RemoteId(variationId),
        sku = sku,
        globalUniqueId = barcode,
        attributesJson = attributesJson
    )
}
