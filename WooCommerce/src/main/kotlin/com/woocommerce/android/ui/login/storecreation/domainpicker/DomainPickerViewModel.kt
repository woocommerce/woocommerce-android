package com.woocommerce.android.ui.login.storecreation.domainpicker

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppConstants
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.support.help.HelpOrigin.STORE_CREATION
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.ui.login.storecreation.domainpicker.DomainPickerViewModel.LoadingState.Idle
import com.woocommerce.android.ui.login.storecreation.domainpicker.DomainPickerViewModel.LoadingState.Loading
import com.woocommerce.android.ui.login.storecreation.domainpicker.DomainSuggestionsRepository.DomainSuggestion
import com.woocommerce.android.ui.login.storecreation.domainpicker.DomainSuggestionsRepository.DomainSuggestion.Free
import com.woocommerce.android.ui.login.storecreation.domainpicker.DomainSuggestionsRepository.DomainSuggestion.Paid
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.products.Product
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class DomainPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val domainSuggestionsRepository: DomainSuggestionsRepository,
    private val newStore: NewStore,
    analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val currencyFormatter: CurrencyFormatter
) : ScopedViewModel(savedStateHandle) {
    companion object {
        const val KEY_IS_FREE_CREDIT_AVAILABLE = "key_is_free_credit_available"
        const val KEY_SEARCH_ONLY_FREE_DOMAINS = "search_only_free_domains"
    }
    private val domainQuery = savedState.getStateFlow(this, newStore.data.name ?: "")
    private val loadingState = MutableStateFlow(Idle)
    private val domainSuggestionsUi = domainSuggestionsRepository.domainSuggestions
    private val selectedDomain = MutableStateFlow("")
    private val products = domainSuggestionsRepository.products
    private val isFreeCreditAvailable: Boolean = savedStateHandle[KEY_IS_FREE_CREDIT_AVAILABLE] ?: false
    private val searchOnlyFreeDomains: Boolean = savedStateHandle[KEY_SEARCH_ONLY_FREE_DOMAINS] ?: true

    val viewState = combine(
        domainQuery,
        domainSuggestionsUi,
        loadingState,
        selectedDomain,
        products
    ) { domainQuery, domainSuggestions, loadingState, selectedDomain, products ->
        DomainPickerState(
            loadingState = loadingState,
            domainQuery = domainQuery,
            domainSuggestionsUi = processFetchedDomainSuggestions(
                domainQuery,
                domainSuggestions,
                selectedDomain,
                products
            )
        )
    }.asLiveData()

    init {
        if (searchOnlyFreeDomains) {
            analyticsTrackerWrapper.track(
                AnalyticsEvent.SITE_CREATION_STEP,
                mapOf(
                    AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_DOMAIN_PICKER
                )
            )
        }
        viewModelScope.launch {
            if (!searchOnlyFreeDomains) {
                domainSuggestionsRepository.fetchProducts()
            }

            domainQuery
                .filter { it.isNotBlank() }
                .onEach { loadingState.value = Loading }
                .debounce { AppConstants.SEARCH_TYPING_DELAY_MS }
                .collectLatest { query ->
                    // Make sure the loading state is correctly set after debounce too
                    loadingState.value = Loading
                    domainSuggestionsRepository.fetchDomainSuggestions(query, searchOnlyFreeDomains)
                        .onFailure {
                            triggerEvent(ShowSnackbar(R.string.store_creation_domain_picker_suggestions_error))
                        }
                    loadingState.value = Idle
                }
        }
    }

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onHelpPressed() {
        triggerEvent(MultiLiveEvent.Event.NavigateToHelpScreen(STORE_CREATION))
    }

    fun onContinueClicked() {
        triggerEvent(NavigateToNextStep)
    }

    fun onDomainSuggestionSelected(clickedDomain: String) {
        newStore.update(domain = clickedDomain)
        selectedDomain.value = clickedDomain
    }

    fun onDomainChanged(query: String) {
        domainQuery.value = query
    }

    private suspend fun processFetchedDomainSuggestions(
        domainQuery: String,
        domainSuggestions: List<DomainSuggestion>,
        selectedDomain: String,
        products: List<Product>
    ) = when {
        domainQuery.isBlank() || domainSuggestions.isEmpty() -> emptyList()
        else -> {
            val preSelectDomain = selectedDomain.ifBlank { domainSuggestions.first().name }
            newStore.update(domain = preSelectDomain)

            domainSuggestions.mapNotNull { domain ->
                when (domain) {
                    is Free -> {
                        DomainSuggestionUi.Free(
                            isSelected = domain.name == preSelectDomain,
                            domain = domain.name
                        )
                    }
                    is Paid -> {
                        val product = products.firstOrNull { it.productId == domain.productId }
                        val price = domain.cost ?: domainSuggestionsRepository.fetchDomainPrice(domain.name).getOrNull()
                        if (price == null || product == null || product.currencyCode == null) {
                            return@mapNotNull null
                        } else {
                            if (isFreeCreditAvailable && domain.cost != null) {
                                // free credit can't be used for premium domains, which don't have cost
                                DomainSuggestionUi.FreeWithCredit(
                                    isSelected = domain.name == preSelectDomain,
                                    domain = domain.name,
                                    price = price.format(product.currencyCode!!)
                                )
                            } else if (product.isDomainOnSale()) {
                                // if the domain is on sale, we need to show the sale price
                                DomainSuggestionUi.OnSale(
                                    isSelected = domain.name == preSelectDomain,
                                    domain = domain.name,
                                    price = price.format(product.currencyCode!!),
                                    salePrice = product.saleCost!!.format(product.currencyCode!!)
                                )
                            } else {
                                // otherwise, we show the regular price
                                DomainSuggestionUi.Paid(
                                    isSelected = domain.name == preSelectDomain,
                                    domain = domain.name,
                                    price = price.format(product.currencyCode!!)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun Product.isDomainOnSale(): Boolean = this.saleCost?.let { it.compareTo(0.0) > 0 } == true

    private fun Double.format(currencyCode: String): String {
        return currencyFormatter.formatCurrency(this.toBigDecimal(), currencyCode)
    }

    data class DomainPickerState(
        val loadingState: LoadingState = Idle,
        val domainQuery: String = "",
        val domainSuggestionsUi: List<DomainSuggestionUi> = emptyList(),
        val selectedDomain: String = "",
        val error: String? = null,
        val freeUrl: String? = null,
        @StringRes val confirmButtonTitle: Int = R.string.continue_button
    )

    sealed interface DomainSuggestionUi {
        val domain: String
        val isSelected: Boolean

        data class Free(
            override val domain: String,
            override val isSelected: Boolean = false
        ) : DomainSuggestionUi

        data class FreeWithCredit(
            override val domain: String,
            override val isSelected: Boolean = false,
            val price: String
        ) : DomainSuggestionUi

        data class Paid(
            override val domain: String,
            override val isSelected: Boolean = false,
            val price: String
        ) : DomainSuggestionUi

        data class OnSale(
            override val domain: String,
            override val isSelected: Boolean = false,
            val price: String,
            val salePrice: String
        ) : DomainSuggestionUi
    }

    enum class LoadingState {
        Idle, Loading
    }

    object NavigateToNextStep : MultiLiveEvent.Event()
}
