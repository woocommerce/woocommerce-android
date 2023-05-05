package com.woocommerce.android.ui.login.storecreation.profiler

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

abstract class BaseStoreProfilerViewModel(
    savedStateHandle: SavedStateHandle,
    private val newStore: NewStore,
) : ScopedViewModel(savedStateHandle) {
    abstract val hasSearchableContent: Boolean
    protected val profilerOptions = MutableStateFlow(emptyList<StoreProfilerOptionUi>())

    val isLoading = MutableStateFlow(false)
    val searchQuery = MutableStateFlow("")
    val storeProfilerState: LiveData<StoreProfilerState> =
        combine(
            profilerOptions,
            isLoading,
            searchQuery
        ) { options, isLoading, searchQuery ->
            StoreProfilerState(
                storeName = newStore.data.name ?: "",
                title = getProfilerStepTitle(),
                description = getProfilerStepDescription(),
                options = when {
                    searchQuery.isBlank() -> options
                    else -> options.filter { it.name.contains(searchQuery, ignoreCase = true) }
                },
                isLoading = isLoading,
                isSearchableContent = hasSearchableContent,
                searchQuery = searchQuery,
            )
        }.asLiveData()

    protected abstract fun getProfilerStepDescription(): String

    protected abstract fun getProfilerStepTitle(): String

    abstract fun onContinueClicked()

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    open fun onSkipPressed() {
        triggerEvent(NavigateToNextStep)
    }

    fun onArrowBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    open fun onOptionSelected(option: StoreProfilerOptionUi) {
        profilerOptions.update { currentOptions ->
            currentOptions.map {
                if (option.name == it.name) it.copy(isSelected = true)
                else it.copy(isSelected = false)
            }
        }
    }

    fun onHelpPressed() {
        triggerEvent(MultiLiveEvent.Event.NavigateToHelpScreen(HelpOrigin.STORE_CREATION))
    }

    data class StoreProfilerState(
        val storeName: String,
        val title: String,
        val description: String,
        val options: List<StoreProfilerOptionUi> = emptyList(),
        val isLoading: Boolean,
        val isSearchableContent: Boolean,
        val searchQuery: String
    )

    data class StoreProfilerOptionUi(
        val name: String,
        val key: String,
        val isSelected: Boolean,
    )

    object NavigateToNextStep : MultiLiveEvent.Event()
    object NavigateToEcommercePlatformsStep : MultiLiveEvent.Event()
}
