package com.woocommerce.android.ui.bookings.filter.teammember

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption.TeamMembers

@HiltViewModel(assistedFactory = BookingTeamMemberFilterViewModel.Factory::class)
class BookingTeamMemberFilterViewModel @AssistedInject constructor(
    @Assisted private val initialMembers: TeamMembers?,
    @Assisted private val onFilterChanged: (TeamMembers) -> Unit,
    private val bookingsRepository: BookingsRepository,
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    private val _uiState = MutableStateFlow(
        BookingTeamMemberFilterUiState(
            selectedMembers = initialMembers ?: TeamMembers.DEFAULT,
            onTeamMemberSelected = ::onTeamMemberSelected,
        )
    )
    val uiState: StateFlow<BookingTeamMemberFilterUiState> = _uiState

    init {
        // First getting from database
        launch {
            bookingsRepository.observeResources().distinctUntilChanged().collect { resources ->
                var newUiStateValue = _uiState.value.copy(teamMembers = listOf(TeamMember.any) + resources)
                if (resources.isNotEmpty()) {
                    newUiStateValue = newUiStateValue.copy(isLoading = false)
                }
                _uiState.update { newUiStateValue }
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
        val newSelectedMembersState = if (member == TeamMember.any) {
            TeamMembers.DEFAULT
        } else {
            val memberSet = _uiState.value.selectedMembers.values.toMutableSet()
            if (memberSet.contains(member?.id)) {
                memberSet.remove(member?.id)
            } else {
                member?.let { memberSet.add(it.id) }
            }
            TeamMembers(memberSet)
        }

        _uiState.update { it.copy(selectedMembers = newSelectedMembersState) }
        onFilterChanged(newSelectedMembersState)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            initial: TeamMembers?,
            onFilterChanged: (TeamMembers) -> Unit
        ): BookingTeamMemberFilterViewModel
    }
}
