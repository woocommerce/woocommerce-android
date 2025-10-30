package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsTypesFilterConfig
import com.woocommerce.android.ui.woopos.common.data.WooPosVariationsTypesFilterConfig
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.localcatalog.WooPosIsLocalCatalogSupported
import com.woocommerce.android.ui.woopos.util.generateWooPosProduct
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

class WooPosSearchByIdentifierTest {

    private lateinit var sut: WooPosSearchByIdentifier
    private val localSearcher: WooPosSearchByIdentifierLocal = mock()
    private val remoteSearcher: WooPosSearchByIdentifierRemote = mock()
    private val filterConfig: WooPosProductsTypesFilterConfig = WooPosProductsTypesFilterConfig()
    private val variationFilterConfig: WooPosVariationsTypesFilterConfig = WooPosVariationsTypesFilterConfig()
    private val localCatalogSupported: WooPosIsLocalCatalogSupported = mock()
    private val selectedSite: SelectedSite = mock()
    private val wooPosLogWrapper: WooPosLogWrapper = mock()
    private val testSite = SiteModel().apply { siteId = 123L }

    @Before
    fun setup() = runTest {
        whenever(selectedSite.get()).thenReturn(testSite)
        whenever(localCatalogSupported.invoke(any())).thenReturn(false)

        sut = WooPosSearchByIdentifier(
            localSearcher,
            remoteSearcher,
            filterConfig,
            variationFilterConfig,
            localCatalogSupported,
            selectedSite,
            wooPosLogWrapper
        )
    }

    @Test
    fun `given product exists locally, when search called, then return local product without remote search`() =
        runTest {
            // GIVEN
            val identifier = "123456"
            val localProduct = generateWooPosProduct()
            val localResult = WooPosSearchByIdentifierResult.Success(localProduct)
            whenever(localSearcher(identifier)).thenReturn(localResult)

            // WHEN
            val result = sut(identifier)

            // THEN
            assertTrue(result is WooPosSearchByIdentifierResult.Success)
            assertEquals(localProduct, (result as WooPosSearchByIdentifierResult.Success).product)
            verify(remoteSearcher, never()).invoke(identifier)
        }

    @Test
    fun `given product not found locally, when search called, then search remotely`() = runTest {
        // GIVEN
        val identifier = "123456"
        val remoteProduct = generateWooPosProduct()
        whenever(localSearcher(identifier)).thenReturn(
            WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NotFound)
        )
        whenever(remoteSearcher(identifier))
            .thenReturn(WooPosSearchByIdentifierResult.Success(remoteProduct))

        // WHEN
        val result = sut(identifier)

        // THEN
        assertTrue(result is WooPosSearchByIdentifierResult.Success)
        assertEquals(remoteProduct, (result as WooPosSearchByIdentifierResult.Success).product)
        verify(remoteSearcher).invoke(identifier)
    }

    @Test
    fun `given product not found anywhere, when search called, then return failure`() = runTest {
        // GIVEN
        val identifier = "NOTFOUND"
        whenever(localSearcher(identifier)).thenReturn(
            WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NotFound)
        )
        whenever(remoteSearcher(identifier))
            .thenReturn(WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NotFound))

        // WHEN
        val result = sut(identifier)

        // THEN
        assertTrue(result is WooPosSearchByIdentifierResult.Failure)
        assertEquals(
            WooPosSearchByIdentifierResult.Error.NotFound,
            (result as WooPosSearchByIdentifierResult.Failure).error
        )
    }

    @Test
    fun `given remote search returns network error, when search called, then return network error`() = runTest {
        // GIVEN
        val identifier = "123456"
        whenever(localSearcher(identifier)).thenReturn(
            WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NotFound)
        )
        whenever(remoteSearcher(identifier))
            .thenReturn(WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NetworkError))

        // WHEN
        val result = sut(identifier)

        // THEN
        assertTrue(result is WooPosSearchByIdentifierResult.Failure)
        assertEquals(
            WooPosSearchByIdentifierResult.Error.NetworkError,
            (result as WooPosSearchByIdentifierResult.Failure).error
        )
    }

    @Test
    fun `given product meets filter criteria, when search called, then return product`() = runTest {
        // GIVEN
        val identifier = "123456"
        val product = generateWooPosProduct(
            productType = WooPosProductModel.WooPosProductType.SIMPLE,
            status = WooPosProductModel.WooPosProductStatus.PUBLISH
        )

        whenever(localSearcher(identifier))
            .thenReturn(WooPosSearchByIdentifierResult.Success(product))

        // WHEN
        val result = sut(identifier)

        // THEN
        assertTrue(result is WooPosSearchByIdentifierResult.Success)
        assertEquals(product, (result as WooPosSearchByIdentifierResult.Success).product)
    }

    @Test
    fun `given product has invalid status, when search called, then return product not supported`() = runTest {
        // GIVEN
        val identifier = "123456"
        val product = generateWooPosProduct(status = WooPosProductModel.WooPosProductStatus.DRAFT)

        whenever(localSearcher(identifier))
            .thenReturn(WooPosSearchByIdentifierResult.Success(product))

        // WHEN
        val result = sut(identifier)

        // THEN
        assertTrue(result is WooPosSearchByIdentifierResult.Failure)
        assertEquals(
            WooPosSearchByIdentifierResult.Error.UnsupportedProduct(product.name),
            (result as WooPosSearchByIdentifierResult.Failure).error
        )
    }

    @Test
    fun `given product is downloadable, when search called, then return product not supported`() = runTest {
        // GIVEN
        val identifier = "123456"
        val product = generateWooPosProduct(isDownloadable = true)

        whenever(localSearcher(identifier))
            .thenReturn(WooPosSearchByIdentifierResult.Success(product))

        // WHEN
        val result = sut(identifier)

        // THEN
        assertTrue(result is WooPosSearchByIdentifierResult.Failure)
        assertEquals(
            WooPosSearchByIdentifierResult.Error.UnsupportedProduct(product.name),
            (result as WooPosSearchByIdentifierResult.Failure).error
        )
    }

    @Test
    fun `given variable product, when search called, then return product not supported`() = runTest {
        // GIVEN
        val identifier = "123456"
        val product = generateWooPosProduct(productType = WooPosProductModel.WooPosProductType.VARIABLE)

        whenever(localSearcher(identifier))
            .thenReturn(WooPosSearchByIdentifierResult.Success(product))

        // WHEN
        val result = sut(identifier)

        // THEN
        assertTrue(result is WooPosSearchByIdentifierResult.Failure)
        assertEquals(
            WooPosSearchByIdentifierResult.Error.UnsupportedProduct(product.name),
            (result as WooPosSearchByIdentifierResult.Failure).error
        )
    }

    @Test
    fun `given local catalog enabled and product not found locally, when search called, then don't search remotely`() =
        runTest {
            // GIVEN
            whenever(localCatalogSupported.invoke(testSite.siteId)).thenReturn(true)
            val identifier = "123456"
            whenever(localSearcher(identifier)).thenReturn(
                WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NotFound)
            )

            // WHEN
            sut(identifier)

            // THEN
            verify(remoteSearcher, never()).invoke(identifier)
        }
}
