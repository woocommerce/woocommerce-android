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
    private val domainSuggestions = savedState.getStateFlow(this, emptyList<DomainSuggestion>())

    val viewState = combine(
        domainQuery,
        domainSuggestions
    ) { domainQuery, domainSuggestions ->
        SiteDomainPickerState(
            isLoading = false,
            domain = domainQuery,
            domainSuggestions = domainSuggestions
        )
    }.asLiveData()

    fun onBackPressed() {

    }

    fun onDomainChanged(query: String) {
        domainQuery.value = query
    }

    data class SiteDomainPickerState(
        val isLoading: Boolean = false,
        val domain: String = "",
        val domainSuggestions: List<DomainSuggestion> = emptyList()
    )

    data class DomainSuggestion(
        val isSelected: Boolean = false,
        val domain: String = ""
    )
}
