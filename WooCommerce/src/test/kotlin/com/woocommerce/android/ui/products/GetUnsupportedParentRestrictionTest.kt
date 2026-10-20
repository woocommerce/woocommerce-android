package com.woocommerce.android.ui.products

import com.woocommerce.android.ui.products.GetUnsupportedParentRestriction.Result
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetUnsupportedParentRestrictionTest : BaseUnitTest() {
    private val productDetailRepository: ProductDetailRepository = mock()
    private val sut = GetUnsupportedParentRestriction(productDetailRepository, OrderCreationProductRestrictions())

    private val variation = ProductTestUtils.generateProduct(productId = VARIATION_ID, parentID = PARENT_ID)

    @Test
    fun `given a subscription parent, when invoked, then the subscription restriction is returned`() = testBlocking {
        // GIVEN
        whenever(productDetailRepository.getProduct(PARENT_ID)).thenReturn(
            ProductTestUtils.generateProduct(productId = PARENT_ID, productType = "variable-subscription")
        )

        // WHEN
        val result = sut(variation)

        // THEN
        assertThat(result).isEqualTo(Result.Restricted(ProductRestriction.SubscriptionProducts))
    }

    @Test
    fun `given a fetched simple parent, when invoked, then it is not restricted`() = testBlocking {
        // GIVEN
        whenever(productDetailRepository.getProduct(PARENT_ID)).thenReturn(null)
        whenever(productDetailRepository.fetchAndGetProduct(PARENT_ID)).thenReturn(
            ProductTestUtils.generateProduct(productId = PARENT_ID, productType = "variable")
        )

        // WHEN
        val result = sut(variation)

        // THEN
        assertThat(result).isEqualTo(Result.NotRestricted)
    }

    @Test
    fun `given the parent cannot be loaded, when invoked, then the parent is unavailable`() = testBlocking {
        // GIVEN
        whenever(productDetailRepository.getProduct(PARENT_ID)).thenReturn(null)
        whenever(productDetailRepository.fetchAndGetProduct(PARENT_ID)).thenReturn(null)

        // WHEN
        val result = sut(variation)

        // THEN
        assertThat(result).isEqualTo(Result.ParentUnavailable)
    }

    private companion object {
        const val VARIATION_ID = 10L
        const val PARENT_ID = 20L
    }
}
