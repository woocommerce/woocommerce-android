package com.woocommerce.android.ui.woopos.home.totals

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.util.CoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.math.BigDecimal
import java.util.Date
import kotlin.test.Test

@ExperimentalCoroutinesApi
@Suppress("UnnecessaryAbstractClass")
@RunWith(MockitoJUnitRunner::class)
class WooPosTotalsViewModelTest {

    init {
        Class.forName("kotlinx.coroutines.test.TestScopeKt")
            .getDeclaredMethod("setCatchNonTestRelatedExceptions", Boolean::class.java)
            .invoke(null, false)
    }

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    @Rule
    @JvmField
    val coroutinesTestRule = CoroutineTestRule(testDispatcher)

    private fun createMockSavedStateHandle(): SavedStateHandle {
        return SavedStateHandle(
            mapOf(
                "orderId" to EMPTY_ORDER_ID,
                "totalsViewState" to WooPosTotalsState.Loading
            )
        )
    }

    private companion object {
        private const val EMPTY_ORDER_ID = -1L
    }

    @Test
    fun `initial state is loading`() = runTest {
        // WHEN
        val savedState = createMockSavedStateHandle()
        val viewModel = createViewModel(savedState = savedState)

        // THEN
        assertThat(viewModel.state.value).isEqualTo(WooPosTotalsState.Loading)
    }

    @Test
    fun `given checkoutstarted, when vm created, then loading state is shown and order creation is started`() =
        runTest {
            // GIVEN
            val productIds = listOf(1L, 2L, 3L)
            val parentToChildrenEventFlow = MutableStateFlow(ParentToChildrenEvent.CheckoutClicked(productIds))
            val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock {
                on { events }.thenReturn(parentToChildrenEventFlow)
            }
            val totalsRepository: WooPosTotalsRepository = mock()

            // WHEN
            val viewModel = createViewModel(
                parentToChildrenEventReceiver = parentToChildrenEventReceiver,
                totalsRepository = totalsRepository,
            )

            // THEN
            assertThat(viewModel.state.value).isEqualTo(WooPosTotalsState.Loading)
            verify(totalsRepository).createOrderWithProducts(productIds = productIds)
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
            val totals = viewModel.state.value as WooPosTotalsState.Totals
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
            assertThat(viewModel.state.value).isEqualTo(WooPosTotalsState.Loading)
            verify(childrenToParentEventSender).sendToParent(ChildToParentEvent.NewTransactionClicked)
        }

    private fun createViewModel(
        parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock(),
        childrenToParentEventSender: WooPosChildrenToParentEventSender = mock(),
        cardReaderFacade: WooPosCardReaderFacade = mock(),
        totalsRepository: WooPosTotalsRepository = mock(),
        priceFormat: WooPosFormatPrice = mock(),
        savedState: SavedStateHandle = SavedStateHandle(),
    ) = WooPosTotalsViewModel(
        parentToChildrenEventReceiver,
        childrenToParentEventSender,
        cardReaderFacade,
        totalsRepository,
        priceFormat,
        savedState
    )
}
