package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.annotation.StringRes
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.Address
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.CancelAddressEditing
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowCountrySelector
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowSuggestedAddress
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType.ORIGIN
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.ValidationResult
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.data.WCLocationModel
import org.wordpress.android.fluxc.store.WCDataStore

@ExperimentalCoroutinesApi
class EditShippingLabelAddressViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val addressValidator: ShippingLabelAddressValidator,
    private val resourceProvider: ResourceProvider,
    private val dataStore: WCDataStore,
    private val site: SelectedSite
) : ScopedViewModel(savedState, dispatchers) {
    private val arguments: EditShippingLabelAddressFragmentArgs by savedState.navArgs()

    val viewStateData = LiveDataDelegate(savedState, ViewState(arguments.address, isStateFieldSpinner = false))
    private var viewState by viewStateData

    private val countries: List<WCLocationModel>
        get() = dataStore.getCountries()

    private val selectedCountry: String?
        get() = countries.firstOrNull { it.code == viewState.address?.country }?.name

    init {
        viewState = viewState.copy(
            title = if (arguments.addressType == ORIGIN) {
                R.string.orderdetail_shipping_label_item_shipfrom
            } else {
                R.string.orderdetail_shipping_label_item_shipto
            }
        )

        arguments.validationResult?.let {
            handleValidationResult(arguments.address, it)
        }

        loadCountriesAndStates()
    }

    fun onDoneButtonClicked(address: Address) {
        if (areRequiredFieldsValid(address)) {
            launch {
                viewState = viewState.copy(isProgressDialogVisible = true)
                clearErrors()
                val result = addressValidator.validateAddress(address, arguments.addressType)
                handleValidationResult(address, result)
                viewState = viewState.copy(isProgressDialogVisible = false)
            }
        }
    }

    private fun loadCountriesAndStates() {
        launch {
            if (countries.isEmpty()) {
                viewState = viewState.copy(isProgressDialogVisible = true)
                dataStore.fetchCountriesAndStates(site.get())
            }
            viewState = viewState.copy(isProgressDialogVisible = false, selectedCountryName = selectedCountry)
        }
    }

    private fun handleValidationResult(address: Address, result: ValidationResult) {
        when (result) {
            ValidationResult.Valid -> triggerEvent(ExitWithResult(address))
            is ValidationResult.Invalid -> viewState = viewState.copy(addressError = getStringResource(result.message))
            is ValidationResult.SuggestedChanges -> triggerEvent(ShowSuggestedAddress(address, result.suggested))
            is ValidationResult.NotFound -> {
                viewState = viewState.copy(
                    bannerMessage = resourceProvider.getString(
                        R.string.shipping_label_validation_error_template,
                        resourceProvider.getString(getStringResource(result.message))
                    )
                )
                triggerEvent(ShowSnackbar(getStringResource(result.message)))
            }
            is ValidationResult.Error -> triggerEvent(
                ShowSnackbar(R.string.shipping_label_edit_address_validation_error)
            )
        }
    }

    private fun clearErrors() {
        viewState = viewState.copy(
            bannerMessage = "",
            nameError = 0,
            addressError = 0,
            cityError = 0,
            zipError = 0
        )
    }

    private fun areRequiredFieldsValid(address: Address): Boolean {
        var allOk = true
        fun getErrorOrClear(field: String): Int {
            return if (field.isBlank()) {
                allOk = false
                R.string.shipping_label_error_required_field
            } else {
                0
            }
        }

        viewState = viewState.copy(
            nameError = getErrorOrClear(address.firstName + address.lastName),
            addressError = getErrorOrClear(address.address1),
            cityError = getErrorOrClear(address.city),
            zipError = getErrorOrClear(address.postcode)
        )

        return allOk
    }

    fun onUseAddressAsIsButtonClicked(address: Address) {
        if (areRequiredFieldsValid(address)) {
            triggerEvent(ExitWithResult(address))
        }
    }

    fun onCountrySpinnerTapped() {
        triggerEvent(ShowCountrySelector(countries, viewState.address?.country))
    }

    fun onCountrySelected(country: String) {
        viewState = viewState.copy(address = viewState.address?.copy(country = country))
        viewState = viewState.copy(selectedCountryName = selectedCountry)
    }

    fun onExit() {
        triggerEvent(CancelAddressEditing)
    }

    // errors are returned as hardcoded strings :facepalm:
    private fun getStringResource(message: String): Int {
        return when (message) {
            "House number is missing" -> R.string.shipping_label_error_address_house_number_missing
            "Street is invalid" -> R.string.shipping_label_error_address_invalid_street
            "Address not found" -> R.string.shipping_label_error_address_not_found
            else -> R.string.shipping_label_edit_address_validation_error
        }
    }

    @Parcelize
    data class ViewState(
        val address: Address? = null,
        val bannerMessage: String? = null,
        val isProgressDialogVisible: Boolean? = null,
        val isStateFieldSpinner: Boolean? = null,
        val selectedCountryName: String? = null,
        @StringRes val nameError: Int? = null,
        @StringRes val addressError: Int? = null,
        @StringRes val cityError: Int? = null,
        @StringRes val zipError: Int? = null,
        @StringRes val title: Int? = null
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<EditShippingLabelAddressViewModel>
}
