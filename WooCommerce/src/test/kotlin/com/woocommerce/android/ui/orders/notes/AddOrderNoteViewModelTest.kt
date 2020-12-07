package com.woocommerce.android.ui.orders.notes

import androidx.lifecycle.SavedStateHandle
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.clearInvocations
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.util.CoroutineTestRule
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class AddOrderNoteViewModelTest : BaseUnitTest() {
    companion object {
        private const val REMOTE_ORDER_ID = "1-1-1"
        private const val REMOTE_ORDER_NUMBER = "100"
    }

    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private val repository: OrderDetailRepository = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val networkStatus: NetworkStatus = mock()

    private val testOrder: Order
        get() {
            return OrderTestUtils.generateTestOrder(REMOTE_ORDER_ID).copy(number = REMOTE_ORDER_NUMBER)
        }

    private lateinit var viewModel: AddOrderNoteViewModel

    private val savedState: SavedStateWithArgs = spy(
        SavedStateWithArgs(
            SavedStateHandle(),
            null,
            AddOrderNoteFragmentArgs(orderId = REMOTE_ORDER_ID, orderNumber = "100")
        )
    )

    fun initViewModel() {
        viewModel = AddOrderNoteViewModel(
            savedState = savedState,
            dispathers = coroutinesTestRule.testDispatchers,
            orderDetailRepository = repository,
            resourceProvider = resourceProvider,
            networkStatus = networkStatus
        )

        clearInvocations(repository, resourceProvider, networkStatus)
    }

    @Test
    fun `hide customer note checkbox if no email`() {
        val testOrder = testOrder.let {
            val address = it.billingAddress.copy(email = "")
            it.copy(billingAddress = address)
        }
        doReturn(testOrder).whenever(repository).getOrder(REMOTE_ORDER_ID)

        initViewModel()
        var state: AddOrderNoteViewModel.ViewState? = null
        viewModel.addOrderNoteViewStateData.observeForever { _, new ->
            state = new
        }

        assertFalse(state!!.showCustomerNoteSwitch)
    }

    @Test
    fun `show customer note checkbox if no email`() {
        val testOrder = testOrder.let {
            val address = it.billingAddress.copy(email = "test@emai.com")
            it.copy(billingAddress = address)
        }
        doReturn(testOrder).whenever(repository).getOrder(REMOTE_ORDER_ID)

        initViewModel()
        var state: AddOrderNoteViewModel.ViewState? = null
        viewModel.addOrderNoteViewStateData.observeForever { _, new ->
            state = new
        }

        assertTrue(state!!.showCustomerNoteSwitch)
    }

    @Test
    fun `Has the right title`() {
        doReturn("title").whenever(resourceProvider)
            .getString(R.string.orderdetail_orderstatus_ordernum, REMOTE_ORDER_NUMBER)

        initViewModel()
        val title = viewModel.screenTitle

        assertEquals("title", title)
        verify(resourceProvider, times(1)).getString(R.string.orderdetail_orderstatus_ordernum, REMOTE_ORDER_NUMBER)
    }

    @Test
    fun `hide the add button if text is empty`() {
        initViewModel()
        viewModel.onOrderTextEntered("")

        var state: AddOrderNoteViewModel.ViewState? = null
        viewModel.addOrderNoteViewStateData.observeForever { _, new ->
            state = new
        }

        assertFalse(state!!.canAddNote)
    }

    @Test
    fun `show the add button if text is not empty`() {
        initViewModel()
        viewModel.onOrderTextEntered("note")

        var state: AddOrderNoteViewModel.ViewState? = null
        viewModel.addOrderNoteViewStateData.observeForever { _, new ->
            state = new
        }

        assertTrue(state!!.canAddNote)
    }

    @Test
    fun `add note successfully`() {
        val note = "note"
        val isCustomerNote = false

        coroutinesTestRule.testDispatcher.runBlockingTest {
            doReturn(true).whenever(networkStatus).isConnected()
            doReturn(testOrder).whenever(repository).getOrder(REMOTE_ORDER_ID)
            doReturn(true).whenever(repository).addOrderNote(eq(REMOTE_ORDER_ID), eq(testOrder.remoteId), any())

            initViewModel()

            val events = mutableListOf<Event>()
            viewModel.event.observeForever { new -> events.add(new) }

            viewModel.onOrderTextEntered(note)
            viewModel.onIsCustomerCheckboxChanged(isCustomerNote)
            viewModel.pushOrderNote()

            verify(repository, times(1)).addOrderNote(eq(REMOTE_ORDER_ID), eq(testOrder.remoteId), argThat {
                this.note == note
                this.isCustomerNote == isCustomerNote
            })
            assertThat(events[0]).isInstanceOf(ShowSnackbar::class.java)
            assertEquals(R.string.add_order_note_added, (events[0] as ShowSnackbar).message)
            assertThat(events[1]).isInstanceOf(ExitWithResult::class.java)
            assertEquals(note, (events[1] as ExitWithResult<OrderNote>).data.note)
            assertEquals(isCustomerNote, (events[1] as ExitWithResult<OrderNote>).data.isCustomerNote)
        }
    }

    @Test
    fun `doesn't add note if not connected`() {
        coroutinesTestRule.testDispatcher.runBlockingTest {
            doReturn(false).whenever(networkStatus).isConnected()

            initViewModel()

            var event: Event? = null
            viewModel.event.observeForever { new -> event = new }

            viewModel.onOrderTextEntered("note")
            viewModel.pushOrderNote()

            verify(repository, times(0)).addOrderNote(any(), any(), any())
            assertThat(event).isInstanceOf(ShowSnackbar::class.java)
            assertEquals(R.string.offline_error, (event as ShowSnackbar).message)
        }
    }

    @Test
    fun `add note failure`() {
        val note = "note"
        val isCustomerNote = false

        coroutinesTestRule.testDispatcher.runBlockingTest {
            doReturn(true).whenever(networkStatus).isConnected()
            doReturn(testOrder).whenever(repository).getOrder(REMOTE_ORDER_ID)
            doReturn(false).whenever(repository).addOrderNote(eq(REMOTE_ORDER_ID), eq(testOrder.remoteId), any())

            initViewModel()

            var event: Event? = null
            viewModel.event.observeForever { new -> event = new }

            viewModel.onOrderTextEntered(note)
            viewModel.onIsCustomerCheckboxChanged(isCustomerNote)
            viewModel.pushOrderNote()

            verify(repository, times(1)).addOrderNote(eq(REMOTE_ORDER_ID), eq(testOrder.remoteId), argThat {
                this.note == note
                this.isCustomerNote == isCustomerNote
            })
            assertThat(event).isInstanceOf(ShowSnackbar::class.java)
            assertEquals(R.string.add_order_note_error, (event as ShowSnackbar).message)
        }
    }
}
