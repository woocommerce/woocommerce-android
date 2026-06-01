package com.woocommerce.android.ui.woopos.orders.details.refund

import app.cash.turbine.test
import com.woocommerce.android.R
import com.woocommerce.android.model.PaymentGateway
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.woopos.orders.WooPosLoadPaymentGateway
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.refunds.RefundRequestItem
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCRefundStore
import java.math.BigDecimal
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosRefundSubmissionProcessorTest {
    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val refundStore: WCRefundStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val loadPaymentGateway: WooPosLoadPaymentGateway = mock()
    private val resourceProvider: ResourceProvider = mock()

    private val site = SiteModel().apply { id = 1 }
    private val refundAmount = BigDecimal("22.00")
    private val refundItems = listOf(
        RefundRequestItem(
            itemId = 1L,
            quantity = 1,
            refundTotal = BigDecimal("20.00"),
            refundTax = emptyList()
        )
    )
    private val order = OrderTestUtils.generateTestOrder(orderId = 123L)
    private val request = WooPosRefundSubmissionRequest(
        order = order,
        refundAmount = refundAmount,
        refundReason = "Customer request",
        refundItems = refundItems
    )
    private val refundModel = WCRefundModel(
        id = 1L,
        dateCreated = Date(),
        amount = refundAmount,
        reason = "",
        automaticGatewayRefund = false,
        items = emptyList(),
        shippingLineItems = emptyList(),
        feeLineItems = emptyList()
    )

    private lateinit var processor: WooPosRefundSubmissionProcessor

    @Before
    fun setUp() = runTest {
        whenever(selectedSite.get()).thenReturn(site)
        whenever(resourceProvider.getString(R.string.error_generic)).thenReturn("Something went wrong")
        whenever(resourceProvider.getString(R.string.woopos_refund_error_gateway_not_found))
            .thenReturn("Unable to process refund.")
        whenever(loadPaymentGateway.invoke(any())).thenReturn(
            Result.success(
                PaymentGateway(
                    title = "WooPayments",
                    description = "",
                    isEnabled = true,
                    methodTitle = "WooPayments",
                    methodDescription = "",
                    supportsRefunds = true
                )
            )
        )
        whenever(
            refundStore.createItemsRefund(
                site = any(),
                orderId = any(),
                amount = any(),
                reason = any(),
                restockItems = any(),
                autoRefund = any(),
                items = any()
            )
        ).thenReturn(WooResult(refundModel))

        processor = WooPosRefundSubmissionProcessor(
            refundStore = refundStore,
            selectedSite = selectedSite,
            loadPaymentGateway = loadPaymentGateway,
            resourceProvider = resourceProvider,
        )
    }

    @Test
    fun `given refund request, when submitted, then backend refund is created`() = runTest {
        processor.submit(request).test {
            assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.Processing)
            assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.Success)
            awaitComplete()
        }

        verify(refundStore).createItemsRefund(
            site = eq(site),
            orderId = eq(order.id),
            amount = eq(refundAmount),
            reason = eq("Customer request"),
            restockItems = eq(true),
            autoRefund = eq(true),
            items = eq(refundItems)
        )
    }

    @Test
    fun `given payment gateway does not support refunds, when submitted, then backend refund is created manually`() =
        runTest {
            whenever(loadPaymentGateway.invoke(any())).thenReturn(
                Result.success(
                    PaymentGateway(
                        title = "Manual gateway",
                        description = "",
                        isEnabled = true,
                        methodTitle = "Manual gateway",
                        methodDescription = "",
                        supportsRefunds = false
                    )
                )
            )

            processor.submit(request).test {
                assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.Processing)
                assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.Success)
                awaitComplete()
            }

            verify(refundStore).createItemsRefund(
                site = eq(site),
                orderId = eq(order.id),
                amount = eq(refundAmount),
                reason = eq("Customer request"),
                restockItems = eq(true),
                autoRefund = eq(false),
                items = eq(refundItems)
            )
        }

    @Test
    fun `given payment gateway load fails, when submitted, then failure is emitted`() = runTest {
        whenever(loadPaymentGateway.invoke(any())).thenReturn(Result.failure(IllegalStateException("missing")))

        processor.submit(request).test {
            assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.Processing)
            assertThat(awaitItem()).isEqualTo(
                WooPosRefundSubmissionState.Failure("Unable to process refund.")
            )
            awaitComplete()
        }
    }

    @Test
    fun `given selected site lookup fails, when submitted, then generic failure is emitted`() = runTest {
        whenever(selectedSite.get()).thenThrow(IllegalStateException("missing site"))

        processor.submit(request).test {
            assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.Processing)
            assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.Failure("Something went wrong"))
            awaitComplete()
        }
    }

    @Test
    fun `given submission is cancelled, when submitted, then cancellation is rethrown`() = runTest {
        val cancellation = CancellationException("cancelled")
        whenever(selectedSite.get()).thenThrow(cancellation)

        processor.submit(request).test {
            assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.Processing)
            assertThat(awaitError()).isSameAs(cancellation)
        }
    }

    @Test
    fun `given backend refund fails, when submitted, then failure message is emitted`() = runTest {
        whenever(
            refundStore.createItemsRefund(
                site = any(),
                orderId = any(),
                amount = any(),
                reason = any(),
                restockItems = any(),
                autoRefund = any(),
                items = any()
            )
        ).thenReturn(
            WooResult(
                WooError(
                    type = WooErrorType.GENERIC_ERROR,
                    original = GenericErrorType.UNKNOWN,
                    message = "Refund failed"
                )
            )
        )

        processor.submit(request).test {
            assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.Processing)
            assertThat(awaitItem()).isEqualTo(WooPosRefundSubmissionState.Failure("Refund failed"))
            awaitComplete()
        }
    }
}
