package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.SHIPPING_LABEL_ADDRESS_SUGGESTIONS_EDIT_SELECTED_ADDRESS_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.SHIPPING_LABEL_ADDRESS_SUGGESTIONS_USE_SELECTED_ADDRESS_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.EditSelectedAddress
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.UseSelectedAddress
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType.ORIGIN
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ShippingLabelAddressSuggestionViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val arguments: ShippingLabelAddressSuggestionFragmentArgs by savedState.navArgs()

    /**
     * Saving more than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can
     * replace @Suppress("OPT_IN_USAGE") with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateData = LiveDataDelegate(
        savedState,
        ViewState(
            enteredAddress = arguments.enteredAddress,
            suggestedAddress = arguments.suggestedAddress,
            selectedAddress = arguments.suggestedAddress
        )
    )
    private var viewState by viewStateData

    init {
        viewState = viewState.copy(
            title = if (arguments.addressType == ORIGIN) {
                R.string.orderdetail_shipping_label_item_shipfrom
            } else {
                R.string.orderdetail_shipping_label_item_shipto
            }
        )
    }

    fun onUseSelectedAddressTapped() {
        trackUseAddressEvent()

        viewState.selectedAddress?.let {
            triggerEvent(UseSelectedAddress(it))
        }
    }

    private fun trackUseAddressEvent() {
        val addressType = if (viewState.selectedAddress == viewState.suggestedAddress) "suggested" else "original"
        AnalyticsTracker.track(
            SHIPPING_LABEL_ADDRESS_SUGGESTIONS_USE_SELECTED_ADDRESS_BUTTON_TAPPED,
            mapOf("type" to addressType)
        )
    }

    fun onEditSelectedAddressTapped() {
        trackEditAddressEvent()

        viewState.selectedAddress?.let {
            triggerEvent(EditSelectedAddress(it))
        }
    }

    private fun trackEditAddressEvent() {
        val addressType = if (viewState.selectedAddress == viewState.suggestedAddress) "suggested" else "original"
        AnalyticsTracker.track(
            SHIPPING_LABEL_ADDRESS_SUGGESTIONS_EDIT_SELECTED_ADDRESS_BUTTON_TAPPED,
            mapOf("type" to addressType)
        )
    }

    fun onSelectedAddressChanged(isSuggestedAddress: Boolean) {
        val address = if (isSuggestedAddress) viewState.suggestedAddress else viewState.enteredAddress
        viewState = viewState.copy(selectedAddress = address)
    }

    fun onExit() {
        triggerEvent(Exit)
    }

    @Parcelize
    data class ViewState(
        val enteredAddress: Address? = null,
        val suggestedAddress: Address? = null,
        val selectedAddress: Address? = null,
        @StringRes val title: Int? = null
    ) : Parcelable {
        @IgnoredOnParcel
        val areButtonsEnabled: Boolean = selectedAddress != null
    }
}
