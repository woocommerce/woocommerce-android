package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Refund
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.woopos.common.data.WooPosRetrieveOrderRefunds
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosRefundViewModelTest {

    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val ordersDataSource: WooPosOrdersDataSource = mock()
    private val retrieveOrderRefunds: WooPosRetrieveOrderRefunds = mock()
    private val getRefundableItems: WooPosGetRefundableItems = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val currencyFormatter: CurrencyFormatter = mock()

    private val testOrderId = 123L
    private val testOrder = OrderTestUtils.generateTestOrder(orderId = testOrderId).copy(
        number = "456",
        currency = "USD",
        items = listOf(
            Order.Item(
                itemId = 1L,
                productId = 10L,
                name = "Test Product",
                price = BigDecimal("20.00"),
                sku = "TEST-SKU",
                quantity = 2f,
                subtotal = BigDecimal("40.00"),
                subtotalTax = BigDecimal("4.00"),
                totalTax = BigDecimal("4.00"),
                total = BigDecimal("40.00"),
                variationId = 0,
                attributesList = emptyList(),
                taxes = emptyList()
            )
        )
    )

    private val testRefundableItem = WooPosRefundableItem(
        orderItemId = 1L,
        productId = 10L,
        variationId = 0,
        name = "Test Product",
        unitPrice = BigDecimal("20.00"),
        unitTax = BigDecimal("2.00"),
        formattedUnitPrice = "$20.00",
        formattedUnitTax = "$2.00",
        rowIndex = 0
    )

    private lateinit var viewModel: WooPosRefundViewModel

    @Before
    fun setUp() {
        whenever(resourceProvider.getString(R.string.error_generic)).thenReturn("An error occurred")
    }

    private fun createViewModel(): WooPosRefundViewModel {
        return WooPosRefundViewModel(
            orderId = testOrderId,
            ordersDataSource = ordersDataSource,
            retrieveOrderRefunds = retrieveOrderRefunds,
            getRefundableItems = getRefundableItems,
            resourceProvider = resourceProvider,
            currencyFormatter = currencyFormatter
        )
    }

    /**
     * Helper to create a WooPosRefundableItem with default values.
     * Note: Items with the same orderItemId but different rowIndex represent
     * expanded units from an order item with quantity > 1.
     */
    private fun createRefundableItem(
        orderItemId: Long,
        productId: Long = 10L,
        variationId: Long = 0,
        name: String = "Product $orderItemId",
        unitPrice: BigDecimal,
        unitTax: BigDecimal,
        rowIndex: Int = 0
    ) = WooPosRefundableItem(
        orderItemId = orderItemId,
        productId = productId,
        variationId = variationId,
        name = name,
        unitPrice = unitPrice,
        unitTax = unitTax,
        formattedUnitPrice = "$$unitPrice",
        formattedUnitTax = "$$unitTax",
        rowIndex = rowIndex
    )

    @Test
    fun `given viewmodel created, when order fetch fails, then state transitions from Loading to Error`() = runTest {
        // GIVEN
        whenever(ordersDataSource.getOrderById(testOrderId)).thenReturn(Result.failure(Exception()))

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosRefundState.Error::class.java)
    }

    @Test
    fun `given order fetch succeeds with refundable items, when init, then state is Content with correct data`() =
        runTest {
            // GIVEN
            val refundableItems = listOf(
                testRefundableItem.copy(rowIndex = 0),
                testRefundableItem.copy(rowIndex = 1)
            )
            whenever(ordersDataSource.getOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(testOrder)).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(testOrder, emptyList())).thenReturn(refundableItems)

            // WHEN
            viewModel = createViewModel()
            advanceUntilIdle()

            // THEN
            val state = viewModel.state.value
            assertThat(state).isInstanceOf(WooPosRefundState.Content::class.java)

            val contentState = state as WooPosRefundState.Content
            assertThat(contentState.orderId).isEqualTo(testOrderId)
            assertThat(contentState.orderNumber).isEqualTo("#456")
            assertThat(contentState.currency).isEqualTo("USD")
            assertThat(contentState.refundableItems).hasSize(2)
            assertThat(contentState.itemsCount).isEqualTo(2)
            assertThat(contentState.subtotal).isEqualTo(BigDecimal("40.00"))
            assertThat(contentState.taxes).isEqualTo(BigDecimal("4.00"))
            assertThat(contentState.total).isEqualTo(BigDecimal("44.00"))
        }

    @Test
    fun `given order fetch fails, when init, then state is Error`() = runTest {
        // GIVEN
        whenever(ordersDataSource.getOrderById(testOrderId)).thenReturn(
            Result.failure(Exception("Network error"))
        )

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosRefundState.Error::class.java)
        assertThat((state as WooPosRefundState.Error).message).isEqualTo("An error occurred")
    }

    @Test
    fun `given order fetch succeeds but no refundable items, when init, then state is NoRefundableItems`() =
        runTest {
            // GIVEN
            whenever(ordersDataSource.getOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(testOrder)).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(testOrder, emptyList())).thenReturn(emptyList())

            // WHEN
            viewModel = createViewModel()
            advanceUntilIdle()

            // THEN
            assertThat(viewModel.state.value).isEqualTo(WooPosRefundState.NoRefundableItems)
        }

    @Test
    fun `given order fetch succeeds and refunds fetch fails, when init, then uses empty refunds list`() = runTest {
        // GIVEN
        val refundableItems = listOf(testRefundableItem)
        whenever(ordersDataSource.getOrderById(testOrderId)).thenReturn(Result.success(testOrder))
        whenever(retrieveOrderRefunds.invoke(testOrder)).thenReturn(
            Result.failure(Exception("Refunds fetch failed"))
        )
        whenever(getRefundableItems.invoke(testOrder, emptyList())).thenReturn(refundableItems)

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosRefundState.Content::class.java)
        assertThat((state as WooPosRefundState.Content).refundableItems).hasSize(1)
    }

    @Test
    fun `given order with refunds, when init, then builds content state with refundable items`() = runTest {
        // GIVEN
        val refunds = listOf(
            Refund(
                id = 1L,
                amount = BigDecimal("20.00"),
                dateCreated = Date(),
                reason = "Customer request",
                automaticGatewayRefund = true,
                items = listOf(
                    Refund.Item(
                        id = 1L,
                        productId = 10L,
                        variationId = 0,
                        quantity = 1,
                        name = "Test Product",
                        subtotal = BigDecimal("20.00"),
                        total = BigDecimal("20.00"),
                        totalTax = BigDecimal("2.00"),
                        price = BigDecimal("20.00"),
                        orderItemId = 1L
                    )
                ),
                shippingLines = emptyList(),
                feeLines = emptyList()
            )
        )
        val refundableItems = listOf(testRefundableItem)

        whenever(ordersDataSource.getOrderById(testOrderId)).thenReturn(Result.success(testOrder))
        whenever(retrieveOrderRefunds.invoke(testOrder)).thenReturn(Result.success(refunds))
        whenever(getRefundableItems.invoke(testOrder, refunds)).thenReturn(refundableItems)

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosRefundState.Content::class.java)
        assertThat((state as WooPosRefundState.Content).refundableItems).hasSize(1)
    }

    @Test
    fun `given multiple refundable items, when init, then calculates subtotal, taxes, and total correctly`() =
        runTest {
            // GIVEN
            val refundableItems = listOf(
                // Two units of the same order item (orderItemId=1, quantity=2)
                createRefundableItem(
                    orderItemId = 1L,
                    productId = 10L,
                    name = "Product 1",
                    unitPrice = BigDecimal("10.00"),
                    unitTax = BigDecimal("1.00"),
                    rowIndex = 0
                ),
                createRefundableItem(
                    orderItemId = 1L,
                    productId = 10L,
                    name = "Product 1",
                    unitPrice = BigDecimal("10.00"),
                    unitTax = BigDecimal("1.00"),
                    rowIndex = 1
                ),
                // One unit of a different order item (orderItemId=2, quantity=1)
                createRefundableItem(
                    orderItemId = 2L,
                    productId = 20L,
                    name = "Product 2",
                    unitPrice = BigDecimal("15.50"),
                    unitTax = BigDecimal("1.55"),
                    rowIndex = 0
                )
            )

            whenever(ordersDataSource.getOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(testOrder)).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(testOrder, emptyList())).thenReturn(refundableItems)

            // WHEN
            viewModel = createViewModel()
            advanceUntilIdle()

            // THEN
            val state = viewModel.state.value as WooPosRefundState.Content
            assertThat(state.subtotal).isEqualTo(BigDecimal("35.50")) // 10 + 10 + 15.50
            assertThat(state.taxes).isEqualTo(BigDecimal("3.55")) // 1 + 1 + 1.55
            assertThat(state.total).isEqualTo(BigDecimal("39.05")) // 35.50 + 3.55
        }

    @Test
    fun `given content state at SelectItems step, when ContinueToReviewClicked event, then step changes to ReviewRefund`() =
        runTest {
            // GIVEN
            val refundableItems = listOf(testRefundableItem)
            whenever(ordersDataSource.getOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(testOrder)).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(testOrder, emptyList())).thenReturn(refundableItems)

            viewModel = createViewModel()
            advanceUntilIdle()

            val initialState = viewModel.state.value as WooPosRefundState.Content
            assertThat(initialState.step).isEqualTo(WooPosRefundState.Content.RefundStep.SelectItems)

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToReviewClicked)

            // THEN
            val updatedState = viewModel.state.value as WooPosRefundState.Content
            assertThat(updatedState.step).isEqualTo(WooPosRefundState.Content.RefundStep.ReviewRefund)
        }

    @Test
    fun `given content state at ReviewRefund step, when BackToSelectItemsClicked event, then step changes to SelectItems`() =
        runTest {
            // GIVEN
            val refundableItems = listOf(testRefundableItem)
            whenever(ordersDataSource.getOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(testOrder)).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(testOrder, emptyList())).thenReturn(refundableItems)

            viewModel = createViewModel()
            advanceUntilIdle()

            // Navigate to ReviewRefund step
            viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToReviewClicked)
            val reviewState = viewModel.state.value as WooPosRefundState.Content
            assertThat(reviewState.step).isEqualTo(WooPosRefundState.Content.RefundStep.ReviewRefund)

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.BackToSelectItemsClicked)

            // THEN
            val updatedState = viewModel.state.value as WooPosRefundState.Content
            assertThat(updatedState.step).isEqualTo(WooPosRefundState.Content.RefundStep.SelectItems)
        }

    @Test
    fun `given content state at ReviewRefund step, when DialogDismissed event, then step resets to SelectItems`() =
        runTest {
            // GIVEN
            val refundableItems = listOf(testRefundableItem)
            whenever(ordersDataSource.getOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(testOrder)).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(testOrder, emptyList())).thenReturn(refundableItems)

            viewModel = createViewModel()
            advanceUntilIdle()

            // Navigate to ReviewRefund step
            viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToReviewClicked)
            val reviewState = viewModel.state.value as WooPosRefundState.Content
            assertThat(reviewState.step).isEqualTo(WooPosRefundState.Content.RefundStep.ReviewRefund)

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.DialogDismissed)

            // THEN
            val updatedState = viewModel.state.value as WooPosRefundState.Content
            assertThat(updatedState.step).isEqualTo(WooPosRefundState.Content.RefundStep.SelectItems)
        }

    @Test
    fun `given content state at SelectItems step, when DialogDismissed event, then step remains at SelectItems`() =
        runTest {
            // GIVEN
            val refundableItems = listOf(testRefundableItem)
            whenever(ordersDataSource.getOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(testOrder)).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(testOrder, emptyList())).thenReturn(refundableItems)

            viewModel = createViewModel()
            advanceUntilIdle()

            val initialState = viewModel.state.value as WooPosRefundState.Content
            assertThat(initialState.step).isEqualTo(WooPosRefundState.Content.RefundStep.SelectItems)

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.DialogDismissed)

            // THEN
            val updatedState = viewModel.state.value as WooPosRefundState.Content
            assertThat(updatedState.step).isEqualTo(WooPosRefundState.Content.RefundStep.SelectItems)
        }

    @Test
    fun `given non-content state, when onUIEvent called, then state remains unchanged`() = runTest {
        // GIVEN
        whenever(ordersDataSource.getOrderById(testOrderId)).thenReturn(
            Result.failure(Exception("Network error"))
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        val errorState = viewModel.state.value
        assertThat(errorState).isInstanceOf(WooPosRefundState.Error::class.java)

        // WHEN
        viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToReviewClicked)

        // THEN - State should remain unchanged
        assertThat(viewModel.state.value).isEqualTo(errorState)
    }

    @Test
    fun `given content state at ReviewRefund step, when ContinueToConfirmClicked event, then step changes to ConfirmRefund`() =
        runTest {
            // GIVEN
            val refundableItems = listOf(testRefundableItem)
            whenever(ordersDataSource.getOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(testOrder)).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(testOrder, emptyList())).thenReturn(refundableItems)

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToReviewClicked)
            val reviewState = viewModel.state.value as WooPosRefundState.Content
            assertThat(reviewState.step).isEqualTo(WooPosRefundState.Content.RefundStep.ReviewRefund)

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToConfirmClicked)

            // THEN
            val updatedState = viewModel.state.value as WooPosRefundState.Content
            assertThat(updatedState.step).isEqualTo(WooPosRefundState.Content.RefundStep.ConfirmRefund)
        }

    @Test
    fun `given content state at ConfirmRefund step, when BackToReviewClicked event, then step changes to ReviewRefund`() =
        runTest {
            // GIVEN
            val refundableItems = listOf(testRefundableItem)
            whenever(ordersDataSource.getOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(testOrder)).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(testOrder, emptyList())).thenReturn(refundableItems)

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToReviewClicked)
            viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToConfirmClicked)
            val confirmState = viewModel.state.value as WooPosRefundState.Content
            assertThat(confirmState.step).isEqualTo(WooPosRefundState.Content.RefundStep.ConfirmRefund)

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.BackToReviewClicked)

            // THEN
            val updatedState = viewModel.state.value as WooPosRefundState.Content
            assertThat(updatedState.step).isEqualTo(WooPosRefundState.Content.RefundStep.ReviewRefund)
        }

    @Test
    fun `given content state at ConfirmRefund step, when DialogDismissed event, then step resets to SelectItems`() =
        runTest {
            // GIVEN
            val refundableItems = listOf(testRefundableItem)
            whenever(ordersDataSource.getOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(testOrder)).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(testOrder, emptyList())).thenReturn(refundableItems)

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToReviewClicked)
            viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToConfirmClicked)
            val confirmState = viewModel.state.value as WooPosRefundState.Content
            assertThat(confirmState.step).isEqualTo(WooPosRefundState.Content.RefundStep.ConfirmRefund)

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.DialogDismissed)

            // THEN
            val updatedState = viewModel.state.value as WooPosRefundState.Content
            assertThat(updatedState.step).isEqualTo(WooPosRefundState.Content.RefundStep.SelectItems)
        }
}
