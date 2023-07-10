package com.woocommerce.android.ui.orders.creation.product.discount

import com.woocommerce.android.model.Order
import com.woocommerce.android.viewmodel.BaseUnitTest
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalculateItemDiscountAmountTest : BaseUnitTest() {
    private val sut = CalculateItemDiscountAmount()

    @Test
    fun `given quantity greater than 1, when calculating amount, then should return discount per single item`() {
        val quantity = 23.23F
        val subtotal = 10000F.toBigDecimal()
        val total = 8000F.toBigDecimal()
        val item = Order.Item.EMPTY.copy(quantity = quantity, subtotal = subtotal, total = total)

        val result = sut(item)

        assertEquals((subtotal - total) / quantity.toBigDecimal(), result)
    }
}