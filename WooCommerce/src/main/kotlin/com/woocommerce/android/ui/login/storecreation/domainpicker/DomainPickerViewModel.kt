package com.woocommerce.android.ui.login.storecreation.domainpicker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppConstants
import com.woocommerce.android.ui.login.storecreation.domainpicker.DomainPickerViewModel.LoadingState.Idle
import com.woocommerce.android.ui.login.storecreation.domainpicker.DomainPickerViewModel.LoadingState.Loading
import com.woocommerce.android.viewmodel.MultiLiveEvent
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
    domainSuggestionsRepository: DomainSuggestionsRepository
) : ScopedViewModel(savedStateHandle) {
    private val domainQuery = savedState.getStateFlow(this, "")
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
            domain = domainQuery,
            domainSuggestionsUi =
            if (domainQuery.isBlank()) {
                emptyList()
            } else {
                domainSuggestions.map { domain ->
                    DomainSuggestionUi(
                        isSelected = domain == selectedDomain,
                        domain = domain
                    )
                }
            }
        )
    }.asLiveData()

    init {
        viewModelScope.launch {
            domainQuery
                .filter { it.isNotBlank() }
                .onEach { loadingState.value = Loading }
                .debounce { AppConstants.SEARCH_TYPING_DELAY_MS }
                .collectLatest {
                    // Make sure the loading state is correctly set after debounce too
                    loadingState.value = Loading
                    domainSuggestionsRepository.fetchDomainSuggestions(domainQuery.value)
                        .onFailure {
                            // TODO handle error cases
                        }
                    loadingState.value = Idle
                }
        }
    }

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onSkipPressed() {
        triggerEvent(NavigateToNextStep)
    }

    fun onContinueClicked() {
        triggerEvent(NavigateToNextStep)
    }

    fun onDomainSuggestionSelected(clickedDomain: String) {
        selectedDomain.value = clickedDomain
    }

    fun onDomainChanged(query: String) {
        domainQuery.value = query
    }

    data class DomainPickerState(
        val loadingState: LoadingState = Idle,
        val domain: String = "",
        val domainSuggestionsUi: List<DomainSuggestionUi> = emptyList()
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
