package com.woocommerce.android.ui.bookings.filter.teammember

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import org.wordpress.android.fluxc.persistence.entity.BookingResourceEntity

@OptIn(ExperimentalCoroutinesApi::class)
class BookingTeamMemberFilterViewModelTest : BaseUnitTest() {

    private fun createViewModel(
        initialMembers: BookingsFilterOption.TeamMembers? = null,
        bookingsRepository: BookingsRepository? = null
    ): BookingTeamMemberFilterViewModel {
        val repository = bookingsRepository ?: mock<BookingsRepository> {
            on { observeResources() } doReturn flowOf(emptyList())
            onBlocking { fetchResources() } doReturn Result.success(Unit)
        }

        return BookingTeamMemberFilterViewModel(
            initialMembers = initialMembers,
            onFilterChanged = {},
            bookingsRepository = repository,
            savedStateHandle = SavedStateHandle()
        )
    }

    private fun member(id: Long, name: String = "Member $id"): TeamMember =
        BookingResourceEntity(
            id = LocalOrRemoteId.RemoteId(id),
            localSiteId = LocalOrRemoteId.LocalId(1),
            name = name,
            qty = 1,
            role = null,
            email = null,
            phoneNumber = null,
            imageId = 0,
            imageUrl = null,
            description = null
        )

    @Test
    fun `given two members selected, when any is selected, then selection becomes DEFAULT (empty)`() = testBlocking {
        // Given
        val m1 = member(1)
        val m2 = member(2)
        val initial = BookingsFilterOption.TeamMembers(setOf(m1.id, m2.id))
        val vm = createViewModel(initial)

        // When
        vm.uiState.value.onTeamMemberSelected(TeamMember.any)

        // Then
        assertThat(vm.uiState.value.selectedMembers).isEqualTo(BookingsFilterOption.TeamMembers.DEFAULT)
    }

    @Test
    fun `given none selected, when a member is selected, then it appears in uiState selectedMembers`() = testBlocking {
        // Given
        val m1 = member(10)
        val vm = createViewModel(BookingsFilterOption.TeamMembers.DEFAULT)

        // When
        vm.uiState.value.onTeamMemberSelected(m1)

        // Then
        val ids = vm.uiState.value.selectedMembers.values
        assertThat(ids).containsExactly(m1.id)
    }

    @Test
    fun `given a member selected, when same member is selected again, then it becomes unselected`() = testBlocking {
        // Given
        val m1 = member(42)
        val initial = BookingsFilterOption.TeamMembers(setOf(m1.id))
        val vm = createViewModel(initial)

        // When
        vm.uiState.value.onTeamMemberSelected(m1)

        // Then
        assertThat(vm.uiState.value.selectedMembers).isEqualTo(BookingsFilterOption.TeamMembers.DEFAULT)
    }

    @Test
    fun `when screen opens, then loading is true and after first non-empty data loading becomes false`() =
        testBlocking {
            // Given a repository that first emits empty list (loading), then we push non-empty
            val resourcesFlow = MutableStateFlow<List<BookingResourceEntity>>(emptyList())
            val bookingsRepository = mock<BookingsRepository> {
                on { observeResources() } doReturn resourcesFlow
                onBlocking { fetchResources() } doAnswer {
                    Thread.sleep(50)
                    Result.success(Unit)
                }
            }

            val vm = createViewModel(BookingsFilterOption.TeamMembers.DEFAULT, bookingsRepository)

            assertTrue(vm.uiState.value.isLoading)

            resourcesFlow.value = listOf(member(1))

            assertThat(vm.uiState.value.isLoading).isFalse()
        }

    @Test
    fun `when resources fetch returns error, then snackbar event is emitted`() = testBlocking {
        // Given an error from REST
        val resourcesFlow = MutableStateFlow<List<BookingResourceEntity>>(emptyList())
        val bookingsRepository = mock<BookingsRepository> {
            on { observeResources() } doReturn resourcesFlow
            onBlocking { fetchResources() } doReturn Result.failure(Exception("boom"))
        }

        val vm = createViewModel(BookingsFilterOption.TeamMembers.DEFAULT, bookingsRepository)

        val event = vm.event.value
        assertThat(event).isInstanceOf(com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar::class.java)
    }
}
