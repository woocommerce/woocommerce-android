package com.woocommerce.android.ui.products

import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class ProductRestrictionsTest : BaseUnitTest() {

    @Test
    fun `given draft product, when order creation products restriction, then return true`() {
        val product = ProductTestUtils.generateProduct(
            customStatus = ProductStatus.DRAFT.name
        )

        val sut = OrderCreationProductRestrictions()

        assertTrue {
            sut.isProductRestricted(product)
        }
    }
}
