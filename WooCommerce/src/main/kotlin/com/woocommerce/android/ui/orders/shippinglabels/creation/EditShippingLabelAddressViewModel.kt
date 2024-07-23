package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.InputField
import com.woocommerce.android.ui.common.OptionalField
import com.woocommerce.android.ui.common.RequiredField
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.CloseKeyboard
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.DialPhoneNumber
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.OpenMapWithAddress
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowCountrySelector
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowStateSelector
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowSuggestedAddress
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType
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
import org.wordpress.android.fluxc.store.WCDataStore
import javax.inject.Inject

@HiltViewModel
class EditShippingLabelAddressViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val addressValidator: ShippingLabelAddressValidator,
    private val resourceProvider: ResourceProvider,
    private val dataStore: WCDataStore,
    private val site: SelectedSite,
    private val appPrefs: AppPrefsWrapper
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

    /**
     * Saving more data than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can replace @Suppress("OPT_IN_USAGE")
     * with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateData = LiveDataDelegate(savedState, ViewState(arguments))
    private var viewState by viewStateData

    private val countries: List<Location>
        get() {
            val fullCountriesList = dataStore.getCountries()
            val supportedCountries = if (arguments.addressType == ORIGIN) {
                fullCountriesList.filter { ACCEPTED_USPS_ORIGIN_COUNTRIES.contains(it.code) }
            } else {
                fullCountriesList
            }
            return supportedCountries.map { it.toAppModel() }
        }

    private val states: List<Location>
        get() = dataStore.getStates(viewState.countryField.location.code).map { it.toAppModel() }

    init {
        viewState = viewState.copy(
            title = if (arguments.addressType == ORIGIN) {
                R.string.orderdetail_shipping_label_item_shipfrom
            } else {
                R.string.orderdetail_shipping_label_item_shipto
            }
        )

        loadCountriesAndStates()
        arguments.validationResult?.let {
            handleValidationResult(arguments.address, it, showToastErrors = false)
        }
    }

    fun onDoneButtonClicked() {
        AnalyticsTracker.track(AnalyticsEvent.SHIPPING_LABEL_EDIT_ADDRESS_DONE_BUTTON_TAPPED)
        viewState = viewState.validateAllFields()
        val address = viewState.getAddress()
        if (viewState.areAllRequiredFieldsValid) {
            triggerEvent(CloseKeyboard)
            launch {
                viewState = viewState.copy(
                    isValidationProgressDialogVisible = true,
                    bannerMessage = null
                )
                val result = addressValidator.validateAddress(
                    address,
                    arguments.addressType,
                    arguments.isCustomsFormRequired
                )
                handleValidationResult(address, result)
                viewState = viewState.copy(isValidationProgressDialogVisible = false)
            }
        } else {
            triggerEvent(ShowSnackbar(R.string.shipping_label_address_data_invalid_snackbar_message))
            triggerScrollToFirstErrorFieldEvent()
        }
    }

    private fun triggerScrollToFirstErrorFieldEvent() {
        val firstErrorField = viewState.findFirstErrorField()
        firstErrorField?.let {
            triggerEvent(CreateShippingLabelEvent.ScrollToFirstErrorField(it, viewState.isStateFieldSpinner))
        }
    }

    private fun ViewState.findFirstErrorField(): Field? {
        return Field.values().firstOrNull { this[it].error != null }
    }

    private fun loadCountriesAndStates() {
        launch {
            if (countries.isEmpty()) {
                viewState = viewState.copy(isLoadingProgressDialogVisible = true)
                dataStore.fetchCountriesAndStates(site.get())
                viewState = viewState.copy(isLoadingProgressDialogVisible = false)
            }
            val isStateFieldSpinner = states.isNotEmpty()
            val countryField = countries.firstOrNull { it.code == viewState.countryField.location.code }?.let {
                viewState.countryField.copy(location = it)
            } ?: viewState.countryField
            val stateField = states.firstOrNull { it.code == viewState.stateField.location.code }?.let {
                viewState.stateField.copy(location = it, isRequired = isStateFieldSpinner)
            } ?: viewState.stateField.copy(isRequired = isStateFieldSpinner)

            viewState = viewState.copy(
                isValidationProgressDialogVisible = false,
                isStateFieldSpinner = isStateFieldSpinner,
                countryField = countryField,
                stateField = stateField
            )
        }
    }

    private fun handleValidationResult(address: Address, result: ValidationResult, showToastErrors: Boolean = true) {
        when (result) {
            ValidationResult.Valid -> {
                exitWithAddress(address)
            }

            is ValidationResult.Invalid -> {
                val validationErrorMessage = getAddressErrorStringRes(result.message)
                viewState = viewState.copy(
                    address1Field = viewState.address1Field.copy(validationError = validationErrorMessage).validate(),
                    bannerMessage = resourceProvider.getString(
                        if (arguments.addressType == ORIGIN) {
                            R.string.shipping_label_edit_origin_address_error_warning
                        } else {
                            R.string.shipping_label_edit_address_error_warning
                        }
                    )
                )
            }

            is ValidationResult.SuggestedChanges -> {
                if (result.isTrivial) {
                    exitWithAddress(result.suggested)
                } else {
                    triggerEvent(ShowSuggestedAddress(address, result.suggested, arguments.addressType))
                }
            }

            is ValidationResult.NotFound -> {
                viewState = viewState.copy(
                    bannerMessage = resourceProvider.getString(
                        R.string.shipping_label_validation_error_template,
                        resourceProvider.getString(getAddressErrorStringRes(result.message))
                    )
                )
                if (showToastErrors) {
                    triggerEvent(ShowSnackbar(getAddressErrorStringRes(result.message)))
                }
            }

            is ValidationResult.Error -> if (showToastErrors) {
                triggerEvent(ShowSnackbar(R.string.shipping_label_edit_address_validation_error))
            }

            is NameMissing -> {
                viewState = viewState.copy(
                    nameField = viewState.nameField.validate()
                )
                if (showToastErrors) {
                    triggerEvent(ShowSnackbar(R.string.shipping_label_address_data_invalid_snackbar_message))
                }
            }

            is PhoneInvalid -> {
                viewState = viewState.copy(
                    phoneField = viewState.phoneField.validate()
                )
                if (showToastErrors) {
                    triggerEvent(ShowSnackbar(R.string.shipping_label_address_data_invalid_snackbar_message))
                }
            }
        }
    }

    fun onUseAddressAsIsButtonClicked() {
        AnalyticsTracker.track(AnalyticsEvent.SHIPPING_LABEL_EDIT_ADDRESS_USE_ADDRESS_AS_IS_BUTTON_TAPPED)
        // Clear remote validation error of `address1`
        viewState = viewState.copy(address1Field = viewState.address1Field.copy(validationError = null))
        // Validate fields locally
        viewState = viewState.validateAllFields()
        if (viewState.areAllRequiredFieldsValid) {
            triggerEvent(CloseKeyboard)
            exitWithAddress(viewState.getAddress())
        } else {
            triggerEvent(ShowSnackbar(R.string.shipping_label_address_data_invalid_snackbar_message))
            triggerScrollToFirstErrorFieldEvent()
        }
    }

    fun onCountrySpinnerTapped() {
        triggerEvent(ShowCountrySelector(countries, viewState.countryField.location.code))
    }

    fun onStateSpinnerTapped() {
        triggerEvent(ShowStateSelector(states, viewState.stateField.location.code))
    }

    fun onOpenMapTapped() {
        AnalyticsTracker.track(AnalyticsEvent.SHIPPING_LABEL_EDIT_ADDRESS_OPEN_MAP_BUTTON_TAPPED)

        triggerEvent(OpenMapWithAddress(viewState.getAddress()))
    }

    fun onContactCustomerTapped() {
        AnalyticsTracker.track(AnalyticsEvent.SHIPPING_LABEL_EDIT_ADDRESS_CONTACT_CUSTOMER_BUTTON_TAPPED)

        triggerEvent(DialPhoneNumber(viewState.phoneField.content))
    }

    fun onCountrySelected(countryCode: String) {
        val currentCountry = viewState.countryField.location
        onFieldEdited(Field.Country, countryCode)
        val isStateFieldSpinner = states.isNotEmpty()
        // Update state
        val stateField = if (viewState.countryField.location != currentCountry) {
            viewState.stateField.copy(location = Location("", ""), isRequired = isStateFieldSpinner)
        } else {
            viewState.stateField
        }
        viewState = viewState.copy(
            stateField = stateField,
            isStateFieldSpinner = isStateFieldSpinner
        )
    }

    fun onAddressSelected(address: Address) {
        exitWithAddress(address)
    }

    private fun exitWithAddress(address: Address) {
        if (arguments.addressType == ORIGIN) {
            appPrefs.setStorePhoneNumber(site.getSelectedSiteId(), address.phone)
        }
        triggerEvent(ExitWithResult(address))
    }

    fun onEditRequested(address: Address) {
        viewState = with(viewState) {
            copy(
                nameField = nameField.copy(content = "${address.firstName} ${address.lastName}"),
                companyField = companyField.copy(content = address.company),
                phoneField = phoneField.copy(content = address.phone),
                address1Field = address1Field.copy(content = address.address1),
                address2Field = address2Field.copy(content = address.address2),
                cityField = cityField.copy(content = address.city),
                stateField = stateField.copy(location = address.state.asLocation()),
                countryField = countryField.copy(location = address.country)
            )
        }
    }

    fun onFieldEdited(field: Field, content: String) {
        viewState = with(viewState) {
            when (field) {
                Field.Name -> copy(
                    nameField = nameField.copy(content = content).validate()
                )

                Field.Company -> copy(
                    companyField = companyField.copy(content = content).validate(),
                    nameField = nameField.copy(companyContent = content).validate()
                )

                Field.Phone -> copy(
                    phoneField = phoneField.copy(content = content).validate()
                )

                Field.Address1 -> copy(
                    address1Field = address1Field.copy(content = content, validationError = null).validate()
                )

                Field.Address2 -> copy(
                    address2Field = address2Field.copy(content = content).validate()
                )

                Field.City -> copy(
                    cityField = cityField.copy(content = content).validate()
                )

                Field.State -> {
                    val state = states.firstOrNull { it.code == content } ?: Location(code = content, name = content)
                    copy(
                        stateField = stateField.copy(location = state).validate()
                    )
                }

                Field.Zip -> copy(
                    zipField = zipField.copy(content = content).validate()
                )

                Field.Country -> {
                    val country = countries.first { it.code == content }
                    val stateField = if (countryField.location != country) {
                        stateField.copy(location = Location("", ""))
                    } else {
                        stateField
                    }
                    copy(
                        countryField = countryField.copy(location = country),
                        stateField = stateField
                    )
                }
            }
        }
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
        private val addressType: AddressType,
        val bannerMessage: String? = null,
        val isValidationProgressDialogVisible: Boolean? = null,
        val isLoadingProgressDialogVisible: Boolean? = null,
        val isStateFieldSpinner: Boolean? = null,
        val nameField: NameField,
        val companyField: OptionalField,
        val phoneField: PhoneField,
        val address1Field: Address1Field,
        val address2Field: OptionalField,
        val cityField: RequiredField,
        val zipField: RequiredField,
        val stateField: LocationField,
        val countryField: LocationField,
        @StringRes val title: Int? = null
    ) : Parcelable {
        constructor(args: EditShippingLabelAddressFragmentArgs) : this(
            addressType = args.addressType,
            nameField = NameField(
                content = "${args.address.firstName} ${args.address.lastName}".trim(),
                companyContent = args.address.company
            ),
            companyField = OptionalField(content = args.address.company),
            address1Field = Address1Field(args.address.address1),
            address2Field = OptionalField(args.address.address2),
            phoneField = PhoneField(args.address.phone, args.isCustomsFormRequired, args.addressType),
            cityField = RequiredField(args.address.city),
            zipField = RequiredField(args.address.postcode),
            stateField = LocationField(args.address.state.asLocation()),
            countryField = LocationField(args.address.country, isRequired = true)
        )

        @IgnoredOnParcel
        val isContactCustomerButtonVisible
            get() = addressType == DESTINATION && phoneField.content.isNotBlank()

        @IgnoredOnParcel
        val areAllRequiredFieldsValid
            get() = Field.values().all { get(it).isValid }

        operator fun get(field: Field): InputField<*> {
            return when (field) {
                Field.Name -> nameField
                Field.Company -> companyField
                Field.Phone -> phoneField
                Field.Address1 -> address1Field
                Field.Address2 -> address2Field
                Field.City -> cityField
                Field.State -> stateField
                Field.Zip -> zipField
                Field.Country -> countryField
            }
        }

        fun validateAllFields(): ViewState = copy(
            nameField = nameField.validate(),
            companyField = companyField.validate(),
            address1Field = address1Field.validate(),
            address2Field = address2Field.validate(),
            phoneField = phoneField.validate(),
            cityField = cityField.validate(),
            zipField = zipField.validate(),
            stateField = stateField.validate(),
            countryField = countryField.validate(),
        )

        fun getAddress(): Address {
            return Address(
                firstName = nameField.content,
                lastName = "",
                company = companyField.content,
                phone = phoneField.content,
                address1 = address1Field.content,
                address2 = address2Field.content,
                city = cityField.content,
                state = AmbiguousLocation.Defined(stateField.location),
                country = countryField.location,
                postcode = zipField.content,
                email = ""
            )
        }
    }

    @Parcelize
    data class NameField(
        override val content: String,
        val companyContent: String
    ) : InputField<NameField>(content) {
        override fun validateInternal(): UiString? {
            return if (content.isNotBlank() || companyContent.isNotBlank()) {
                null
            } else {
                UiStringRes(R.string.error_required_field)
            }
        }
    }

    @Parcelize
    data class Address1Field(
        override val content: String,
        @StringRes val validationError: Int? = null
    ) : InputField<Address1Field>(content) {
        override fun validateInternal(): UiString? {
            return when {
                validationError != null -> UiStringRes(validationError)
                content.isBlank() -> UiStringRes(R.string.error_required_field)
                else -> null
            }
        }
    }

    @Parcelize
    data class PhoneField(
        override val content: String,
        val isCustomsFormRequired: Boolean,
        val addressType: AddressType
    ) : InputField<PhoneField>(content) {
        override fun validateInternal(): UiString? {
            return if (content.isValidPhoneNumber(addressType, isCustomsFormRequired)) {
                null
            } else {
                when {
                    content.isBlank() -> UiStringRes(R.string.shipping_label_address_phone_required)
                    addressType == ORIGIN ->
                        UiStringRes(R.string.shipping_label_origin_address_phone_invalid)

                    addressType == DESTINATION ->
                        UiStringRes(R.string.shipping_label_destination_address_phone_invalid)

                    else -> null
                }
            }
        }
    }

    @Parcelize
    data class LocationField(
        val location: Location,
        val isRequired: Boolean = false
    ) : InputField<LocationField>(location.name) {
        override fun validateInternal(): UiString? {
            return if (isRequired && content.isBlank()) {
                UiStringRes(R.string.error_required_field)
            } else {
                null
            }
        }
    }

    enum class Field {
        Name, Company, Phone, Address1, Address2, City, State, Zip, Country
    }
}
