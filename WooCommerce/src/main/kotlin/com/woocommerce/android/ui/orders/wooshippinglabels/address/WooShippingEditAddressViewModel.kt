package com.woocommerce.android.ui.orders.wooshippinglabels.address

import android.os.Parcelable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_STATE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_TYPE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_STARTED
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.combine
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.orders.details.editing.address.LocationCode
import com.woocommerce.android.ui.orders.wooshippinglabels.address.NormalizeAddressException.Companion.UNKNOWN_ERROR
import com.woocommerce.android.ui.orders.wooshippinglabels.address.destination.UpdateDestinationAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.address.origin.GetAcceptedOriginCountries
import com.woocommerce.android.ui.orders.wooshippinglabels.address.origin.UpdateOriginAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.DestinationShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.toAddress
import com.woocommerce.android.util.StringUtils.combineStrings
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
@Suppress("TooManyFunctions")
class WooShippingEditAddressViewModel @Inject constructor(
    private val addressValidator: AddressValidationHelper,
    private val getAcceptedOriginCountries: GetAcceptedOriginCountries,
    private val getAllCountries: GetAllCountries,
    private val getStatesByCountryCode: GetStatesByCountryCode,
    private val normalizeAddress: NormalizeAddress,
    private val resourceProvider: ResourceProvider,
    private val updateOriginAddress: UpdateOriginAddress,
    private val updateDestinationAddress: UpdateDestinationAddress,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val navArgs: WooShippingEditAddressFragmentArgs by savedState.navArgs()

    private var name by mutableStateOf(InputValue(value = "", isRequired = true))
    private var company by mutableStateOf(InputValue(value = "", isRequired = false))
    private var address by mutableStateOf(InputValue(value = "", isRequired = true))
    private var city by mutableStateOf(InputValue(value = "", isRequired = true))
    private var postalCode by mutableStateOf(InputValue(value = "", isRequired = true))
    private var email by mutableStateOf(InputValue(value = "", isRequired = true))
    private var country = MutableStateFlow(Location.EMPTY)
    private var phone by mutableStateOf(InputValue(value = "", isRequired = true))

    private var rawState by mutableStateOf("")
    private val selectedState = MutableStateFlow(Location.EMPTY)

    private val state = combine(snapshotFlow { rawState }, selectedState) { rawState, selectedState ->
        if (selectedState != Location.EMPTY) {
            selectedState
        } else {
            AmbiguousLocation.Raw(rawState).asLocation()
        }
    }

    private val countriesState = MutableStateFlow<LocationState>(LocationState.Loading)
    private val statesState = MutableStateFlow<LocationState>(LocationState.Loading)

    private val addressValidationState = MutableStateFlow<AddressValidationState>(AddressValidationState.NotStarted)

    private val currentAddress = when (val currentFlow = navArgs.flow) {
        is EditAddressFlow.EditDestinationAddress -> currentFlow.address.address
        is EditAddressFlow.EditOriginAddress -> currentFlow.address.toAddress()
    }.let { MutableStateFlow(it) }

    private val isVerified = when (val currentFlow = navArgs.flow) {
        is EditAddressFlow.EditDestinationAddress -> currentFlow.address.isVerified
        is EditAddressFlow.EditOriginAddress -> currentFlow.address.isVerified
    }.let { MutableStateFlow(it) }

    private val addressId = (navArgs.flow as? EditAddressFlow.EditOriginAddress)?.address?.id

    val screenTitle = when (navArgs.flow) {
        is EditAddressFlow.EditDestinationAddress ->
            resourceProvider.getString(R.string.woo_shipping_edit_destination_address_title)

        is EditAddressFlow.EditOriginAddress ->
            resourceProvider.getString(R.string.woo_shipping_edit_origin_address_title)
    }

    private val nameValidatedFlow = snapshotFlow { name }.combine(snapshotFlow { company }) { name, company ->
        Pair(name, company)
    }.transformLatestWithDelay(delayMillis = DELAY_TIME_MILLIS) { inputValues ->
        if (inputValues.first.error == null) {
            inputValues.copy(
                first = inputValues.first.copy(
                    error = addressValidator.validateAtLeastOneOf(inputValues.first.value, inputValues.second.value)
                )
            )
        } else {
            inputValues
        }
    }

    private val addressValidatedFlow = snapshotFlow { address }
        .transformLatestWithDelay(delayMillis = DELAY_TIME_MILLIS) { inputValue ->
            if (inputValue.error == null) {
                inputValue.copy(error = addressValidator.validateFieldRequired(inputValue.value))
            } else {
                inputValue
            }
        }

    private val cityValidatedFlow = snapshotFlow { city }
        .transformLatestWithDelay(delayMillis = DELAY_TIME_MILLIS) { inputValue ->
            if (inputValue.error == null) {
                inputValue.copy(error = addressValidator.validateFieldRequired(inputValue.value))
            } else {
                inputValue
            }
        }

    private val postalCodeValidatedFlow = snapshotFlow { postalCode }
        .transformLatestWithDelay(delayMillis = DELAY_TIME_MILLIS) { inputValue ->
            if (inputValue.error == null) {
                inputValue.copy(error = addressValidator.validateFieldRequired(inputValue.value))
            } else {
                inputValue
            }
        }

    private val emailValidatedFlow = snapshotFlow { email }
        .transformLatestWithDelay(delayMillis = DELAY_TIME_MILLIS) { inputValue ->
            if (inputValue.isRequired && inputValue.error == null) {
                inputValue.copy(error = addressValidator.validateEmail(inputValue.value))
            } else {
                inputValue
            }
        }

    private val phoneValidatedFlow = snapshotFlow { phone }
        .combine(country) { phoneValue, countryValue -> Pair(phoneValue, countryValue) }
        .transformLatestWithDelay(delayMillis = DELAY_TIME_MILLIS) { (inputValue, countryValue) ->
            val validatedPhone = if (inputValue.isRequired && inputValue.error == null) {
                inputValue.copy(error = validatePhoneByCountry(inputValue.value, countryValue))
            } else {
                inputValue
            }
            Pair(validatedPhone, countryValue)
        }.map { it.first }

    private val isCompanyExpanded = MutableStateFlow(false)

    private val editableAddress = combine(
        nameValidatedFlow,
        addressValidatedFlow,
        cityValidatedFlow,
        postalCodeValidatedFlow,
        emailValidatedFlow,
        phoneValidatedFlow,
        country,
        state
    ) { name, address, city, postalCode, email, phone, country, state ->
        EditableAddress(
            name = name.first,
            company = name.second,
            country = country,
            state = state,
            address = address,
            city = city,
            postalCode = postalCode,
            email = email,
            phone = phone
        )
    }

    val viewState: MutableStateFlow<EditAddressViewState> = MutableStateFlow(
        EditAddressViewState(
            isCompanyExpanded = false,
            editableAddress = EditableAddress(),
            loading = LoadingState.Hidden,
            error = null,
            shouldUseStatesInput = false,
            addressStatus = AddressStatus.Unverified,
            addressValidationState = AddressValidationState.NotStarted
        )
    )

    init {
        trackScreenShownEvent()
        launch { observeChanges() }
        fillAddressForm(currentAddress.value)
        launch {
            loadCountries()
            loadStates()
        }
    }

    private fun trackScreenShownEvent() {
        analyticsTracker.track(
            AnalyticsEvent.WCS_EDITING_ADDRESS_STEP,
            mapOf(KEY_TYPE to getAnalyticsType(), KEY_STATE to VALUE_STARTED)
        )
    }

    private fun fillAddressForm(addressInformation: Address) {
        val fullName = combineStrings(
            addressInformation.firstName,
            addressInformation.lastName
        )
        val fullAddress = combineStrings(
            addressInformation.address1,
            addressInformation.address2
        )
        name = name.copy(value = fullName, error = null)
        company = company.copy(value = addressInformation.company, error = null)
        country.value = findLocationByCode(addressInformation.country.code, countriesState.value)
        address = address.copy(value = fullAddress, error = null)
        city = city.copy(value = addressInformation.city, error = null)
        findLocationByCode(addressInformation.state.codeOrRaw, statesState.value).let {
            selectedState.value = it
            rawState = addressInformation.state.codeOrRaw
        }
        postalCode = postalCode.copy(value = addressInformation.postcode, error = null)
        email = email.copy(value = addressInformation.email, error = null)
        phone = phone.copy(value = addressInformation.phone, error = null)
        isCompanyExpanded.value = addressInformation.company.isNotNullOrEmpty()
    }

    private fun findLocationByCode(code: String, state: LocationState): Location {
        val default = AmbiguousLocation.Raw(code).asLocation()
        return when (val currentState = state) {
            is LocationState.Loaded -> currentState.locations.firstOrNull { it.code == code } ?: default
            else -> default
        }
    }

    private suspend fun loadCountries() {
        val getCountries = when (navArgs.flow) {
            is EditAddressFlow.EditDestinationAddress -> getAllCountries()
            is EditAddressFlow.EditOriginAddress -> getAcceptedOriginCountries()
        }
        getCountries.fold(
            onSuccess = {
                countriesState.value = LocationState.Loaded(it)
                country.value = findLocationByCode(country.value.code, countriesState.value)
            },
            onFailure = { countriesState.value = LocationState.Error }
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun loadStates() {
        country.mapLatest { country ->
            getStatesByCountryCode(country.code)
        }.collectLatest { states ->
            statesState.value = LocationState.Loaded(states)
            val stateCode = if (country.value.code == currentAddress.value.country.code) {
                currentAddress.value.state.codeOrRaw
            } else {
                ""
            }
            when {
                states.isNotEmpty() && stateCode.isNotEmpty() -> {
                    selectedState.value = findLocationByCode(stateCode, statesState.value)
                        .takeIf { it != Location.EMPTY } ?: states.first()
                    rawState = ""
                }

                states.isNotEmpty() -> {
                    selectedState.value = states.first()
                }

                else -> {
                    rawState = stateCode
                    selectedState.value = Location.EMPTY
                }
            }
        }
    }

    fun handleBackPress(): Boolean {
        if (allowBackNavigation()) {
            onNavigateBack()
        }
        return false
    }

    fun allowBackNavigation() = when (viewState.value.addressValidationState) {
        is AddressValidationState.AddressSelection,
        is AddressValidationState.NormalizedAddressUpdateFailed -> {
            onCloseAddressSelection()
            false
        }

        else -> true
    }

    fun onNavigateBack() {
        when (navArgs.flow) {
            is EditAddressFlow.EditDestinationAddress -> triggerEvent(
                Event.ExitWithResult(DestinationShippingAddress(currentAddress.value, isVerified.value))
            )

            is EditAddressFlow.EditOriginAddress -> triggerEvent(Event.Exit)
        }
    }

    fun onExpandCompany() {
        isCompanyExpanded.value = true
    }

    private suspend fun observeChanges() {
        combine(
            editableAddress,
            isCompanyExpanded,
            countriesState,
            statesState,
            addressValidationState,
            currentAddress
        ) { address, isExpanded, countriesState, statesState, addressValidation, currentAddress ->
            val loading = getLoadingState(countriesState, statesState, addressValidation)
            val error = getErrorState(countriesState, addressValidation, address)
            val validationException = (addressValidation as? AddressValidationState.VerificationFailed)?.exception

            val addressStatus = when {
                validationException != null && navArgs.flow is EditAddressFlow.EditDestinationAddress -> {
                    AddressStatus.VerifyFailed(addressValidation.exception)
                }

                address.hasIncorrectOrMissingData || !isFormValid() -> AddressStatus.MissingInfo
                hasOnlyNoAddressChanges(address, currentAddress) -> AddressStatus.SaveChanges
                isSameAddress(address, currentAddress) && isVerified.value -> AddressStatus.Verified
                navArgs.flow is EditAddressFlow.EditDestinationAddress &&
                    addressValidation is AddressValidationState.VerificationFailed -> AddressStatus.VerifyFailed()

                else -> AddressStatus.Unverified
            }

            EditAddressViewState(
                isCompanyExpanded = isExpanded,
                editableAddress = address,
                loading = loading,
                error = error,
                shouldUseStatesInput = statesState is LocationState.Loaded && statesState.locations.isEmpty(),
                addressStatus = addressStatus,
                addressValidationState = addressValidation
            )
        }.collectLatest { viewState.value = it }
    }

    private fun isFormValid(): Boolean {
        return addressValidator.validateAtLeastOneOf(name.value, company.value) == null &&
            addressValidator.validateFieldRequired(address.value) == null &&
            addressValidator.validateFieldRequired(city.value) == null &&
            addressValidator.validateFieldRequired(postalCode.value) == null &&
            (!email.isRequired || addressValidator.validateEmail(email.value) == null) &&
            (!phone.isRequired || validatePhoneByCountry(phone.value, country.value) == null)
    }

    private fun validatePhoneByCountry(value: String, country: Location): String? =
        if (country.code == US_COUNTRY_CODE) {
            addressValidator.validateUSCustomsPhone(value)
        } else {
            addressValidator.validatePhoneNumber(value)
        }

    private fun getErrorState(
        countriesState: LocationState,
        addressValidation: AddressValidationState,
        editableAddress: EditableAddress
    ): EditAddressError? {
        if (countriesState is LocationState.Error) {
            return EditAddressError(
                resourceProvider.getString(R.string.woo_shipping_fetching_countries_and_states_failed)
            ) { onRefreshCountries() }
        }

        val error = when (addressValidation) {
            is AddressValidationState.VerificationFailed -> {
                val hasUserChangedAddress = editableAddress.toAddress() != addressValidation.editableAddress.toAddress()
                if (hasUserChangedAddress) {
                    addressValidationState.value = AddressValidationState.NotStarted
                    null
                } else if (addressValidation.exception != null) {
                    // This error is shown at the field level, so we don't need a general error here.
                    null
                } else {
                    EditAddressError(
                        message = resourceProvider.getString(R.string.woo_shipping_verifying_address_failed),
                        isIndefinite = navArgs.flow is EditAddressFlow.EditOriginAddress,
                    ) { onNormalizeAddress(addressValidation.editableAddress) }
                }
            }

            is AddressValidationState.AddressUpdateFailed -> {
                val hasUserChangedAddress = editableAddress != addressValidation.editableAddress
                if (hasUserChangedAddress) {
                    addressValidationState.value = AddressValidationState.NotStarted
                    null
                } else {
                    EditAddressError(
                        resourceProvider.getString(R.string.woo_shipping_updating_address_failed)
                    ) { onUpdateAddress(addressValidation.editableAddress) }
                }
            }

            is AddressValidationState.NormalizedAddressUpdateFailed -> {
                EditAddressError(
                    resourceProvider.getString(R.string.woo_shipping_updating_address_failed)
                ) { onUpdateNormalizedOriginAddress(addressValidation.selection) }
            }

            else -> null
        }
        return error
    }

    private fun getLoadingState(
        countriesState: LocationState,
        statesState: LocationState,
        addressSelection: AddressValidationState
    ): LoadingState = when {
        countriesState is LocationState.DisplayLoading || statesState is LocationState.DisplayLoading -> {
            LoadingState.DisplayLoading(
                resourceProvider.getString(R.string.loading),
                resourceProvider.getString(R.string.woo_shipping_fetching_countries_and_states)
            )
        }

        addressSelection is AddressValidationState.VerifyingAddress -> {
            LoadingState.DisplayLoading(
                resourceProvider.getString(R.string.woo_shipping_address_validate_title),
                resourceProvider.getString(R.string.woo_shipping_address_validate_message)
            )
        }

        addressSelection is AddressValidationState.UpdatingAddress -> {
            LoadingState.DisplayLoading(
                resourceProvider.getString(R.string.woo_shipping_address_update_title),
                resourceProvider.getString(R.string.woo_shipping_address_update_message)
            )
        }

        else -> LoadingState.Hidden
    }

    private fun isSameAddress(newAddress: EditableAddress, currentAddress: Address): Boolean {
        val originalFullAddress = combineStrings(currentAddress.address1, currentAddress.address2)

        val isSameAddress = originalFullAddress == newAddress.address.value
        val isSameCity = currentAddress.city == newAddress.city.value
        val isSameState = currentAddress.state.codeOrRaw == newAddress.state.code
        val isSameCountry = currentAddress.country.code == newAddress.country.code
        val isSamePostalCode = currentAddress.postcode == newAddress.postalCode.value

        return isSameAddress && isSameCity && isSameState && isSameCountry && isSamePostalCode
    }

    private fun hasOnlyNoAddressChanges(newAddress: EditableAddress, currentAddress: Address): Boolean {
        val originalFullName = combineStrings(currentAddress.firstName, currentAddress.lastName)
        val isDifferentName = newAddress.name.value != originalFullName
        val isDifferentCompany = newAddress.company.value != currentAddress.company
        val isDifferentEmail = newAddress.email.value != currentAddress.email
        val isDifferentPhone = newAddress.phone.value != currentAddress.phone
        val isSameAddress = isSameAddress(newAddress, currentAddress)
        val isVerified = isVerified.value
        val hasNoAddressChanges = isDifferentName || isDifferentCompany || isDifferentEmail || isDifferentPhone
        return hasNoAddressChanges && isSameAddress && isVerified
    }

    fun onNameChange(value: String) {
        val isCompanyRequired = value.isEmpty() && company.value.isNotEmpty()
        name = InputValue(value = value, isRequired = isCompanyRequired.not(), error = null)
        company = company.copy(isRequired = isCompanyRequired)
    }

    fun onCompanyChange(value: String) {
        val isCompanyRequired = value.isNotEmpty() && name.value.isEmpty()
        company = InputValue(value = value, isRequired = isCompanyRequired, error = null)
        name = name.copy(isRequired = !isCompanyRequired)
    }

    fun onAddressChange(value: String) {
        address = address.copy(value = value, error = null)
    }

    fun onCityChange(value: String) {
        city = city.copy(value = value, error = null)
    }

    fun onPostalCodeChange(value: String) {
        postalCode = postalCode.copy(value = value, error = null)
    }

    fun onEmailChange(value: String) {
        email = email.copy(value = value, error = null)
    }

    fun onPhoneChange(value: String) {
        phone = phone.copy(value = value, error = null)
    }

    fun onRawStateChange(value: String) {
        rawState = value
    }

    fun onCountryChange() {
        when (val state = countriesState.value) {
            is LocationState.Loaded -> triggerEvent(ShowCountrySelector(state.locations))
            is LocationState.Loading -> countriesState.value = LocationState.DisplayLoading
            else -> {}
        }
    }

    fun onStateChange() {
        when (val state = statesState.value) {
            is LocationState.Loaded -> triggerEvent(ShowStateSelector(state.locations))
            is LocationState.Loading -> countriesState.value = LocationState.DisplayLoading
            else -> {}
        }
    }

    fun onRefreshCountries() {
        countriesState.value = LocationState.DisplayLoading
        launch { loadCountries() }
    }

    fun onCountryChanged(code: LocationCode) {
        country.value = findLocationByCode(code, countriesState.value)
    }

    fun onStateChanged(code: LocationCode) {
        selectedState.value = findLocationByCode(code, statesState.value)
    }

    fun onNormalizeAddress(editableAddress: EditableAddress) {
        addressValidationState.value = AddressValidationState.VerifyingAddress
        launch {
            val enteredAddress = editableAddress.toAddress()
            normalizeAddress(enteredAddress).fold(
                onSuccess = {
                    addressValidationState.value = AddressValidationState.AddressSelection(it, it.normalizedAddress)
                    analyticsTracker.track(
                        AnalyticsEvent.WCS_EDITING_ADDRESS_STEP,
                        mapOf(
                            KEY_TYPE to getAnalyticsType(),
                            KEY_STATE to getAnalyticsVerificationValue(isVerified.value)
                        )
                    )
                },
                onFailure = {
                    val normalizeAddressException = it as? NormalizeAddressException
                    analyticsTracker.track(
                        stat = AnalyticsEvent.WCS_EDITING_ADDRESS_STEP,
                        properties = mapOf(
                            KEY_TYPE to getAnalyticsType(),
                            KEY_STATE to getAnalyticsVerificationValue(false),
                        ),
                        errorContext = WooShippingEditAddressViewModel::class.simpleName,
                        errorType = normalizeAddressException?.errors?.keys?.first() ?: UNKNOWN_ERROR,
                        errorDescription = normalizeAddressException?.errors?.values?.first()
                    )

                    normalizeAddressException?.let { exception ->
                        name = name.copy(error = exception.nameError)
                        company = company.copy(error = exception.companyError)
                        address = address.copy(error = exception.addressError)
                        city = city.copy(error = exception.cityError)
                        postalCode = postalCode.copy(error = exception.postcodeError)
                        email = email.copy(error = exception.emailError)
                        phone = phone.copy(error = exception.phoneError)
                    }

                    addressValidationState.value = AddressValidationState.VerificationFailed(
                        editableAddress.copy(
                            name = editableAddress.name.copy(error = normalizeAddressException?.nameError),
                            company = editableAddress.company.copy(error = normalizeAddressException?.companyError),
                            address = editableAddress.address.copy(error = normalizeAddressException?.addressError),
                            city = editableAddress.city.copy(error = normalizeAddressException?.cityError),
                            postalCode = editableAddress.postalCode.copy(
                                error = normalizeAddressException?.postcodeError
                            ),
                            email = editableAddress.email.copy(error = normalizeAddressException?.emailError),
                            phone = editableAddress.phone.copy(error = normalizeAddressException?.phoneError)
                        ),
                        normalizeAddressException
                    )
                }
            )
        }
    }

    fun onAddressSelectionChange(addressSelection: AddressValidationState.AddressSelection) {
        addressValidationState.value = addressSelection
    }

    fun onCloseAddressSelection() {
        addressValidationState.value = AddressValidationState.NotStarted
    }

    fun onUpdateNormalizedAddress(selection: AddressValidationState.AddressSelection) {
        when (val currentFlow = navArgs.flow) {
            is EditAddressFlow.EditDestinationAddress ->
                onUpdateNormalizedDestinationAddress(selection, currentFlow.orderId)

            is EditAddressFlow.EditOriginAddress -> onUpdateNormalizedOriginAddress(selection)
        }
        analyticsTracker.track(
            AnalyticsEvent.WCS_EDITING_ADDRESS_STEP,
            mapOf(KEY_TYPE to getAnalyticsType(), KEY_STATE to "confirmed")
        )
    }

    private fun onUpdateNormalizedDestinationAddress(
        selection: AddressValidationState.AddressSelection,
        orderId: Long
    ) {
        addressValidationState.value = AddressValidationState.UpdatingAddress
        launch {
            updateDestinationAddress(selection.selectedAddress, orderId, isVerified = true).fold(
                onSuccess = {
                    onUpdateAddress(it.address, it.isVerified)
                },
                onFailure = {
                    addressValidationState.value = AddressValidationState.NormalizedAddressUpdateFailed(selection)
                }
            )
        }
    }

    private fun onUpdateNormalizedOriginAddress(selection: AddressValidationState.AddressSelection) {
        addressValidationState.value = AddressValidationState.UpdatingAddress
        launch {
            updateOriginAddress(selection.selectedAddress, addressId).fold(
                onSuccess = {
                    onUpdateAddress(it.toAddress(), it.isVerified)
                },
                onFailure = {
                    addressValidationState.value = AddressValidationState.NormalizedAddressUpdateFailed(selection)
                }
            )
        }
    }

    fun onUpdateAddress(editableAddress: EditableAddress) {
        when (val currentFlow = navArgs.flow) {
            is EditAddressFlow.EditDestinationAddress ->
                onUpdateDestinationAddress(editableAddress, currentFlow.orderId)

            is EditAddressFlow.EditOriginAddress -> onUpdateOriginAddress(editableAddress)
        }
    }

    private fun onUpdateDestinationAddress(editableAddress: EditableAddress, orderId: Long) {
        val latestAddressValidationState = addressValidationState.value
        addressValidationState.value = AddressValidationState.UpdatingAddress
        launch {
            val address = editableAddress.toAddress()
            val verified = if (latestAddressValidationState is AddressValidationState.VerificationFailed) {
                false
            } else {
                isVerified.value
            }
            updateDestinationAddress(address, orderId, verified).fold(
                onSuccess = { result ->
                    onUpdateAddress(result.address, result.isVerified)
                    if (latestAddressValidationState is AddressValidationState.VerificationFailed) {
                        // Use address as entered button was tapped
                        analyticsTracker.track(
                            AnalyticsEvent.WCS_EDITING_ADDRESS_STEP,
                            mapOf(KEY_TYPE to getAnalyticsType(), KEY_STATE to "confirmed_without_verification")
                        )
                        onNavigateBack()
                    }
                },
                onFailure = {
                    addressValidationState.value = AddressValidationState.AddressUpdateFailed(editableAddress)
                }
            )
        }
    }

    private fun onUpdateOriginAddress(editableAddress: EditableAddress) {
        addressValidationState.value = AddressValidationState.UpdatingAddress
        launch {
            val address = editableAddress.toAddress()
            updateOriginAddress(address, addressId).fold(
                onSuccess = {
                    onUpdateAddress(it.toAddress(), it.isVerified)
                },
                onFailure = {
                    addressValidationState.value = AddressValidationState.AddressUpdateFailed(editableAddress)
                }
            )
        }
    }

    private fun onUpdateAddress(updatedAddress: Address, updatedIsVerified: Boolean) {
        isVerified.value = updatedIsVerified
        fillAddressForm(updatedAddress)
        addressValidationState.value = AddressValidationState.NotStarted
        currentAddress.value = updatedAddress
    }

    private fun getAnalyticsType() = when (navArgs.flow) {
        is EditAddressFlow.EditOriginAddress -> "origin"
        is EditAddressFlow.EditDestinationAddress -> "destination"
    }

    private fun getAnalyticsVerificationValue(
        verified: Boolean
    ) = if (verified) "validation_success" else "validation_failed"

    data class ShowCountrySelector(val countries: List<Location>) : Event()

    data class ShowStateSelector(val states: List<Location>) : Event()

    companion object {
        private const val DELAY_TIME_MILLIS = 500L
        private const val US_COUNTRY_CODE = "US"
    }
}

@Parcelize
sealed class EditAddressFlow : Parcelable {
    data class EditOriginAddress(val address: OriginShippingAddress) : EditAddressFlow()
    data class EditDestinationAddress(val address: DestinationShippingAddress, val orderId: Long) : EditAddressFlow()
}
