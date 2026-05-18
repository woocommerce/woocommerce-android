package com.woocommerce.android.ui.woopos.markorderascomplete

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.BackToCheckoutFromMarkAsPaid
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.MarkAsPaidConfirmed
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.MarkAsPaidFailed
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.MarkAsPaidNotePostFailed
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.MarkAsPaidSuccess
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosMarkOrderAsCompleteViewModelTest {

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    @Rule
    @JvmField
    val instantTaskRule = InstantTaskExecutorRule()

    private val repository: WooPosMarkOrderAsCompleteRepository = mock()
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val tracker: WooPosAnalyticsTracker = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val priceFormat: WooPosFormatPrice = mock()

    private val orderId = 123L

    @Before
    fun setUp() = runTest {
        val testOrder = Order.getEmptyOrder(Date(), Date()).copy(id = orderId, total = BigDecimal("42.00"))
        whenever(repository.getOrderById(orderId)).thenReturn(testOrder)
        whenever(priceFormat(BigDecimal("42.00"))).thenReturn("$42.00")
        whenever(resourceProvider.getString(R.string.woopos_mark_order_as_complete_total, "$42.00"))
            .thenReturn("Order total: $42.00")
        whenever(resourceProvider.getString(R.string.woopos_mark_order_as_complete_confirm_button))
            .thenReturn("Mark order as complete")
        whenever(resourceProvider.getString(R.string.woopos_mark_order_as_complete_error_message))
            .thenReturn("Something went wrong. Please try again.")
    }

    private fun createViewModel() = WooPosMarkOrderAsCompleteViewModel(
        repository = repository,
        childrenToParentEventSender = childrenToParentEventSender,
        analyticsTracker = tracker,
        resourceProvider = resourceProvider,
        priceFormat = priceFormat,
        savedState = SavedStateHandle(mapOf(MARK_ORDER_AS_COMPLETE_ROUTE_ORDER_ID_KEY to orderId)),
    )

    @Test
    fun `given order loads, when VM initializes, then state is Confirming with order total`() = runTest {
        // WHEN
        val viewModel = createViewModel()

        // THEN
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosMarkOrderAsCompleteState.Confirming::class.java)
        val confirming = state as WooPosMarkOrderAsCompleteState.Confirming
        assertThat(confirming.totalText).isEqualTo("Order total: $42.00")
        assertThat(confirming.note).isEmpty()
        assertThat(confirming.errorMessage).isNull()
        assertThat(confirming.button.status).isEqualTo(WooPosMarkOrderAsCompleteState.Confirming.Button.Status.ENABLED)
    }

    @Test
    fun `when note changed, then state note updated and error cleared`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosMarkOrderAsCompleteUIEvent.NoteChanged("Bank transfer"))

        // THEN
        val updated = viewModel.state.value as WooPosMarkOrderAsCompleteState.Confirming
        assertThat(updated.note).isEqualTo("Bank transfer")
        assertThat(updated.errorMessage).isNull()
    }

    @Test
    fun `given repo succeeds, when confirm clicked, then analytics tracked, parent event sent, GoBack emitted`() =
        runTest {
            // GIVEN
            whenever(repository.markOrderAsComplete(eq(orderId), anyOrNull()))
                .thenReturn(Result.success(MarkOrderAsCompleteOutcome.SUCCESS))
            val viewModel = createViewModel()

            // WHEN / THEN
            viewModel.navigationEvent.test {
                viewModel.onUIEvent(WooPosMarkOrderAsCompleteUIEvent.ConfirmClicked)
                assertThat(awaitItem()).isEqualTo(WooPosNavigationEvent.GoBack)
            }
            verify(tracker).track(MarkAsPaidConfirmed)
            verify(tracker).track(MarkAsPaidSuccess)
            verify(childrenToParentEventSender).sendToParent(
                eq(ChildToParentEvent.OrderSuccessfullyPaidExternally),
            )
        }

    @Test
    fun `given repo succeeds with note, when confirm clicked, then note forwarded to repository`() = runTest {
        // GIVEN
        whenever(repository.markOrderAsComplete(eq(orderId), eq("Bank transfer")))
            .thenReturn(Result.success(MarkOrderAsCompleteOutcome.SUCCESS))
        val viewModel = createViewModel()
        viewModel.onUIEvent(WooPosMarkOrderAsCompleteUIEvent.NoteChanged("Bank transfer"))

        // WHEN / THEN
        viewModel.navigationEvent.test {
            viewModel.onUIEvent(WooPosMarkOrderAsCompleteUIEvent.ConfirmClicked)
            assertThat(awaitItem()).isEqualTo(WooPosNavigationEvent.GoBack)
        }
        verify(repository).markOrderAsComplete(orderId, "Bank transfer")
    }

    @Test
    fun `given repo fails, when confirm clicked, then state has error message and button re-enabled`() = runTest {
        // GIVEN
        whenever(repository.markOrderAsComplete(eq(orderId), anyOrNull())).thenReturn(Result.failure(Exception("boom")))
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosMarkOrderAsCompleteUIEvent.ConfirmClicked)

        // THEN
        val finalState = viewModel.state.value as WooPosMarkOrderAsCompleteState.Confirming
        assertThat(finalState.errorMessage).isEqualTo("Something went wrong. Please try again.")
        assertThat(finalState.button.status).isEqualTo(WooPosMarkOrderAsCompleteState.Confirming.Button.Status.ENABLED)
        verify(tracker).track(MarkAsPaidFailed)
    }

    @Test
    fun `when back clicked, then BackToCheckoutFromMarkAsPaid tracked and GoBack emitted`() = runTest {
        // GIVEN
        val viewModel = createViewModel()

        // WHEN / THEN
        viewModel.navigationEvent.test {
            viewModel.onBackClicked()
            assertThat(awaitItem()).isEqualTo(WooPosNavigationEvent.GoBack)
        }
        verify(tracker).track(BackToCheckoutFromMarkAsPaid)
    }

    @Test
    fun `given repo succeeds with failed note, when confirm clicked, then MarkAsPaidNotePostFailed tracked`() = runTest {
        // GIVEN
        whenever(repository.markOrderAsComplete(eq(orderId), anyOrNull()))
            .thenReturn(Result.success(MarkOrderAsCompleteOutcome.SUCCESS_WITH_FAILED_NOTE))
        val viewModel = createViewModel()

        // WHEN / THEN
        viewModel.navigationEvent.test {
            viewModel.onUIEvent(WooPosMarkOrderAsCompleteUIEvent.ConfirmClicked)
            assertThat(awaitItem()).isEqualTo(WooPosNavigationEvent.GoBack)
        }
        verify(tracker).track(MarkAsPaidNotePostFailed)
        verify(tracker).track(MarkAsPaidSuccess)
    }

    @Test
    fun `given repo succeeds without failed note, when confirm clicked, then MarkAsPaidNotePostFailed not tracked`() =
        runTest {
            // GIVEN
            whenever(repository.markOrderAsComplete(eq(orderId), anyOrNull()))
                .thenReturn(Result.success(MarkOrderAsCompleteOutcome.SUCCESS))
            val viewModel = createViewModel()

            // WHEN / THEN
            viewModel.navigationEvent.test {
                viewModel.onUIEvent(WooPosMarkOrderAsCompleteUIEvent.ConfirmClicked)
                assertThat(awaitItem()).isEqualTo(WooPosNavigationEvent.GoBack)
            }
            verify(tracker, never()).track(MarkAsPaidNotePostFailed)
        }

    @Test
    fun `given saved state has LOADING button, when VM restored, then button is reset to ENABLED`() = runTest {
        // GIVEN: saved state simulates process death mid-confirm
        val savedState = SavedStateHandle(
            mapOf(
                MARK_ORDER_AS_COMPLETE_ROUTE_ORDER_ID_KEY to orderId,
                "woo_pos_mark_order_as_complete_state" to WooPosMarkOrderAsCompleteState.Confirming(
                    totalText = "Order total: $42.00",
                    note = "Bank transfer",
                    errorMessage = null,
                    button = WooPosMarkOrderAsCompleteState.Confirming.Button(
                        text = "Mark order as complete",
                        status = WooPosMarkOrderAsCompleteState.Confirming.Button.Status.LOADING,
                    ),
                ),
            )
        )

        // WHEN
        val viewModel = WooPosMarkOrderAsCompleteViewModel(
            repository = repository,
            childrenToParentEventSender = childrenToParentEventSender,
            analyticsTracker = tracker,
            resourceProvider = resourceProvider,
            priceFormat = priceFormat,
            savedState = savedState,
        )

        // THEN
        val state = viewModel.state.value as WooPosMarkOrderAsCompleteState.Confirming
        assertThat(state.button.status).isEqualTo(WooPosMarkOrderAsCompleteState.Confirming.Button.Status.ENABLED)
        assertThat(state.note).isEqualTo("Bank transfer")
    }
}
