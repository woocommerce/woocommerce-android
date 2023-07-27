package com.woocommerce.android.ui.products

import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class ProductRestrictionsTest : BaseUnitTest() {

    //region order creation
    @Test
    fun `given draft product, when order creation products restriction, then the product is restricted`() {
        val product = ProductTestUtils.generateProduct(
            customStatus = ProductStatus.DRAFT.name
        )

        val sut = OrderCreationProductRestrictions()

        assertTrue {
            sut.isProductRestricted(product)
        }
    }

    @Test
    fun `given published product, when order creation products restriction, then the product is not restricted`() {
        val product = ProductTestUtils.generateProduct(
            customStatus = ProductStatus.PUBLISH.name
        )

        val sut = OrderCreationProductRestrictions()

        assertFalse {
            sut.isProductRestricted(product)
        }
    }

    @Test
    fun `given variable product with 0 number of variations, when order creation products restriction, then product is restricted`() {
        val product = ProductTestUtils.generateProduct(
            isVariable = true,
            variationIds = ""
        )

        val sut = OrderCreationProductRestrictions()

        assertTrue {
            sut.isProductRestricted(product)
        }
    }

    @Test
    fun `given variable product with greater than 0 number of variations, when order creation products restriction, then product is not restricted`() {
        val product = ProductTestUtils.generateProduct(
            isVariable = true,
            variationIds = "[123]"
        )

        val sut = OrderCreationProductRestrictions()

        assertFalse {
            sut.isProductRestricted(product)
        }
    }

    @Test
    fun `given product with price not specified, when order creation products restriction, then product is restricted`() {
        val product = ProductTestUtils.generateProduct(
            amount = ""
        )

        val sut = OrderCreationProductRestrictions()

        assertTrue {
            sut.isProductRestricted(product)
        }
    }
    //endregion

    //region product selector
    @Test
    fun `given draft product, when product filters products restriction, then the product is restricted`() {
        val product = ProductTestUtils.generateProduct(
            customStatus = ProductStatus.DRAFT.name
        )

        val sut = ProductFilterProductRestrictions()

        assertTrue {
            sut.isProductRestricted(product)
        }
    }

    @Test
    fun `given published product, when product filters products restriction, then the product is not restricted`() {
        val product = ProductTestUtils.generateProduct(
            customStatus = ProductStatus.PUBLISH.name
        )

        val sut = ProductFilterProductRestrictions()

        assertFalse {
            sut.isProductRestricted(product)
        }
    }

    @Test
    fun `given variable product with 0 number of variations, when product filters products restriction, then product is restricted`() {
        val product = ProductTestUtils.generateProduct(
            isVariable = true,
            variationIds = ""
        )

        val sut = ProductFilterProductRestrictions()

        assertTrue {
            sut.isProductRestricted(product)
        }
    }

    @Test
    fun `given product with price not specified, when product filters products restriction, then product is restricted`() {
        val product = ProductTestUtils.generateProduct(
            amount = ""
        )

        val sut = ProductFilterProductRestrictions()

        assertTrue {
            sut.isProductRestricted(product)
        }
    }

    //endregion
}
