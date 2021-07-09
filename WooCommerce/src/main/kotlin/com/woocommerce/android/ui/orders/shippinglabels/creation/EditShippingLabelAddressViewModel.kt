package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowSuggestedAddress
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType.DESTINATION
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType.ORIGIN
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.ValidationResult
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.ValidationResult.NameMissing
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.ValidationResult.PhoneInvalid
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.data.WCLocationModel
import org.wordpress.android.fluxc.store.WCDataStore
import javax.inject.Inject

@HiltViewModel
class EditShippingLabelAddressViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val addressValidator: ShippingLabelAddressValidator,
    private val resourceProvider: ResourceProvider,
    private val dataStore: WCDataStore,
    private val site: SelectedSite
) : ScopedViewModel(savedState) {
    companion object {
        val ACCEPTED_USPS_ORIGIN_COUNTRIES = arrayOf(
            "US", // United States
            "PR", // Puerto Rico
            "VI", // Virgin Islands
            "GU", // Guam
            "AS", // American Samoa
            "UM", // United States Minor Outlying Islands
            "MH", // Marshall Islands
            "FM", // Micronesia
            "MP" // Northern Mariana Islands
        )
    }

    private val arguments: EditShippingLabelAddressFragmentArgs by savedState.navArgs()

    val viewStateData = LiveDataDelegate(savedState, ViewState(arguments.address))
    private var viewState by viewStateData

    private val countries: List<WCLocationModel>
        get() {
            val fullCountriesList = dataStore.getCountries()
            return if (arguments.addressType == ORIGIN) {
                fullCountriesList.filter { ACCEPTED_USPS_ORIGIN_COUNTRIES.contains(it.code) }
            } else fullCountriesList
        }

    private val states: List<WCLocationModel>
        get() = emptyList() // TODO viewState.address?.country?.let { dataStore.getStates(it) } ?: emptyList()

    private val selectedCountry: String?
        get() = "" // TODO countries.firstOrNull { it.code == viewState.address?.country }?.name

    private val selectedState: String
        get() = ""// TODO states.firstOrNull { it.code == viewState.address?.state }?.name ?: ""

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
                val validationErrorMessage = getAddressErrorStringRes(it.message)
                viewState = viewState.copy(
                    address1Field = viewState.address1Field.copy(validationError = validationErrorMessage).validate(),
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
        AnalyticsTracker.track(Stat.SHIPPING_LABEL_EDIT_ADDRESS_DONE_BUTTON_TAPPED)
        validateFields(address)
        if (viewState.areAllRequiredFieldsValid) {
            launch {
                viewState = viewState.copy(isValidationProgressDialogVisible = true)
                val result = addressValidator.validateAddress(
                    address,
                    arguments.addressType,
                    arguments.requiresPhoneNumber
                )
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
                //selectedStateName = if (selectedState.isBlank()) viewState.address?.state else selectedState,
                isStateFieldSpinner = states.isNotEmpty()
            )
        }
    }

    private fun handleValidationResult(address: Address, result: ValidationResult) {
        when (result) {
            ValidationResult.Valid -> triggerEvent(ExitWithResult(address))
            is ValidationResult.Invalid -> {
                val validationErrorMessage = getAddressErrorStringRes(result.message)
                viewState = viewState.copy(
                    address1Field = viewState.address1Field.copy(validationError = validationErrorMessage).validate(),
                )
            }
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
                    nameField = viewState.nameField.validate()
                )
                triggerEvent(ShowSnackbar(R.string.shipping_label_address_data_invalid_snackbar_message))
            }
            is PhoneInvalid -> {
                viewState = viewState.copy(
                    phoneError = validatePhone(address)
                )
                triggerEvent(ShowSnackbar(string.shipping_label_address_data_invalid_snackbar_message))
            }
        }
    }

    private fun clearErrors() {
        viewState = viewState.copy(
            bannerMessage = "",
            cityError = 0,
            zipError = 0
        )
    }

    private fun validateFields(address: Address) {
        fun getErrorOrClear(field: String): Int? {
            return if (field.isBlank()) {
                R.string.shipping_label_error_required_field
            } else {
                null
            }
        }

        viewState = viewState.copy(
            phoneError = validatePhone(address),
            cityError = getErrorOrClear(address.city),
            zipError = getErrorOrClear(address.postcode)
        )
    }

    private fun validatePhone(address: Address): Int? {
        if (!arguments.requiresPhoneNumber) return null
        val addressType = arguments.addressType
        return when {
            address.phone.isBlank() -> R.string.shipping_label_address_phone_required
            addressType == ORIGIN && !address.hasValidPhoneNumber(addressType) ->
                R.string.shipping_label_origin_address_phone_invalid
            addressType == DESTINATION && !address.hasValidPhoneNumber(addressType) ->
                R.string.shipping_label_destination_address_phone_invalid
            else -> null
        }
    }

    fun updateAddress(address: Address) {
        // TODO viewState = viewState.copy(address = address)
    }

    fun onUseAddressAsIsButtonClicked() {
        AnalyticsTracker.track(Stat.SHIPPING_LABEL_EDIT_ADDRESS_USE_ADDRESS_AS_IS_BUTTON_TAPPED)
//        viewState.address?.let {
//            validateFields(it)
//        }
//        if (viewState.areAllRequiredFieldsValid) {
//            triggerEvent(ExitWithResult(viewState.address))
//        } else {
//            triggerEvent(ShowSnackbar(R.string.shipping_label_address_data_invalid_snackbar_message))
//        }
    }

    fun onCountrySpinnerTapped() {
        // triggerEvent(ShowCountrySelector(countries, viewState.address?.country))
    }

    fun onStateSpinnerTapped() {
        // triggerEvent(ShowStateSelector(states, viewState.address?.state))
    }

    fun onOpenMapTapped() {
        AnalyticsTracker.track(Stat.SHIPPING_LABEL_EDIT_ADDRESS_OPEN_MAP_BUTTON_TAPPED)

//        viewState.address?.let { address ->
//            triggerEvent(OpenMapWithAddress(address))
//        }
    }

    fun onContactCustomerTapped() {
        AnalyticsTracker.track(Stat.SHIPPING_LABEL_EDIT_ADDRESS_CONTACT_CUSTOMER_BUTTON_TAPPED)

//        viewState.address?.phone?.let {
//            triggerEvent(DialPhoneNumber(it))
//        }
    }

    fun onCountrySelected(country: String) {
//        viewState = viewState.copy(address = viewState.address?.copy(country = country))
//        viewState = viewState.copy(
//            selectedCountryName = selectedCountry,
//            selectedStateName = selectedState,
//            isStateFieldSpinner = states.isNotEmpty()
//        )
    }

    fun onStateSelected(state: String) {
//        viewState = viewState.copy(address = viewState.address?.copy(state = state))
//        viewState = viewState.copy(selectedStateName = selectedState)
    }

    fun onAddressSelected(address: Address) {
        triggerEvent(ExitWithResult(address))
    }

    fun onEditRequested(address: Address) {
        updateAddress(address)
    }

    fun onFieldEdited(field: Field, content: String) {
        viewState = viewState.updateAndValidateField(field, content)
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
        val bannerMessage: String? = null,
        val isValidationProgressDialogVisible: Boolean? = null,
        val isLoadingProgressDialogVisible: Boolean? = null,
        val isStateFieldSpinner: Boolean? = null,
        val selectedCountryName: String? = null,
        val selectedStateName: String? = null,
        val nameField: NameField,
        val companyField: OptionalField,
        val address1Field: Address1Field,
        val address2Field: OptionalField,
        @StringRes val phoneError: Int? = null,
        @StringRes val cityError: Int? = null,
        @StringRes val zipError: Int? = null,
        @StringRes val title: Int? = null
    ) : Parcelable {
        constructor(address: Address): this(
            nameField = NameField(
                content = "${address.firstName} ${address.lastName}".trim(),
                companyContent = address.company
            ),
            companyField = OptionalField(
                content = address.company
            ),
            address1Field = Address1Field(address.address1),
            address2Field = OptionalField(address.address2)
        )

        @IgnoredOnParcel
        val isContactCustomerButtonVisible = false//!address?.phone.isNullOrBlank()

        @IgnoredOnParcel
        val areAllRequiredFieldsValid
            get() = true//Field.values().all { get(it).isValid }

        operator fun get(field: Field): InputField<*> {
            return when (field) {
                Field.Name -> nameField
                Field.Company -> companyField
                Field.Phone -> TODO()
                Field.Address1 -> address1Field
                Field.Address2 -> address2Field
                Field.City -> TODO()
                Field.State -> TODO()
                Field.Zip -> TODO()
                Field.Country -> TODO()
            }
        }

        fun updateAndValidateField(field: Field, content: String): ViewState {
            return when (field) {
                Field.Name -> copy(
                    nameField = nameField.copy(content = content).validate())
                Field.Company -> copy(
                    companyField = companyField.copy(content = content).validate(),
                    nameField = nameField.copy(companyContent = content).validate()
                )
                Field.Phone -> TODO()
                Field.Address1 -> copy(
                    address1Field = address1Field.copy(content = content, validationError = null).validate()
                )
                Field.Address2 -> copy(
                    address2Field = address2Field.copy(content = content).validate()
                )
                Field.City -> TODO()
                Field.State -> TODO()
                Field.Zip -> TODO()
                Field.Country -> TODO()
            }
        }

        fun validateAllFields(): ViewState {
            return copy(
                nameField = nameField.validate()
            )
        }
    }

    @Parcelize
    data class NameField(
        override val content: String,
        val companyContent: String
    ): InputField<NameField>(content) {
        override fun validateInternal(): UiString? {
            return if(content.isNotBlank() || companyContent.isNotBlank())  null
            else UiStringRes(R.string.shipping_label_error_required_field)
        }
    }

    @Parcelize
    data class Address1Field(
        override val content: String,
        @StringRes val validationError: Int? = null
    ): InputField<Address1Field>(content) {
        override fun validateInternal(): UiString? {
            return when {
                validationError != null -> UiStringRes(validationError)
                content.isBlank() -> UiStringRes(R.string.shipping_label_error_required_field)
                else -> null
            }
        }
    }

    enum class Field {
        Name, Company, Phone, Address1, Address2, City, State, Zip, Country
    }

    abstract class InputField<T: InputField<T>>(open val content: String): Parcelable {
        var error: UiString? = null
        private var hasBeenValidated: Boolean = false
        val isValid: Boolean
            get() {
                return if(!hasBeenValidated) validateInternal() == null
                else error == null
            }

        fun validate(): T {
            error = validateInternal()
            hasBeenValidated = true
            return this as T
        }

        final override fun hashCode(): Int {
            return content.hashCode() + error.hashCode() + hasBeenValidated.hashCode()
        }

        final override fun equals(other: Any?): Boolean {
            if (other !is InputField<*>) return false
            return content == other.content &&
                error == other.error &&
                hasBeenValidated == other.hasBeenValidated
        }

        /**
         * Perform specific field's validation
         * @return [UiString] holding the error to be displayed or null if it's valid
         */
        protected abstract fun validateInternal(): UiString?
    }

    @Parcelize
    data class RequiredField(
        override val content: String
    ) : InputField<RequiredField>(content) {
        override fun validateInternal(): UiString? {
            return if (content.isBlank()) UiStringRes(R.string.shipping_label_error_required_field)
            else null
        }
    }

    @Parcelize
    data class CustomField(
        override val content: String,
        val validateInput: (String) -> Boolean
    ): InputField<CustomField>(content) {
        override fun validateInternal(): UiString? {
            return if (validateInput(content)) null
            else UiStringRes(R.string.shipping_label_error_required_field)
        }
    }

    @Parcelize
    data class OptionalField(
        override val content: String
    ) : InputField<OptionalField>(content) {
        override fun validateInternal(): UiString? = null
    }
}
