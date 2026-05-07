package com.woocommerce.android.ui.bookings.filter.teammember

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import org.wordpress.android.fluxc.persistence.entity.BookingResourceEntity

@OptIn(ExperimentalCoroutinesApi::class)
class BookingTeamMemberFilterViewModelTest : BaseUnitTest() {

    private fun createViewModel(
        initialMembers: BookingsFilterOption.TeamMembers? = null,
        bookingsRepository: BookingsRepository? = null,
        selectionMode: BookingTeamMemberFilterViewModel.SelectionMode =
            BookingTeamMemberFilterViewModel.SelectionMode.MULTI,
        filterByProductId: Long? = null,
    ): BookingTeamMemberFilterViewModel {
        val repository = bookingsRepository ?: mock<BookingsRepository> {
            on { observeResources() } doReturn flowOf(emptyList())
            on { fetchResources() } doReturn Result.success(Unit)
        }

        return BookingTeamMemberFilterViewModel(
            initialMembers = initialMembers,
            onFilterChanged = {},
            selectionMode = selectionMode,
            filterByProductId = filterByProductId,
            bookingsRepository = repository,
            savedStateHandle = SavedStateHandle()
        ).apply {
            uiState.observeForever { }
        }
    }

    private fun member(
        id: Long,
        name: String = "Member $id",
        productBookingIds: List<Long>? = null,
    ): TeamMember =
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
            description = null,
            productBookingIds = productBookingIds,
        )

    @Test
    fun `given two members selected, when any is selected, then selection becomes DEFAULT (empty)`() = testBlocking {
        // Given
        val m1 = member(1)
        val m2 = member(2)
        val initial = BookingsFilterOption.TeamMembers(setOf(m1.id, m2.id))
        val vm = createViewModel(initial)

        // When
        vm.uiState.value?.onTeamMemberSelected(TeamMember.any)

        // Then
        assertThat(vm.uiState.value?.selectedMembers).isEqualTo(BookingsFilterOption.TeamMembers.DEFAULT)
    }

    @Test
    fun `given none selected, when a member is selected, then it appears in uiState selectedMembers`() = testBlocking {
        // Given
        val m1 = member(10)
        val vm = createViewModel(BookingsFilterOption.TeamMembers.DEFAULT)

        // When
        vm.uiState.value?.onTeamMemberSelected(m1)

        // Then
        val ids = vm.uiState.value?.selectedMembers?.values
        assertThat(ids).containsExactly(m1.id)
    }

    @Test
    fun `given a member selected, when same member is selected again, then it becomes unselected`() = testBlocking {
        // Given
        val m1 = member(42)
        val initial = BookingsFilterOption.TeamMembers(setOf(m1.id))
        val vm = createViewModel(initial)

        // When
        vm.uiState.value?.onTeamMemberSelected(m1)

        // Then
        assertThat(vm.uiState.value?.selectedMembers).isEqualTo(BookingsFilterOption.TeamMembers.DEFAULT)
    }

    @Test
    fun `when screen opens, then loading is true and after first non-empty data loading becomes false`() =
        testBlocking {
            // Given a repository that first emits empty list (loading), then we push non-empty
            val resourcesFlow = MutableStateFlow<List<BookingResourceEntity>>(emptyList())
            val fetchDeferred = CompletableDeferred<Result<Unit>>()
            val bookingsRepository = mock<BookingsRepository> {
                on { observeResources() } doReturn resourcesFlow
                on { fetchResources() } doSuspendableAnswer { fetchDeferred.await() }
            }

            val vm = createViewModel(BookingsFilterOption.TeamMembers.DEFAULT, bookingsRepository)

            assertThat(vm.uiState.value?.skeletonVisible).isTrue()

            resourcesFlow.value = listOf(member(1))
            fetchDeferred.complete(Result.success(Unit))

            assertThat(vm.uiState.value?.skeletonVisible).isFalse()
        }

    @Test
    fun `when resources fetch returns error, then snackbar event is emitted`() = testBlocking {
        // Given an error from REST
        val resourcesFlow = MutableStateFlow<List<BookingResourceEntity>>(emptyList())
        val bookingsRepository = mock<BookingsRepository> {
            on { observeResources() } doReturn resourcesFlow
            on { fetchResources() } doReturn Result.failure(Exception("boom"))
        }

        val vm = createViewModel(BookingsFilterOption.TeamMembers.DEFAULT, bookingsRepository)

        val event = vm.event.value
        assertThat(event).isInstanceOf(com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar::class.java)
    }

    @Test
    fun `given single mode, when member selected, then it replaces previous selection`() = testBlocking {
        // GIVEN
        val m1 = member(1)
        val m2 = member(2)
        val initial = BookingsFilterOption.TeamMembers(setOf(m1.id))
        val vm = createViewModel(
            initialMembers = initial,
            selectionMode = BookingTeamMemberFilterViewModel.SelectionMode.SINGLE,
        )

        // WHEN
        vm.uiState.value?.onTeamMemberSelected(m2)

        // THEN
        val ids = vm.uiState.value?.selectedMembers?.values
        assertThat(ids).containsExactly(m2.id)
    }

    @Test
    fun `given single mode, when resources loaded, then no Any option is shown`() = testBlocking {
        // GIVEN
        val m1 = member(1)
        val resourcesFlow = MutableStateFlow(listOf(m1))
        val bookingsRepository = mock<BookingsRepository> {
            on { observeResources() } doReturn resourcesFlow
            on { fetchResources() } doReturn Result.success(Unit)
        }

        // WHEN
        val vm = createViewModel(
            bookingsRepository = bookingsRepository,
            selectionMode = BookingTeamMemberFilterViewModel.SelectionMode.SINGLE,
        )

        // THEN
        val members = vm.uiState.value?.teamMembers
        assertThat(members).doesNotContain(TeamMember.any)
        assertThat(members).containsExactly(m1)
    }

    @Test
    fun `given filterByProductId, when resources loaded, then only matching members are shown`() = testBlocking {
        // GIVEN
        val productId = 100L
        val matchingMember = member(1, productBookingIds = listOf(productId, 200L))
        val nonMatchingMember = member(2, productBookingIds = listOf(200L))
        val nullProductIdsMember = member(3, productBookingIds = null)
        val resourcesFlow = MutableStateFlow(
            listOf(matchingMember, nonMatchingMember, nullProductIdsMember)
        )
        val bookingsRepository = mock<BookingsRepository> {
            on { observeResources() } doReturn resourcesFlow
            on { fetchResources() } doReturn Result.success(Unit)
        }

        // WHEN
        val vm = createViewModel(
            bookingsRepository = bookingsRepository,
            filterByProductId = productId,
            selectionMode = BookingTeamMemberFilterViewModel.SelectionMode.SINGLE,
        )

        // THEN
        val members = vm.uiState.value?.teamMembers
        assertThat(members).containsExactly(matchingMember)
    }

    @Test
    fun `given no filterByProductId, when resources loaded, then all members are shown`() = testBlocking {
        // GIVEN
        val m1 = member(1, productBookingIds = listOf(100L))
        val m2 = member(2, productBookingIds = null)
        val resourcesFlow = MutableStateFlow(listOf(m1, m2))
        val bookingsRepository = mock<BookingsRepository> {
            on { observeResources() } doReturn resourcesFlow
            on { fetchResources() } doReturn Result.success(Unit)
        }

        // WHEN
        val vm = createViewModel(
            bookingsRepository = bookingsRepository,
            filterByProductId = null,
            selectionMode = BookingTeamMemberFilterViewModel.SelectionMode.SINGLE,
        )

        // THEN
        val members = vm.uiState.value?.teamMembers
        assertThat(members).containsExactly(m1, m2)
    }
}
