package com.woocommerce.android.ui.blaze.creation.targets

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppConstants
import com.woocommerce.android.R
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.ui.blaze.Location
import com.woocommerce.android.ui.blaze.creation.targets.TargetSelectionViewState.SearchState
import com.woocommerce.android.ui.blaze.creation.targets.TargetSelectionViewState.SearchState.Hidden
import com.woocommerce.android.ui.blaze.creation.targets.TargetSelectionViewState.SearchState.Inactive
import com.woocommerce.android.ui.blaze.creation.targets.TargetSelectionViewState.SearchState.NoResults
import com.woocommerce.android.ui.blaze.creation.targets.TargetSelectionViewState.SearchState.Ready
import com.woocommerce.android.ui.blaze.creation.targets.TargetSelectionViewState.SearchState.Results
import com.woocommerce.android.ui.blaze.creation.targets.TargetSelectionViewState.SearchState.Results.SearchItem
import com.woocommerce.android.ui.blaze.creation.targets.TargetSelectionViewState.SearchState.Searching
import com.woocommerce.android.ui.blaze.creation.targets.TargetSelectionViewState.SelectionItem
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class BlazeCampaignTargetLocationSelectionViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val blazeRepository: BlazeRepository,
    savedStateHandle: SavedStateHandle
) : TargetSelectionViewModel, ScopedViewModel(savedStateHandle) {
    private val navArgs: BlazeCampaignTargetLocationSelectionFragmentArgs by savedStateHandle.navArgs()

    private val searchState = savedStateHandle.getStateFlow<SearchState>(
        scope = viewModelScope,
        initialValue = Inactive
    )
    private val items = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = navArgs.locations.map { TargetLocation(it, true) }
    )
    private val searchQuery = savedStateHandle.getStateFlow(viewModelScope, initialValue = "")

    override val viewState = combine(
        items,
        searchQuery,
        searchState
    ) { items, query, searchState ->
        TargetSelectionViewState(
            items = items.map { item -> SelectionItem(item.location.id.toString(), item.location.name) },
            selectedItems = items.filter { it.isSelected }
                .map { item -> SelectionItem(item.location.id.toString(), item.location.name) },
            title = resourceProvider.getString(R.string.blaze_campaign_preview_details_location),
            searchQuery = query,
            searchState = searchState
        )
    }.asLiveData()

    init {
        observeSearchQuery()
    }

    private suspend fun fetchLocations(query: String) = blazeRepository.fetchLocations(query)
        .getOrNull()
        ?.asSequence()
        ?.filterNot { location ->
            location.id in items.value.map { it.location.id }
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
            .onEach { query ->
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

    override fun onItemToggled(item: SelectionItem) {
        items.update { oldItems ->
            oldItems.map { oldItem ->
                if (oldItem.location.id.toString() == item.id) {
                    oldItem.copy(isSelected = !oldItem.isSelected)
                } else {
                    oldItem
                }
            }
        }
    }

    override fun onSearchItemTapped(item: SearchItem) {
        items.update { oldItems ->
            oldItems + TargetLocation(
                location = Location(item.id.toLong(), item.title),
                isSelected = true
            )
        }
        searchQuery.update { "" }
        searchState.update { Inactive }
    }

    override fun onAllButtonTapped() {
        if (items.value.all { it.isSelected }) {
            items.update { oldItems ->
                oldItems.map { it.copy(isSelected = false) }
            }
        }
    }

    override fun onBackPressed() {
        if (searchState.value is Hidden || searchState.value is Inactive) {
            triggerEvent(Exit)
        } else {
            searchState.update { Inactive }
        }
    }

    override fun onSaveTapped() {
        // Empty selection set means all items are selected
        val items = items.value.asSequence()
            .filter { it.isSelected }
            .map { it.location }
            .toList()
        triggerEvent(ExitWithResult(TargetLocationResult(items)))
    }

    override fun onSearchQueryChanged(query: String) {
        searchQuery.update { query }
    }

    override fun onSearchActiveStateChanged(isActive: Boolean) {
        when {
            isActive -> searchState.update { Ready }
            else -> searchState.update { Inactive }
        }
    }

    @Parcelize
    data class TargetLocation(
        val location: Location,
        val isSelected: Boolean
    ) : Parcelable

    @Parcelize
    data class TargetLocationResult(
        val locations: List<Location>
    ) : Parcelable
}
