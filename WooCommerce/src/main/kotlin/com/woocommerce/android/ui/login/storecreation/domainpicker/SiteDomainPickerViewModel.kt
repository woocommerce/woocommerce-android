package com.woocommerce.android.ui.login.storecreation.domainpicker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class SiteDomainPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    private val domainQuery = savedState.getStateFlow(this, "")
    private val domainSuggestionsUi = savedState.getStateFlow(this, emptyList<DomainSuggestionUi>())

    val viewState = combine(
        domainQuery,
        domainSuggestionsUi
    ) { domainQuery, domainSuggestions ->
        // TODO using test data at the moment
        SiteDomainPickerState(
            isLoading = false,
            domain = "White Christmas Tress",
            domainSuggestionUis = listOf(
                DomainSuggestionUi("whitechristmastrees.mywc.mysite"),
                DomainSuggestionUi("whitechristmastrees.business.mywc.mysite", isSelected = true),
                DomainSuggestionUi("whitechristmastrees.business.test"),
                DomainSuggestionUi("whitechristmastrees.business.scroll"),
                DomainSuggestionUi("whitechristmastrees.business.more"),
                DomainSuggestionUi("whitechristmastrees.business.another"),
                DomainSuggestionUi("whitechristmastrees.business.any"),
                DomainSuggestionUi("whitechristmastrees.business.domain"),
                DomainSuggestionUi("whitechristmastrees.business.site"),
                DomainSuggestionUi("whitechristmastrees.business.other"),
            )
        )
    }.asLiveData()

    fun onBackPressed() {
        // TODO
    }

    fun onSkipPressed() {
        // TODO
    }

    fun onDomainChanged(query: String) {
        domainQuery.value = query
    }

    data class SiteDomainPickerState(
        val isLoading: Boolean = false,
        val domain: String = "",
        val domainSuggestionUis: List<DomainSuggestionUi> = emptyList()
    )

    data class DomainSuggestionUi(
        val domain: String = "",
        val isSelected: Boolean = false
    )
}
