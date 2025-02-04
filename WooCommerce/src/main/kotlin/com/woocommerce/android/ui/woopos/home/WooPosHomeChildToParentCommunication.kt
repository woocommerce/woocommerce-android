package com.woocommerce.android.ui.woopos.home

import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@ActivityRetainedScoped
class WooPosChildrenToParentCommunication @Inject constructor() :
    WooPosChildrenToParentEventReceiver, WooPosChildrenToParentEventSender {
    private val _events = MutableSharedFlow<ChildToParentEvent>()
    override val events = _events.asSharedFlow()

    override suspend fun sendToParent(event: ChildToParentEvent) {
        _events.emit(event)
    }
}

sealed class ChildToParentEvent {
    data class CheckoutClicked(
        val itemClickedDataList: List<WooPosItemsViewModel.ItemClickedData>
    ) : ChildToParentEvent()

    data object BackFromCheckoutToCartClicked : ChildToParentEvent()
    data class ItemClickedInProductSelector(val itemData: WooPosItemsViewModel.ItemClickedData) : ChildToParentEvent()
    data object NewTransactionClicked : ChildToParentEvent()
    data object PaymentCollecting : ChildToParentEvent()
    data object PaymentInProgress : ChildToParentEvent()
    data object PaymentFailed : ChildToParentEvent()
    data object ReturnedFromCardReaderPaymentToCheckout : ChildToParentEvent()
    data object GoBackToCheckoutAfterFailedPayment : ChildToParentEvent()
    data object OrderSuccessfullyPaidByCard : ChildToParentEvent()
    data object ExitPosClicked : ChildToParentEvent()
    data object ProductsDialogInfoIconClicked : ChildToParentEvent()
    sealed class ProductsStatusChanged : ChildToParentEvent() {
        data object FullScreen : ProductsStatusChanged()
        data object WithCart : ProductsStatusChanged()
    }

    data class ToastMessageDisplayed(val message: String) : ChildToParentEvent()
    sealed class NavigationEvent : ChildToParentEvent() {
        data class ToCashPayment(val orderId: Long) : NavigationEvent()
        data class ToEmailReceipt(val orderId: Long) : NavigationEvent()
        data object ReturnHomeFromCashWhenCardPaymentStarted : NavigationEvent()
        data object ExitPos : NavigationEvent()
    }
}

interface WooPosChildrenToParentEventReceiver {
    val events: Flow<ChildToParentEvent>
}

interface WooPosChildrenToParentEventSender {
    suspend fun sendToParent(event: ChildToParentEvent)
}
