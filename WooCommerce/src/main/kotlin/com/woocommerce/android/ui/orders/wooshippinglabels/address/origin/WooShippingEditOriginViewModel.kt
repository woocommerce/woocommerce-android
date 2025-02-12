package com.woocommerce.android.ui.orders.wooshippinglabels.address.origin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.extensions.combine
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.orders.details.editing.address.LocationCode
import com.woocommerce.android.ui.orders.wooshippinglabels.address.AddressValidationHelper
import com.woocommerce.android.ui.orders.wooshippinglabels.address.GetStatesByCountryCode
import com.woocommerce.android.ui.orders.wooshippinglabels.models.AddressNormalizationModel
import com.woocommerce.android.util.StringUtils.combineStrings
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Suppress("TooManyFunctions")
class WooShippingEditOriginViewModel @Inject constructor(
    private val addressValidator: AddressValidationHelper,
    private val getAcceptedOriginCountries: GetAcceptedOriginCountries,
    private val getStatesByCountryCode: GetStatesByCountryCode,
    private val normalizeAddress: NormalizeAddress,
    private val resourceProvider: ResourceProvider,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private var name by mutableStateOf(InputValue(""))
    private var company by mutableStateOf(InputValue(""))
    private var address by mutableStateOf(InputValue(""))
    private var city by mutableStateOf(InputValue(""))
    private var postalCode by mutableStateOf(InputValue(""))
    private var email by mutableStateOf(InputValue(""))
    private var phone by mutableStateOf(InputValue(""))

    private var country = MutableStateFlow(Location.EMPTY)

    private var rawState by mutableStateOf("")
    private val selectedState = MutableStateFlow(Location.EMPTY)

    private val state = combine(
        snapshotFlow { rawState },
        selectedState
    ) { rawState, selectedState ->
        if (selectedState != Location.EMPTY) selectedState else AmbiguousLocation.Raw(rawState).asLocation()
    }

    private val countriesState = MutableStateFlow<LocationState>(LocationState.Loading)
    private val statesState = MutableStateFlow<LocationState>(LocationState.Loading)

    private val normalizedAddressStatus =
        MutableStateFlow<NormalizedAddressStatus>(NormalizedAddressStatus.Closed)

    private val navArgs: WooShippingEditOriginAddressFragmentArgs by savedState.navArgs()

    private val nameValidatedFlow = snapshotFlow { name }
        .combine(snapshotFlow { company }) { name, company ->
            Pair(name, company)
        }.transformLatestWithDelay(
            delayMillis = DELAY_TIME_MILLIS,
        ) { inputValues ->
            inputValues.copy(
                first = inputValues.first.copy(
                    error = addressValidator.validateAtLeastOneOf(
                        inputValues.first.value,
                        inputValues.second.value
                    )
                )
            )
        }

    private val addressValidatedFlow = snapshotFlow { address }
        .transformLatestWithDelay(
            delayMillis = DELAY_TIME_MILLIS,
        ) { inputValue ->
            inputValue.copy(error = addressValidator.validateFieldRequired(inputValue.value))
        }

    private val cityValidatedFlow = snapshotFlow { city }
        .transformLatestWithDelay(
            delayMillis = DELAY_TIME_MILLIS,
        ) { inputValue ->
            inputValue.copy(error = addressValidator.validateFieldRequired(inputValue.value))
        }

    private val postalCodeValidatedFlow = snapshotFlow { postalCode }
        .transformLatestWithDelay(
            delayMillis = DELAY_TIME_MILLIS,
        ) { inputValue ->
            inputValue.copy(error = addressValidator.validateFieldRequired(inputValue.value))
        }

    private val emailValidatedFlow = snapshotFlow { email }
        .transformLatestWithDelay(
            delayMillis = DELAY_TIME_MILLIS,
        ) { inputValue ->
            inputValue.copy(error = addressValidator.validateFieldRequired(inputValue.value))
        }

    private val phoneValidatedFlow = snapshotFlow { phone }
        .transformLatestWithDelay(
            delayMillis = DELAY_TIME_MILLIS,
        ) { inputValue ->
            inputValue.copy(error = addressValidator.validateUSCustomsPhone(inputValue.value))
        }

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

    val viewState: MutableStateFlow<ViewState> = MutableStateFlow(
        ViewState(
            isCompanyExpanded = false,
            editableAddress = EditableAddress(),
            loading = LoadingState.Hidden,
            shouldDisplayLoadingCountriesError = false,
            shouldUseStatesInput = false,
            addressStatus = AddressStatus.UNVERIFIED,
            addressSelection = NormalizedAddressStatus.Closed
        )
    )

    init {
        launch { observeChanges() }
        fillAddressForm()
        launch {
            loadCountries()
            loadStates()
        }
    }

    private fun fillAddressForm() {
        val fullName = combineStrings(
            navArgs.originAddress.firstName.orEmpty(),
            navArgs.originAddress.lastName.orEmpty()
        )
        val fullAddress = combineStrings(
            navArgs.originAddress.address1.orEmpty(),
            navArgs.originAddress.address2.orEmpty()
        )
        name = InputValue(fullName)
        company = InputValue(navArgs.originAddress.company.orEmpty())
        country.value = findLocationByCode(navArgs.originAddress.country, countriesState.value)
        address = InputValue(fullAddress)
        city = InputValue(navArgs.originAddress.city.orEmpty())
        selectedState.value = findLocationByCode(navArgs.originAddress.state.orEmpty(), statesState.value)
        postalCode = InputValue(navArgs.originAddress.postcode)
        email = InputValue(navArgs.originAddress.email.orEmpty())
        phone = InputValue(navArgs.originAddress.phone.orEmpty())
        isCompanyExpanded.value = navArgs.originAddress.company.isNotNullOrEmpty()
    }

    private fun findLocationByCode(code: String, state: LocationState): Location {
        val default = AmbiguousLocation.Raw(code).asLocation()
        return when (val currentState = state) {
            is LocationState.Loaded -> {
                currentState.locations.firstOrNull { it.code == code } ?: default
            }

            else -> default
        }
    }

    private suspend fun loadCountries() {
        getAcceptedOriginCountries().fold(
            onSuccess = {
                countriesState.value = LocationState.Loaded(it)
                country.value = findLocationByCode(country.value.code, countriesState.value)
            },
            onFailure = { countriesState.value = LocationState.Error }
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun loadStates() {
        country.mapLatest { country -> getStatesByCountryCode(country.code) }
            .collectLatest { states ->
                statesState.value = LocationState.Loaded(states)
                rawState = ""
                if (states.isNotEmpty()) {
                    findLocationByCode(selectedState.value.code, statesState.value)
                        .takeIf { it != Location.EMPTY }
                        ?.let { selectedState.value = it } ?: run {
                        selectedState.value = states.first()
                    }
                } else {
                    selectedState.value = Location.EMPTY
                }
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
            normalizedAddressStatus
        ) { address, isExpanded, countriesState, statesState, addressSelection ->

            val loading =
                when {
                    countriesState is LocationState.DisplayLoading || statesState is LocationState.DisplayLoading -> {
                        LoadingState.DisplayLoading(
                            resourceProvider.getString(R.string.loading),
                            resourceProvider.getString(R.string.woo_shipping_fetching_countries_and_states)
                        )
                    }
                    addressSelection is NormalizedAddressStatus.VerifyingAddress -> {
                        LoadingState.DisplayLoading(
                            resourceProvider.getString(R.string.woo_shipping_address_validate_title),
                            resourceProvider.getString(R.string.woo_shipping_address_validate_message)
                        )
                    }
                    else -> LoadingState.Hidden
                }


            val addressStatus = when {
                hasIncorrectOrMissingData(address) -> AddressStatus.MISSING_INFO
                isSameAddress(address) && navArgs.originAddress.isVerified -> AddressStatus.VERIFIED
                else -> AddressStatus.UNVERIFIED
            }

            ViewState(
                isCompanyExpanded = isExpanded,
                editableAddress = address,
                loading = loading,
                shouldDisplayLoadingCountriesError = countriesState is LocationState.Error,
                shouldUseStatesInput = statesState is LocationState.Loaded && statesState.locations.isEmpty(),
                addressStatus = addressStatus,
                addressSelection = addressSelection
            )
        }
            .collectLatest {
                viewState.value = it
            }
    }

    private fun isSameAddress(newAddress: EditableAddress): Boolean {
        val originalAddress = navArgs.originAddress
        val originalFullAddress = combineStrings(
            originalAddress.address1.orEmpty(),
            originalAddress.address2.orEmpty()
        )

        val isSameAddress = originalFullAddress == newAddress.address.value
        val isSameCity = originalAddress.city == newAddress.city.value
        val isSameState = originalAddress.state == newAddress.state.code
        val isSameCountry = originalAddress.country == newAddress.country.code
        val isSamePostalCode = originalAddress.postcode == newAddress.postalCode.value

        return isSameAddress && isSameCity && isSameState && isSameCountry && isSamePostalCode
    }

    private fun hasIncorrectOrMissingData(editableAddress: EditableAddress): Boolean {
        return editableAddress.address.error.isNotNullOrEmpty() ||
            editableAddress.city.error.isNotNullOrEmpty() ||
            editableAddress.postalCode.error.isNotNullOrEmpty() ||
            editableAddress.email.error.isNotNullOrEmpty() ||
            editableAddress.phone.error.isNotNullOrEmpty() ||
            editableAddress.name.error.isNotNullOrEmpty() ||
            editableAddress.company.error.isNotNullOrEmpty()
    }

    fun onNameChange(value: String) {
        name = InputValue(value)
    }

    fun onCompanyChange(value: String) {
        company = InputValue(value)
    }

    fun onAddressChange(value: String) {
        address = InputValue(value)
    }

    fun onCityChange(value: String) {
        city = InputValue(value)
    }

    fun onPostalCodeChange(value: String) {
        postalCode = InputValue(value)
    }

    fun onEmailChange(value: String) {
        email = InputValue(value)
    }

    fun onPhoneChange(value: String) {
        phone = InputValue(value)
    }

    fun onRawStateChange(value: String) {
        rawState = value
    }

    fun onCountryChange() {
        when (val state = countriesState.value) {
            is LocationState.Loaded -> {
                triggerEvent(ShowCountrySelector(state.locations))
            }

            is LocationState.Loading -> {
                countriesState.value = LocationState.DisplayLoading
            }

            else -> {}
        }
    }

    fun onStateChange() {
        when (val state = statesState.value) {
            is LocationState.Loaded -> {
                triggerEvent(ShowStateSelector(state.locations))
            }

            is LocationState.Loading -> {
                countriesState.value = LocationState.DisplayLoading
            }

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
        launch {
            val address = editableAddress.toAddress()
            normalizedAddressStatus.value = NormalizedAddressStatus.VerifyingAddress
            normalizeAddress(address).fold(
                onSuccess = {
                    normalizedAddressStatus.value =
                        NormalizedAddressStatus.AddressSelection(it, it.normalizedAddress)
                },
                onFailure = {
                    normalizedAddressStatus.value = NormalizedAddressStatus.Failure
                }
            )
        }
    }

    fun onAddressSelectionChange(addressSelection: NormalizedAddressStatus.AddressSelection) {
        normalizedAddressStatus.value = addressSelection
    }

    fun onCloseAddressSelection() {
        normalizedAddressStatus.value = NormalizedAddressStatus.Closed
    }

    data class ViewState(
        val isCompanyExpanded: Boolean,
        val editableAddress: EditableAddress,
        val loading: LoadingState,
        val shouldDisplayLoadingCountriesError: Boolean,
        val shouldUseStatesInput: Boolean,
        val addressStatus: AddressStatus,
        val addressSelection: NormalizedAddressStatus
    )

    sealed class LoadingState {
        data object Hidden : LoadingState()
        data class DisplayLoading(
            val title: String,
            val message: String
        ) : LoadingState()
    }

    sealed class LocationState {
        data object Loading : LocationState()
        data object DisplayLoading : LocationState()
        data object Error : LocationState()
        data class Loaded(val locations: List<Location>) : LocationState()
    }

    data class ShowCountrySelector(
        val countries: List<Location>
    ) : MultiLiveEvent.Event()

    data class ShowStateSelector(
        val states: List<Location>
    ) : MultiLiveEvent.Event()

    companion object {
        private const val DELAY_TIME_MILLIS = 500L
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.transformLatestWithDelay(
    delayMillis: Long,
    transform: (T) -> T,
): Flow<T> = this.transformLatest { value ->
    emit(value)
    delay(delayMillis)
    emit(transform(value))
}

data class EditableAddress(
    val name: InputValue = InputValue.EMPTY,
    val company: InputValue = InputValue.EMPTY,
    val country: Location = Location.EMPTY,
    val address: InputValue = InputValue.EMPTY,
    val city: InputValue = InputValue.EMPTY,
    val state: Location = Location.EMPTY,
    val postalCode: InputValue = InputValue.EMPTY,
    val email: InputValue = InputValue.EMPTY,
    val phone: InputValue = InputValue.EMPTY
) {
    fun toAddress(): Address {
        return Address(
            firstName = name.value,
            lastName = "",
            company = company.value,
            address1 = address.value,
            address2 = "",
            city = city.value,
            state = AmbiguousLocation.Defined(state),
            postcode = postalCode.value,
            country = country,
            email = email.value,
            phone = phone.value
        )
    }
}

sealed class NormalizedAddressStatus {
    data object Closed : NormalizedAddressStatus()
    data object VerifyingAddress : NormalizedAddressStatus()
    data object Failure : NormalizedAddressStatus()
    data class AddressSelection(
        val addressNormalization: AddressNormalizationModel,
        val selectedAddress: Address
    ) : NormalizedAddressStatus()
}

enum class AddressStatus {
    VERIFIED,
    UNVERIFIED,
    MISSING_INFO
}

data class InputValue(
    val value: String,
    val error: String? = null
) {
    companion object {
        val EMPTY = InputValue("")
    }
}
