package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Refund
import com.woocommerce.android.tools.SelectedSite
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
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.refunds.RefundRequestItem
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCRefundStore
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
    private val groupRefundItems: WooPosGroupRefundItems = mock()
    private val calculateRefundSubtotal = WooPosCalculateRefundSubtotal()
    private val calculateRefundTax = WooPosCalculateRefundTax()
    private val resourceProvider: ResourceProvider = mock()
    private val currencyFormatter: CurrencyFormatter = mock()
    private val refundStore: WCRefundStore = mock()
    private val selectedSite: SelectedSite = mock()

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
                taxes = listOf(
                    Order.LineTaxEntry(rateId = 1L, taxAmount = BigDecimal("4.00"))
                )
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

    private val testSite = SiteModel().apply { id = 1 }

    private val testRefundModel = WCRefundModel(
        id = 1L,
        dateCreated = Date(),
        amount = BigDecimal("22.00"),
        reason = "",
        automaticGatewayRefund = false,
        items = emptyList(),
        shippingLineItems = emptyList(),
        feeLineItems = emptyList()
    )

    @Before
    fun setUp() {
        whenever(resourceProvider.getString(R.string.error_generic)).thenReturn("An error occurred")
        whenever(selectedSite.get()).thenReturn(testSite)
    }

    private fun createViewModel(): WooPosRefundViewModel {
        return WooPosRefundViewModel(
            orderId = testOrderId,
            ordersDataSource = ordersDataSource,
            retrieveOrderRefunds = retrieveOrderRefunds,
            getRefundableItems = getRefundableItems,
            groupRefundItems = groupRefundItems,
            calculateRefundSubtotal = calculateRefundSubtotal,
            calculateRefundTax = calculateRefundTax,
            resourceProvider = resourceProvider,
            currencyFormatter = currencyFormatter,
            refundStore = refundStore,
            selectedSite = selectedSite
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
        whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.failure(Exception()))

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
            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
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
        whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(
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
            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
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
        whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
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

        whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
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
    @Suppress("LongMethod")
    fun `given multiple refundable items, when init, then calculates subtotal, taxes, and total correctly`() =
        runTest {
            // GIVEN
            val orderWithMultipleItems = testOrder.copy(
                items = listOf(
                    Order.Item(
                        itemId = 1L,
                        productId = 10L,
                        name = "Product 1",
                        price = BigDecimal("10.00"),
                        sku = "PROD-1",
                        quantity = 2f,
                        subtotal = BigDecimal("20.00"),
                        subtotalTax = BigDecimal("2.00"),
                        totalTax = BigDecimal("2.00"),
                        total = BigDecimal("20.00"),
                        variationId = 0,
                        attributesList = emptyList(),
                        taxes = listOf(
                            Order.LineTaxEntry(rateId = 1L, taxAmount = BigDecimal("2.00"))
                        )
                    ),
                    Order.Item(
                        itemId = 2L,
                        productId = 20L,
                        name = "Product 2",
                        price = BigDecimal("15.50"),
                        sku = "PROD-2",
                        quantity = 1f,
                        subtotal = BigDecimal("15.50"),
                        subtotalTax = BigDecimal("1.55"),
                        totalTax = BigDecimal("1.55"),
                        total = BigDecimal("15.50"),
                        variationId = 0,
                        attributesList = emptyList(),
                        taxes = listOf(
                            Order.LineTaxEntry(rateId = 1L, taxAmount = BigDecimal("1.55"))
                        )
                    )
                )
            )

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

            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(orderWithMultipleItems))
            whenever(retrieveOrderRefunds.invoke(orderWithMultipleItems)).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(orderWithMultipleItems, emptyList())).thenReturn(refundableItems)

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
            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
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
            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(testOrder)).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(testOrder, emptyList())).thenReturn(refundableItems)

            viewModel = createViewModel()
            advanceUntilIdle()

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
            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(testOrder)).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(testOrder, emptyList())).thenReturn(refundableItems)

            viewModel = createViewModel()
            advanceUntilIdle()

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
            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
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
        whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(
            Result.failure(Exception("Network error"))
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        val errorState = viewModel.state.value
        assertThat(errorState).isInstanceOf(WooPosRefundState.Error::class.java)

        // WHEN
        viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToReviewClicked)

        // THEN
        assertThat(viewModel.state.value).isEqualTo(errorState)
    }

    @Test
    fun `given content state at ReviewRefund step, when ContinueToConfirmRefundClicked event, then step changes to ConfirmRefund`() =
        runTest {
            // GIVEN
            val refundableItems = listOf(testRefundableItem)
            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(testOrder)).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(testOrder, emptyList())).thenReturn(refundableItems)

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToReviewClicked)
            val reviewState = viewModel.state.value as WooPosRefundState.Content
            assertThat(reviewState.step).isEqualTo(WooPosRefundState.Content.RefundStep.ReviewRefund)

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToConfirmRefundClicked)

            // THEN
            val updatedState = viewModel.state.value as WooPosRefundState.Content
            assertThat(updatedState.step).isEqualTo(WooPosRefundState.Content.RefundStep.ConfirmRefund)
        }

    @Test
    fun `given content state at ConfirmRefund step, when BackToReviewClicked event, then step changes to ReviewRefund`() =
        runTest {
            // GIVEN
            val refundableItems = listOf(testRefundableItem)
            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(testOrder)).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(testOrder, emptyList())).thenReturn(refundableItems)

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToReviewClicked)
            viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToConfirmRefundClicked)
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
            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(testOrder)).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(testOrder, emptyList())).thenReturn(refundableItems)

            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToReviewClicked)
            viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToConfirmRefundClicked)
            val confirmState = viewModel.state.value as WooPosRefundState.Content
            assertThat(confirmState.step).isEqualTo(WooPosRefundState.Content.RefundStep.ConfirmRefund)

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.DialogDismissed)

            // THEN
            val updatedState = viewModel.state.value as WooPosRefundState.Content
            assertThat(updatedState.step).isEqualTo(WooPosRefundState.Content.RefundStep.SelectItems)
        }

    @Test
    fun `given valid refund request, when refund confirmed, then state transitions through Processing to RefundSuccess`() =
        runTest {
            // GIVEN
            val refundableItems = listOf(testRefundableItem)
            val groupedItems = listOf(
                RefundRequestItem(
                    itemId = 1L,
                    quantity = 1,
                    refundTotal = BigDecimal("20.00"),
                    refundTax = emptyList()
                )
            )

            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(testOrder)).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(testOrder, emptyList())).thenReturn(refundableItems)
            whenever(groupRefundItems.invoke(refundableItems, testOrder)).thenReturn(groupedItems)
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
            ).thenReturn(WooResult(testRefundModel))

            viewModel = createViewModel()
            advanceUntilIdle()

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.OnRefundConfirmed)
            advanceUntilIdle()

            // THEN
            val finalState = viewModel.state.value
            assertThat(finalState).isInstanceOf(WooPosRefundState.RefundSuccess::class.java)

            val successState = finalState as WooPosRefundState.RefundSuccess
            assertThat(successState.orderId).isEqualTo(testOrderId)
            assertThat(successState.orderNumber).isEqualTo("#456")
        }

    @Test
    fun `given valid refund request, when refund confirmed, then refund store called with correct parameters`() =
        runTest {
            // GIVEN
            val refundableItems = listOf(testRefundableItem)
            val groupedItems = listOf(
                RefundRequestItem(
                    itemId = 1L,
                    quantity = 1,
                    refundTotal = BigDecimal("20.00"),
                    refundTax = emptyList()
                )
            )

            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(testOrder)).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(testOrder, emptyList())).thenReturn(refundableItems)
            whenever(groupRefundItems.invoke(refundableItems, testOrder)).thenReturn(groupedItems)
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
            ).thenReturn(WooResult(testRefundModel))

            viewModel = createViewModel()
            advanceUntilIdle()

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.OnRefundConfirmed)
            advanceUntilIdle()

            // THEN
            verify(refundStore).createItemsRefund(
                site = testSite,
                orderId = testOrderId,
                amount = BigDecimal("22.00"), // subtotal (20.00) + taxes (2.00)
                reason = "",
                restockItems = true,
                autoRefund = false,
                items = groupedItems
            )
        }

    @Test
    fun `given refund store returns error, when refund confirmed, then state transitions to RefundError`() =
        runTest {
            // GIVEN
            val refundableItems = listOf(testRefundableItem)
            val groupedItems = listOf(
                RefundRequestItem(
                    itemId = 1L,
                    quantity = 1,
                    refundTotal = BigDecimal("20.00"),
                    refundTax = emptyList()
                )
            )
            val errorMessage = "Payment gateway error"

            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(testOrder)).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(testOrder, emptyList())).thenReturn(refundableItems)
            whenever(groupRefundItems.invoke(refundableItems, testOrder)).thenReturn(groupedItems)
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
                        original = BaseRequest.GenericErrorType.UNKNOWN,
                        message = errorMessage
                    )
                )
            )

            viewModel = createViewModel()
            advanceUntilIdle()

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.OnRefundConfirmed)
            advanceUntilIdle()

            // THEN
            val finalState = viewModel.state.value
            assertThat(finalState).isInstanceOf(WooPosRefundState.RefundError::class.java)
            assertThat((finalState as WooPosRefundState.RefundError).message).isEqualTo(errorMessage)
        }

    @Test
    fun `given refund store returns error without message, when refund confirmed, then uses generic error message`() =
        runTest {
            // GIVEN
            val refundableItems = listOf(testRefundableItem)
            val groupedItems = listOf(
                RefundRequestItem(
                    itemId = 1L,
                    quantity = 1,
                    refundTotal = BigDecimal("20.00"),
                    refundTax = emptyList()
                )
            )

            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(testOrder)).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(testOrder, emptyList())).thenReturn(refundableItems)
            whenever(groupRefundItems.invoke(refundableItems, testOrder)).thenReturn(groupedItems)
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
                        original = BaseRequest.GenericErrorType.UNKNOWN,
                        message = null
                    )
                )
            )

            viewModel = createViewModel()
            advanceUntilIdle()

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.OnRefundConfirmed)
            advanceUntilIdle()

            // THEN
            val finalState = viewModel.state.value
            assertThat(finalState).isInstanceOf(WooPosRefundState.RefundError::class.java)
            assertThat((finalState as WooPosRefundState.RefundError).message).isEqualTo("An error occurred")
        }

    @Test
    fun `given already processing refund, when refund confirmed again, then duplicate request is ignored`() =
        runTest {
            // GIVEN
            val refundableItems = listOf(testRefundableItem)
            val groupedItems = listOf(
                RefundRequestItem(
                    itemId = 1L,
                    quantity = 1,
                    refundTotal = BigDecimal("20.00"),
                    refundTax = emptyList()
                )
            )

            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(testOrder)).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(testOrder, emptyList())).thenReturn(refundableItems)
            whenever(groupRefundItems.invoke(refundableItems, testOrder)).thenReturn(groupedItems)
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
            ).thenReturn(WooResult(testRefundModel))

            viewModel = createViewModel()
            advanceUntilIdle()

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.OnRefundConfirmed)
            viewModel.onUIEvent(WooPosRefundUIEvent.OnRefundConfirmed) // duplicate request
            advanceUntilIdle()

            // THEN - verify only called once despite two events
            verify(refundStore).createItemsRefund(
                site = any(),
                orderId = any(),
                amount = any(),
                reason = any(),
                restockItems = any(),
                autoRefund = any(),
                items = any()
            )
        }

    @Test
    fun `given content state not at Processing step, when dismiss requested, then dismissal is allowed`() =
        runTest {
            // GIVEN
            val refundableItems = listOf(testRefundableItem)

            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(testOrder)).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(testOrder, emptyList())).thenReturn(refundableItems)

            viewModel = createViewModel()
            advanceUntilIdle()

            val initialState = viewModel.state.value as WooPosRefundState.Content
            assertThat(initialState.step).isEqualTo(WooPosRefundState.Content.RefundStep.SelectItems)

            // WHEN
            val canDismiss = viewModel.onDismissRequest()

            // THEN
            assertThat(canDismiss).isTrue()
        }
}
