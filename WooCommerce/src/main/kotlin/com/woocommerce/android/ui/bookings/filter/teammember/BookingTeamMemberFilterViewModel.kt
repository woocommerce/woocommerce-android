package com.woocommerce.android.ui.bookings.filter.teammember

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.bookings.BookingsRepository
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
        launch {
            bookingsRepository.observeResources().distinctUntilChanged().collect { resources ->
                _uiState.update { current -> current.copy(teamMembers = listOf(TeamMember.any) + resources) }
            }
        }
        launch { bookingsRepository.fetchResources() }
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
