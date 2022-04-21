package com.woocommerce.android.ui.orders.notes

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.WooException
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class AddOrderNoteViewModelTest : BaseUnitTest() {
    companion object {
        private const val ORDER_ID = 1L
        private const val REMOTE_ORDER_NUMBER = "100"
    }

    private val repository: OrderDetailRepository = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val networkStatus: NetworkStatus = mock()

    private val testOrder: Order
        get() {
            return OrderTestUtils.generateTestOrder(ORDER_ID).copy(number = REMOTE_ORDER_NUMBER)
        }

    private lateinit var viewModel: AddOrderNoteViewModel

    private val savedState: SavedStateHandle =
        AddOrderNoteFragmentArgs(orderId = ORDER_ID, orderNumber = "100").initSavedStateHandle()

    private fun initViewModel() {
        viewModel = AddOrderNoteViewModel(
            savedState = savedState,
            orderDetailRepository = repository,
            resourceProvider = resourceProvider,
            networkStatus = networkStatus
        )
    }

    @Test
    fun `hide customer note checkbox if no email`() = testBlocking {
        val testOrder = testOrder.let {
            val address = it.billingAddress.copy(email = "")
            it.copy(billingAddress = address)
        }
        doReturn(testOrder).whenever(repository).getOrderById(ORDER_ID)

        initViewModel()
        var state: AddOrderNoteViewModel.ViewState? = null
        viewModel.addOrderNoteViewStateData.observeForever { _, new ->
            state = new
        }

        assertFalse(state!!.showCustomerNoteSwitch)
    }

    @Test
    fun `show customer note checkbox if no email`() = testBlocking {
        val testOrder = testOrder.let {
            val address = it.billingAddress.copy(email = "test@emai.com")
            it.copy(billingAddress = address)
        }
        doReturn(testOrder).whenever(repository).getOrderById(ORDER_ID)

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
    fun `add note successfully`() = testBlocking {
        val note = "note"
        val isCustomerNote = false

        doReturn(true).whenever(networkStatus).isConnected()
        doReturn(testOrder).whenever(repository).getOrderById(ORDER_ID)
        doReturn(Result.success(Unit)).whenever(repository).addOrderNote(eq(testOrder.id), any())

        initViewModel()

        val events = mutableListOf<Event>()
        viewModel.event.observeForever { new -> events.add(new) }

        viewModel.onOrderTextEntered(note)
        viewModel.onIsCustomerCheckboxChanged(isCustomerNote)
        viewModel.pushOrderNote()

        verify(repository, times(1)).addOrderNote(
            eq(testOrder.id),
            argThat {
                this.note == note && this.isCustomerNote == isCustomerNote
            }
        )
        assertThat(events[0]).isInstanceOf(ShowSnackbar::class.java)
        assertEquals(R.string.add_order_note_added, (events[0] as ShowSnackbar).message)
        assertThat(events[1]).isInstanceOf(ExitWithResult::class.java)
        assertEquals(note, (events[1] as ExitWithResult<OrderNote>).data.note)
        assertEquals(isCustomerNote, (events[1] as ExitWithResult<OrderNote>).data.isCustomerNote)
    }

    @Test
    fun `doesn't add note if not connected`() = testBlocking {
        doReturn(false).whenever(networkStatus).isConnected()

        initViewModel()

        var event: Event? = null
        viewModel.event.observeForever { new -> event = new }

        viewModel.onOrderTextEntered("note")
        viewModel.pushOrderNote()

        verify(repository, times(0)).addOrderNote(any(), any())
        assertThat(event).isInstanceOf(ShowSnackbar::class.java)
        assertEquals(R.string.offline_error, (event as ShowSnackbar).message)
    }

    @Test
    fun `add note failure`() = testBlocking {
        val note = "note"
        val isCustomerNote = false

        doReturn(true).whenever(networkStatus).isConnected()
        doReturn(testOrder).whenever(repository).getOrderById(ORDER_ID)
        val error = WooError(WooErrorType.GENERIC_ERROR, GenericErrorType.UNKNOWN, "")
        doReturn(Result.failure<Unit>(WooException(error)))
            .whenever(repository).addOrderNote(eq(testOrder.id), any())

        initViewModel()

        var event: Event? = null
        viewModel.event.observeForever { new -> event = new }

        viewModel.onOrderTextEntered(note)
        viewModel.onIsCustomerCheckboxChanged(isCustomerNote)
        viewModel.pushOrderNote()

        verify(repository, times(1)).addOrderNote(
            eq(testOrder.id),
            argThat {
                this.note == note && this.isCustomerNote == isCustomerNote
            }
        )
        assertThat(event).isInstanceOf(ShowSnackbar::class.java)
        assertEquals(R.string.add_order_note_error, (event as ShowSnackbar).message)
    }
}
