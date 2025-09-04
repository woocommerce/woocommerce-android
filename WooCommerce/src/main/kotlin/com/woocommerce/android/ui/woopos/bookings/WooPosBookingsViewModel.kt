package com.woocommerce.android.ui.woopos.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosBookingsViewModel @Inject constructor(
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
) : ViewModel() {

    fun onBookingPaymentClick(booking: WooPosBooking) {
        viewModelScope.launch {
            val bookingItem = WooPosItemsViewModel.ItemClickedData.Product.Simple(booking.id)
            
            childrenToParentEventSender.sendToParent(
                ChildToParentEvent.ItemClickedInItemsList(
                    itemData = bookingItem,
                    eventForTracking = null
                )
            )
        }
    }
}
