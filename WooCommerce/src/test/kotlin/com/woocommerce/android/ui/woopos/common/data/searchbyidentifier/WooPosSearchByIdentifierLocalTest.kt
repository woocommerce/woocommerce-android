package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.common.data.WooPosVariationMapper
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModelMapper
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource.SyncStrategy
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsLRUCache
import com.woocommerce.android.ui.woopos.util.generateWooPosProduct
import com.woocommerce.android.ui.woopos.util.generateWooPosVariation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosProductEntity
import org.wordpress.android.fluxc.persistence.entity.pos.WooPosVariationEntity
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import kotlin.test.assertTrue

class WooPosSearchByIdentifierLocalTest {

    private lateinit var sut: WooPosSearchByIdentifierLocal
    private val productsCache: WooPosProductsCache = mock()
    private val variationsCache: WooPosVariationsLRUCache = mock()
    private val localCatalogStore: WooPosLocalCatalogStore = mock()
    private val productModelMapper: WooPosProductModelMapper = mock()
    private val variationMapper: WooPosVariationMapper = mock()
    private val selectedSite: SelectedSite = mock()

    private val testSite = SiteModel().apply {
        siteId = 999
        id = 123
    }

    @Before
    fun setup() = runTest {
        whenever(selectedSite.get()).thenReturn(testSite)
        whenever(productsCache.getAll()).thenReturn(emptyList())
        whenever(variationsCache.getAll()).thenReturn(emptyList())
        whenever(productsCache.getProductById(any())).thenReturn(null)
        whenever(localCatalogStore.findEvenUnsupportedProductByIdentifier(any(), any()))
            .thenReturn(Result.failure(Exception("Not found")))
        whenever(localCatalogStore.findEvenUnsupportedVariationByIdentifier(any(), any()))
            .thenReturn(Result.failure(Exception("Not found")))
        whenever(localCatalogStore.getProduct(any(), any()))
            .thenReturn(Result.failure(Exception("Not found")))

        sut = WooPosSearchByIdentifierLocal(
            productsCache,
            variationsCache,
            localCatalogStore,
            productModelMapper,
            variationMapper,
            selectedSite
        )
    }

    @Test
    fun `given product with matching global unique id, when search called, then return product`() = runTest {
        // GIVEN
        val identifier = "1234567890123"
        val product = generateWooPosProduct(globalUniqueId = identifier)
        whenever(productsCache.getAll()).thenReturn(listOf(product))

        // WHEN
        val result = sut(identifier, syncStrategy = SyncStrategy.REMOTE)

        // THEN
        assertEquals(WooPosSearchByIdentifierResult.Success(product), result)
    }

    @Test
    fun `given no matching products, when search called, then return failure`() = runTest {
        // GIVEN
        val identifier = "NOTFOUND"
        whenever(productsCache.getAll()).thenReturn(emptyList())
        whenever(variationsCache.getAll()).thenReturn(emptyList())

        // WHEN
        val result = sut(identifier, syncStrategy = SyncStrategy.REMOTE)

        // THEN
        assertEquals(WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NotFound), result)
    }

    @Test
    fun `given product with lowercase global unique id, when search with uppercase, then return product`() = runTest {
        // GIVEN
        val identifier = "ABC123"
        val product = generateWooPosProduct(globalUniqueId = "abc123")
        whenever(productsCache.getAll()).thenReturn(listOf(product))

        // WHEN
        val result = sut(identifier, syncStrategy = SyncStrategy.REMOTE)

        // THEN
        assertEquals(WooPosSearchByIdentifierResult.Success(product), result)
    }

    @Test
    fun `given variation with matching global unique id, when search called, then return variation`() = runTest {
        // GIVEN
        val identifier = "VAR123456"
        val productId = 1L
        val product = generateWooPosProduct(
            productId = productId,
            productType = WooPosProductModel.WooPosProductType.Variable
        )
        val variation = generateWooPosVariation(
            remoteVariationId = 10L,
            remoteProductId = productId,
            globalUniqueId = identifier
        )
        whenever(productsCache.getProductById(productId)).thenReturn(product)
        whenever(variationsCache.getAll()).thenReturn(listOf(variation))

        // WHEN
        val result = sut(identifier, syncStrategy = SyncStrategy.REMOTE)

        // THEN
        assertTrue(result is WooPosSearchByIdentifierResult.VariationSuccess)
        assertEquals(variation, result.variation)
        assertEquals(product, result.parentProduct)
    }

    @Test
    fun `given multiple variations with one matching, when search called, then return correct variation`() = runTest {
        // GIVEN
        val identifier = "MATCH-VAR"
        val productId = 1L
        val product = generateWooPosProduct(
            productId = productId,
            productType = WooPosProductModel.WooPosProductType.Variable
        )
        val variation1 = generateWooPosVariation(
            remoteVariationId = 10L,
            remoteProductId = productId,
            globalUniqueId = "OTHER-VAR"
        )
        val variation2 = generateWooPosVariation(
            remoteVariationId = 20L,
            remoteProductId = productId,
            globalUniqueId = identifier
        )
        whenever(productsCache.getProductById(productId)).thenReturn(product)
        whenever(variationsCache.getAll()).thenReturn(listOf(variation1, variation2))

        // WHEN
        val result = sut(identifier, syncStrategy = SyncStrategy.REMOTE)

        // THEN
        assertTrue(result is WooPosSearchByIdentifierResult.VariationSuccess)
        assertEquals(variation2, result.variation)
        assertEquals(product, result.parentProduct)
    }

    @Test
    fun `given variation with case insensitive match, when search called, then return variation`() = runTest {
        // GIVEN
        val identifier = "VAR-UPPER"
        val productId = 1L
        val product = generateWooPosProduct(
            productId = productId,
            productType = WooPosProductModel.WooPosProductType.Variable
        )
        val variation = generateWooPosVariation(
            remoteVariationId = 10L,
            remoteProductId = productId,
            globalUniqueId = "var-upper"
        )
        whenever(productsCache.getProductById(productId)).thenReturn(product)
        whenever(variationsCache.getAll()).thenReturn(listOf(variation))

        // WHEN
        val result = sut(identifier, syncStrategy = SyncStrategy.REMOTE)

        // THEN
        assertTrue(result is WooPosSearchByIdentifierResult.VariationSuccess)
        assertEquals(variation, result.variation)
        assertEquals(product, result.parentProduct)
    }

    @Test
    fun `given variation found but parent product not found, when search called, then return failure`() = runTest {
        // GIVEN
        val identifier = "VAR123456"
        val productId = 1L
        val variation = generateWooPosVariation(
            remoteVariationId = 10L,
            remoteProductId = productId,
            globalUniqueId = identifier
        )
        whenever(variationsCache.getAll()).thenReturn(listOf(variation))
        whenever(productsCache.getProductById(productId)).thenReturn(null)

        // WHEN
        val result = sut(identifier, syncStrategy = SyncStrategy.REMOTE)

        // THEN
        assertTrue(result is WooPosSearchByIdentifierResult.Failure)
        assertEquals(WooPosSearchByIdentifierResult.Error.NotFound, result.error)
    }

    @Test
    fun `given local catalog enabled and product found, when search called, then return product from local catalog`() =
        runTest {
            // GIVEN
            val identifier = "PRODUCT123"
            val productEntity = WooPosProductEntity(
                localSiteId = testSite.localId(),
                remoteId = LocalOrRemoteId.RemoteId(1),
                globalUniqueId = identifier,
                name = "Test Product"
            )
            val productModel = generateWooPosProduct(globalUniqueId = identifier)

            whenever(localCatalogStore.findEvenUnsupportedProductByIdentifier(testSite.localId(), identifier))
                .thenReturn(Result.success(productEntity))
            whenever(productModelMapper.fromEntity(productEntity)).thenReturn(productModel)

            // WHEN
            val result = sut(identifier, syncStrategy = SyncStrategy.LOCAL_CATALOG_FILE)

            // THEN
            assertEquals(WooPosSearchByIdentifierResult.Success(productModel), result)
        }

    @Test
    fun `given local catalog enabled and variation found, when search called, then return variation from catalog`() =
        runTest {
            // GIVEN
            val identifier = "VAR123"
            val productId = 1L
            val variationId = 10L

            val variationEntity = WooPosVariationEntity(
                localSiteId = testSite.localId(),
                remoteProductId = LocalOrRemoteId.RemoteId(productId),
                remoteVariationId = LocalOrRemoteId.RemoteId(variationId),
                globalUniqueId = identifier
            )
            val parentEntity = WooPosProductEntity(
                localSiteId = testSite.localId(),
                remoteId = LocalOrRemoteId.RemoteId(productId),
                name = "Parent Product"
            )

            val variation = generateWooPosVariation(globalUniqueId = identifier)
            val parentProduct = generateWooPosProduct()

            whenever(localCatalogStore.findEvenUnsupportedProductByIdentifier(testSite.localId(), identifier))
                .thenReturn(Result.failure(Exception("Not found")))
            whenever(localCatalogStore.findEvenUnsupportedVariationByIdentifier(testSite.localId(), identifier))
                .thenReturn(Result.success(variationEntity))
            whenever(localCatalogStore.getProduct(testSite.localId(), LocalOrRemoteId.RemoteId(productId)))
                .thenReturn(Result.success(parentEntity))
            whenever(variationMapper.fromWooPosVariationEntity(variationEntity)).thenReturn(variation)
            whenever(productModelMapper.fromEntity(parentEntity)).thenReturn(parentProduct)

            // WHEN
            val result = sut(identifier, syncStrategy = SyncStrategy.LOCAL_CATALOG_FILE)

            // THEN
            assertTrue(result is WooPosSearchByIdentifierResult.VariationSuccess)
            assertEquals(variation, result.variation)
            assertEquals(parentProduct, result.parentProduct)
        }

    @Test
    fun `given catalog supported and variation found but parent not found, when search called, then return failure`() =
        runTest {
            // GIVEN
            val identifier = "VAR123"
            val productId = 1L
            val variationId = 10L

            val variationEntity = WooPosVariationEntity(
                localSiteId = testSite.localId(),
                remoteProductId = LocalOrRemoteId.RemoteId(productId),
                remoteVariationId = LocalOrRemoteId.RemoteId(variationId),
                globalUniqueId = identifier
            )

            whenever(localCatalogStore.findEvenUnsupportedVariationByIdentifier(testSite.localId(), identifier))
                .thenReturn(Result.success(variationEntity))
            whenever(localCatalogStore.getProduct(testSite.localId(), LocalOrRemoteId.RemoteId(productId)))
                .thenReturn(Result.failure(Exception("Parent not found")))

            // WHEN
            val result = sut(identifier, syncStrategy = SyncStrategy.LOCAL_CATALOG_FILE)

            // THEN
            assertTrue(result is WooPosSearchByIdentifierResult.Failure)
            assertEquals(WooPosSearchByIdentifierResult.Error.NotFound, result.error)
        }

    @Test
    fun `given local catalog supported but product not found, when search called, then return failure`() = runTest {
        // GIVEN
        val identifier = "NOTFOUND"

        whenever(localCatalogStore.findEvenUnsupportedProductByIdentifier(testSite.localId(), identifier))
            .thenReturn(Result.failure(Exception("Not found")))
        whenever(localCatalogStore.findEvenUnsupportedVariationByIdentifier(testSite.localId(), identifier))
            .thenReturn(Result.failure(Exception("Not found")))

        // WHEN
        val result = sut(identifier, syncStrategy = SyncStrategy.LOCAL_CATALOG_FILE)

        // THEN
        assertTrue(result is WooPosSearchByIdentifierResult.Failure)
        assertEquals(WooPosSearchByIdentifierResult.Error.NotFound, result.error)
    }
}
