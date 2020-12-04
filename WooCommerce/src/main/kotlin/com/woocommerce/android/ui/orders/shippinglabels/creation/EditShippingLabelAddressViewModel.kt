package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.annotation.StringRes
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType.ORIGIN
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.ValidationResult
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class EditShippingLabelAddressViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val addressValidator: ShippingLabelAddressValidator
) : ScopedViewModel(savedState, dispatchers) {
    private val arguments: EditShippingLabelAddressFragmentArgs by savedState.navArgs()

    val viewStateData = LiveDataDelegate(savedState, ViewState(arguments.address))
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

    fun onDoneButtonClicked(address: Address) {
        launch {
            when (val result = addressValidator.validateAddress(address, arguments.addressType)) {
                ValidationResult.Valid, is ValidationResult.Invalid -> triggerEvent(ExitWithResult(address))
                ValidationResult.NotRecognized -> triggerEvent(ShowSnackbar(R.string.shipping_label_edit_address_address))
                is ValidationResult.Error -> triggerEvent(ShowSnackbar(R.string.shipping_label_preview_error))
            }
        }
    }

    fun onUseAddressAsIsButtonClicked(address: Address) {
        triggerEvent(ExitWithResult(address))
    }

    @Parcelize
    data class ViewState(
        val address: Address? = null,
        @StringRes val title: Int? = null
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<EditShippingLabelAddressViewModel>
}
