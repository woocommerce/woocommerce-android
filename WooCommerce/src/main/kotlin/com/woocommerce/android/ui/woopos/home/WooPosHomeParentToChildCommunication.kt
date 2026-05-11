package com.woocommerce.android.ui.woopos.home

import com.woocommerce.android.ui.woopos.common.composeui.modifier.BarcodeInputDetector
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.math.BigDecimal
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
    data object CouponsValidationFailed : ParentToChildrenEvent()
    data class MissingVariationEvent(val variationId: Long) : ParentToChildrenEvent()
    data object RemoveCouponsClicked : ParentToChildrenEvent()
    data object RefreshProductList : ParentToChildrenEvent()
    data class CouponsRemoved(
        val cartDataList: List<WooPosItemsViewModel.ItemClickedData>
    ) : ParentToChildrenEvent()
    data class RemoveProductsClicked(
        val productIdsToRemove: List<Long>
    ) : ParentToChildrenEvent()
    data class ProductsRemoved(
        val cartDataList: List<WooPosItemsViewModel.ItemClickedData>
    ) : ParentToChildrenEvent()
    data class ItemClickedInItemsList(
        val itemData: WooPosItemsViewModel.ItemClickedData,
        val eventForTracking: WooPosAnalyticsEvent.Event.ItemAddedToCart?
    ) : ParentToChildrenEvent()
    data class BarcodeEvent(
        val result: BarcodeInputDetector.BarcodeResult
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

    data class CustomAmountSubmitted(
        val customAmountId: Long,
        val name: String,
        val amount: BigDecimal,
        val isTaxable: Boolean,
        val editingItemNumber: Int? = null,
    ) : ParentToChildrenEvent()

    sealed class SearchEvent : ParentToChildrenEvent() {
        data class ChangedQuery(val query: String) : SearchEvent()
        data class RecentSearchSelected(val query: String) : SearchEvent()
        object Finished : SearchEvent()
        object Started : SearchEvent()
    }

    data class OrderCreated(val data: WooPosOrderCreatedData) : ParentToChildrenEvent()

    sealed class SettingsEvent : ParentToChildrenEvent() {
        data object RetrySyncRequested : SettingsEvent()
    }
}

interface WooPosParentToChildrenEventReceiver {
    val events: Flow<ParentToChildrenEvent>
}

interface WooPosParentToChildrenEventSender {
    suspend fun sendToChildren(event: ParentToChildrenEvent)
}
