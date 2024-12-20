package com.woocommerce.android.ui.orders.wooshippinglabels.purchased

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.shippinglabels.ShipmentTrackingUrls
import com.woocommerce.android.ui.orders.wooshippinglabels.ShippableItemUI
import com.woocommerce.android.ui.orders.wooshippinglabels.ShippableItemsUI
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus.PurchaseInProgress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus.Purchased
import com.woocommerce.android.ui.orders.wooshippinglabels.purchased.WooShippingLabelPaperSize.LABEL
import com.woocommerce.android.ui.orders.wooshippinglabels.purchased.printing.FetchShippingLabelFile
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.io.File
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class WooShippingLabelPurchasedViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val fetchShippingLabelFile: FetchShippingLabelFile,
    private val observeShippingLabelStatus: ObserveShippingLabelStatus
) : ScopedViewModel(savedState) {
    private val navArgs by savedState.navArgs<WooShippingLabelPurchasedFragmentArgs>()
    private val purchaseData = navArgs.purchaseData

    private val trackingLink: String?
        get() = ShipmentTrackingUrls.fromCarrier(
            carrierId = purchaseData.carrierId,
            trackingNumber = purchaseData.trackingNumber
        )

    private val _viewState = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = ViewState(
            paperSizeOption = LABEL
        )
    )
    val viewState = _viewState.asLiveData()

    init {
        observeShippingLabelPurchaseStatus()
        extractPurchaseDataToViewState()
    }

    fun onPrintShippingLabelClicked() {
        _viewState.update { it.copy(isLoadingData = true) }
        launch {
            val paperSize = _viewState.value.paperSizeOption
            val labelFile = fetchShippingLabelFile(
                labelIds = listOf(purchaseData.labelId),
                paperSize = paperSize.name.lowercase(Locale.US)
            )

            labelFile?.let {
                triggerEvent(OpenShippingLabelFile(it))
            } ?: triggerEvent(ShowError(R.string.shipping_label_purchased_print_error))

            _viewState.update { it.copy(isLoadingData = false) }
        }
    }

    fun onLabelPaperSizeOptionSelected(paperSize: WooShippingLabelPaperSize) {
        _viewState.update { it.copy(paperSizeOption = paperSize) }
    }

    fun onTrackShipmentClicked() {
        trackingLink
            ?.let { triggerEvent(OpenUrl(it)) }
            ?: triggerEvent(ShowError(R.string.shipping_label_purchased_tracking_error))
    }

    fun onSchedulePickUpClicked() {
        Carrier.fromCarrierId(purchaseData.carrierId)?.let {
            triggerEvent(OpenUrl(it.pickupUrl))
        } ?: triggerEvent(ShowError(R.string.shipping_label_purchased_pickup_error))
    }

    fun onRefundClicked() { triggerEvent(StartRefundRequest) }

    fun onLearnMoreClicked() { triggerEvent(OpenLearnMoreScreen) }

    private fun extractPurchaseDataToViewState() {
        _viewState.update { state ->
            state.copy(
                shippableItems = ShippableItemsUI(
                    formattedTotalWeight = purchaseData.formattedTotalWeight,
                    formattedTotalPrice = purchaseData.formattedTotalPrice,
                    shippableItems = purchaseData.items.map {
                        ShippableItemUI(
                            itemId = it.itemId,
                            productId = it.productId,
                            title = it.title,
                            formattedSize = it.dimensions,
                            formattedWeight = it.weight,
                            formattedPrice = it.formattedPrice,
                            quantity = it.quantity,
                            imageUrl = it.imageUrl
                        )
                    }
                )
            )
        }
    }

    private fun observeShippingLabelPurchaseStatus() {
        launch {
            observeShippingLabelStatus(
                orderId = purchaseData.orderId,
                labelId = purchaseData.labelId
            ).onEach { status ->
                when (status) {
                    PurchaseInProgress -> {
                        _viewState.update { it.copy(isPurchaseFinished = false) }
                    }
                    Purchased -> {
                        _viewState.update { it.copy(isPurchaseFinished = true) }
                    }
                    else -> {
                        _viewState.update { it.copy(isPurchaseFinished = null) }
                    }
                }
            }.launchIn(this)
        }
    }

    @Parcelize
    data class ViewState(
        val paperSizeOption: WooShippingLabelPaperSize,
        val shippableItems: ShippableItemsUI? = null,
        val isLoadingData: Boolean = false,
        val isPurchaseFinished: Boolean? = false
    ) : Parcelable

    data class OpenShippingLabelFile(val file: File) : MultiLiveEvent.Event()
    data class OpenUrl(val url: String) : MultiLiveEvent.Event()
    data class ShowError(val errorResId: Int) : MultiLiveEvent.Event()
    object StartRefundRequest : MultiLiveEvent.Event()
    object OpenLearnMoreScreen : MultiLiveEvent.Event()

    enum class Carrier(val pickupUrl: String) {
        USPS("https://tools.usps.com/schedule-pickup-steps.htm"),
        UPS("https://wwwapps.ups.com/pickup/request"),
        DHL("https://mydhl.express.dhl/us/en/schedule-pickup.html#/schedule-pickup#label-reference");

        companion object {
            fun fromCarrierId(carrierId: String): Carrier? {
                return when (carrierId) {
                    "usps" -> USPS
                    "ups" -> UPS
                    "dhlexpress" -> DHL
                    else -> null
                }
            }
        }
    }
}
