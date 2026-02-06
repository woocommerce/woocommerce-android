package com.woocommerce.android.ui.woopos.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LargeClass", "MagicNumber")
@HiltViewModel
class WooPosBookingsViewModel @Inject constructor(
    private val cardReaderFacade: WooPosCardReaderFacade
) : ViewModel() {

    private val _state = MutableStateFlow<WooPosBookingsState>(WooPosBookingsState.Loading)
    val state: StateFlow<WooPosBookingsState> = _state.asStateFlow()

    private val _scrollToTopEvent = MutableSharedFlow<Unit>()
    val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

    init {
        loadBookings()
    }

    private fun loadBookings() {
        viewModelScope.launch {
            delay(3000L)
            _state.value = createDummyContentState()
        }
    }

    private fun createDummyContentState(): WooPosBookingsState.Content {
        val details1 = createDummyDetails(1L, "#001")
        val details2 = createDummyDetails(2L, "#002")

        val item1 = WooPosBookingsState.BookingItemViewState(
            id = 1L,
            title = "#001",
            date = "Feb 5, 2026 at 10:00 AM",
            total = "$50.00",
            customerEmail = "john@example.com",
            isSelected = true,
            status = PosBookingStatus("Completed", BookingStatusColorKey.COMPLETED),
            statusSlug = "completed",
            createdAtMillis = System.currentTimeMillis()
        )

        val item2 = WooPosBookingsState.BookingItemViewState(
            id = 2L,
            title = "#002",
            date = "Feb 4, 2026 at 2:30 PM",
            total = "$75.00",
            customerEmail = "jane@example.com",
            isSelected = false,
            status = PosBookingStatus("Processing", BookingStatusColorKey.PROCESSING),
            statusSlug = "processing",
            createdAtMillis = System.currentTimeMillis() - 86400000
        )

        return WooPosBookingsState.Content(
            items = WooPosBookingsState.Content.Items.Loaded(
                items = mapOf(
                    item1 to WooPosBookingsState.BookingDetailsViewState.Computed(orderId = 1L, details = details1),
                    item2 to WooPosBookingsState.BookingDetailsViewState.Computed(orderId = 2L, details = details2)
                )
            ),
            pullToRefreshState = WooPosPullToRefreshState.Enabled,
            selectedDetails = details1,
            paginationState = WooPosPaginationState.None,
            dialogState = WooPosBookingsState.Content.DialogState.Hidden
        )
    }

    private fun createDummyDetails(
        id: Long,
        number: String
    ) = WooPosBookingsState.BookingDetailsViewState.Computed.Details(
        id = id,
        number = number,
        dateTime = "Feb 5, 2026 at 10:00 AM",
        customerEmail = "customer@example.com",
        status = PosBookingStatus("Completed", BookingStatusColorKey.COMPLETED),
        lineItems = listOf(
            WooPosBookingsState.BookingDetailsViewState.Computed.Details.LineItemRow(
                id = 1L,
                name = "Haircut",
                attributesDescription = "30 min",
                qtyAndUnitPrice = "1 x $30.00",
                lineTotal = "$30.00",
                imageUrl = null
            ),
            WooPosBookingsState.BookingDetailsViewState.Computed.Details.LineItemRow(
                id = 2L,
                name = "Beard Trim",
                attributesDescription = "15 min",
                qtyAndUnitPrice = "1 x $20.00",
                lineTotal = "$20.00",
                imageUrl = null
            )
        ),
        breakdown = WooPosBookingsState.BookingDetailsViewState.Computed.Details.TotalsBreakdown(
            products = "$50.00",
            discount = null,
            discountCode = null,
            taxes = "$0.00",
            shipping = null,
            refunds = emptyList(),
            netPayment = null
        ),
        total = "$50.00",
        totalPaid = "$50.00",
        paymentMethodTitle = "Cash",
        actionsState = WooPosBookingsState.BookingActionsState.Loaded(
            listOf(WooPosBookingsState.BookingAction.EmailReceipt(id))
        )
    )

    fun onRefresh() {
        return Unit
    }

    @Suppress("UnusedParameter")
    fun onBookingSelected(bookingId: Long) {
        return Unit
    }

    fun onEndOfBookingsListReached() {
        return Unit
    }

    fun onPaginationErrorTryAgain() {
        return Unit
    }

    fun onBookingsEmptyActionClicked() {
        return Unit
    }

    fun onBookingsLoadingErrorRetryButtonClicked() {
        return Unit
    }

    fun onUIEvent(event: WooPosBookingsUIEvent) {
        when (event) {
            is WooPosBookingsUIEvent.BookingActionClicked -> Unit
            is WooPosBookingsUIEvent.PayByCardClicked -> onPayByCardClicked()
        }
    }

    private fun onPayByCardClicked() {
        when (cardReaderFacade.readerStatus.value) {
            is CardReaderStatus.Connected -> TODO()
            is CardReaderStatus.Connecting -> {
                // no-op
            }
            is CardReaderStatus.NotConnected -> cardReaderFacade.connectToReader()
        }
    }
}
