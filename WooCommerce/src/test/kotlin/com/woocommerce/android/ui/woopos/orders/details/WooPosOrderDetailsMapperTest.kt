package com.woocommerce.android.ui.woopos.orders.details

import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.woopos.orders.RefundsFetchResult
import com.woocommerce.android.ui.woopos.orders.WooPosOrderActionsProvider
import com.woocommerce.android.ui.woopos.orders.details.refund.RefundInfo
import com.woocommerce.android.ui.woopos.orders.details.refund.WooPosRefundInfoBuilder
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
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

    private suspend fun setupMocks() {
        whenever(orderStatusMapper.mapOrderStatus(any())).thenReturn(mock())
        whenever(resourceProvider.getString(any())).thenReturn("at")
        whenever(refundInfoBuilder.buildRefundInfo(any(), any())).thenReturn(
            RefundInfo(emptyList(), BigDecimal.ZERO)
        )
        whenever(refundInfoBuilder.buildTotalsBreakdown(any(), any())).thenReturn(mock())
        whenever(orderActionsProvider.getAvailableActions(any())).thenReturn(emptyList())
        whenever(formatPrice(any<BigDecimal>(), anyOrNull<String>())).thenReturn("")
        whenever(formatPrice(any<BigDecimal>())).thenReturn("")
    }

    @Test
    fun `given order is paid, when mapOrderDetails, then totalPaid equals formatted total`() = testBlocking {
        setupMocks()
        val order = OrderTestUtils.generateTestOrder()
            .copy(datePaid = DateTimeUtils.dateUTCFromIso8601("2018-02-02T16:11:13Z"))
        whenever(formatPrice(order.total, order.currency)).thenReturn("$106.00")
        whenever(formatPrice(any<BigDecimal>())).thenReturn("")

        val result = sut.mapOrderDetails(order, RefundsFetchResult.Success(emptyList()))

        assertThat(result.totalPaid).isEqualTo("$106.00")
    }

    @Test
    fun `given order is unpaid, when mapOrderDetails, then totalPaid equals formatted zero`() = testBlocking {
        setupMocks()
        val order = OrderTestUtils.generateTestOrder().copy(datePaid = null)
        whenever(formatPrice(BigDecimal.ZERO, order.currency)).thenReturn("$0.00")
        whenever(formatPrice(order.total, order.currency)).thenReturn("$106.00")
        whenever(formatPrice(any<BigDecimal>())).thenReturn("")

        val result = sut.mapOrderDetails(order, RefundsFetchResult.Success(emptyList()))

        assertThat(result.totalPaid).isEqualTo("$0.00")
    }

    @Test
    fun `given order is paid, when mapOrderDetailsWithoutActions, then totalPaid equals formatted total`() =
        testBlocking {
            setupMocks()
            val order = OrderTestUtils.generateTestOrder()
                .copy(datePaid = DateTimeUtils.dateUTCFromIso8601("2018-02-02T16:11:13Z"))
            whenever(formatPrice(order.total)).thenReturn("$106.00")

            val result = sut.mapOrderDetailsWithoutActions(order)

            assertThat(result.totalPaid).isEqualTo("$106.00")
        }

    @Test
    fun `given order is unpaid, when mapOrderDetailsWithoutActions, then totalPaid equals formatted zero`() =
        testBlocking {
            setupMocks()
            val order = OrderTestUtils.generateTestOrder().copy(datePaid = null)
            whenever(formatPrice(BigDecimal.ZERO)).thenReturn("$0.00")
            whenever(formatPrice(order.total)).thenReturn("$106.00")

            val result = sut.mapOrderDetailsWithoutActions(order)

            assertThat(result.totalPaid).isEqualTo("$0.00")
        }
}
