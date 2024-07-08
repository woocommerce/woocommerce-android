package com.woocommerce.android.ui.inbox

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.INBOX_NOTES_LOADED
import com.woocommerce.android.analytics.AnalyticsEvent.INBOX_NOTES_LOAD_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.INBOX_NOTE_ACTION
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR_CONTEXT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR_DESC
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR_TYPE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_INBOX_NOTE_ACTION
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_IS_LOADING_MORE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_INBOX_NOTE_ACTION_DISMISS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_INBOX_NOTE_ACTION_DISMISS_ALL
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_INBOX_NOTE_ACTION_OPEN
import com.woocommerce.android.ui.inbox.domain.InboxRepository
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val dateUtils: DateUtils,
    private val inboxRepository: InboxRepository,
    savedState: SavedStateHandle,
) : ScopedViewModel(savedState) {
    companion object {
        const val DEFAULT_DISMISS_LABEL = "Dismiss" // Inbox notes are not localised and always displayed in English
    }

    private val _inboxState = MutableLiveData<InboxState>()
    val inboxState: LiveData<InboxState> = _inboxState

    init {
        _inboxState.value = InboxState(isLoading = true)
        viewModelScope.launch {
            refreshNotes()
            inboxNotesLocalUpdates().collectLatest { _inboxState.value = it }
        }
    }

    private fun trackInboxNotesLoaded(result: Result<Unit>, isLoadingMore: Boolean = false) {
        result.fold(
            onSuccess = {
                AnalyticsTracker.track(
                    INBOX_NOTES_LOADED,
                    mapOf(KEY_IS_LOADING_MORE to isLoadingMore.toString())
                )
            },
            onFailure = {
                AnalyticsTracker.track(
                    INBOX_NOTES_LOAD_FAILED,
                    mapOf(
                        KEY_ERROR_CONTEXT to it::class.java.simpleName,
                        KEY_ERROR_TYPE to it,
                        KEY_ERROR_DESC to it.message
                    )
                )
            }
        )
    }

    private fun trackInboxNoteActionClicked(actionValue: String) {
        AnalyticsTracker.track(
            INBOX_NOTE_ACTION,
            mapOf(KEY_INBOX_NOTE_ACTION to actionValue)
        )
    }

    fun dismissAllNotes() {
        trackInboxNoteActionClicked(VALUE_INBOX_NOTE_ACTION_DISMISS_ALL)
        viewModelScope.launch {
            _inboxState.value = _inboxState.value?.copy(isLoading = true)
            inboxRepository
                .dismissAllNotesForCurrentSite()
                .fold(
                    onFailure = { showSyncError() },
                    onSuccess = {}
                )
            _inboxState.value = _inboxState.value?.copy(isLoading = false)
        }
    }

    private fun onSwipeToRefresh() {
        viewModelScope.launch {
            _inboxState.value = _inboxState.value?.copy(isRefreshing = true)
            inboxRepository
                .fetchInboxNotes()
                .fold(
                    onFailure = { showSyncError() },
                    onSuccess = {}
                )
            _inboxState.value = _inboxState.value?.copy(isRefreshing = false)
        }
    }

    private suspend fun refreshNotes() {
        val result = inboxRepository.fetchInboxNotes()
        trackInboxNotesLoaded(result)
        result.fold(
            onFailure = { showSyncError() },
            onSuccess = {}
        )
    }

    private fun inboxNotesLocalUpdates() =
        inboxRepository.observeInboxNotes()
            .map { inboxNotes ->
                val notes = inboxNotes.map {
                    it.toInboxNoteUi(
                        dateUtils,
                        resourceProvider,
                        ::handleInboxNoteAction,
                        ::dismissNote
                    )
                }
                InboxState(
                    isLoading = false,
                    notes = notes,
                    onRefresh = ::onSwipeToRefresh,
                    isRefreshing = false
                )
            }

    private fun handleInboxNoteAction(actionId: Long, noteId: Long) {
        val clickedNote = inboxState.value?.notes?.firstOrNull { noteId == it.id }
        clickedNote?.let {
            trackInboxNoteActionClicked(VALUE_INBOX_NOTE_ACTION_OPEN)
            when {
                it.isSurvey -> markSurveyAsAnswered(clickedNote.id, actionId)
                else -> openActionUrl(clickedNote, actionId)
            }
        }
    }

    private fun openActionUrl(clickedNote: InboxNoteUi, actionId: Long) {
        val clickedAction = clickedNote.actions.firstOrNull { actionId == it.id }
        clickedAction?.let {
            if (it.url.isNotEmpty()) {
                viewModelScope.launch {
                    inboxRepository.markInboxNoteAsActioned(clickedNote.id, actionId)
                }
                triggerEvent(InboxNoteActionEvent.OpenUrlEvent(it.url))
            }
        }
    }

    private fun markSurveyAsAnswered(noteId: Long, actionId: Long) {
        viewModelScope.launch {
            inboxRepository.markInboxNoteAsActioned(noteId, actionId)
        }
    }

    private fun dismissNote(actionId: Long, noteId: Long) {
        trackInboxNoteActionClicked(VALUE_INBOX_NOTE_ACTION_DISMISS)
        viewModelScope.launch {
            updateDismissingActionState(noteId, actionId, isDismissing = true)
            inboxRepository
                .dismissNote(noteId)
                .fold(
                    onFailure = { showSyncError() },
                    onSuccess = {}
                )
            updateDismissingActionState(noteId, actionId, isDismissing = false)
        }
    }

    private fun updateDismissingActionState(noteId: Long, actionId: Long, isDismissing: Boolean) {
        _inboxState.value = _inboxState.value?.copy(
            notes = _inboxState.value?.notes?.map { note ->
                if (note.id == noteId) {
                    val updatedActions =
                        note.actions.map { action ->
                            if (action.id == actionId) {
                                action.copy(isDismissing = isDismissing)
                            } else {
                                action
                            }
                        }
                    note.copy(actions = updatedActions)
                } else {
                    note
                }
            } ?: emptyList()
        )
    }

    private fun showSyncError() {
        triggerEvent(Event.ShowSnackbar(R.string.inbox_screen_sync_error))
    }

    data class InboxState(
        val isLoading: Boolean = false,
        val notes: List<InboxNoteUi> = emptyList(),
        val onRefresh: () -> Unit = {},
        val isRefreshing: Boolean = false
    )
}
