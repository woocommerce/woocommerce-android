package com.woocommerce.android.ui.bookings.filter.teammember

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption.TeamMembers

@HiltViewModel(assistedFactory = BookingTeamMemberFilterViewModel.Factory::class)
class BookingTeamMemberFilterViewModel @AssistedInject constructor(
    @Assisted private val initialMembers: TeamMembers?,
    @Assisted private val onFilterChanged: (TeamMembers) -> Unit,
    @Assisted private val selectionMode: SelectionMode,
    @Assisted private val filterByProductId: Long?,
    private val bookingsRepository: BookingsRepository,
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    private val _uiState = MutableStateFlow(
        BookingTeamMemberFilterUiState(
            selectedMembers = initialMembers ?: TeamMembers.DEFAULT,
            onTeamMemberSelected = ::onTeamMemberSelected,
        )
    )
    val uiState: LiveData<BookingTeamMemberFilterUiState> = _uiState.asLiveData()

    init {
        // First getting from database
        launch {
            bookingsRepository.observeResources()
                .distinctUntilChanged()
                .map { resources ->
                    if (filterByProductId != null) {
                        resources.filter { it.productBookingIds?.contains(filterByProductId) == true }
                    } else {
                        resources
                    }
                }
                .collect { resources ->
                    val members = when (selectionMode) {
                        SelectionMode.SINGLE -> resources
                        SelectionMode.MULTI -> listOf(TeamMember.any) + resources
                    }
                    _uiState.update { it.copy(teamMembers = members) }
                }
        }

        // Then fetching from server
        launch {
            _uiState.update { current -> current.copy(isLoading = true) }
            bookingsRepository.fetchResources()
                .onSuccess {
                    _uiState.update { current -> current.copy(isLoading = false) }
                }
                .onFailure {
                    _uiState.update { current -> current.copy(isLoading = false) }
                    triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.bookings_resources_fetch_error))
                }
        }
    }

    private fun onTeamMemberSelected(member: TeamMember?) {
        val newSelectedMembersState = when (selectionMode) {
            SelectionMode.SINGLE -> member?.let { TeamMembers(setOf(it.id)) } ?: TeamMembers.DEFAULT
            SelectionMode.MULTI -> toggleMultiSelection(member)
        }

        _uiState.update { it.copy(selectedMembers = newSelectedMembersState) }
        onFilterChanged(newSelectedMembersState)
    }

    private fun toggleMultiSelection(member: TeamMember?): TeamMembers {
        if (member == TeamMember.any) return TeamMembers.DEFAULT
        val memberSet = _uiState.value.selectedMembers.values.toMutableSet()
        if (memberSet.contains(member?.id)) {
            memberSet.remove(member?.id)
        } else {
            member?.let { memberSet.add(it.id) }
        }
        return TeamMembers(memberSet)
    }

    enum class SelectionMode { SINGLE, MULTI }

    @AssistedFactory
    interface Factory {
        fun create(
            initial: TeamMembers?,
            onFilterChanged: (TeamMembers) -> Unit,
            selectionMode: SelectionMode = SelectionMode.MULTI,
            filterByProductId: Long? = null,
        ): BookingTeamMemberFilterViewModel
    }
}
