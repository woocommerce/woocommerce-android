package com.woocommerce.android.ui.woopos.orders.details

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.woopos.orders.OrderStatusColorKey
import com.woocommerce.android.ui.woopos.orders.PosOrderStatus
import com.woocommerce.android.ui.woopos.orders.RefundsFetchResult
import com.woocommerce.android.ui.woopos.orders.WooPosOrderActionsProvider
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState.OrderDetailsViewState.Computed.Details.TotalsBreakdown
import com.woocommerce.android.ui.woopos.orders.details.refund.RefundInfo
import com.woocommerce.android.ui.woopos.orders.details.refund.WooPosRefundInfoBuilder
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.util.DateTimeUtils
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosOrderDetailsMapperTest : BaseUnitTest() {

    private val resourceProvider: ResourceProvider = mock()
    private val getProductById: com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById = mock()
    private val formatPrice: WooPosFormatPrice = mock()
    private val orderStatusMapper: WooPosOrderStatusMapper = mock()
    private val refundInfoBuilder: WooPosRefundInfoBuilder = mock()
    private val orderActionsProvider: WooPosOrderActionsProvider = mock()
    private val bookingInfoMapper: WooPosBookingInfoMapper = mock()

    private val sut = WooPosOrderDetailsMapper(
        resourceProvider = resourceProvider,
        getProductById = getProductById,
        formatPrice = formatPrice,
        orderStatusMapper = orderStatusMapper,
        refundInfoBuilder = refundInfoBuilder,
        orderActionsProvider = orderActionsProvider,
        bookingInfoMapper = bookingInfoMapper,
    )

    private val paidOrder: Order = OrderTestUtils.generateTestOrder().copy(
        datePaid = DateTimeUtils.dateUTCFromIso8601("2018-02-02T16:11:13Z"),
        status = Order.Status.Completed,
    )

    @Before
    fun setup() = runBlocking {
        whenever(orderStatusMapper.mapOrderStatus(any())).thenReturn(
            PosOrderStatus(text = "Completed", colorKey = OrderStatusColorKey.COMPLETED)
        )
        whenever(resourceProvider.getString(any())).thenReturn("at")
        whenever(refundInfoBuilder.buildRefundInfo(any(), any())).thenReturn(
            RefundInfo(emptyList(), BigDecimal.ZERO)
        )
        whenever(refundInfoBuilder.buildTotalsBreakdown(any(), any())).thenReturn(
            TotalsBreakdown(
                products = "$10.00",
                discount = null,
                discountCode = null,
                taxes = "$0.00",
                shipping = null,
                refunds = emptyList(),
                netPayment = null,
            )
        )
        whenever(orderActionsProvider.getAvailableActions(any())).thenReturn(emptyList())
        whenever(formatPrice(any<BigDecimal>(), anyOrNull<String>())).thenReturn("$0.00")
        whenever(formatPrice(any<BigDecimal>())).thenReturn("$0.00")
        Unit
    }

    @Test
    fun `given order is paid, when mapOrderDetails, then totalPaid equals formatted total`() = testBlocking {
        whenever(formatPrice(paidOrder.total, paidOrder.currency)).thenReturn("$106.00")

        val result = sut.mapOrderDetails(paidOrder, RefundsFetchResult.Success(emptyList()))

        assertThat(result.totalPaid).isEqualTo("$106.00")
    }

    @Test
    fun `given order is unpaid, when mapOrderDetails, then totalPaid equals formatted zero`() = testBlocking {
        val unpaidOrder = paidOrder.copy(datePaid = null)
        whenever(formatPrice(unpaidOrder.total, unpaidOrder.currency)).thenReturn("$106.00")
        whenever(formatPrice(BigDecimal.ZERO, unpaidOrder.currency)).thenReturn("$0.00")

        val result = sut.mapOrderDetails(unpaidOrder, RefundsFetchResult.Success(emptyList()))

        assertThat(result.totalPaid).isEqualTo("$0.00")
    }

    @Test
    fun `given order is paid, when mapOrderDetailsWithoutActions, then totalPaid equals formatted total`() =
        testBlocking {
            whenever(formatPrice(paidOrder.total)).thenReturn("$106.00")

            val result = sut.mapOrderDetailsWithoutActions(paidOrder)

            assertThat(result.totalPaid).isEqualTo("$106.00")
        }

    @Test
    fun `given order is unpaid, when mapOrderDetailsWithoutActions, then totalPaid equals formatted zero`() =
        testBlocking {
            val unpaidOrder = paidOrder.copy(datePaid = null)
            whenever(formatPrice(unpaidOrder.total)).thenReturn("$106.00")
            whenever(formatPrice(BigDecimal.ZERO)).thenReturn("$0.00")

            val result = sut.mapOrderDetailsWithoutActions(unpaidOrder)

            assertThat(result.totalPaid).isEqualTo("$0.00")
        }
}
