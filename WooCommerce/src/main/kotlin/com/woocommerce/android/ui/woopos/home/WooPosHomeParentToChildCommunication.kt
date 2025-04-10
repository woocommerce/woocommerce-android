package com.woocommerce.android.ui.woopos.home

import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@ActivityRetainedScoped
class WooPosParentToChildrenCommunication @Inject constructor() :
    WooPosParentToChildrenEventReceiver, WooPosParentToChildrenEventSender {
    private val _events = MutableSharedFlow<ParentToChildrenEvent>()
    override val events = _events.asSharedFlow()

    override suspend fun sendToChildren(event: ParentToChildrenEvent) {
        _events.emit(event)
    }
}

sealed class ParentToChildrenEvent {
    data object BackFromCheckoutToCartClicked : ParentToChildrenEvent()
    data class ItemClickedInProductSelector(
        val itemData: WooPosItemsViewModel.ItemClickedData
    ) : ParentToChildrenEvent()
    data class CheckoutClicked(
        val itemClickedDataList: List<WooPosItemsViewModel.ItemClickedData>
    ) : ParentToChildrenEvent()
    data class OrderSuccessfullyPaid(val paymentMethod: PaymentMethod) : ParentToChildrenEvent() {
        enum class PaymentMethod {
            CARD,
            CASH
        }
    }

    sealed class SearchEvent : ParentToChildrenEvent() {
        data class ChangedQuery(val query: String) : SearchEvent()
        data class RecentSearchSelected(val query: String) : SearchEvent()
        object Finished : SearchEvent()
        object Started : SearchEvent()
    }
}

interface WooPosParentToChildrenEventReceiver {
    val events: Flow<ParentToChildrenEvent>
}

interface WooPosParentToChildrenEventSender {
    suspend fun sendToChildren(event: ParentToChildrenEvent)
}
