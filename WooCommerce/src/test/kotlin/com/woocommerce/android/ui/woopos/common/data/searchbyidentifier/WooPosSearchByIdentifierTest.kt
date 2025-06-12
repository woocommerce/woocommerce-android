package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductBackorderStatus
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductTaxStatus
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.settings.ProductCatalogVisibility
import com.woocommerce.android.ui.woopos.common.barcode.WooPosBarcodeFormat
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsTypesFilterConfig
import com.woocommerce.android.ui.woopos.common.data.WooPosVariationsTypesFilterConfig
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Date

class WooPosSearchByIdentifierTest {

    private lateinit var sut: WooPosSearchByIdentifier
    private val localSearcher: WooPosSearchByIdentifierLocal = mock()
    private val remoteSearcher: WooPosSearchByIdentifierRemote = mock()
    private val filterConfig: WooPosProductsTypesFilterConfig = WooPosProductsTypesFilterConfig()
    private val variationFilterConfig: WooPosVariationsTypesFilterConfig = WooPosVariationsTypesFilterConfig()

    @Before
    fun setup() {
        sut = WooPosSearchByIdentifier(localSearcher, remoteSearcher, filterConfig, variationFilterConfig)
    }

    @Test
    fun `given product exists locally, when search called, then return local product without remote search`() =
        runTest {
            // GIVEN
            val identifier = "123456"
            val format = WooPosBarcodeFormat.FormatEAN13
            val localProduct = createProduct()
            val localResult = WooPosSearchByIdentifierResult.Success(localProduct)
            whenever(localSearcher(identifier, format)).thenReturn(localResult)

            // WHEN
            val result = sut(identifier, format)

            // THEN
            assertTrue(result is WooPosSearchByIdentifierResult.Success)
            assertEquals(localProduct, (result as WooPosSearchByIdentifierResult.Success).product)
            verify(remoteSearcher, never()).invoke(identifier, format)
        }

    @Test
    fun `given product not found locally, when search called, then search remotely`() = runTest {
        // GIVEN
        val identifier = "123456"
        val format = WooPosBarcodeFormat.FormatUnknown
        val remoteProduct = createProduct()
        whenever(localSearcher(identifier, format)).thenReturn(null)
        whenever(remoteSearcher(identifier, format))
            .thenReturn(WooPosSearchByIdentifierResult.Success(remoteProduct))

        // WHEN
        val result = sut(identifier, format)

        // THEN
        assertTrue(result is WooPosSearchByIdentifierResult.Success)
        assertEquals(remoteProduct, (result as WooPosSearchByIdentifierResult.Success).product)
        verify(remoteSearcher).invoke(identifier, format)
    }

    @Test
    fun `given product not found anywhere, when search called, then return failure`() = runTest {
        // GIVEN
        val identifier = "NOTFOUND"
        val format = WooPosBarcodeFormat.FormatUnknown
        whenever(localSearcher(identifier, format)).thenReturn(null)
        whenever(remoteSearcher(identifier, format))
            .thenReturn(WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.ProductNotFound))

        // WHEN
        val result = sut(identifier, format)

        // THEN
        assertTrue(result is WooPosSearchByIdentifierResult.Failure)
        assertEquals(
            WooPosSearchByIdentifierResult.Error.ProductNotFound,
            (result as WooPosSearchByIdentifierResult.Failure).error
        )
    }

    @Test
    fun `given no format specified, when search called, then use FormatUnknown`() = runTest {
        // GIVEN
        val identifier = "123456"
        val product = createProduct()
        whenever(localSearcher(identifier, WooPosBarcodeFormat.FormatUnknown))
            .thenReturn(WooPosSearchByIdentifierResult.Success(product))

        // WHEN
        val result = sut(identifier) // Using default parameter

        // THEN
        assertTrue(result is WooPosSearchByIdentifierResult.Success)
        verify(localSearcher).invoke(identifier, WooPosBarcodeFormat.FormatUnknown)
    }

    @Test
    fun `given remote search returns network error, when search called, then return network error`() = runTest {
        // GIVEN
        val identifier = "123456"
        val format = WooPosBarcodeFormat.FormatEAN13
        whenever(localSearcher(identifier, format)).thenReturn(null)
        whenever(remoteSearcher(identifier, format))
            .thenReturn(WooPosSearchByIdentifierResult.Failure(WooPosSearchByIdentifierResult.Error.NetworkError))

        // WHEN
        val result = sut(identifier, format)

        // THEN
        assertTrue(result is WooPosSearchByIdentifierResult.Failure)
        assertEquals(
            WooPosSearchByIdentifierResult.Error.NetworkError,
            (result as WooPosSearchByIdentifierResult.Failure).error
        )
    }

    @Test
    fun `given cleanup called, when onCleanup invoked, then remote searcher cleanup is called`() {
        // WHEN
        sut.onCleanup()

        // THEN
        verify(remoteSearcher).onCleanup()
    }

    @Test
    fun `given product meets filter criteria, when search called, then return product`() = runTest {
        // GIVEN
        val identifier = "123456"
        val format = WooPosBarcodeFormat.FormatEAN13
        val product = createProduct(type = ProductType.SIMPLE.value, status = ProductStatus.PUBLISH)

        whenever(localSearcher(identifier, format))
            .thenReturn(WooPosSearchByIdentifierResult.Success(product))

        // WHEN
        val result = sut(identifier, format)

        // THEN
        assertTrue(result is WooPosSearchByIdentifierResult.Success)
        assertEquals(product, (result as WooPosSearchByIdentifierResult.Success).product)
    }

    @Test
    fun `given product has invalid status, when search called, then return product not supported`() = runTest {
        // GIVEN
        val identifier = "123456"
        val product = createProduct(status = ProductStatus.DRAFT)

        whenever(localSearcher(identifier, WooPosBarcodeFormat.FormatUnknown))
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
        val product = createProduct(isDownloadable = true)

        whenever(localSearcher(identifier, WooPosBarcodeFormat.FormatUnknown))
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
    fun `given unsupported product type, when search called, then return product not supported`() = runTest {
        // GIVEN
        val identifier = "123456"
        val product = createProduct(type = ProductType.GROUPED.value)

        whenever(localSearcher(identifier, WooPosBarcodeFormat.FormatUnknown))
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
        val product = createProduct(type = ProductType.VARIABLE.value)

        whenever(localSearcher(identifier, WooPosBarcodeFormat.FormatUnknown)).thenReturn(product)

        // WHEN
        val result = sut(identifier)

        // THEN
        assertTrue(result is WooPosSearchByIdentifierResult.Failure)
        assertEquals(
            WooPosSearchByIdentifierResult.Error.UnsupportedProduct(product.name),
            (result as WooPosSearchByIdentifierResult.Failure).error
        )
    }

    @Suppress("LongMethod")
    private fun createProduct(
        remoteId: Long = 1,
        name: String = "Test Product",
        type: String = ProductType.SIMPLE.value,
        status: ProductStatus = ProductStatus.PUBLISH,
        isDownloadable: Boolean = false
    ) = Product(
        remoteId = remoteId,
        parentId = 0,
        name = name,
        description = "",
        shortDescription = "",
        slug = "",
        type = type,
        status = status,
        catalogVisibility = ProductCatalogVisibility.VISIBLE,
        isFeatured = false,
        stockStatus = ProductStockStatus.InStock,
        backorderStatus = ProductBackorderStatus.No,
        dateCreated = Date(),
        firstImageUrl = null,
        totalSales = 0,
        reviewsAllowed = true,
        isVirtual = false,
        ratingCount = 0,
        averageRating = 0f,
        permalink = "",
        externalUrl = "",
        buttonText = "",
        price = BigDecimal.TEN,
        salePrice = null,
        regularPrice = BigDecimal.TEN,
        taxClass = Product.TAX_CLASS_DEFAULT,
        isStockManaged = false,
        stockQuantity = 0.0,
        sku = "",
        globalUniqueId = "",
        shippingClass = "",
        shippingClassId = 0,
        isDownloadable = isDownloadable,
        downloads = emptyList(),
        downloadLimit = 0,
        downloadExpiry = 0,
        purchaseNote = "",
        numVariations = 0,
        images = emptyList(),
        attributes = emptyList(),
        saleEndDateGmt = null,
        saleStartDateGmt = null,
        isSoldIndividually = false,
        taxStatus = ProductTaxStatus.Taxable,
        isSaleScheduled = false,
        isPurchasable = true,
        menuOrder = 0,
        categories = emptyList(),
        tags = emptyList(),
        groupedProductIds = emptyList(),
        crossSellProductIds = emptyList(),
        upsellProductIds = emptyList(),
        variationIds = emptyList(),
        length = 0f,
        width = 0f,
        height = 0f,
        weight = 0f,
        isSampleProduct = false,
        specialStockStatus = null,
        isConfigurable = false,
        minAllowedQuantity = null,
        maxAllowedQuantity = null,
        bundleMinSize = null,
        bundleMaxSize = null,
        groupOfQuantity = null,
        combineVariationQuantities = null,
        password = null
    )
}
