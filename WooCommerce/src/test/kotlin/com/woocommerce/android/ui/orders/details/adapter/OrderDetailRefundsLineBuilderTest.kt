package com.woocommerce.android.ui.orders.details.adapter

import com.woocommerce.android.R
import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.Refund.Item
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class OrderDetailRefundsLineBuilderTest : BaseUnitTest() {
    private val resourceProvider: ResourceProvider = mock {
        on {
            getQuantityString(
                quantity = 1,
                default = R.string.orderdetail_product_multiple,
                one = R.string.orderdetail_product
            )
        }.thenReturn("Product")
        on {
            getQuantityString(
                quantity = 2,
                default = R.string.orderdetail_product_multiple,
                one = R.string.orderdetail_product
            )
        }.thenReturn("Products")
        on { getString(R.string.product_shipping) }.thenReturn("Shipping")
        on { getString(R.string.orderdetail_payment_fees) }.thenReturn("Fees")
    }

    private val refund = mock<Refund> {
        on { items }.thenReturn(emptyList())
        on { shippingLines }.thenReturn(emptyList())
        on { feeLines }.thenReturn(emptyList())
    }

    private val builder = OrderDetailRefundsLineBuilder(resourceProvider)

    @Test
    fun `given refund with product, when building line, then product returned`() {
        // GIVEN
        val item = mock<Item> {
            on { quantity }.thenReturn(1)
        }
        whenever(refund.items).thenReturn(
            listOf(item)
        )

        // WHEN
        val result = builder.buildRefundLine(refund)

        // THEN
        assertThat(result).isEqualTo("1 product")
    }

    @Test
    fun `given refund with products, when building line, then products returned`() {
        // GIVEN
        val itemOne = mock<Item> {
            on { quantity }.thenReturn(3)
        }
        val itemTwo = mock<Item> {
            on { quantity }.thenReturn(4)
        }
        val itemThree = mock<Item> {
            on { quantity }.thenReturn(5)
        }
        whenever(refund.items).thenReturn(
            listOf(itemOne, itemTwo, itemThree)
        )
        whenever(
            resourceProvider.getQuantityString(
                quantity = 12,
                default = R.string.orderdetail_product_multiple,
                one = R.string.orderdetail_product
            )
        ).thenReturn("Products")

        // WHEN
        val result = builder.buildRefundLine(refund)

        // THEN
        assertThat(result).isEqualTo("12 products")
    }

    @Test
    fun `given refund with fee, when building line, then fees returned`() {
        // GIVEN
        whenever(refund.feeLines).thenReturn(
            listOf(mock())
        )

        // WHEN
        val result = builder.buildRefundLine(refund)

        // THEN
        assertThat(result).isEqualTo("fees")
    }

    @Test
    fun `given refund with shipping, when building line, then shipping returned`() {
        // GIVEN
        whenever(refund.shippingLines).thenReturn(
            listOf(mock())
        )

        // WHEN
        val result = builder.buildRefundLine(refund)

        // THEN
        assertThat(result).isEqualTo("shipping")
    }

    @Test
    fun `given refund with product and shipping, when building line, then product and shipping returned`() {
        // GIVEN
        val item = mock<Item> {
            on { quantity }.thenReturn(1)
        }
        whenever(refund.items).thenReturn(
            listOf(item)
        )
        whenever(refund.shippingLines).thenReturn(
            listOf(mock())
        )

        // WHEN
        val result = builder.buildRefundLine(refund)

        // THEN
        assertThat(result).isEqualTo("1 product, shipping")
    }

    @Test
    fun `given refund with shipping and fee, when building line, then shipping and fee returned`() {
        // GIVEN
        whenever(refund.shippingLines).thenReturn(
            listOf(mock())
        )
        whenever(refund.feeLines).thenReturn(
            listOf(mock())
        )

        // WHEN
        val result = builder.buildRefundLine(refund)

        // THEN
        assertThat(result).isEqualTo("shipping, fees")
    }

    @Test
    fun `given refund with product, shipping and fee, when building line, then product shipping and fee returned`() {
        // GIVEN
        val item = mock<Item> {
            on { quantity }.thenReturn(1)
        }
        whenever(refund.items).thenReturn(
            listOf(item)
        )
        whenever(refund.shippingLines).thenReturn(
            listOf(mock())
        )
        whenever(refund.feeLines).thenReturn(
            listOf(mock())
        )

        // WHEN
        val result = builder.buildRefundLine(refund)

        // THEN
        assertThat(result).isEqualTo("1 product, shipping, fees")
    }

    @Test
    fun `given refund with product and fees, when building line, then product and fees returned`() {
        // GIVEN
        val item = mock<Item> {
            on { quantity }.thenReturn(1)
        }
        whenever(refund.items).thenReturn(
            listOf(item)
        )
        whenever(refund.feeLines).thenReturn(
            listOf(mock())
        )

        // WHEN
        val result = builder.buildRefundLine(refund)

        // THEN
        assertThat(result).isEqualTo("1 product, fees")
    }

    @Test
    fun `given refund with 2 products and fees, when building line, then 2 products and fees returned`() {
        // GIVEN
        val item = mock<Item> {
            on { quantity }.thenReturn(2)
        }
        whenever(refund.items).thenReturn(
            listOf(item)
        )
        whenever(refund.feeLines).thenReturn(
            listOf(mock())
        )

        // WHEN
        val result = builder.buildRefundLine(refund)

        // THEN
        assertThat(result).isEqualTo("2 products, fees")
    }

    @Test
    fun `given refund with 5 products and shipping, when building line, then 5 products and shipping returned`() {
        // GIVEN
        val item = mock<Item> {
            on { quantity }.thenReturn(5)
        }
        whenever(refund.items).thenReturn(
            listOf(item)
        )
        whenever(refund.shippingLines).thenReturn(
            listOf(mock())
        )
        whenever(
            resourceProvider.getQuantityString(
                quantity = 5,
                default = R.string.orderdetail_product_multiple,
                one = R.string.orderdetail_product
            )
        ).thenReturn("Products")

        // WHEN
        val result = builder.buildRefundLine(refund)

        // THEN
        assertThat(result).isEqualTo("5 products, shipping")
    }

    @Test
    fun `given refund with 10 products ship fees, when building line, then 10 products, ship and fees returned`() {
        // GIVEN
        val item = mock<Item> {
            on { quantity }.thenReturn(10)
        }
        whenever(refund.items).thenReturn(
            listOf(item)
        )
        whenever(refund.shippingLines).thenReturn(
            listOf(mock())
        )
        whenever(refund.feeLines).thenReturn(
            listOf(mock())
        )
        whenever(
            resourceProvider.getQuantityString(
                quantity = 10,
                default = R.string.orderdetail_product_multiple,
                one = R.string.orderdetail_product
            )
        ).thenReturn("Products")

        // WHEN
        val result = builder.buildRefundLine(refund)

        // THEN
        assertThat(result).isEqualTo("10 products, shipping, fees")
    }
}
