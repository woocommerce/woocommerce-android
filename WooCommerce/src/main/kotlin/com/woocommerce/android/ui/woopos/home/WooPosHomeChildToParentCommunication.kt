package com.woocommerce.android.ui.woopos.home

import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
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
    data class ItemClickedInProductSelector(val itemData: WooPosItemsViewModel.ItemClickedData) : ChildToParentEvent()
    data object NewTransactionClicked : ChildToParentEvent()
    data object PaymentCollecting : ChildToParentEvent()
    data object PaymentInProgress : ChildToParentEvent()
    data object PaymentFailed : ChildToParentEvent()
    data object ReturnedFromCardReaderPaymentToCheckout : ChildToParentEvent()
    data object GoBackToCheckoutAfterFailedPayment : ChildToParentEvent()
    data object OrderSuccessfullyPaidByCard : ChildToParentEvent()
    data object ExitPosClicked : ChildToParentEvent()
    data object SimpleProductExplanationMenuItemClicked : ChildToParentEvent()

    data class ToastMessageDisplayed(val message: String) : ChildToParentEvent()
    sealed class NavigationEvent : ChildToParentEvent() {
        data class ToCashPayment(val orderId: Long) : NavigationEvent()
        data class ToEmailReceipt(val orderId: Long) : NavigationEvent()
        data object ReturnHomeFromCashWhenCardPaymentStarted : NavigationEvent()
        data object ExitPos : NavigationEvent()
    }

    sealed class SearchEvent : ChildToParentEvent() {
        data class QueryChanged(val query: String) : SearchEvent()
        data class RecentSearchSelected(val query: String) : SearchEvent()
        object Finished : SearchEvent()
        object Started : SearchEvent()
    }

    data class OrderCreated(
        val updatedProducts: List<ProductInfo>,
        val updatedCoupons: List<CouponLine>
    ) : ChildToParentEvent() {
        sealed class ProductInfo(
            open val id: Long,
            open val name: String,
            open val actualPrice: BigDecimal,
            open val subtotalPrice: BigDecimal,
            open val quantity: Float,
        ) {
            data class Simple(
                override val id: Long,
                override val name: String,
                override val actualPrice: BigDecimal,
                override val subtotalPrice: BigDecimal,
                override val quantity: Float,
            ) : ProductInfo(id, name, actualPrice, subtotalPrice, quantity)

            data class Variation(
                override val id: Long,
                override val name: String,
                override val actualPrice: BigDecimal,
                override val subtotalPrice: BigDecimal,
                override val quantity: Float,
                val variationId: Long,
            ) : ProductInfo(id, name, actualPrice, subtotalPrice, quantity)
        }

        data class CouponLine(
            val id: Long,
            val code: String,
            val discountAmount: BigDecimal,
        )
    }
}

interface WooPosChildrenToParentEventReceiver {
    val events: Flow<ChildToParentEvent>
}

interface WooPosChildrenToParentEventSender {
    suspend fun sendToParent(event: ChildToParentEvent)
}
