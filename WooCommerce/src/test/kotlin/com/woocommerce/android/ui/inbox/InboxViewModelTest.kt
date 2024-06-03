package com.woocommerce.android.ui.inbox

import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.inbox.InboxViewModel.Companion.DEFAULT_DISMISS_LABEL
import com.woocommerce.android.ui.inbox.InboxViewModel.InboxState
import com.woocommerce.android.ui.inbox.domain.InboxNote
import com.woocommerce.android.ui.inbox.domain.InboxRepository
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class InboxViewModelTest : BaseUnitTest() {
    private val savedState: SavedStateHandle = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val dateUtils: DateUtils = mock()
    private val inboxRepository: InboxRepository = mock()

    private lateinit var viewModel: InboxViewModel
    private var observer: Observer<InboxState> = mock()
    private val captor = argumentCaptor<InboxState>()

    @Before
    fun setup() = testBlocking {
        givenFetchInboxNotesReturns(Result.success(Unit))
        givenObserveNotesEmits(emptyList())
        givenStringIsReturnedForStringId()
    }

    @Test
    fun `when view model is created, inbox notes are fetched`() = testBlocking {
        whenViewModelIsCreated()

        verify(inboxRepository).fetchInboxNotes()
    }

    @Test
    fun `when view model is created, subscribe to inbox note changes`() = testBlocking {
        whenViewModelIsCreated()

        verify(inboxRepository).observeInboxNotes()
    }

    @Test
    fun `given notes fetched successfully, when view model is created, inbox state has notesUi`() =
        testBlocking {
            givenObserveNotesEmits(listOf(NOTE))

            whenViewModelIsCreated()

            assertThat(viewModel.inboxState.value?.notes).isNotEmpty
        }

    @Test
    fun `given notes fetched successfully, when view model is created, ensure all notes have dismiss action`() =
        testBlocking {
            givenObserveNotesEmits(listOf(NOTE))

            whenViewModelIsCreated()

            viewModel.inboxState.value?.notes!!.forEach {
                assertThat(it.actions.last().label).isEqualTo(DEFAULT_DISMISS_LABEL)
            }
        }

    @Test
    fun `given inbox notes loaded, when refresh notes, show is refreshing state and fetch notes`() =
        testBlocking {
            whenViewModelIsCreated()
            givenObserveNotesEmits(listOf(NOTE))
            reset(inboxRepository)

            viewModel.inboxState.observeForever(observer)
            viewModel.inboxState.value?.onRefresh?.invoke()

            verify(observer, Mockito.times(3)).onChanged(captor.capture())
            assertThat(captor.allValues[1].isRefreshing).isTrue
            assertThat(captor.allValues.last().isRefreshing).isFalse
            verify(inboxRepository).fetchInboxNotes()
        }

    private suspend fun givenFetchInboxNotesReturns(result: Result<Unit>) {
        whenever(inboxRepository.fetchInboxNotes()).thenReturn(result)
    }

    private suspend fun givenObserveNotesEmits(notes: List<InboxNote>) {
        whenever(inboxRepository.observeInboxNotes())
            .thenReturn(
                flow { emit(notes) }
            )
    }

    private fun whenViewModelIsCreated() {
        viewModel = InboxViewModel(
            resourceProvider,
            dateUtils,
            inboxRepository,
            savedState
        )
    }

    private fun givenStringIsReturnedForStringId(string: String = "") {
        whenever(resourceProvider.getString(any())).thenReturn(string)
    }

    private companion object {
        val NOTE_ACTION = com.woocommerce.android.ui.inbox.domain.InboxNoteAction(
            id = 3,
            label = "",
            isPrimary = true,
            url = "",
        )
        val NOTE = InboxNote(
            id = 2,
            title = "",
            description = "",
            dateCreated = "",
            status = InboxNote.Status.UNACTIONED,
            type = InboxNote.NoteType.INFO,
            actions = listOf(NOTE_ACTION)
        )
        val DEFAULT_DISMISS_ACTION_UI =
            InboxNoteActionUi(
                id = 0,
                parentNoteId = 2,
                label = DEFAULT_DISMISS_LABEL,
                textColor = R.color.color_surface_variant,
                onClick = { _, _ -> },
                url = ""
            )
    }
}
