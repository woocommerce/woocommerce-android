package com.woocommerce.android.ui.woopos.home

import com.woocommerce.android.ui.woopos.home.WooPosHomeState.Cart
import com.woocommerce.android.ui.woopos.home.WooPosHomeUIEvent.ExitConfirmationDialogDismissed
import com.woocommerce.android.ui.woopos.home.WooPosHomeUIEvent.SystemBackClicked
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosHomeViewModelTest : BaseUnitTest() {
    private val childrenToParentEventReceiver: WooPosChildrenToParentEventReceiver = mock()
    private val parentToChildrenEventSender: WooPosParentToChildrenEventSender = mock()

    @Test
    fun `given state checkout, when SystemBackClicked passed, then BackFromCheckoutToCartClicked event should be sent`() =
        runTest {
            // GIVEN
            whenever(childrenToParentEventReceiver.events).thenReturn(
                flowOf(ChildToParentEvent.CheckoutClicked)
            )
            val viewModel = createViewModel()

            // WHEN
            viewModel.onUIEvent(SystemBackClicked)

            // THEN
            verify(parentToChildrenEventSender).sendToChildren(ParentToChildrenEvent.BackFromCheckoutToCartClicked)
        }

    @Test
    fun `given state is Cart, when SystemBackClicked passed, then exitConfirmationDialog with WooPosExitConfirmationDialog`() =
        runTest {
            // GIVEN
            val viewModel = createViewModel()

            // WHEN
            viewModel.onUIEvent(SystemBackClicked)

            // THEN
            val state = viewModel.state.value as Cart
            assertThat(state.exitConfirmationDialog).isEqualTo(WooPosExitConfirmationDialog)
        }

    @Test
    fun `given state is Cart, when ExitConfirmationDialogDismissed passed, then exitConfirmationDialog should be null`() =
        runTest {
            // GIVEN
            val viewModel = createViewModel()

            // WHEN
            viewModel.onUIEvent(ExitConfirmationDialogDismissed)

            // THEN
            val state = viewModel.state.value as Cart
            assertThat(state.exitConfirmationDialog).isNull()
        }

    private fun createViewModel() = WooPosHomeViewModel(
        childrenToParentEventReceiver,
        parentToChildrenEventSender
    )
}
