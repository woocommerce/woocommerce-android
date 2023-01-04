package com.woocommerce.android.ui.login.storecreation.domainpicker

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
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class DomainPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    domainSuggestionsRepository: DomainSuggestionsRepository,
    private val newStore: NewStore,
    val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {
    private val domainQuery = savedState.getStateFlow(this, newStore.data.name ?: "")
    private val loadingState = MutableStateFlow(Idle)
    private val domainSuggestionsUi = domainSuggestionsRepository.domainSuggestions
    private val selectedDomain = MutableStateFlow("")

    val viewState = combine(
        domainQuery,
        domainSuggestionsUi,
        loadingState,
        selectedDomain
    ) { domainQuery, domainSuggestions, loadingState, selectedDomain ->
        DomainPickerState(
            loadingState = loadingState,
            domainQuery = domainQuery,
            domainSuggestionsUi = processFetchedDomainSuggestions(domainQuery, domainSuggestions, selectedDomain)
        )
    }.asLiveData()

    init {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SITE_CREATION_STEP,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_DOMAIN_PICKER
            )
        )
        viewModelScope.launch {
            domainQuery
                .filter { it.isNotBlank() }
                .onEach { loadingState.value = Loading }
                .debounce { AppConstants.SEARCH_TYPING_DELAY_MS }
                .collectLatest { query ->
                    // Make sure the loading state is correctly set after debounce too
                    loadingState.value = Loading
                    domainSuggestionsRepository.fetchDomainSuggestions(query)
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

    private fun processFetchedDomainSuggestions(
        domainQuery: String,
        domainSuggestions: List<String>,
        selectedDomain: String
    ) = when {
        domainQuery.isBlank() || domainSuggestions.isEmpty() -> emptyList()
        else -> {
            val preSelectDomain = selectedDomain.ifBlank {
                domainSuggestions
                    .firstOrNull { it.substringBefore(".") == domainQuery }
                    ?: domainSuggestions.first()
            }
            newStore.update(domain = preSelectDomain)

            domainSuggestions.map { domain ->
                DomainSuggestionUi(
                    isSelected = domain == preSelectDomain,
                    domain = domain
                )
            }
        }
    }

    data class DomainPickerState(
        val loadingState: LoadingState = Idle,
        val domainQuery: String = "",
        val domainSuggestionsUi: List<DomainSuggestionUi> = emptyList(),
        val selectedDomain: String = "",
        val error: String? = null
    )

    data class DomainSuggestionUi(
        val domain: String = "",
        val isSelected: Boolean = false
    )

    enum class LoadingState {
        Idle, Loading
    }

    object NavigateToNextStep : MultiLiveEvent.Event()
}
