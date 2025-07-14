package com.woocommerce.android.ui.prefs.domain

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppConstants
import com.woocommerce.android.R
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.ui.prefs.domain.DomainSuggestionsRepository.DomainSuggestion
import com.woocommerce.android.ui.prefs.domain.DomainSuggestionsRepository.DomainSuggestion.Free
import com.woocommerce.android.ui.prefs.domain.DomainSuggestionsRepository.DomainSuggestion.Paid
import com.woocommerce.android.ui.prefs.domain.DomainSuggestionsRepository.DomainSuggestion.Premium
import com.woocommerce.android.ui.prefs.domain.DomainSuggestionsViewModel.DomainSuggestionUi.FreeWithCredit
import com.woocommerce.android.ui.prefs.domain.DomainSuggestionsViewModel.DomainSuggestionUi.OnSale
import com.woocommerce.android.ui.prefs.domain.DomainSuggestionsViewModel.LoadingState.Idle
import com.woocommerce.android.ui.prefs.domain.DomainSuggestionsViewModel.LoadingState.Loading
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.products.Product

@OptIn(FlowPreview::class)
abstract class DomainSuggestionsViewModel(
    savedStateHandle: SavedStateHandle,
    private val domainSuggestionsRepository: DomainSuggestionsRepository,
    private val currencyFormatter: CurrencyFormatter,
    initialQuery: String,
    private val searchOnlyFreeDomains: Boolean,
    private val isFreeCreditAvailable: Boolean,
    private val freeUrl: String? = null
) : ScopedViewModel(savedStateHandle) {
    companion object {
        const val KEY_IS_FREE_CREDIT_AVAILABLE = "isFreeCreditAvailable"
        const val KEY_INITIAL_QUERY = "initialQuery"
    }

    protected abstract val helpOrigin: HelpOrigin

    var domainQuery by mutableStateOf(initialQuery)
        private set

    private val loadingState = MutableStateFlow(Loading)
    private val domainSuggestions = domainSuggestionsRepository.domainSuggestions
    private val selectedDomain = MutableStateFlow<DomainSuggestion?>(null)
    private val products = domainSuggestionsRepository.products

    private val priceMap = mutableMapOf<String, String>()

    val viewState = combine(
        domainSuggestions,
        loadingState,
        selectedDomain,
        products
    ) { domainSuggestions, loadingState, selectedDomain, products ->
        DomainSearchState(
            loadingState = loadingState,
            domainSuggestionsUi = processFetchedDomainSuggestions(
                domainSuggestions,
                selectedDomain,
                products
            ),
            selectedDomain = selectedDomain?.name.orEmpty(),
            freeUrl = freeUrl
        )
    }.asLiveData()

    init {
        viewModelScope.launch {
            if (!searchOnlyFreeDomains) {
                domainSuggestionsRepository.fetchProducts()
            }

            snapshotFlow { domainQuery }
                .onEach {
                    if (it.isBlank()) {
                        domainSuggestions.value = emptyList()
                        selectedDomain.value = null
                        loadingState.value = Idle
                    } else {
                        loadingState.value = Loading
                    }
                }
                .filter { it.isNotBlank() }
                .debounce { AppConstants.SEARCH_TYPING_DELAY_MS }
                .collectLatest { query ->
                    // Make sure the loading state is correctly set after debounce too
                    loadingState.value = Loading
                    domainSuggestionsRepository.fetchDomainSuggestions(query, searchOnlyFreeDomains)
                        .onFailure {
                            triggerEvent(ShowSnackbar(R.string.domain_picker_suggestions_error))
                        }
                    loadingState.value = Idle
                }
        }

        viewModelScope.launch {
            domainSuggestions.collectLatest {
                if (selectedDomain.value == null && it.isNotEmpty()) {
                    selectedDomain.value = it.first()
                }
            }
        }
    }

    open fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onHelpPressed() {
        triggerEvent(MultiLiveEvent.Event.NavigateToHelpScreen(helpOrigin))
    }

    fun onContinueClicked() {
        navigateToNextStep(selectedDomain.value!!)
    }

    abstract fun navigateToNextStep(selectedDomain: DomainSuggestion)

    fun onDomainSuggestionSelected(domain: String) {
        selectedDomain.value = domainSuggestions.value.first { it.name == domain }
    }

    fun onDomainQueryChanged(query: String) {
        domainQuery = query
    }

    private suspend fun processFetchedDomainSuggestions(
        domainSuggestions: List<DomainSuggestion>,
        currentSelection: DomainSuggestion?,
        products: List<Product>
    ) = when {
        domainQuery.isBlank() || domainSuggestions.isEmpty() -> emptyList()
        else -> {
            domainSuggestions.mapNotNull { domain ->
                when (domain) {
                    is Free -> {
                        DomainSuggestionUi.Free(
                            isSelected = domain.name == currentSelection?.name,
                            domain = domain.name
                        )
                    }
                    is Paid -> {
                        val product = products.firstOrNull { it.productId == domain.productId }
                        if (product?.currencyCode == null) {
                            return@mapNotNull null
                        } else {
                            if (isFreeCreditAvailable && domain.cost != null) {
                                // free credit can't be used for premium domains, which don't have cost
                                FreeWithCredit(
                                    isSelected = domain.name == currentSelection?.name,
                                    domain = domain.name,
                                    price = domain.cost.format(product.currencyCode!!)
                                )
                            } else if (product.isDomainOnSale()) {
                                // if the domain is on sale, we need to show the sale price
                                OnSale(
                                    isSelected = domain.name == currentSelection?.name,
                                    domain = domain.name,
                                    price = domain.cost!!.format(product.currencyCode!!),
                                    salePrice = product.saleCost!!.format(product.currencyCode!!)
                                )
                            } else {
                                // otherwise, we show the regular price
                                DomainSuggestionUi.Paid(
                                    isSelected = domain.name == currentSelection?.name,
                                    domain = domain.name,
                                    price = domain.cost!!.format(product.currencyCode!!)
                                )
                            }
                        }
                    }
                    is Premium -> {
                        val product = products.firstOrNull { it.productId == domain.productId }
                        val price = getDomainPrice(domain)
                        if (product != null && price != null) {
                            DomainSuggestionUi.Paid(
                                isSelected = domain.name == currentSelection?.name,
                                domain = domain.name,
                                price = price.format(product.currencyCode!!)
                            )
                        } else {
                            null
                        }
                    }
                }
            }
        }
    }

    private suspend fun getDomainPrice(domain: DomainSuggestion): String? {
        if (!priceMap.containsKey(domain.name)) {
            domainSuggestionsRepository.fetchDomainPrice(domain.name).getOrNull()?.let {
                priceMap[domain.name] = it
            }
        }
        return priceMap[domain.name]
    }

    private fun Product.isDomainOnSale(): Boolean = this.saleCost?.let { it.compareTo(0.0) > 0 } == true

    private fun Double.format(currencyCode: String): String {
        return currencyFormatter.formatCurrency(this.toBigDecimal(), currencyCode)
    }

    data class DomainSearchState(
        val loadingState: LoadingState = Idle,
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
}
