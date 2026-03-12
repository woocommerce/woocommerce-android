package com.woocommerce.android.ui.woopos.orders.details.refund

import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Refund
import com.woocommerce.android.ui.woopos.orders.RefundsFetchResult
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Date
import kotlin.test.Test

class WooPosRefundInfoBuilderTest {
    private lateinit var sut: WooPosRefundInfoBuilder
    private val resourceProvider: ResourceProvider = mock()
    private val formatPrice: WooPosFormatPrice = mock()

    @Before
    fun setup() {
        whenever(resourceProvider.getString(R.string.date_time_connector)).thenReturn("at")
        whenever(resourceProvider.getString(eq(R.string.woopos_orders_details_refund_label_numbered), any()))
            .thenAnswer { "Refund #${it.arguments[1]}" }
        whenever(formatPrice.invoke(any<BigDecimal>(), any())).thenAnswer { "$${it.arguments[0]}" }

        sut = WooPosRefundInfoBuilder(resourceProvider, formatPrice)
    }

    @Test
    fun `when refunds have distinct dates, then newest refund is first and labeled Refund 1`() {
        val oldest = createRefund(id = 1, amount = BigDecimal("5.00"), dateCreated = Date(1000L))
        val middle = createRefund(id = 2, amount = BigDecimal("10.00"), dateCreated = Date(2000L))
        val newest = createRefund(id = 3, amount = BigDecimal("15.00"), dateCreated = Date(3000L))
        val refundResult = RefundsFetchResult.Success(listOf(middle, oldest, newest))

        val result = sut.buildRefundInfo(createOrder(), refundResult)

        assertThat(result.refundRows).hasSize(3)
        assertThat(result.refundRows[0].refund.id).isEqualTo(3L)
        assertThat(result.refundRows[0].label).isEqualTo("Refund #1")
        assertThat(result.refundRows[1].refund.id).isEqualTo(2L)
        assertThat(result.refundRows[1].label).isEqualTo("Refund #2")
        assertThat(result.refundRows[2].refund.id).isEqualTo(1L)
        assertThat(result.refundRows[2].label).isEqualTo("Refund #3")
    }

    @Test
    fun `when refunds fetched successfully, then totalRefunded sums all refund amounts`() {
        val refundResult = RefundsFetchResult.Success(
            listOf(
                createRefund(id = 1, amount = BigDecimal("5.00"), dateCreated = Date(1000L)),
                createRefund(id = 2, amount = BigDecimal("10.00"), dateCreated = Date(2000L)),
            )
        )

        val result = sut.buildRefundInfo(createOrder(), refundResult)

        assertThat(result.totalRefunded).isEqualByComparingTo(BigDecimal("15.00"))
    }

    @Test
    fun `when refund fetch errors, then returns empty rows with error message`() {
        whenever(resourceProvider.getString(R.string.woopos_orders_details_refund_error))
            .thenReturn("Error loading refunds")

        val result = sut.buildRefundInfo(createOrder(), RefundsFetchResult.Error)

        assertThat(result.refundRows).isEmpty()
        assertThat(result.totalRefunded).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(result.refundLoadError).isEqualTo("Error loading refunds")
    }

    private fun createRefund(
        id: Long = 1,
        amount: BigDecimal = BigDecimal("10.00"),
        dateCreated: Date = Date()
    ) = Refund(
        id = id,
        dateCreated = dateCreated,
        amount = amount,
        reason = "Test refund",
        automaticGatewayRefund = false,
        items = emptyList(),
        shippingLines = emptyList(),
        feeLines = emptyList(),
    )

    private fun createOrder() = Order.getEmptyOrder(
        dateCreated = Date(),
        dateModified = Date()
    ).copy(
        id = 1L,
        currency = "USD",
        total = BigDecimal("100.00"),
    )
}
