package com.woocommerce.android.ui.login.storecreation.domainpicker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SiteDomainPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    private val domainQuery = savedState.getStateFlow(this, "")
    private val domainSuggestionsUi = savedState.getStateFlow(
        this,
        listOf(
            DomainSuggestionUi("whitechristmastrees.mywc.mysite"),
            DomainSuggestionUi("whitechristmastrees.business.mywc.mysite"),
            DomainSuggestionUi("whitechristmastrees.business.scroll"),
            DomainSuggestionUi("whitechristmastrees.business.more"),
            DomainSuggestionUi("whitechristmastrees.business.another"),
            DomainSuggestionUi("whitechristmastrees.business.any"),
            DomainSuggestionUi("whitechristmastrees.business.domain"),
            DomainSuggestionUi("whitechristmastrees.business.site"),
            DomainSuggestionUi("whitechristmastrees.business.other"),
            DomainSuggestionUi("whitechristmastrees.business.more"),
            DomainSuggestionUi("whitechristmastrees.business.more"),
            DomainSuggestionUi("whitechristmastrees.business.more"),
            DomainSuggestionUi("whitechristmastrees.business.more"),
            DomainSuggestionUi("whitechristmastrees.business.more"),
            DomainSuggestionUi("whitechristmastrees.business.more"),
            DomainSuggestionUi("whitechristmastrees.business.last"),
        )
    )

    val viewState = combine(
        domainQuery,
        domainSuggestionsUi
    ) { domainQuery, domainSuggestions ->
        SiteDomainPickerState(
            isLoading = false,
            domain = domainQuery,
            domainSuggestionsUi = domainSuggestions
        )
    }.asLiveData()

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onSkipPressed() {
        // TODO
    }

    fun onContinueClicked() {
        // TODO
    }

    fun onDomainSuggestionSelected(selectedIndex: Int) {
        domainSuggestionsUi.update {
            domainSuggestionsUi.value
                .mapIndexed { index, domain ->
                    if (index == selectedIndex) {
                        domain.copy(isSelected = true)
                    } else domain.copy(isSelected = false)
                }
        }
    }

    fun onDomainChanged(query: String) {
        domainQuery.value = query
    }

    data class SiteDomainPickerState(
        val isLoading: Boolean = false,
        val domain: String = "",
        val domainSuggestionsUi: List<DomainSuggestionUi> = emptyList()
    )

    data class DomainSuggestionUi(
        val domain: String = "",
        val isSelected: Boolean = false
    )
}
