package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.annotation.StringRes
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.assisted.AssistedFactory
import com.woocommerce.android.R
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.Address
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.DialPhoneNumber
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.OpenMapWithAddress
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowCountrySelector
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowStateSelector
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowSuggestedAddress
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType.ORIGIN
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.ValidationResult
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.ValidationResult.NameMissing
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel

import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.data.WCLocationModel
import org.wordpress.android.fluxc.store.WCDataStore

class EditShippingLabelAddressViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val addressValidator: ShippingLabelAddressValidator,
    private val resourceProvider: ResourceProvider,
    private val dataStore: WCDataStore,
    private val site: SelectedSite
) : ScopedViewModel(savedState, dispatchers) {
    private val arguments: EditShippingLabelAddressFragmentArgs by savedState.navArgs()

    val viewStateData = LiveDataDelegate(savedState, ViewState(arguments.address))
    private var viewState by viewStateData

    private val countries: List<WCLocationModel>
        get() = dataStore.getCountries()

    private val states: List<WCLocationModel>
        get() = viewState.address?.country?.let { dataStore.getStates(it) } ?: emptyList()

    private val selectedCountry: String?
        get() = countries.firstOrNull { it.code == viewState.address?.country }?.name

    private val selectedState: String
        get() = states.firstOrNull { it.code == viewState.address?.state }?.name ?: ""

    init {
        viewState = viewState.copy(
            title = if (arguments.addressType == ORIGIN) {
                R.string.orderdetail_shipping_label_item_shipfrom
            } else {
                R.string.orderdetail_shipping_label_item_shipto
            }
        )

        arguments.validationResult?.let {
            if (it is ValidationResult.Invalid) {
                viewState = viewState.copy(
                    addressError = getAddressErrorStringRes(it.message),
                    bannerMessage = resourceProvider.getString(R.string.shipping_label_edit_address_error_warning)
                )
            } else if (it is ValidationResult.NotFound) {
                viewState = viewState.copy(
                    bannerMessage = resourceProvider.getString(R.string.shipping_label_edit_address_error_warning)
                )
            }
        }

        loadCountriesAndStates()
    }

    fun onDoneButtonClicked(address: Address) {
        if (areRequiredFieldsValid(address)) {
            launch {
                viewState = viewState.copy(address = address, isValidationProgressDialogVisible = true)
                val result = addressValidator.validateAddress(address, arguments.addressType)
                clearErrors()
                handleValidationResult(address, result)
                viewState = viewState.copy(isValidationProgressDialogVisible = false)
            }
        }
    }

    private fun loadCountriesAndStates() {
        launch {
            if (countries.isEmpty()) {
                viewState = viewState.copy(isLoadingProgressDialogVisible = true)
                dataStore.fetchCountriesAndStates(site.get())
                viewState = viewState.copy(isLoadingProgressDialogVisible = false)
            }
            viewState = viewState.copy(
                isValidationProgressDialogVisible = false,
                selectedCountryName = selectedCountry,
                selectedStateName = if (selectedState.isBlank()) viewState.address?.state else selectedState,
                isStateFieldSpinner = states.isNotEmpty()
            )
        }
    }

    private fun handleValidationResult(address: Address, result: ValidationResult) {
        when (result) {
            ValidationResult.Valid -> triggerEvent(ExitWithResult(address))
            is ValidationResult.Invalid -> viewState = viewState.copy(
                addressError = getAddressErrorStringRes(result.message)
            )
            is ValidationResult.SuggestedChanges -> {
                triggerEvent(ShowSuggestedAddress(address, result.suggested, arguments.addressType))
            }
            is ValidationResult.NotFound -> {
                viewState = viewState.copy(
                    bannerMessage = resourceProvider.getString(
                        R.string.shipping_label_validation_error_template,
                        resourceProvider.getString(getAddressErrorStringRes(result.message))
                    )
                )
                triggerEvent(ShowSnackbar(getAddressErrorStringRes(result.message)))
            }
            is ValidationResult.Error -> triggerEvent(
                ShowSnackbar(R.string.shipping_label_edit_address_validation_error)
            )
            is NameMissing -> {
                viewState = viewState.copy(
                    nameError = R.string.shipping_label_error_required_field
                )
                triggerEvent(ShowSnackbar(R.string.shipping_label_missing_data_snackbar_message))
            }
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
            nameError = getErrorOrClear(address.firstName + address.lastName + address.company),
            addressError = getErrorOrClear(address.address1),
            cityError = getErrorOrClear(address.city),
            zipError = getErrorOrClear(address.postcode)
        )

        return allOk
    }

    fun updateAddress(address: Address) {
        viewState = viewState.copy(address = address)
    }

    fun onUseAddressAsIsButtonClicked() {
        viewState.address?.let { address ->
            if (areRequiredFieldsValid(address)) {
                triggerEvent(ExitWithResult(address))
            } else {
                triggerEvent(ShowSnackbar(R.string.shipping_label_missing_data_snackbar_message))
            }
        }
    }

    fun onCountrySpinnerTapped() {
        triggerEvent(ShowCountrySelector(countries, viewState.address?.country))
    }

    fun onStateSpinnerTapped() {
        triggerEvent(ShowStateSelector(states, viewState.address?.state))
    }

    fun onOpenMapTapped() {
        viewState.address?.let { address ->
            triggerEvent(OpenMapWithAddress(address))
        }
    }

    fun onContactCustomerTapped() {
        viewState.address?.phone?.let {
            triggerEvent(DialPhoneNumber(it))
        }
    }

    fun onCountrySelected(country: String) {
        viewState = viewState.copy(address = viewState.address?.copy(country = country))
        viewState = viewState.copy(
            selectedCountryName = selectedCountry,
            selectedStateName = selectedState,
            isStateFieldSpinner = states.isNotEmpty()
        )
    }

    fun onStateSelected(state: String) {
        viewState = viewState.copy(address = viewState.address?.copy(state = state))
        viewState = viewState.copy(selectedStateName = selectedState)
    }

    fun onAddressSelected(address: Address) {
        triggerEvent(ExitWithResult(address))
    }

    fun onEditRequested(address: Address) {
        updateAddress(address)
    }

    fun onExit() {
        triggerEvent(Exit)
    }

    // errors are returned as hardcoded strings :facepalm:
    private fun getAddressErrorStringRes(message: String): Int {
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
        val isValidationProgressDialogVisible: Boolean? = null,
        val isLoadingProgressDialogVisible: Boolean? = null,
        val isStateFieldSpinner: Boolean? = null,
        val selectedCountryName: String? = null,
        val selectedStateName: String? = null,
        @StringRes val nameError: Int? = null,
        @StringRes val addressError: Int? = null,
        @StringRes val cityError: Int? = null,
        @StringRes val zipError: Int? = null,
        @StringRes val title: Int? = null
    ) : Parcelable {
        val isContactCustomerButtonVisible = !address?.phone.isNullOrBlank()
    }

    @AssistedFactory
    interface Factory : ViewModelAssistedFactory<EditShippingLabelAddressViewModel>
}
