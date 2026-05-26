package com.woocommerce.android.ui.woopos.orders.details.refund

import com.woocommerce.android.R
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Refund
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.common.data.WooPosRetrieveOrderRefunds
import com.woocommerce.android.ui.woopos.orders.WooPosGetPaymentMethod
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersDataSource
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.refunds.RefundRequestItem
import org.wordpress.android.fluxc.model.settings.CurrencyPosition
import org.wordpress.android.fluxc.model.settings.Settings
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore
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
    private val selectedSite: SelectedSite = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val analyticsTracker: WooPosAnalyticsTracker = mock()
    private val loadPaymentMethod: WooPosGetPaymentMethod = mock()
    private val refundSubmissionProcessor: WooPosRefundSubmissionProcessor = mock()
    private val cardReaderFacade: WooPosCardReaderFacade = mock()
    private val readerStatus = MutableStateFlow<CardReaderStatus>(CardReaderStatus.NotConnected())

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

    @Before
    fun setUp() = runTest {
        val testSettings = Settings(
            currencyCode = "USD",
            currencyPosition = CurrencyPosition.LEFT,
            currencyThousandSeparator = ",",
            currencyDecimalSeparator = ".",
            currencyDecimalNumber = 2,
            countryCode = "US",
            stateCode = "CA",
            address = "",
            address2 = "",
            city = "",
            postalCode = "",
            couponsEnabled = true
        )

        whenever(resourceProvider.getString(R.string.error_generic)).thenReturn("An error occurred")
        whenever(selectedSite.get()).thenReturn(testSite)
        whenever(wooCommerceStore.getSiteSettings(testSite)).thenReturn(testSettings)
        whenever(currencyFormatter.formatCurrency(any<BigDecimal>(), any<String>(), any<Boolean>())).thenReturn("$0.00")
        whenever(wooCommerceStore.fetchSiteGeneralSettings(testSite)).thenReturn(WooResult(testSettings))
        whenever(wooCommerceStore.fetchSiteSettingsTaxRoundAtSubtotal(testSite)).thenReturn(WooResult(false))
        whenever(cardReaderFacade.readerStatus).thenReturn(readerStatus)

        whenever(loadPaymentMethod.invoke(any())).thenReturn(Result.success("Manual refund"))
        whenever(refundSubmissionProcessor.submit(any())).thenReturn(
            flowOf(
                WooPosRefundSubmissionState.Processing,
                WooPosRefundSubmissionState.Success
            )
        )
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
            selectedSite = selectedSite,
            wooCommerceStore = wooCommerceStore,
            getPaymentMethod = loadPaymentMethod,
            refundSubmissionProcessor = refundSubmissionProcessor,
            analyticsTracker = analyticsTracker,
            cardReaderFacade = cardReaderFacade
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
    private fun createOrderItem(
        itemId: Long,
        productId: Long = 10L,
        name: String = "Product $itemId",
        price: BigDecimal,
        quantity: Float = 1f,
        tax: BigDecimal
    ) = Order.Item(
        itemId = itemId,
        productId = productId,
        name = name,
        price = price,
        sku = "SKU-$itemId",
        quantity = quantity,
        subtotal = price * quantity.toBigDecimal(),
        subtotalTax = tax * quantity.toBigDecimal(),
        totalTax = tax * quantity.toBigDecimal(),
        total = price * quantity.toBigDecimal(),
        variationId = 0,
        attributesList = emptyList(),
        taxes = listOf(Order.LineTaxEntry(rateId = 1L, taxAmount = tax * quantity.toBigDecimal()))
    )

    @Test
    fun `given viewmodel created, when order fetch fails, then state transitions from Loading to Error`() = runTest {
        // GIVEN
        whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.failure(Exception()))

        // WHEN
        viewModel = createViewModel()
        viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
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
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)

            // WHEN
            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
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
            assertThat(contentState.subtotal).isEqualByComparingTo(BigDecimal("40.00"))
            assertThat(contentState.taxes).isEqualByComparingTo(BigDecimal("4.00"))
            assertThat(contentState.total).isEqualByComparingTo(BigDecimal("44.00"))
        }

    @Test
    fun `given order fetch fails, when init, then state is Error`() = runTest {
        // GIVEN
        whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(
            Result.failure(Exception("Network error"))
        )

        // WHEN
        viewModel = createViewModel()
        viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
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
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(emptyList())

            // WHEN
            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            // THEN
            assertThat(viewModel.state.value).isEqualTo(WooPosRefundState.NoRefundableItems)
        }

    @Test
    fun `given order fetch succeeds and refunds fetch fails, when init, then doesn't allow refund`() = runTest {
        // GIVEN
        val refundableItems = listOf(testRefundableItem)
        whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
        whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(
            Result.failure(Exception("Refunds fetch failed"))
        )
        whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)

        // WHEN
        viewModel = createViewModel()
        viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosRefundState.Error::class.java)
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
        whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(refunds))
        whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)

        // WHEN
        viewModel = createViewModel()
        viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
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
            whenever(
                retrieveOrderRefunds.invoke(eq(orderWithMultipleItems), any())
            ).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)

            // WHEN
            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            // THEN
            val state = viewModel.state.value as WooPosRefundState.Content
            assertThat(
                state.subtotal
            ).isEqualByComparingTo(BigDecimal("35.50")) // 10 + 10 + 15.50
            assertThat(state.taxes).isEqualByComparingTo(BigDecimal("3.55")) // 1 + 1 + 1.55
            assertThat(state.total).isEqualByComparingTo(BigDecimal("39.05")) // 35.50 + 3.55
        }

    @Test
    fun `given content state at SelectItems step, when ContinueToReviewClicked event, then step changes to ReviewRefund`() =
        runTest {
            // GIVEN
            val refundableItems = listOf(testRefundableItem)
            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)

            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
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
    fun `given no items selected, when ContinueToReviewClicked event, then step remains at SelectItems`() =
        runTest {
            // GIVEN
            val refundableItems = listOf(testRefundableItem)
            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)

            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosRefundUIEvent.SelectAllToggled)

            val stateBeforeContinue = viewModel.state.value as WooPosRefundState.Content
            assertThat(stateBeforeContinue.selectedItemIds).isEmpty()
            assertThat(stateBeforeContinue.step).isEqualTo(WooPosRefundState.Content.RefundStep.SelectItems)

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToReviewClicked)

            // THEN
            val stateAfterContinue = viewModel.state.value as WooPosRefundState.Content
            assertThat(stateAfterContinue.step).isEqualTo(WooPosRefundState.Content.RefundStep.SelectItems)
            assertThat(stateAfterContinue.selectedItemIds).isEmpty()
        }

    @Test
    fun `given content state, when OnRefundReasonChanged event, then refundReason is updated`() =
        runTest {
            // GIVEN
            val refundableItems = listOf(testRefundableItem)
            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)

            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToReviewClicked)
            val initialState = viewModel.state.value as WooPosRefundState.Content
            assertThat(initialState.refundReason).isEmpty()

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.OnRefundReasonChanged("Customer bought wrong item"))

            // THEN
            val updatedState = viewModel.state.value as WooPosRefundState.Content
            assertThat(updatedState.refundReason).isEqualTo("Customer bought wrong item")
        }

    @Test
    fun `given content state with refund reason, when navigating between steps, then refundReason is preserved`() =
        runTest {
            // GIVEN
            val refundableItems = listOf(testRefundableItem)
            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)

            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToReviewClicked)
            viewModel.onUIEvent(WooPosRefundUIEvent.OnRefundReasonChanged("Customer changed mind"))

            val reviewState = viewModel.state.value as WooPosRefundState.Content
            assertThat(reviewState.refundReason).isEqualTo("Customer changed mind")

            // WHEN - Navigate back to SelectItems
            viewModel.onUIEvent(WooPosRefundUIEvent.BackToSelectItemsClicked)

            // THEN - Reason is preserved
            val selectItemsState = viewModel.state.value as WooPosRefundState.Content
            assertThat(selectItemsState.refundReason).isEqualTo("Customer changed mind")
            assertThat(selectItemsState.step).isEqualTo(WooPosRefundState.Content.RefundStep.SelectItems)

            // WHEN - Navigate forward to ReviewRefund again
            viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToReviewClicked)

            // THEN - Reason is still preserved
            val reviewStateAgain = viewModel.state.value as WooPosRefundState.Content
            assertThat(reviewStateAgain.refundReason).isEqualTo("Customer changed mind")
            assertThat(reviewStateAgain.step).isEqualTo(WooPosRefundState.Content.RefundStep.ReviewRefund)
        }

    @Test
    fun `given content state at ReviewRefund step, when BackToSelectItemsClicked event, then step changes to SelectItems`() =
        runTest {
            // GIVEN
            val refundableItems = listOf(testRefundableItem)
            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)

            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
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
    fun `given content state at ReviewRefund step, when RefundFlowDismissed event, then state resets to Loading`() =
        runTest {
            // GIVEN
            val refundableItems = listOf(testRefundableItem)
            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)

            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToReviewClicked)
            val reviewState = viewModel.state.value as WooPosRefundState.Content
            assertThat(reviewState.step).isEqualTo(WooPosRefundState.Content.RefundStep.ReviewRefund)

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowDismissed)
            advanceUntilIdle()

            // THEN
            assertThat(viewModel.state.value).isInstanceOf(WooPosRefundState.Loading::class.java)
        }

    @Test
    fun `given content state at SelectItems step, when RefundFlowDismissed event, then state resets to Loading`() =
        runTest {
            // GIVEN
            val refundableItems = listOf(testRefundableItem)
            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)

            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            val initialState = viewModel.state.value as WooPosRefundState.Content
            assertThat(initialState.step).isEqualTo(WooPosRefundState.Content.RefundStep.SelectItems)

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowDismissed)

            // THEN
            assertThat(viewModel.state.value).isInstanceOf(WooPosRefundState.Loading::class.java)
        }

    @Test
    fun `given non-content state, when onUIEvent called, then state remains unchanged`() = runTest {
        // GIVEN
        whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(
            Result.failure(Exception("Network error"))
        )

        viewModel = createViewModel()
        viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
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
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)

            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
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
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)

            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
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
    fun `given content state at ConfirmRefund step, when RefundFlowDismissed event, then state resets to Loading`() =
        runTest {
            // GIVEN
            val refundableItems = listOf(testRefundableItem)
            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)

            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToReviewClicked)
            viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToConfirmRefundClicked)
            val confirmState = viewModel.state.value as WooPosRefundState.Content
            assertThat(confirmState.step).isEqualTo(WooPosRefundState.Content.RefundStep.ConfirmRefund)

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowDismissed)
            advanceUntilIdle()

            // THEN
            assertThat(viewModel.state.value).isInstanceOf(WooPosRefundState.Loading::class.java)
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
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)
            whenever(groupRefundItems.invoke(eq(refundableItems), eq(testOrder), any())).thenReturn(groupedItems)
            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
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
    fun `given interac refund requires reader connection, when reader connects, then refund resumes`() =
        runTest {
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
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)
            whenever(groupRefundItems.invoke(eq(refundableItems), eq(testOrder), any())).thenReturn(groupedItems)
            whenever(refundSubmissionProcessor.submit(any())).thenReturn(
                flowOf(WooPosRefundSubmissionState.ReaderConnectionRequired),
                flowOf(
                    WooPosRefundSubmissionState.PreparingReader,
                    WooPosRefundSubmissionState.Success
                )
            )

            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosRefundUIEvent.OnRefundConfirmed)
            advanceUntilIdle()

            val waitingState = viewModel.state.value as WooPosRefundState.Content
            assertThat(waitingState.step).isEqualTo(WooPosRefundState.Content.RefundStep.ReaderDisconnected)

            readerStatus.value = CardReaderStatus.Connected(mock())
            advanceUntilIdle()

            verify(refundSubmissionProcessor, times(2)).submit(any())
            assertThat(viewModel.state.value).isInstanceOf(WooPosRefundState.RefundSuccess::class.java)
        }

    @Test
    fun `given valid refund request without reason, when refund confirmed, then processor receives empty reason`() =
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
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)
            whenever(groupRefundItems.invoke(eq(refundableItems), eq(testOrder), any())).thenReturn(groupedItems)
            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.OnRefundConfirmed)
            advanceUntilIdle()

            // THEN
            verify(refundSubmissionProcessor).submit(
                argThat {
                    orderId == testOrderId &&
                        refundAmount.compareTo(BigDecimal("22.00")) == 0 &&
                        refundReason == "" &&
                        refundItems == groupedItems
                }
            )
        }

    @Test
    fun `given valid refund request with reason, when refund confirmed, then processor receives provided reason`() =
        runTest {
            // GIVEN
            val testReason = "Customer bought wrong item"
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
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)
            whenever(groupRefundItems.invoke(eq(refundableItems), eq(testOrder), any())).thenReturn(groupedItems)
            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.OnRefundReasonChanged(testReason))
            viewModel.onUIEvent(WooPosRefundUIEvent.OnRefundConfirmed)
            advanceUntilIdle()

            // THEN
            verify(refundSubmissionProcessor).submit(
                argThat {
                    orderId == testOrderId &&
                        refundAmount.compareTo(BigDecimal("22.00")) == 0 &&
                        refundReason == testReason &&
                        refundItems == groupedItems
                }
            )
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
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)
            whenever(groupRefundItems.invoke(eq(refundableItems), eq(testOrder), any())).thenReturn(groupedItems)
            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.OnRefundConfirmed)
            viewModel.onUIEvent(WooPosRefundUIEvent.OnRefundConfirmed) // duplicate request
            advanceUntilIdle()

            // THEN - verify only called once despite two events
            verify(refundSubmissionProcessor).submit(any())
        }

    @Test
    fun `given content state not at Processing step, when dismiss requested, then dismissal is allowed`() =
        runTest {
            // GIVEN
            val refundableItems = listOf(testRefundableItem)

            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)

            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            val initialState = viewModel.state.value as WooPosRefundState.Content
            assertThat(initialState.step).isEqualTo(WooPosRefundState.Content.RefundStep.SelectItems)

            // WHEN
            val canDismiss = viewModel.onDismissRequest()

            // THEN
            assertThat(canDismiss).isTrue()
        }

    @Test
    fun `given all items selected initially, when item deselected, then selectedItemIds updated and totals recalculated`() =
        runTest {
            // GIVEN
            val orderWithTwoItems = testOrder.copy(
                items = listOf(
                    createOrderItem(
                        itemId = 1L,
                        productId = 10L,
                        price = BigDecimal("10.00"),
                        tax = BigDecimal("1.00")
                    ),
                    createOrderItem(itemId = 2L, productId = 20L, price = BigDecimal("20.00"), tax = BigDecimal("2.00"))
                )
            )

            val item1 = createRefundableItem(
                orderItemId = 1L,
                productId = 10L,
                unitPrice = BigDecimal("10.00"),
                unitTax = BigDecimal("1.00"),
                rowIndex = 0
            )
            val item2 = createRefundableItem(
                orderItemId = 2L,
                productId = 20L,
                unitPrice = BigDecimal("20.00"),
                unitTax = BigDecimal("2.00"),
                rowIndex = 0
            )
            val refundableItems = listOf(item1, item2)

            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(orderWithTwoItems))
            whenever(retrieveOrderRefunds.invoke(eq(orderWithTwoItems), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)

            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            val initialState = viewModel.state.value as WooPosRefundState.Content
            assertThat(initialState.selectedItemIds).containsExactlyInAnyOrder(item1.uniqueId, item2.uniqueId)
            assertThat(initialState.subtotal).isEqualByComparingTo(BigDecimal("30.00"))
            assertThat(initialState.taxes).isEqualByComparingTo(BigDecimal("3.00"))
            assertThat(initialState.total).isEqualByComparingTo(BigDecimal("33.00"))

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.ItemSelectionToggled(item1.uniqueId))

            // THEN
            val updatedState = viewModel.state.value as WooPosRefundState.Content
            assertThat(updatedState.selectedItemIds).containsExactly(item2.uniqueId)
            assertThat(updatedState.subtotal).isEqualByComparingTo(BigDecimal("20.00"))
            assertThat(updatedState.taxes).isEqualByComparingTo(BigDecimal("2.00"))
            assertThat(updatedState.total).isEqualByComparingTo(BigDecimal("22.00"))
        }

    @Test
    fun `given item not selected, when item selected, then selectedItemIds updated and totals recalculated`() =
        runTest {
            // GIVEN
            val orderWithTwoItems = testOrder.copy(
                items = listOf(
                    createOrderItem(
                        itemId = 1L,
                        productId = 10L,
                        price = BigDecimal("10.00"),
                        tax = BigDecimal("1.00")
                    ),
                    createOrderItem(itemId = 2L, productId = 20L, price = BigDecimal("20.00"), tax = BigDecimal("2.00"))
                )
            )

            val item1 = createRefundableItem(
                orderItemId = 1L,
                productId = 10L,
                unitPrice = BigDecimal("10.00"),
                unitTax = BigDecimal("1.00"),
                rowIndex = 0
            )
            val item2 = createRefundableItem(
                orderItemId = 2L,
                productId = 20L,
                unitPrice = BigDecimal("20.00"),
                unitTax = BigDecimal("2.00"),
                rowIndex = 0
            )
            val refundableItems = listOf(item1, item2)

            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(orderWithTwoItems))
            whenever(retrieveOrderRefunds.invoke(eq(orderWithTwoItems), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)

            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosRefundUIEvent.ItemSelectionToggled(item1.uniqueId))

            val stateBeforeSelect = viewModel.state.value as WooPosRefundState.Content
            assertThat(stateBeforeSelect.selectedItemIds).containsExactly(item2.uniqueId)
            assertThat(stateBeforeSelect.subtotal).isEqualByComparingTo(BigDecimal("20.00"))

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.ItemSelectionToggled(item1.uniqueId))

            // THEN
            val updatedState = viewModel.state.value as WooPosRefundState.Content
            assertThat(updatedState.selectedItemIds).containsExactlyInAnyOrder(item1.uniqueId, item2.uniqueId)
            assertThat(updatedState.subtotal).isEqualByComparingTo(BigDecimal("30.00"))
            assertThat(updatedState.taxes).isEqualByComparingTo(BigDecimal("3.00"))
            assertThat(updatedState.total).isEqualByComparingTo(BigDecimal("33.00"))
        }

    @Test
    fun `given all items selected, when select all toggled, then all items deselected and totals become zero`() =
        runTest {
            // GIVEN
            val orderWithTwoItems = testOrder.copy(
                items = listOf(
                    createOrderItem(
                        itemId = 1L,
                        productId = 10L,
                        price = BigDecimal("10.00"),
                        tax = BigDecimal("1.00")
                    ),
                    createOrderItem(itemId = 2L, productId = 20L, price = BigDecimal("20.00"), tax = BigDecimal("2.00"))
                )
            )

            val item1 = createRefundableItem(
                orderItemId = 1L,
                productId = 10L,
                unitPrice = BigDecimal("10.00"),
                unitTax = BigDecimal("1.00"),
                rowIndex = 0
            )
            val item2 = createRefundableItem(
                orderItemId = 2L,
                productId = 20L,
                unitPrice = BigDecimal("20.00"),
                unitTax = BigDecimal("2.00"),
                rowIndex = 0
            )
            val refundableItems = listOf(item1, item2)

            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(orderWithTwoItems))
            whenever(retrieveOrderRefunds.invoke(eq(orderWithTwoItems), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)

            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            val initialState = viewModel.state.value as WooPosRefundState.Content
            assertThat(initialState.selectedItemIds).hasSize(2)

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.SelectAllToggled)

            // THEN
            val updatedState = viewModel.state.value as WooPosRefundState.Content
            assertThat(updatedState.selectedItemIds).isEmpty()
            assertThat(updatedState.itemsCount).isEqualTo(0)
            assertThat(updatedState.subtotal).isEqualByComparingTo(BigDecimal("0.00"))
            assertThat(updatedState.taxes).isEqualByComparingTo(BigDecimal("0.00"))
            assertThat(updatedState.total).isEqualByComparingTo(BigDecimal("0.00"))
        }

    @Test
    fun `given no items selected, when select all toggled, then all items selected and totals recalculated`() =
        runTest {
            // GIVEN
            val orderWithTwoItems = testOrder.copy(
                items = listOf(
                    createOrderItem(
                        itemId = 1L,
                        productId = 10L,
                        price = BigDecimal("10.00"),
                        tax = BigDecimal("1.00")
                    ),
                    createOrderItem(itemId = 2L, productId = 20L, price = BigDecimal("20.00"), tax = BigDecimal("2.00"))
                )
            )

            val item1 = createRefundableItem(
                orderItemId = 1L,
                productId = 10L,
                unitPrice = BigDecimal("10.00"),
                unitTax = BigDecimal("1.00"),
                rowIndex = 0
            )
            val item2 = createRefundableItem(
                orderItemId = 2L,
                productId = 20L,
                unitPrice = BigDecimal("20.00"),
                unitTax = BigDecimal("2.00"),
                rowIndex = 0
            )
            val refundableItems = listOf(item1, item2)

            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(orderWithTwoItems))
            whenever(retrieveOrderRefunds.invoke(eq(orderWithTwoItems), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)

            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosRefundUIEvent.SelectAllToggled)

            val stateBeforeToggle = viewModel.state.value as WooPosRefundState.Content
            assertThat(stateBeforeToggle.selectedItemIds).isEmpty()

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.SelectAllToggled)

            // THEN
            val updatedState = viewModel.state.value as WooPosRefundState.Content
            assertThat(updatedState.selectedItemIds).containsExactlyInAnyOrder(item1.uniqueId, item2.uniqueId)
            assertThat(updatedState.itemsCount).isEqualTo(2)
            assertThat(updatedState.subtotal).isEqualByComparingTo(BigDecimal("30.00"))
            assertThat(updatedState.taxes).isEqualByComparingTo(BigDecimal("3.00"))
            assertThat(updatedState.total).isEqualByComparingTo(BigDecimal("33.00"))
        }

    @Test
    fun `given some items selected, when select all toggled, then all items selected`() =
        runTest {
            // GIVEN
            val orderWithThreeItems = testOrder.copy(
                items = listOf(
                    createOrderItem(
                        itemId = 1L,
                        productId = 10L,
                        price = BigDecimal("10.00"),
                        tax = BigDecimal("1.00")
                    ),
                    createOrderItem(
                        itemId = 2L,
                        productId = 20L,
                        price = BigDecimal("20.00"),
                        tax = BigDecimal("2.00")
                    ),
                    createOrderItem(itemId = 3L, productId = 30L, price = BigDecimal("15.00"), tax = BigDecimal("1.50"))
                )
            )

            val item1 = createRefundableItem(
                orderItemId = 1L,
                productId = 10L,
                unitPrice = BigDecimal("10.00"),
                unitTax = BigDecimal("1.00"),
                rowIndex = 0
            )
            val item2 = createRefundableItem(
                orderItemId = 2L,
                productId = 20L,
                unitPrice = BigDecimal("20.00"),
                unitTax = BigDecimal("2.00"),
                rowIndex = 0
            )
            val item3 = createRefundableItem(
                orderItemId = 3L,
                productId = 30L,
                unitPrice = BigDecimal("15.00"),
                unitTax = BigDecimal("1.50"),
                rowIndex = 0
            )
            val refundableItems = listOf(item1, item2, item3)

            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(orderWithThreeItems))
            whenever(
                retrieveOrderRefunds.invoke(eq(orderWithThreeItems), any())
            ).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)

            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosRefundUIEvent.ItemSelectionToggled(item2.uniqueId))

            val stateBeforeToggle = viewModel.state.value as WooPosRefundState.Content
            assertThat(stateBeforeToggle.selectedItemIds).containsExactlyInAnyOrder(item1.uniqueId, item3.uniqueId)

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.SelectAllToggled)

            // THEN
            val updatedState = viewModel.state.value as WooPosRefundState.Content
            assertThat(updatedState.selectedItemIds).containsExactlyInAnyOrder(
                item1.uniqueId,
                item2.uniqueId,
                item3.uniqueId
            )
        }

    @Test
    fun `given at ReviewRefund step, when item selection toggled, then step is preserved`() =
        runTest {
            // GIVEN
            val orderWithTwoItems = testOrder.copy(
                items = listOf(
                    createOrderItem(
                        itemId = 1L,
                        productId = 10L,
                        price = BigDecimal("10.00"),
                        tax = BigDecimal("1.00")
                    ),
                    createOrderItem(itemId = 2L, productId = 20L, price = BigDecimal("20.00"), tax = BigDecimal("2.00"))
                )
            )

            val item1 = createRefundableItem(
                orderItemId = 1L,
                productId = 10L,
                unitPrice = BigDecimal("10.00"),
                unitTax = BigDecimal("1.00"),
                rowIndex = 0
            )
            val item2 = createRefundableItem(
                orderItemId = 2L,
                productId = 20L,
                unitPrice = BigDecimal("20.00"),
                unitTax = BigDecimal("2.00"),
                rowIndex = 0
            )
            val refundableItems = listOf(item1, item2)

            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(orderWithTwoItems))
            whenever(retrieveOrderRefunds.invoke(eq(orderWithTwoItems), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)

            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToReviewClicked)

            val stateBeforeToggle = viewModel.state.value as WooPosRefundState.Content
            assertThat(stateBeforeToggle.step).isEqualTo(WooPosRefundState.Content.RefundStep.ReviewRefund)

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.ItemSelectionToggled(item1.uniqueId))

            // THEN
            val updatedState = viewModel.state.value as WooPosRefundState.Content
            assertThat(updatedState.step).isEqualTo(WooPosRefundState.Content.RefundStep.ReviewRefund)
            assertThat(updatedState.selectedItemIds).containsExactly(item2.uniqueId)
        }

    @Test
    fun `given at ConfirmRefund step, when select all toggled, then step is preserved`() =
        runTest {
            // GIVEN
            val refundableItems = listOf(
                createRefundableItem(
                    orderItemId = 1L,
                    unitPrice = BigDecimal("10.00"),
                    unitTax = BigDecimal("1.00"),
                    rowIndex = 0
                )
            )

            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)

            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToReviewClicked)
            viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToConfirmRefundClicked)

            val stateBeforeToggle = viewModel.state.value as WooPosRefundState.Content
            assertThat(stateBeforeToggle.step).isEqualTo(WooPosRefundState.Content.RefundStep.ConfirmRefund)

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.SelectAllToggled)

            // THEN
            val updatedState = viewModel.state.value as WooPosRefundState.Content
            assertThat(updatedState.step).isEqualTo(WooPosRefundState.Content.RefundStep.ConfirmRefund)
        }

    @Test
    fun `given multiple items with same orderItemId but different rowIndex, when one deselected, then only that unit deselected`() =
        runTest {
            // GIVEN
            val orderWithThreeUnits = testOrder.copy(
                items = listOf(
                    createOrderItem(
                        itemId = 1L,
                        productId = 10L,
                        price = BigDecimal("10.00"),
                        quantity = 3f,
                        tax = BigDecimal("1.00")
                    )
                )
            )

            val item1Unit1 = createRefundableItem(
                orderItemId = 1L,
                productId = 10L,
                unitPrice = BigDecimal("10.00"),
                unitTax = BigDecimal("1.00"),
                rowIndex = 0
            )
            val item1Unit2 = createRefundableItem(
                orderItemId = 1L,
                productId = 10L,
                unitPrice = BigDecimal("10.00"),
                unitTax = BigDecimal("1.00"),
                rowIndex = 1
            )
            val item1Unit3 = createRefundableItem(
                orderItemId = 1L,
                productId = 10L,
                unitPrice = BigDecimal("10.00"),
                unitTax = BigDecimal("1.00"),
                rowIndex = 2
            )
            val refundableItems = listOf(item1Unit1, item1Unit2, item1Unit3)

            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(orderWithThreeUnits))
            whenever(
                retrieveOrderRefunds.invoke(eq(orderWithThreeUnits), any())
            ).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)

            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            val initialState = viewModel.state.value as WooPosRefundState.Content
            assertThat(initialState.selectedItemIds).hasSize(3)
            assertThat(initialState.subtotal).isEqualByComparingTo(BigDecimal("30.00"))

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.ItemSelectionToggled(item1Unit2.uniqueId))

            // THEN
            val updatedState = viewModel.state.value as WooPosRefundState.Content
            assertThat(updatedState.selectedItemIds).containsExactlyInAnyOrder(
                item1Unit1.uniqueId,
                item1Unit3.uniqueId
            )
            assertThat(updatedState.itemsCount).isEqualTo(2)
            assertThat(updatedState.subtotal).isEqualByComparingTo(BigDecimal("20.00"))
            assertThat(updatedState.taxes).isEqualByComparingTo(BigDecimal("2.00"))
            assertThat(updatedState.total).isEqualByComparingTo(BigDecimal("22.00"))
        }

    @Suppress("LongMethod")
    @Test
    fun `given partial selection of items, when refund confirmed, then only selected items are refunded`() =
        runTest {
            // GIVEN
            val orderWithThreeItems = testOrder.copy(
                items = listOf(
                    createOrderItem(
                        itemId = 1L,
                        productId = 10L,
                        price = BigDecimal("10.00"),
                        tax = BigDecimal("1.00")
                    ),
                    createOrderItem(
                        itemId = 2L,
                        productId = 20L,
                        price = BigDecimal("20.00"),
                        tax = BigDecimal("2.00")
                    ),
                    createOrderItem(itemId = 3L, productId = 30L, price = BigDecimal("15.00"), tax = BigDecimal("1.50"))
                )
            )

            val item1 = createRefundableItem(
                orderItemId = 1L,
                productId = 10L,
                unitPrice = BigDecimal("10.00"),
                unitTax = BigDecimal("1.00"),
                rowIndex = 0
            )
            val item2 = createRefundableItem(
                orderItemId = 2L,
                productId = 20L,
                unitPrice = BigDecimal("20.00"),
                unitTax = BigDecimal("2.00"),
                rowIndex = 0
            )
            val item3 = createRefundableItem(
                orderItemId = 3L,
                productId = 30L,
                unitPrice = BigDecimal("15.00"),
                unitTax = BigDecimal("1.50"),
                rowIndex = 0
            )
            val refundableItems = listOf(item1, item2, item3)
            val selectedItems = listOf(item1, item3)

            val groupedItems = listOf(
                RefundRequestItem(
                    itemId = 1L,
                    quantity = 1,
                    refundTotal = BigDecimal("10.00"),
                    refundTax = emptyList()
                ),
                RefundRequestItem(
                    itemId = 3L,
                    quantity = 1,
                    refundTotal = BigDecimal("15.00"),
                    refundTax = emptyList()
                )
            )

            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(orderWithThreeItems))
            whenever(
                retrieveOrderRefunds.invoke(eq(orderWithThreeItems), any())
            ).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)
            whenever(
                groupRefundItems.invoke(eq(selectedItems), eq(orderWithThreeItems), any())
            ).thenReturn(groupedItems)
            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosRefundUIEvent.ItemSelectionToggled(item2.uniqueId))

            val stateBeforeConfirm = viewModel.state.value as WooPosRefundState.Content
            assertThat(stateBeforeConfirm.selectedItemIds).containsExactlyInAnyOrder(item1.uniqueId, item3.uniqueId)

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.OnRefundConfirmed)
            advanceUntilIdle()

            // THEN
            verify(groupRefundItems).invoke(eq(selectedItems), eq(orderWithThreeItems), any())
            verify(refundSubmissionProcessor).submit(
                argThat {
                    orderId == testOrderId &&
                        refundAmount.compareTo(BigDecimal("27.50")) == 0 &&
                        refundReason == "" &&
                        refundItems == groupedItems
                }
            )
        }

    @Test
    fun `given content state created, when init completes, then refund flow started event tracked`() = runTest {
        val refundableItems = listOf(testRefundableItem)
        whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
        whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
        whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)

        viewModel = createViewModel()
        viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
        advanceUntilIdle()

        verify(analyticsTracker).track(WooPosAnalyticsEvent.Event.RefundFlowStarted)
    }

    @Test
    fun `given all items selected, when select all toggled to deselect, then refund select all tapped event tracked with deselected action`() =
        runTest {
            val refundableItems = listOf(testRefundableItem)
            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)

            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosRefundUIEvent.SelectAllToggled)
            advanceUntilIdle()

            verify(analyticsTracker).track(
                argThat { this is WooPosAnalyticsEvent.Event.RefundSelectAllTapped && action == "deselected" }
            )
        }

    @Test
    fun `given no items selected, when select all toggled to select, then refund select all tapped event tracked with selected action`() =
        runTest {
            val refundableItems = listOf(testRefundableItem)
            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)

            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosRefundUIEvent.SelectAllToggled)
            advanceUntilIdle()
            viewModel.onUIEvent(WooPosRefundUIEvent.SelectAllToggled)
            advanceUntilIdle()

            verify(analyticsTracker).track(
                argThat { this is WooPosAnalyticsEvent.Event.RefundSelectAllTapped && action == "selected" }
            )
        }

    @Test
    fun `given all items selected, when refund confirmed, then refund confirm tapped event tracked with full refund type and no reason`() =
        runTest {
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
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)
            whenever(groupRefundItems.invoke(eq(refundableItems), eq(testOrder), any())).thenReturn(groupedItems)
            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosRefundUIEvent.OnRefundConfirmed)
            advanceUntilIdle()

            verify(analyticsTracker).track(
                argThat {
                    this is WooPosAnalyticsEvent.Event.RefundConfirmTapped &&
                        refundType == "full" && !hasReason
                }
            )
        }

    @Test
    @Suppress("LongMethod")
    fun `given partial refund with reason, when confirmed, then tracks event`() =
        runTest {
            val orderWithTwoItems = testOrder.copy(
                items = listOf(
                    createOrderItem(
                        itemId = 1L,
                        productId = 10L,
                        price = BigDecimal("10.00"),
                        tax = BigDecimal("1.00")
                    ),
                    createOrderItem(itemId = 2L, productId = 20L, price = BigDecimal("20.00"), tax = BigDecimal("2.00"))
                )
            )

            val item1 = createRefundableItem(
                orderItemId = 1L,
                productId = 10L,
                unitPrice = BigDecimal("10.00"),
                unitTax = BigDecimal("1.00"),
                rowIndex = 0
            )
            val item2 = createRefundableItem(
                orderItemId = 2L,
                productId = 20L,
                unitPrice = BigDecimal("20.00"),
                unitTax = BigDecimal("2.00"),
                rowIndex = 0
            )
            val refundableItems = listOf(item1, item2)
            val selectedItems = listOf(item1)

            val groupedItems = listOf(
                RefundRequestItem(
                    itemId = 1L,
                    quantity = 1,
                    refundTotal = BigDecimal("10.00"),
                    refundTax = emptyList()
                )
            )

            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(orderWithTwoItems))
            whenever(retrieveOrderRefunds.invoke(eq(orderWithTwoItems), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)
            whenever(groupRefundItems.invoke(eq(selectedItems), eq(orderWithTwoItems), any())).thenReturn(groupedItems)
            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosRefundUIEvent.ItemSelectionToggled(item2.uniqueId))
            viewModel.onUIEvent(WooPosRefundUIEvent.OnRefundReasonChanged("Customer request"))
            viewModel.onUIEvent(WooPosRefundUIEvent.OnRefundConfirmed)
            advanceUntilIdle()

            verify(analyticsTracker).track(
                argThat {
                    this is WooPosAnalyticsEvent.Event.RefundConfirmTapped &&
                        refundType == "partial" && hasReason
                }
            )
        }

    @Test
    fun `given refund confirmed, when processing starts, then refund processing started event tracked`() =
        runTest {
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
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)
            whenever(groupRefundItems.invoke(eq(refundableItems), eq(testOrder), any())).thenReturn(groupedItems)
            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosRefundUIEvent.OnRefundConfirmed)
            advanceUntilIdle()

            verify(analyticsTracker).track(WooPosAnalyticsEvent.Event.RefundProcessingStarted)
        }

    @Test
    fun `given refund processing succeeds, when API call completes, then refund processing success event tracked`() =
        runTest {
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
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)
            whenever(groupRefundItems.invoke(eq(refundableItems), eq(testOrder), any())).thenReturn(groupedItems)
            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosRefundUIEvent.OnRefundConfirmed)
            advanceUntilIdle()

            verify(analyticsTracker).track(WooPosAnalyticsEvent.Event.RefundProcessingSuccess)
        }

    @Test
    fun `given refund processing fails, when API call completes, then refund processing failed event tracked`() =
        runTest {
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
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)
            whenever(groupRefundItems.invoke(eq(refundableItems), eq(testOrder), any())).thenReturn(groupedItems)
            whenever(refundSubmissionProcessor.submit(any())).thenReturn(
                flowOf(
                    WooPosRefundSubmissionState.Processing,
                    WooPosRefundSubmissionState.Failure("Refund failed")
                )
            )

            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosRefundUIEvent.OnRefundConfirmed)
            advanceUntilIdle()

            verify(analyticsTracker).track(WooPosAnalyticsEvent.Event.RefundProcessingFailed)
            val errorState = viewModel.state.value as WooPosRefundState.Error
            assertThat(errorState.canRetry).isTrue()
        }

    @Test
    fun `given backend notification fails after terminal refund succeeds, when API call completes, then error is not retryable`() =
        runTest {
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
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)
            whenever(groupRefundItems.invoke(eq(refundableItems), eq(testOrder), any())).thenReturn(groupedItems)
            whenever(refundSubmissionProcessor.submit(any())).thenReturn(
                flowOf(
                    WooPosRefundSubmissionState.ProcessingReaderRefund,
                    WooPosRefundSubmissionState.NotifyingStore,
                    WooPosRefundSubmissionState.Failure(
                        message = "Backend failed",
                        retryBackendNotificationOnly = true,
                    )
                )
            )

            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosRefundUIEvent.OnRefundConfirmed)
            advanceUntilIdle()

            val errorState = viewModel.state.value as WooPosRefundState.Error
            assertThat(errorState.message).isEqualTo("Backend failed")
            assertThat(errorState.canRetry).isFalse()
        }

    @Test
    fun `given at select items step, when dialog dismissed, then refund flow aborted event tracked with select items step`() =
        runTest {
            val refundableItems = listOf(testRefundableItem)
            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)

            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowDismissed)
            advanceUntilIdle()

            verify(analyticsTracker).track(
                argThat {
                    this is WooPosAnalyticsEvent.Event.RefundFlowAborted && refundStep == "select_items"
                }
            )
        }

    @Test
    fun `given at review refund step, when dialog dismissed, then refund flow aborted event tracked with review refund step`() =
        runTest {
            val refundableItems = listOf(testRefundableItem)
            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)

            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToReviewClicked)
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowDismissed)
            advanceUntilIdle()

            verify(analyticsTracker).track(
                argThat {
                    this is WooPosAnalyticsEvent.Event.RefundFlowAborted && refundStep == "review_refund"
                }
            )
        }

    @Test
    fun `given at confirm refund step, when dialog dismissed, then refund flow aborted event tracked with confirm refund step`() =
        runTest {
            val refundableItems = listOf(testRefundableItem)
            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(testOrder))
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)

            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToReviewClicked)
            viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToConfirmRefundClicked)
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowDismissed)
            advanceUntilIdle()

            verify(analyticsTracker).track(
                argThat {
                    this is WooPosAnalyticsEvent.Event.RefundFlowAborted && refundStep == "confirm_refund"
                }
            )
        }

    @Test
    fun `given order with billing email, when refund succeeds, then receiptSentMessage is set`() =
        runTest {
            // GIVEN
            val orderWithEmail = testOrder.copy(
                customer = Order.Customer(
                    billingAddress = Address.EMPTY.copy(email = "customer@example.com"),
                    shippingAddress = Address.EMPTY
                )
            )
            val refundableItems = listOf(testRefundableItem)
            val groupedItems = listOf(
                RefundRequestItem(
                    itemId = 1L,
                    quantity = 1,
                    refundTotal = BigDecimal("20.00"),
                    refundTax = emptyList()
                )
            )

            whenever(ordersDataSource.refreshOrderById(testOrderId)).thenReturn(Result.success(orderWithEmail))
            whenever(retrieveOrderRefunds.invoke(eq(orderWithEmail), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)
            whenever(groupRefundItems.invoke(eq(refundableItems), eq(orderWithEmail), any())).thenReturn(groupedItems)
            whenever(
                resourceProvider.getString(R.string.woopos_receipt_sent_to_customer, "customer@example.com")
            ).thenReturn("A receipt has been sent to customer@example.com.")

            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.OnRefundConfirmed)
            advanceUntilIdle()

            // THEN
            val state = viewModel.state.value as WooPosRefundState.RefundSuccess
            assertThat(state.receiptSentMessage).isEqualTo("A receipt has been sent to customer@example.com.")
        }

    @Test
    fun `given order without billing email, when refund succeeds, then receiptSentMessage is null`() =
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
            whenever(retrieveOrderRefunds.invoke(eq(testOrder), any())).thenReturn(Result.success(emptyList()))
            whenever(getRefundableItems.invoke(any(), any())).thenReturn(refundableItems)
            whenever(groupRefundItems.invoke(eq(refundableItems), eq(testOrder), any())).thenReturn(groupedItems)
            viewModel = createViewModel()
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
            advanceUntilIdle()

            // WHEN
            viewModel.onUIEvent(WooPosRefundUIEvent.OnRefundConfirmed)
            advanceUntilIdle()

            // THEN
            val state = viewModel.state.value as WooPosRefundState.RefundSuccess
            assertThat(state.receiptSentMessage).isNull()
        }
}
