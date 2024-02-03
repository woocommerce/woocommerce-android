package com.woocommerce.android.ui.blaze.creation.targets

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppConstants
import com.woocommerce.android.R
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.ui.blaze.creation.targets.BlazeCampaignTargetSelectionViewModel.SearchState.Hidden
import com.woocommerce.android.ui.blaze.creation.targets.BlazeCampaignTargetSelectionViewModel.SearchState.Inactive
import com.woocommerce.android.ui.blaze.creation.targets.BlazeCampaignTargetSelectionViewModel.SearchState.NoResults
import com.woocommerce.android.ui.blaze.creation.targets.BlazeCampaignTargetSelectionViewModel.SearchState.Ready
import com.woocommerce.android.ui.blaze.creation.targets.BlazeCampaignTargetSelectionViewModel.SearchState.Results
import com.woocommerce.android.ui.blaze.creation.targets.BlazeCampaignTargetSelectionViewModel.SearchState.Results.SearchItem
import com.woocommerce.android.ui.blaze.creation.targets.BlazeCampaignTargetSelectionViewModel.SearchState.Searching
import com.woocommerce.android.ui.blaze.creation.targets.BlazeCampaignTargetSelectionViewModel.ViewState.SelectionItem
import com.woocommerce.android.ui.blaze.creation.targets.BlazeTargetType.DEVICE
import com.woocommerce.android.ui.blaze.creation.targets.BlazeTargetType.INTEREST
import com.woocommerce.android.ui.blaze.creation.targets.BlazeTargetType.LANGUAGE
import com.woocommerce.android.ui.blaze.creation.targets.BlazeTargetType.LOCATION
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class BlazeCampaignTargetSelectionViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val blazeRepository: BlazeRepository,
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: BlazeCampaignTargetSelectionFragmentArgs by savedStateHandle.navArgs()

    private val searchState = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = if (navArgs.targetType == LOCATION) Inactive else Hidden
    )

    private val selectedIds = savedStateHandle.getStateFlow(viewModelScope, navArgs.selectedIds.toSet())
    private val foundItems = savedStateHandle.getStateFlow(viewModelScope, initialValue = emptyList<SelectionItem>())
    private val searchQuery = savedStateHandle.getStateFlow(viewModelScope, initialValue = "")

    private val items: Flow<List<SelectionItem>> = when (navArgs.targetType) {
        LANGUAGE -> blazeRepository.observeLanguages().map { languages ->
            languages.map { language ->
                SelectionItem(
                    id = language.code,
                    title = language.name
                )
            }
        }
        DEVICE -> blazeRepository.observeDevices().map { devices ->
            devices.map { device ->
                SelectionItem(
                    id = device.id,
                    title = device.name
                )
            }
        }
        INTEREST -> blazeRepository.observeInterests().map { interests ->
            interests.map { interest ->
                SelectionItem(
                    id = interest.id,
                    title = interest.description
                )
            }
        }
        LOCATION -> foundItems
    }

    private val screenTitle: String
        get() = when (navArgs.targetType) {
            LANGUAGE -> resourceProvider.getString(R.string.blaze_campaign_preview_details_language)
            DEVICE -> resourceProvider.getString(R.string.blaze_campaign_preview_details_devices)
            INTEREST -> resourceProvider.getString(R.string.blaze_campaign_preview_details_interests)
            LOCATION -> resourceProvider.getString(R.string.blaze_campaign_preview_details_location)
        }

    val viewState = combine(
        items,
        selectedIds,
        searchQuery,
        searchState
    ) { items, selectedIds, query, searchState ->
        ViewState(
            items = items,
            selectedItems = selectedIds.map { id -> items.first { it.id == id } },
            title = screenTitle,
            searchQuery = query,
            searchState = searchState
        )
    }.asLiveData()

    init {
        observeSearchQuery()
    }

    private suspend fun fetchLocations(query: String) = blazeRepository.fetchLocations(query)
        ?.asSequence()
        ?.filterNot { location ->
            location.id.toString() in foundItems.value.asSequence().map { it.id }
        }?.map { location ->
            SearchItem(
                id = location.id.toString(),
                title = location.name,
                subtitle = location.parent,
                type = location.type
            )
        }?.toList() ?: emptyList()

    private fun observeSearchQuery() {
        searchQuery
            .debounce { query -> if (query.isEmpty()) 0L else AppConstants.SEARCH_TYPING_DELAY_MS }
            .onEach{ query ->
                if (query.length > 2) {
                    searchState.update { Searching }

                    val items = fetchLocations(query)

                    searchState.update {
                        if (items.isEmpty()) {
                            NoResults
                        } else {
                            Results(items)
                        }
                    }
                } else {
                    searchState.update { Ready }
                }
            }.launchIn(viewModelScope)
    }

    fun onItemToggled(item: SelectionItem) {
        selectedIds.update { selectedIds ->
            if (selectedIds.contains(item.id)) {
                selectedIds - item.id
            } else {
                selectedIds + item.id
            }
        }
    }

    fun onSearchItemTapped(item: SearchItem) {
        foundItems.update { oldItems ->
            oldItems + SelectionItem(
                id = item.id,
                title = item.title
            )
        }
        selectedIds.update { selectedIds ->
            selectedIds + item.id
        }
        searchQuery.update { "" }
        searchState.update { Inactive }
    }

    fun onAllButtonTapped() {
        selectedIds.update {
            if (viewState.value?.searchState !is Hidden) {
                emptySet()
            } else {
                if (it.size == viewState.value?.items?.size) {
                    emptySet()
                } else {
                    viewState.value?.items?.map { item -> item.id }?.toSet() ?: emptySet()
                }
            }
        }
    }

    fun onBackPressed() {
        if (searchState.value is Hidden || searchState.value is Inactive) {
            triggerEvent(Exit)
        } else {
            searchState.update { Inactive }
        }
    }

    fun onSaveTapped() {
        // Empty selection set means all items are selected
        val result = if (selectedIds.value.size == viewState.value?.items?.size)
            emptyList()
        else
            selectedIds.value.toList()
        triggerEvent(ExitWithResult(TargetSelectionResult(navArgs.targetType, result)))
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.update { query }
    }

    fun onSearchActiveStateChanged(isActive: Boolean) {
        when {
            isActive -> searchState.update { Ready }
            else -> searchState.update { Inactive }
        }
    }

    data class ViewState(
        val items: List<SelectionItem>,
        val selectedItems: List<SelectionItem>,
        val title: String,
        val searchQuery: String,
        val searchState: SearchState
    ) {

        val isSaveButtonEnabled: Boolean
            get() {
                return if (searchState !is Hidden) {
                    true
                } else {
                    selectedItems.isNotEmpty()
                }
            }

        val isAllButtonToggled: Boolean
            get() {
                return if (searchState !is Hidden) {
                    selectedItems.isEmpty()
                } else {
                    selectedItems.size == items.size
                }
            }

        data class SelectionItem(
            val id: String,
            val title: String
        )
    }

    sealed class SearchState(
        val isVisible: Boolean = true
    ) : Parcelable {
        @Parcelize
        object Hidden : SearchState(isVisible = false)

        @Parcelize
        object Inactive : SearchState()

        @Parcelize
        object Ready : SearchState()

        @Parcelize
        object Searching : SearchState()

        @Parcelize
        object NoResults : SearchState()

        @Parcelize
        data class Results(val resultItems: List<SearchItem>) : SearchState() {
            @Parcelize
            data class SearchItem(
                val id: String,
                val title: String,
                val subtitle: String? = null,
                val type: String? = null
            ) : Parcelable
        }
    }

    @Parcelize
    data class TargetSelectionResult(
        val targetType: BlazeTargetType,
        val selectedIds: List<String>
    ) : Parcelable
}
