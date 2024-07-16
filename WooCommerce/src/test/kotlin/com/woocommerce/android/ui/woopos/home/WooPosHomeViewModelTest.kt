package com.woocommerce.android.ui.woopos.home

import com.woocommerce.android.ui.woopos.home.WooPosHomeUIEvent.SystemBackClicked
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertFalse

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class WooPosHomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule(testDispatcher)

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
            viewModel.onUIEvent(SystemBackClicked)

            // THEN
            verify(parentToChildrenEventSender).sendToChildren(ParentToChildrenEvent.BackFromCheckoutToCartClicked)
        }

    @Test
    fun `given state is Cart, when SystemBackClicked passed, then should propagate the event down`() =
        runTest {
            // GIVEN
            val viewModel = createViewModel()

            // WHEN
            val result = viewModel.onUIEvent(SystemBackClicked)

            // THEN
            assertFalse(result)
        }

    @Test
    fun `given state checkout is paid, when SystemBackClicked passed, then OrderSuccessfullyPaid event should be sent`() = runTest {
        // GIVEN
        whenever(childrenToParentEventReceiver.events).thenReturn(
            flowOf(ChildToParentEvent.OrderSuccessfullyPaid)
        )
        val viewModel = createViewModel()

        // WHEN
        viewModel.onUIEvent(SystemBackClicked)

        // THEN
        verify(parentToChildrenEventSender).sendToChildren(ParentToChildrenEvent.OrderSuccessfullyPaid)
    }

    private fun createViewModel() = WooPosHomeViewModel(
        childrenToParentEventReceiver,
        parentToChildrenEventSender
    )
}
