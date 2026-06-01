package com.woocommerce.android.ui.woopos.home

import com.woocommerce.android.ui.woopos.home.cart.WooPosCartItemViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.math.BigDecimal
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
    data class ItemClickedInItemsList(
        val itemData: WooPosItemsViewModel.ItemClickedData,
        val eventForTracking: WooPosAnalyticsEvent.Event.ItemAddedToCart?
    ) : ChildToParentEvent()
    data object OnNewTransactionStarted : ChildToParentEvent()
    data object PaymentCollecting : ChildToParentEvent()
    data object PaymentInProgress : ChildToParentEvent()
    data object PaymentFailed : ChildToParentEvent()
    data object ReturnedFromCardReaderPaymentToCheckout : ChildToParentEvent()
    data object GoBackToCheckoutAfterFailedPayment : ChildToParentEvent()
    data object OrderSuccessfullyPaidByCard : ChildToParentEvent()
    data object OrderSuccessfullyPaidExternally : ChildToParentEvent()
    data object ExitPosClicked : ChildToParentEvent()
    data object SetupBarcodeScannerClicked : ChildToParentEvent()
    data object CouponsValidationFailed : ChildToParentEvent()
    data class MissingVariationEvent(val variationId: Long) : ChildToParentEvent()
    data object RemoveCouponsClicked : ChildToParentEvent()
    data class CouponsRemoved(
        val cartDataList: List<WooPosItemsViewModel.ItemClickedData>
    ) : ChildToParentEvent()
    data class RemoveProductsClicked(val removedProductsIds: List<Long>) :
        ChildToParentEvent()
    data class ProductsRemoved(
        val cartDataList: List<WooPosItemsViewModel.ItemClickedData>
    ) : ChildToParentEvent()

    data class ToastMessageDisplayed(val message: String) : ChildToParentEvent()
    data object RefreshProductList : ChildToParentEvent()
    data object ShowCardReaderConnectionDialog : ChildToParentEvent()

    data class CustomAmountDialogRequested(
        val editing: WooPosCartItemViewState.CustomAmount? = null,
    ) : ChildToParentEvent()

    data class CustomAmountSubmitted(
        val name: String,
        val amount: BigDecimal,
        val isTaxable: Boolean,
        val editingItemNumber: Int? = null,
    ) : ChildToParentEvent()

    sealed class NavigationEvent : ChildToParentEvent() {
        data class ToCashPayment(val orderId: Long) : NavigationEvent()
        data class ToMarkOrderAsPaid(val orderId: Long) : NavigationEvent()
        data class ToScanToPay(val orderId: Long) : NavigationEvent()
        data class ToEmailReceipt(val orderId: Long) : NavigationEvent()
        data object ReturnHomeFromCashWhenCardPaymentStarted : NavigationEvent()
        data object ExitPos : NavigationEvent()
        data object ToSettings : NavigationEvent()
        data object ToOrders : NavigationEvent()
    }

    sealed class SearchEvent : ChildToParentEvent() {
        data class QueryChanged(val query: String) : SearchEvent()
        data class RecentSearchSelected(val query: String) : SearchEvent()
        object Finished : SearchEvent()
        object Started : SearchEvent()
    }

    data class OrderCreated(val data: WooPosOrderCreatedData) : ChildToParentEvent()

    sealed class SettingsEvent : ChildToParentEvent() {
        data class ShowSyncErrorDialog(val errorMessage: String) : SettingsEvent()
        data object ShowCardReaderConnectionDialog : SettingsEvent()
        data object ShowCardReaderUpdateDialog : SettingsEvent()
    }
}

interface WooPosChildrenToParentEventReceiver {
    val events: Flow<ChildToParentEvent>
}

interface WooPosChildrenToParentEventSender {
    suspend fun sendToParent(event: ChildToParentEvent)
}
