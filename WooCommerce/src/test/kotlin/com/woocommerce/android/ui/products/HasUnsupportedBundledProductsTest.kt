package com.woocommerce.android.ui.products

import com.woocommerce.android.model.BundleProductRules
import com.woocommerce.android.model.BundledProduct
import com.woocommerce.android.ui.products.HasUnsupportedBundledProducts.Result
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class HasUnsupportedBundledProductsTest : BaseUnitTest() {
    private val getBundledProducts: GetBundledProducts = mock()
    private val productDetailRepository: ProductDetailRepository = mock()

    private lateinit var sut: HasUnsupportedBundledProducts

    @Before
    fun setUp() {
        sut = HasUnsupportedBundledProducts(
            getBundledProducts,
            productDetailRepository,
            OrderCreationProductRestrictions()
        )
    }

    @Test
    fun `given a bundle with a subscription, when it is checked, then it holds unsupported products`() = testBlocking {
        // given
        setUpBundle(child(SIMPLE_CHILD_ID, "simple"), child(SUBSCRIPTION_CHILD_ID, "variable-subscription"))

        // when & then
        assertThat(sut(BUNDLE_ID)).isEqualTo(Result.YES)
    }

    @Test
    fun `given a bundle without subscriptions, when it is checked, then it holds no unsupported products`() =
        testBlocking {
            // given
            setUpBundle(child(SIMPLE_CHILD_ID, "simple"))

            // when & then
            assertThat(sut(BUNDLE_ID)).isEqualTo(Result.NO)
        }

    /**
     * A child kept out of the product list for its own sake says nothing about whether the bundle can be sold.
     */
    @Test
    fun `given a bundle holding a draft product, when it is checked, then it holds no unsupported products`() =
        testBlocking {
            // given
            setUpBundle(child(SIMPLE_CHILD_ID, "simple", customStatus = ProductStatus.DRAFT.name))

            // when & then
            assertThat(sut(BUNDLE_ID)).isEqualTo(Result.NO)
        }

    @Test
    fun `given a bundled product which can't be resolved, when it is checked, then the answer is unknown`() =
        testBlocking {
            // given
            setUpBundle(child(SIMPLE_CHILD_ID, "simple"), unresolvedChild(UNRESOLVED_CHILD_ID))

            // when & then
            assertThat(sut(BUNDLE_ID)).isEqualTo(Result.UNKNOWN)
        }

    @Test
    fun `given a subscription next to an unresolved child, when it is checked, then it holds unsupported products`() =
        testBlocking {
            // given
            setUpBundle(child(SUBSCRIPTION_CHILD_ID, "variable-subscription"), unresolvedChild(UNRESOLVED_CHILD_ID))

            // when & then
            assertThat(sut(BUNDLE_ID)).isEqualTo(Result.YES)
        }

    private fun setUpBundle(vararg children: BundledProduct) {
        whenever(getBundledProducts.invoke(eq(BUNDLE_ID))).thenReturn(flowOf(children.toList()))
    }

    private fun child(productId: Long, type: String, customStatus: String? = null): BundledProduct {
        val product = ProductTestUtils.generateProduct(
            productId = productId,
            productType = type,
            customStatus = customStatus
        )
        whenever(productDetailRepository.getProduct(productId)).thenReturn(product)

        return bundledProduct(productId, type)
    }

    private fun unresolvedChild(productId: Long): BundledProduct {
        whenever(productDetailRepository.getProduct(productId)).thenReturn(null)

        return bundledProduct(productId, type = "")
    }

    private fun bundledProduct(productId: Long, type: String) = BundledProduct(
        id = productId,
        parentProductId = BUNDLE_ID,
        bundledProductId = productId,
        title = "Child product",
        stockStatus = ProductStockStatus.InStock,
        rules = BundleProductRules(),
        productType = ProductType.fromString(type)
    )

    private companion object {
        const val BUNDLE_ID = 5L
        const val SIMPLE_CHILD_ID = 20L
        const val SUBSCRIPTION_CHILD_ID = 21L
        const val UNRESOLVED_CHILD_ID = 22L
    }
}
