package com.woocommerce.android.ui.woopos.home

import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test

@ExperimentalCoroutinesApi
class WooPosHomeViewModelTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val childrenToParentEventReceiver: WooPosChildrenToParentEventReceiver = mock()
    private val parentToChildrenEventSender: WooPosParentToChildrenEventSender = mock()

    @Test
    fun `given state checkout, when SystemBackClicked passed, then BackFromCheckoutToCartClicked event should be sent`() =
        runTest {
            // GIVEN
            whenever(childrenToParentEventReceiver.events).thenReturn(
                flowOf(ChildToParentEvent.CheckoutClicked(emptyList()))
            )
            val viewModel = createViewModel()

            // WHEN
            viewModel.onUIEvent(WooPosHomeUIEvent.SystemBackClicked)

            // THEN
            verify(parentToChildrenEventSender).sendToChildren(ParentToChildrenEvent.BackFromCheckoutToCartClicked)
            assertThat(viewModel.state.value.screenPositionState)
                .isEqualTo(WooPosHomeState.ScreenPositionState.Cart.NotEmpty)
        }

    @Test
    fun `given state is Cart, when SystemBackClicked passed, then should show exit confirmation dialog`() = runTest {
        // GIVEN
        val eventsFlow = MutableSharedFlow<ChildToParentEvent>()
        whenever(childrenToParentEventReceiver.events).thenReturn(eventsFlow)
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosHomeUIEvent.SystemBackClicked)

        // THEN
        assertThat(viewModel.state.value.exitConfirmationDialog).isEqualTo(WooPosExitConfirmationDialog)
    }

    @Test
    fun `given state checkout is paid, when SystemBackClicked passed, then OrderSuccessfullyPaid event should be sent`() = runTest {
        // GIVEN
        whenever(childrenToParentEventReceiver.events).thenReturn(
            flowOf(ChildToParentEvent.OrderSuccessfullyPaid)
        )
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosHomeUIEvent.SystemBackClicked)

        // THEN
        verify(parentToChildrenEventSender).sendToChildren(ParentToChildrenEvent.OrderSuccessfullyPaid)
        assertThat(viewModel.state.value.screenPositionState)
            .isEqualTo(WooPosHomeState.ScreenPositionState.Cart.Empty)
    }

    @Test
    fun `given state is Checkout NotPaid, when ExitConfirmationDialogDismissed passed, then exit confirmation dialog should be dismissed`() = runTest {
        // GIVEN
        whenever(childrenToParentEventReceiver.events).thenReturn(
            flowOf(ChildToParentEvent.CheckoutClicked(emptyList()))
        )
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(WooPosHomeUIEvent.ExitConfirmationDialogDismissed)

        // THEN
        assertThat(viewModel.state.value.exitConfirmationDialog).isNull()
    }

    @Test
    fun `given state is Cart Empty, when ExitPosClicked passed, then exit confirmation dialog should be shown`() = runTest {
        // GIVEN
        val eventsFlow = MutableSharedFlow<ChildToParentEvent>()
        whenever(childrenToParentEventReceiver.events).thenReturn(eventsFlow)
        val viewModel = createViewModel()

        // WHEN
        eventsFlow.emit(ChildToParentEvent.ExitPosClicked)

        // THEN
        assertThat(viewModel.state.value.exitConfirmationDialog)
            .isEqualTo(WooPosExitConfirmationDialog)
    }

    @Test
    fun `given state is Cart NotEmpty, when ExitPosClicked passed, then exit confirmation dialog should be shown`() = runTest {
        // GIVEN
        val eventsFlow = MutableSharedFlow<ChildToParentEvent>()
        whenever(childrenToParentEventReceiver.events).thenReturn(eventsFlow)
        val viewModel = createViewModel()

        // WHEN
        eventsFlow.emit(ChildToParentEvent.CartStatusChanged.NotEmpty)
        eventsFlow.emit(ChildToParentEvent.ExitPosClicked)

        // THEN
        assertThat(viewModel.state.value.exitConfirmationDialog)
            .isEqualTo(WooPosExitConfirmationDialog)
    }

    private fun createViewModel() = WooPosHomeViewModel(
        childrenToParentEventReceiver,
        parentToChildrenEventSender
    )
}
