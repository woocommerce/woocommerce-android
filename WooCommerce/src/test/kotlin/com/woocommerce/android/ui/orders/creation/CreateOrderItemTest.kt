package com.woocommerce.android.ui.orders.creation


import com.woocommerce.android.ui.orders.creation.configuration.GetProductConfiguration
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.ui.products.variations.VariationDetailRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CreateOrderItemTest : BaseUnitTest() {
    private val variationDetailRepository = mock<VariationDetailRepository>()
    private val productDetailRepository = mock<ProductDetailRepository>()
    private val getProductRules = mock<GetProductRules>()
    private val getProductConfiguration = mock<GetProductConfiguration>()

    @Test
    fun `when product not found in cache, should attempt to fetch it from remote`() = testBlocking {
        whenever(productDetailRepository.getProduct(any())).thenReturn(null)
        val sut = CreateOrderItem(
            coroutineDispatchers = coroutinesTestRule.testDispatchers,
            variationDetailRepository,
            productDetailRepository,
            getProductRules,
            getProductConfiguration
        )
        sut.invoke(1L)
        verify(productDetailRepository).fetchAndGetProduct(1L)
    }

    @Test
    fun `when product is in cache, should not attempt to fetch it from remote`() = testBlocking {
        whenever(productDetailRepository.getProduct(any())).thenReturn(ProductTestUtils.generateProduct())
        val sut = CreateOrderItem(
            coroutineDispatchers = coroutinesTestRule.testDispatchers,
            variationDetailRepository,
            productDetailRepository,
            getProductRules,
            getProductConfiguration
        )
        sut.invoke(1L)
        verify(productDetailRepository, never()).fetchAndGetProduct(1L)
    }
}