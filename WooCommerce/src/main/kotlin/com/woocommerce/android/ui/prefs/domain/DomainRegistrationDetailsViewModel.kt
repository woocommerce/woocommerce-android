package com.woocommerce.android.ui.prefs.domain

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.prefs.domain.DomainRegistrationDetailsViewModel.DomainContactFormModel.Companion
import com.woocommerce.android.ui.prefs.domain.DomainPhoneNumberUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.TransactionAction.FETCH_SUPPORTED_COUNTRIES
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.generated.TransactionActionBuilder
import org.wordpress.android.fluxc.model.DomainContactModel
import org.wordpress.android.fluxc.network.rest.wpcom.site.SupportedStateResponse
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.SupportedDomainCountry
import org.wordpress.android.fluxc.store.AccountStore.OnDomainContactFetched
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnDomainSupportedStatesFetched
import org.wordpress.android.fluxc.store.SiteStore.OnPrimaryDomainDesignated
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.store.TransactionsStore
import org.wordpress.android.fluxc.store.TransactionsStore.CreateShoppingCartPayload
import org.wordpress.android.fluxc.store.TransactionsStore.OnShoppingCartCreated
import org.wordpress.android.fluxc.store.TransactionsStore.OnShoppingCartRedeemed
import org.wordpress.android.fluxc.store.TransactionsStore.OnSupportedCountriesFetched
import org.wordpress.android.fluxc.store.TransactionsStore.RedeemShoppingCartError
import org.wordpress.android.fluxc.store.TransactionsStore.RedeemShoppingCartPayload
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject

const val SITE_CHECK_DELAY_MS = 5000L
const val MAX_SITE_CHECK_TRIES = 10

@HiltViewModel
class DomainRegistrationDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dispatcher: Dispatcher,
    @Suppress("unused") private val transactionsStore: TransactionsStore, // needed for events to work
    private val siteStore: SiteStore,
    private val selectedSite: SelectedSite
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: DomainRegistrationDetailsFragmentArgs by savedStateHandle.navArgs()

    private var siteCheckTries = 0

    private var supportedCountries: List<SupportedDomainCountry>? = null
    private var supportedStates: List<SupportedStateResponse>? = null

    private val _uiState = MutableLiveData(ViewState())
    val uiState: LiveData<ViewState>
        get() = _uiState

    private val _domainContactForm = MutableLiveData<DomainContactFormModel>()
    val domainContactForm: LiveData<DomainContactFormModel>
        get() = _domainContactForm

    init {
        dispatcher.register(this)
        fetchSupportedCountries()
    }

    override fun onCleared() {
        dispatcher.unregister(this)
        super.onCleared()
    }

    private fun fetchSupportedCountries() {
        _uiState.value = _uiState.value?.copy(isFormProgressIndicatorVisible = true)
        dispatcher.dispatch(TransactionActionBuilder.generateNoPayloadAction(FETCH_SUPPORTED_COUNTRIES))
    }

    private fun finishRegistration(isSuccess: Boolean) {
        _uiState.value = uiState.value?.copy(isRegistrationProgressIndicatorVisible = false)

        if (isSuccess) {
            triggerEvent(NavigateToPurchaseSuccessScreen(navArgs.domainProductDetails.domainName))
        } else {
            triggerEvent(NavigateToDomainDashboard)
        }
    }

    fun onCountrySelectorClicked() {
        supportedCountries?.let { triggerEvent(ShowCountryPickerDialog(it)) }
    }

    fun onStateSelectorClicked() {
        supportedStates?.let { triggerEvent(ShowStatePickerDialog(it)) }
    }

    fun onRegisterDomainButtonClicked() {
        _uiState.value = uiState.value?.copy(isRegistrationProgressIndicatorVisible = true)
        _domainContactForm.value = _domainContactForm.value?.copy(
            countryCode = uiState.value?.selectedCountry?.code,
            state = uiState.value?.selectedState?.code
        )
        dispatcher.dispatch(
            TransactionActionBuilder.newCreateShoppingCartAction(
                CreateShoppingCartPayload(
                    selectedSite.get(),
                    navArgs.domainProductDetails.productId,
                    navArgs.domainProductDetails.domainName,
                    uiState.value?.isPrivacyProtectionEnabled!!
                )
            )
        )
    }

    fun onCountrySelected(country: SupportedDomainCountry) {
        if (country != uiState.value?.selectedCountry) {
            supportedStates = null
            _uiState.value =
                uiState.value?.copy(
                    selectedCountry = country,
                    selectedState = null,
                    isStateProgressIndicatorVisible = true,
                    isDomainRegistrationButtonEnabled = false,
                    isStateInputEnabled = false
                )

            _domainContactForm.value = _domainContactForm.value?.copy(
                countryCode = country.code,
                state = null,
                phoneNumberPrefix = DomainPhoneNumberUtils.getPhoneNumberPrefix(country.code)
            )
            dispatcher.dispatch(SiteActionBuilder.newFetchDomainSupportedStatesAction(country.code))
        }
    }

    fun onStateSelected(state: SupportedStateResponse) {
        _uiState.value = uiState.value?.copy(selectedState = state)
    }

    fun onTosLinkClicked() {
        triggerEvent(ShowTermsOfService)
    }

    fun onDomainContactDetailsChanged(form: DomainContactFormModel) {
        val isFormBusy = uiState.value?.isFormProgressIndicatorVisible == true ||
                uiState.value?.isRegistrationProgressIndicatorVisible == true

        if (!isFormBusy) {
            _domainContactForm.value = form
        }
    }

    fun togglePrivacyProtection(isEnabled: Boolean) {
        _uiState.value = uiState.value?.copy(isPrivacyProtectionEnabled = isEnabled)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSupportedCountriesFetched(event: OnSupportedCountriesFetched) {
        if (event.isError) {
            _uiState.value = _uiState.value?.copy(isFormProgressIndicatorVisible = false)
            event.error?.message?.let { triggerEvent(ShowErrorMessage(it)) }
            AppLog.e(T.DOMAIN_REGISTRATION, "An error occurred while fetching supported countries")
        } else {
            supportedCountries = event.countries?.toCollection(ArrayList())
            dispatcher.dispatch(AccountActionBuilder.newFetchDomainContactAction())
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDomainContactFetched(event: OnDomainContactFetched) {
        if (event.isError) {
            _uiState.value = _uiState.value?.copy(isFormProgressIndicatorVisible = false)
            event.error?.message?.let { triggerEvent(ShowErrorMessage(it)) }
            AppLog.e(T.DOMAIN_REGISTRATION, "An error occurred while fetching domain contact details")
        } else {
            _domainContactForm.value = Companion.fromDomainContactModel(event.contactModel)
            _uiState.value = _uiState.value?.copy(isFormProgressIndicatorVisible = false)

            val countryCode = event.contactModel?.countryCode

            if (event.contactModel != null && !TextUtils.isEmpty(countryCode)) {
                _uiState.value =
                    uiState.value?.copy(
                        selectedCountry = supportedCountries?.firstOrNull {
                            it.code == event.contactModel?.countryCode
                        },
                        isStateProgressIndicatorVisible = true,
                        isDomainRegistrationButtonEnabled = false
                    )

                // if customer does not have a phone number we will try to prefill a country code
                if (TextUtils.isEmpty(event.contactModel?.phone)) {
                    val countryCodePrefix = DomainPhoneNumberUtils.getPhoneNumberPrefix(countryCode!!)
                    _domainContactForm.value = _domainContactForm.value?.copy(
                        phoneNumberPrefix = countryCodePrefix
                    )
                }

                dispatcher.dispatch(
                    SiteActionBuilder.newFetchDomainSupportedStatesAction(event.contactModel?.countryCode)
                )
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDomainSupportedStatesFetched(event: OnDomainSupportedStatesFetched) {
        if (event.isError) {
            _uiState.value =
                uiState.value?.copy(
                    isStateProgressIndicatorVisible = false,
                    isDomainRegistrationButtonEnabled = true
                )
            event.error?.message?.let { triggerEvent(ShowErrorMessage(it)) }
            AppLog.e(T.DOMAIN_REGISTRATION, "An error occurred while fetching supported countries")
        } else {
            _uiState.value = uiState.value?.copy(
                selectedState = event.supportedStates?.firstOrNull { it.code == domainContactForm.value?.state },
                isStateProgressIndicatorVisible = false,
                isDomainRegistrationButtonEnabled = true,
                isStateInputEnabled = !event.supportedStates.isNullOrEmpty()
            )
            supportedStates = event.supportedStates
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onShoppingCartCreated(event: OnShoppingCartCreated) {
        if (event.isError) {
            _uiState.value = uiState.value?.copy(isRegistrationProgressIndicatorVisible = false)
            AppLog.e(
                T.DOMAIN_REGISTRATION,
                "An error occurred while creating a shopping cart : " + event.error.message
            )
            triggerEvent(ShowErrorMessage(event.error.message))
            return
        }

        dispatcher.dispatch(
            TransactionActionBuilder.newRedeemCartWithCreditsAction(
                RedeemShoppingCartPayload(
                    event.cartDetails!!,
                    Companion.toDomainContactModel(domainContactForm.value)!!
                )
            )
        )
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onCartRedeemed(event: OnShoppingCartRedeemed) {
        if (event.isError) {
            _uiState.value = uiState.value?.copy(isRegistrationProgressIndicatorVisible = false)
            triggerEvent(ShowFormValidationError(event.error))
            triggerEvent(ShowErrorMessage(event.error.message))
            AppLog.e(
                T.DOMAIN_REGISTRATION,
                "An error occurred while redeeming a shopping cart : " + event.error.type +
                    " " + event.error.message
            )
            return
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPrimaryDomainDesignated(event: OnPrimaryDomainDesignated) {
        if (event.isError) { // in case of error we notify used and proceed to next step
            event.error?.message?.let { triggerEvent(ShowErrorMessage(it)) }
            AppLog.e(
                T.DOMAIN_REGISTRATION,
                "An error occurred while redeeming a shopping cart : " + event.error.type +
                    " " + event.error.message
            )
        }

        dispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(selectedSite.get()))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSiteChanged(event: OnSiteChanged) {
        if (event.isError) {
            AppLog.e(
                T.DOMAIN_REGISTRATION,
                "An error occurred while updating site details : " + event.error.message
            )
            event.error?.message?.let { triggerEvent(ShowErrorMessage(it)) }
            finishRegistration(isSuccess = false)
            return
        }

        val updatedSite = siteStore.getSiteByLocalId(selectedSite.get().id)

        // New domain is not is not reflected in SiteModel yet, try refreshing a site until we get it
        if (updatedSite?.url?.endsWith(".wordpress.com") == true && siteCheckTries < MAX_SITE_CHECK_TRIES) {
            AppLog.v(
                T.DOMAIN_REGISTRATION,
                "Newly registered domain is still not reflected in site model. Refreshing site model..."
            )
            launch {
                delay(SITE_CHECK_DELAY_MS)
                dispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(selectedSite.get()))
                siteCheckTries++
            }
        } else {
            // Everything looks good! Let's wait a bit before moving on
            launch {
                AppLog.v(T.DOMAIN_REGISTRATION, "Finishing registration...")
                delay(SITE_CHECK_DELAY_MS)
                finishRegistration(isSuccess = true)
            }
        }
    }

    data class DomainContactFormModel(
        val firstName: String?,
        val lastName: String?,
        val organization: String?,
        val addressLine1: String?,
        val addressLine2: String?,
        val postalCode: String?,
        val city: String?,
        val state: String?,
        val countryCode: String?,
        val email: String?,
        val phoneNumberPrefix: String?,
        val phoneNumber: String?
    ) {
        companion object {
            fun toDomainContactModel(domainContactFormModel: DomainContactFormModel?): DomainContactModel? {
                if (domainContactFormModel == null) {
                    return null
                }

                return DomainContactModel(
                    firstName = domainContactFormModel.firstName,
                    lastName = domainContactFormModel.lastName,
                    organization = domainContactFormModel.organization,
                    addressLine1 = domainContactFormModel.addressLine1,
                    addressLine2 = domainContactFormModel.addressLine2,
                    postalCode = domainContactFormModel.postalCode,
                    city = domainContactFormModel.city,
                    state = domainContactFormModel.state,
                    countryCode = domainContactFormModel.countryCode,
                    email = domainContactFormModel.email,
                    phone = DomainPhoneNumberUtils.formatPhoneNumberandPrefix(
                        domainContactFormModel.phoneNumberPrefix,
                        domainContactFormModel.phoneNumber
                    ),
                    fax = null
                )
            }

            fun fromDomainContactModel(domainContactModel: DomainContactModel?): DomainContactFormModel? {
                if (domainContactModel == null) {
                    return null
                }

                return DomainContactFormModel(
                    firstName = domainContactModel.firstName,
                    lastName = domainContactModel.lastName,
                    organization = domainContactModel.organization,
                    addressLine1 = domainContactModel.addressLine1,
                    addressLine2 = domainContactModel.addressLine2,
                    postalCode = domainContactModel.postalCode,
                    city = domainContactModel.city,
                    state = domainContactModel.state,
                    countryCode = domainContactModel.countryCode,
                    email = domainContactModel.email,
                    phoneNumberPrefix = DomainPhoneNumberUtils.getPhoneNumberPrefixFromFullPhoneNumber(
                        domainContactModel.phone
                    ),
                    phoneNumber = DomainPhoneNumberUtils.getPhoneNumberWithoutPrefix(domainContactModel.phone)
                )
            }
        }
    }

    data class ViewState(
        val isFormProgressIndicatorVisible: Boolean = false,
        val isStateProgressIndicatorVisible: Boolean = false,
        val isRegistrationProgressIndicatorVisible: Boolean = false,
        val isDomainRegistrationButtonEnabled: Boolean = false,
        val isPrivacyProtectionEnabled: Boolean = true,
        val selectedState: SupportedStateResponse? = null,
        val selectedCountry: SupportedDomainCountry? = null,
        val isStateInputEnabled: Boolean = false
    )

    data class ShowErrorMessage(val message: String) : MultiLiveEvent.Event()
    data class ShowFormValidationError(val error: RedeemShoppingCartError) : MultiLiveEvent.Event()
    data class ShowCountryPickerDialog(val countries: List<SupportedDomainCountry>) : MultiLiveEvent.Event()
    data class ShowStatePickerDialog(val states: List<SupportedStateResponse>) : MultiLiveEvent.Event()
    data class NavigateToPurchaseSuccessScreen(val domain: String) : MultiLiveEvent.Event()
    object NavigateToDomainDashboard : MultiLiveEvent.Event()
    object ShowTermsOfService : MultiLiveEvent.Event()
}
