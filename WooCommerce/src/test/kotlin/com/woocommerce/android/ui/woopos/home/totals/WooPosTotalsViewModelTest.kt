package com.woocommerce.android.ui.woopos.home.totals

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderPaymentResult
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Date
import kotlin.test.Test

@ExperimentalCoroutinesApi
class WooPosTotalsViewModelTest {

    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private fun createMockSavedStateHandle(): SavedStateHandle {
        return SavedStateHandle(
            mapOf(
                "orderId" to EMPTY_ORDER_ID,
                "totalsViewState" to WooPosTotalsViewState.Loading
            )
        )
    }

    private val cardReaderFacade: WooPosCardReaderFacade = mock()
    private val analyticsTracker: WooPosAnalyticsTracker = mock()

    private companion object {
        private const val EMPTY_ORDER_ID = -1L
    }

    @Test
    fun `initial state is loading`() = runTest {
        // GIVEN
        val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock {
            on { events }.thenReturn(MutableStateFlow(ParentToChildrenEvent.BackFromCheckoutToCartClicked))
        }
        val savedState = createMockSavedStateHandle()

        // WHEN
        val viewModel = createViewModel(
            savedState = savedState,
            parentToChildrenEventReceiver = parentToChildrenEventReceiver
        )

        // THEN
        assertThat(viewModel.state.value).isEqualTo(WooPosTotalsViewState.Loading)
    }

    @Test
    fun `given checkoutstarted, when vm created, then order creation is started`() = runTest {
        // GIVEN
        val productIds = listOf(1L, 2L, 3L)
        val parentToChildrenEventFlow = MutableStateFlow(ParentToChildrenEvent.CheckoutClicked(productIds))
        val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock {
            on { events }.thenReturn(parentToChildrenEventFlow)
        }

        val order = Order.getEmptyOrder(
            dateCreated = Date(),
            dateModified = Date()
        ).copy(
            id = 123L,
            totalTax = BigDecimal("2.00"),
            items = listOf(
                Order.Item.EMPTY.copy(
                    subtotal = BigDecimal("1.00"),
                ),
                Order.Item.EMPTY.copy(
                    subtotal = BigDecimal("1.00"),
                ),
                Order.Item.EMPTY.copy(
                    subtotal = BigDecimal("1.00"),
                )
            )
        )

        val totalsRepository: WooPosTotalsRepository = mock {
            onBlocking { createOrderWithProducts(productIds) }.thenReturn(Result.success(order))
        }

        val priceFormat: WooPosFormatPrice = mock {
            onBlocking { invoke(BigDecimal("1.00")) }.thenReturn("$1.00")
            onBlocking { invoke(BigDecimal("2.00")) }.thenReturn("$2.00")
            onBlocking { invoke(BigDecimal("3.00")) }.thenReturn("$3.00")
            onBlocking { invoke(BigDecimal("5.00")) }.thenReturn("$5.00")
        }

        // WHEN
        val viewModel = createViewModel(
            parentToChildrenEventReceiver = parentToChildrenEventReceiver,
            totalsRepository = totalsRepository,
            priceFormat = priceFormat,
        )

        // THEN
        assertThat(viewModel.state.value).isEqualTo(
            WooPosTotalsViewState.Totals(
                orderSubtotalText = "$3.00",
                orderTaxText = "$2.00",
                orderTotalText = "$5.00"
            )
        )
        verify(totalsRepository).createOrderWithProducts(productIds)
    }

    @Test
    fun `given checkoutstarted and successfully created order, when vm created, then totals state correctly calculated`() =
        runTest {
            // GIVEN
            val productIds = listOf(1L, 2L, 3L)
            val parentToChildrenEventFlow = MutableStateFlow(ParentToChildrenEvent.CheckoutClicked(productIds))
            val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock {
                on { events }.thenReturn(parentToChildrenEventFlow)
            }
            val order = Order.getEmptyOrder(
                dateCreated = Date(),
                dateModified = Date()
            ).copy(
                totalTax = BigDecimal("2.00"),
                items = listOf(
                    Order.Item.EMPTY.copy(
                        subtotal = BigDecimal("1.00"),
                    ),
                    Order.Item.EMPTY.copy(
                        subtotal = BigDecimal("1.00"),
                    ),
                    Order.Item.EMPTY.copy(
                        subtotal = BigDecimal("1.00"),
                    )
                )
            )
            val totalsRepository: WooPosTotalsRepository = mock {
                onBlocking { createOrderWithProducts(productIds = productIds) }.thenReturn(
                    Result.success(order)
                )
            }
            val priceFormat: WooPosFormatPrice = mock {
                onBlocking { invoke(BigDecimal("2.00")) }.thenReturn("2.00$")
                onBlocking { invoke(BigDecimal("3.00")) }.thenReturn("3.00$")
                onBlocking { invoke(BigDecimal("5.00")) }.thenReturn("5.00$")
            }

            // WHEN
            val viewModel = createViewModel(
                parentToChildrenEventReceiver = parentToChildrenEventReceiver,
                totalsRepository = totalsRepository,
                priceFormat = priceFormat,
            )

            // THEN
            val totals = viewModel.state.value as WooPosTotalsViewState.Totals
            assertThat(totals.orderTotalText).isEqualTo("5.00$")
            assertThat(totals.orderTaxText).isEqualTo("2.00$")
            assertThat(totals.orderSubtotalText).isEqualTo("3.00$")
        }

    @Test
    fun `given OnNewTransactionClicked, should send NewTransactionClicked event and reset state to initial`() =
        runTest {
            // GIVEN
            val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock {
                on { events }.thenReturn(mock())
            }
            val savedState = createMockSavedStateHandle()
            val childrenToParentEventSender: WooPosChildrenToParentEventSender = mock()
            val viewModel = createViewModel(
                savedState = savedState,
                parentToChildrenEventReceiver = parentToChildrenEventReceiver,
                childrenToParentEventSender = childrenToParentEventSender,
            )

            // WHEN
            viewModel.onUIEvent(WooPosTotalsUIEvent.OnNewTransactionClicked)

            // THEN
            assertThat(viewModel.state.value).isEqualTo(WooPosTotalsViewState.Loading)
            verify(childrenToParentEventSender).sendToParent(ChildToParentEvent.NewTransactionClicked)
        }

    @Test
    fun `given order creation fails, when vm created, then error state is shown`() = runTest {
        // GIVEN
        val productIds = listOf(1L, 2L, 3L)
        val parentToChildrenEventFlow = MutableStateFlow(ParentToChildrenEvent.CheckoutClicked(productIds))
        val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock {
            on { events }.thenReturn(parentToChildrenEventFlow)
        }
        val errorMessage = "Order creation failed"
        val totalsRepository: WooPosTotalsRepository = mock {
            onBlocking { createOrderWithProducts(productIds = productIds) }.thenReturn(
                Result.failure(Exception(errorMessage))
            )
        }

        val resourceProvider: ResourceProvider = mock {
            on { getString(any()) }.thenReturn(errorMessage)
        }

        // WHEN
        val viewModel = createViewModel(
            resourceProvider = resourceProvider,
            parentToChildrenEventReceiver = parentToChildrenEventReceiver,
            totalsRepository = totalsRepository,
        )

        // THEN
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosTotalsViewState.Error::class.java)
        state as WooPosTotalsViewState.Error
        assertThat(state.message).isEqualTo(errorMessage)
    }

    @Test
    fun `when RetryClicked event is triggered, should retry creating order and show loading state`() = runTest {
        // GIVEN
        val productIds = listOf(1L, 2L, 3L)
        val parentToChildrenEventFlow = MutableStateFlow(ParentToChildrenEvent.CheckoutClicked(productIds))
        val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock {
            on { events }.thenReturn(parentToChildrenEventFlow)
        }
        val errorMessage = "Order creation failed"
        val totalsRepository: WooPosTotalsRepository = mock {
            onBlocking { createOrderWithProducts(productIds) }.thenReturn(
                Result.failure(Exception(errorMessage))
            )
        }

        val resourceProvider: ResourceProvider = mock {
            on { getString(any()) }.thenReturn(errorMessage)
        }

        val savedState = createMockSavedStateHandle()
        val priceFormat: WooPosFormatPrice = mock {
            onBlocking { invoke(BigDecimal("1.00")) }.thenReturn("$1.00")
            onBlocking { invoke(BigDecimal("2.00")) }.thenReturn("$2.00")
            onBlocking { invoke(BigDecimal("3.00")) }.thenReturn("$3.00")
            onBlocking { invoke(BigDecimal("5.00")) }.thenReturn("$5.00")
        }

        val viewModel = createViewModel(
            resourceProvider = resourceProvider,
            savedState = savedState,
            parentToChildrenEventReceiver = parentToChildrenEventReceiver,
            totalsRepository = totalsRepository,
            priceFormat = priceFormat,
        )

        // WHEN
        viewModel.onUIEvent(WooPosTotalsUIEvent.RetryOrderCreationClicked)

        // Ensure that the view model state transitions to error state
        assertThat(viewModel.state.value).isInstanceOf(WooPosTotalsViewState.Error::class.java)

        // Mock repository to simulate success on retry
        val order = Order.getEmptyOrder(
            dateCreated = Date(),
            dateModified = Date()
        ).copy(
            totalTax = BigDecimal("2.00"),
            items = listOf(
                Order.Item.EMPTY.copy(subtotal = BigDecimal("1.00")),
                Order.Item.EMPTY.copy(subtotal = BigDecimal("1.00")),
                Order.Item.EMPTY.copy(subtotal = BigDecimal("1.00"))
            )
        )

        whenever(totalsRepository.createOrderWithProducts(productIds)).thenReturn(
            Result.success(order)
        )

        // Trigger RetryOrderCreationClicked again to simulate a successful retry
        viewModel.onUIEvent(WooPosTotalsUIEvent.RetryOrderCreationClicked)

        // Ensure the view model state transitions to the success state with correct totals
        val state = viewModel.state.value as WooPosTotalsViewState.Totals
        assertThat(state.orderTotalText).isEqualTo("$5.00")
        assertThat(state.orderTaxText).isEqualTo("$2.00")
        assertThat(state.orderSubtotalText).isEqualTo("$3.00")
    }

    @Test
    fun `given vm created, when collect payment clicked multiple times within delay, then debounce prevents it`() =
        runTest {
            // GIVEN
            val productIds = listOf(1L, 2L, 3L)
            val parentToChildrenEventFlow = MutableStateFlow(ParentToChildrenEvent.CheckoutClicked(productIds))
            val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock {
                on { events }.thenReturn(parentToChildrenEventFlow)
            }
            val order = Order.getEmptyOrder(
                dateCreated = Date(),
                dateModified = Date()
            ).copy(
                totalTax = BigDecimal("2.00"),
                items = listOf(
                    Order.Item.EMPTY.copy(
                        subtotal = BigDecimal("1.00"),
                    ),
                    Order.Item.EMPTY.copy(
                        subtotal = BigDecimal("1.00"),
                    ),
                    Order.Item.EMPTY.copy(
                        subtotal = BigDecimal("1.00"),
                    )
                )
            )
            val totalsRepository: WooPosTotalsRepository = mock {
                onBlocking { createOrderWithProducts(productIds = productIds) }.thenReturn(
                    Result.success(order)
                )
            }
            val priceFormat: WooPosFormatPrice = mock {
                onBlocking { invoke(BigDecimal("2.00")) }.thenReturn("2.00$")
                onBlocking { invoke(BigDecimal("3.00")) }.thenReturn("3.00$")
                onBlocking { invoke(BigDecimal("5.00")) }.thenReturn("5.00$")
            }
            whenever(cardReaderFacade.collectPayment(any())).thenReturn(
                WooPosCardReaderPaymentResult.Success
            )

            // WHEN
            val viewModel = createViewModel(
                parentToChildrenEventReceiver = parentToChildrenEventReceiver,
                totalsRepository = totalsRepository,
                priceFormat = priceFormat,
            )
            viewModel.onUIEvent(WooPosTotalsUIEvent.CollectPaymentClicked)
            viewModel.onUIEvent(WooPosTotalsUIEvent.CollectPaymentClicked)
            viewModel.onUIEvent(WooPosTotalsUIEvent.CollectPaymentClicked)
            viewModel.onUIEvent(WooPosTotalsUIEvent.CollectPaymentClicked)
            viewModel.onUIEvent(WooPosTotalsUIEvent.CollectPaymentClicked)

            // THEN
            // Advance time by less than the debounce delay
            advanceTimeBy(800 / 2)
            verify(cardReaderFacade, times(1)).collectPayment(any())
            advanceUntilIdle()
            verify(cardReaderFacade, times(1)).collectPayment(any())
        }

    @Test
    fun `given vm created, when collect payment clicked multiple times after delay, then click event is handled for all of it`() =
        runTest {
            // GIVEN
            val productIds = listOf(1L, 2L, 3L)
            val parentToChildrenEventFlow = MutableStateFlow(ParentToChildrenEvent.CheckoutClicked(productIds))
            val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock {
                on { events }.thenReturn(parentToChildrenEventFlow)
            }
            val order = Order.getEmptyOrder(
                dateCreated = Date(),
                dateModified = Date()
            ).copy(
                totalTax = BigDecimal("2.00"),
                items = listOf(
                    Order.Item.EMPTY.copy(
                        subtotal = BigDecimal("1.00"),
                    ),
                    Order.Item.EMPTY.copy(
                        subtotal = BigDecimal("1.00"),
                    ),
                    Order.Item.EMPTY.copy(
                        subtotal = BigDecimal("1.00"),
                    )
                )
            )
            val totalsRepository: WooPosTotalsRepository = mock {
                onBlocking { createOrderWithProducts(productIds = productIds) }.thenReturn(
                    Result.success(order)
                )
            }
            val priceFormat: WooPosFormatPrice = mock {
                onBlocking { invoke(BigDecimal("2.00")) }.thenReturn("2.00$")
                onBlocking { invoke(BigDecimal("3.00")) }.thenReturn("3.00$")
                onBlocking { invoke(BigDecimal("5.00")) }.thenReturn("5.00$")
            }
            whenever(cardReaderFacade.collectPayment(any())).thenReturn(
                WooPosCardReaderPaymentResult.Failure
            )

            // WHEN
            val viewModel = createViewModel(
                parentToChildrenEventReceiver = parentToChildrenEventReceiver,
                totalsRepository = totalsRepository,
                priceFormat = priceFormat,
            )
            viewModel.onUIEvent(WooPosTotalsUIEvent.CollectPaymentClicked)
            advanceUntilIdle()
            verify(cardReaderFacade, times(1)).collectPayment(any())
            viewModel.onUIEvent(WooPosTotalsUIEvent.CollectPaymentClicked)
            advanceUntilIdle()
            viewModel.onUIEvent(WooPosTotalsUIEvent.CollectPaymentClicked)
            advanceUntilIdle()
            viewModel.onUIEvent(WooPosTotalsUIEvent.CollectPaymentClicked)
            advanceUntilIdle()
            viewModel.onUIEvent(WooPosTotalsUIEvent.CollectPaymentClicked)
            advanceUntilIdle()
            verify(cardReaderFacade, times(5)).collectPayment(any())
        }

    @Test
    fun `when order is created, then should track order creation success`() {
        val productIds = listOf(1L, 2L, 3L)
        val parentToChildrenEventFlow = MutableStateFlow(ParentToChildrenEvent.CheckoutClicked(productIds))
        val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock {
            on { events }.thenReturn(parentToChildrenEventFlow)
        }
        val order = Order.getEmptyOrder(
            dateCreated = Date(),
            dateModified = Date()
        ).copy(
            totalTax = BigDecimal("2.00"),
            items = listOf(
                Order.Item.EMPTY.copy(
                    subtotal = BigDecimal("1.00"),
                ),
                Order.Item.EMPTY.copy(
                    subtotal = BigDecimal("1.00"),
                ),
                Order.Item.EMPTY.copy(
                    subtotal = BigDecimal("1.00"),
                )
            )
        )
        val totalsRepository: WooPosTotalsRepository = mock {
            onBlocking { createOrderWithProducts(productIds = productIds) }.thenReturn(Result.success(order))
        }

        createViewModel(
            parentToChildrenEventReceiver = parentToChildrenEventReceiver,
            totalsRepository = totalsRepository,
        )
    }

    @Test
    fun `when fails to create order, then should track order creation failure`() = runTest {
        val productIds = listOf(1L, 2L, 3L)
        val parentToChildrenEventFlow = MutableStateFlow(ParentToChildrenEvent.CheckoutClicked(productIds))
        val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock {
            on { events }.thenReturn(parentToChildrenEventFlow)
        }
        val errorMessage = "Order creation failed"
        val totalsRepository: WooPosTotalsRepository = mock {
            onBlocking { createOrderWithProducts(productIds = productIds) }.thenReturn(
                Result.failure(Exception(errorMessage))
            )
        }

        val resourceProvider: ResourceProvider = mock {
            on { getString(any()) }.thenReturn(errorMessage)
        }

        createViewModel(
            resourceProvider = resourceProvider,
            parentToChildrenEventReceiver = parentToChildrenEventReceiver,
            totalsRepository = totalsRepository,
        )

        verify(
            analyticsTracker
        ).track(
            WooPosAnalyticsEvent.Error.OrderCreationError(
                WooPosTotalsViewModel::class,
                Exception::class.java.simpleName,
                errorMessage
            )
        )
    }

    private fun createViewModel(
        resourceProvider: ResourceProvider = mock(),
        parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock(),
        childrenToParentEventSender: WooPosChildrenToParentEventSender = mock(),
        totalsRepository: WooPosTotalsRepository = mock(),
        priceFormat: WooPosFormatPrice = mock(),
        savedState: SavedStateHandle = SavedStateHandle(),
    ) = WooPosTotalsViewModel(
        resourceProvider,
        parentToChildrenEventReceiver,
        childrenToParentEventSender,
        cardReaderFacade,
        totalsRepository,
        priceFormat,
        analyticsTracker,
        savedState,
    )
}
