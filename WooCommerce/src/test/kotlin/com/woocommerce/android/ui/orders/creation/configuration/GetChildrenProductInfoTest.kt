package com.woocommerce.android.ui.orders.creation.configuration

import com.woocommerce.android.model.BundleProductRules
import com.woocommerce.android.model.BundledProduct
import com.woocommerce.android.ui.products.GetBundledProducts
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class GetChildrenProductInfoTest : BaseUnitTest() {

    // Mock dependencies
    private val productDetailRepository: ProductDetailRepository = mock()
    private val getBundledProducts: GetBundledProducts = mock()

    private lateinit var getChildrenProductInfo: GetChildrenProductInfo

    @Before
    fun setUp() {
        getChildrenProductInfo = GetChildrenProductInfo(productDetailRepository, getBundledProducts)
    }

    @Test
    fun `invoke with bundle product type should return a flow of product info map`() = testBlocking {
        // Given
        val productBundle = ProductTestUtils.generateProduct(
            productId = 1L,
            productName = "Product 1",
            imageUrl = "image1.jpg",
            productType = "bundle"
        )
        val childProduct1 = BundledProduct(
            id = 2L,
            parentProductId = 1L,
            bundledProductId = 2L,
            title = "Child product 1",
            stockStatus = ProductStockStatus.InStock,
            rules = BundleProductRules(),
            imageUrl = "image2.jpg",
            isVariable = false
        )
        val childProduct2 = BundledProduct(
            id = 3L,
            parentProductId = 1L,
            bundledProductId = 3L,
            title = "Child product 2",
            stockStatus = ProductStockStatus.InStock,
            rules = BundleProductRules(),
            isVariable = false
        )
        val bundledProducts = listOf(childProduct1, childProduct2)

        whenever(productDetailRepository.getProduct(productBundle.remoteId)).thenReturn(productBundle)
        whenever(getBundledProducts.invoke(productBundle.remoteId)).thenReturn(flowOf(bundledProducts))

        // When
        val resultFlow = getChildrenProductInfo(productBundle.remoteId)

        // Assert
        resultFlow.collect { productInfoMap ->
            assert(productInfoMap.size == 2)
            with(productInfoMap[childProduct1.id]) {
                assertNotNull(this)
                assertEquals(this.id, childProduct1.id)
                assertEquals(this.title, childProduct1.title)
                assertEquals(this.imageUrl, childProduct1.imageUrl)
            }
            with(productInfoMap[childProduct2.id]) {
                assertNotNull(this)
                assertEquals(this.id, childProduct2.id)
                assertEquals(this.title, childProduct2.title)
                assertEquals(this.imageUrl, childProduct2.imageUrl)
            }
        }

        // Verify
        verify(productDetailRepository).getProduct(productBundle.remoteId)
        verify(getBundledProducts).invoke(productBundle.remoteId)
    }

    @Test
    fun `invoke with non-children product type should return an empty flow`() = testBlocking {
        // Given
        val product = ProductTestUtils.generateProduct(
            productId = 1L,
            productName = "Product 1",
            imageUrl = "image1.jpg"
        )
        whenever(productDetailRepository.getProduct(product.remoteId)).thenReturn(product)

        // When
        val resultFlow = getChildrenProductInfo(product.remoteId)

        // Assert
        resultFlow.collect { productInfoMap -> assert(productInfoMap.isEmpty()) }

        // Verify
        verify(productDetailRepository).getProduct(product.remoteId)
        verify(getBundledProducts, never()).invoke(product.remoteId)
    }
}
