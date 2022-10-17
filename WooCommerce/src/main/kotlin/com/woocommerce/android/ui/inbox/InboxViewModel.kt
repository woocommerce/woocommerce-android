package com.woocommerce.android.ui.inbox

import androidx.annotation.ColorRes
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
import com.woocommerce.android.ui.inbox.domain.InboxNote
import com.woocommerce.android.ui.inbox.domain.InboxNote.NoteType.SURVEY
import com.woocommerce.android.ui.inbox.domain.InboxNote.Status.ACTIONED
import com.woocommerce.android.ui.inbox.domain.InboxNoteAction
import com.woocommerce.android.ui.inbox.domain.InboxRepository
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.wordpress.android.util.DateTimeUtils
import java.util.Date
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
                val notes = inboxNotes.map { it.toInboxNoteUi() }
                InboxState(
                    isLoading = false,
                    notes = notes,
                    onRefresh = ::onSwipeToRefresh,
                    isRefreshing = false
                )
            }

    private fun InboxNote.toInboxNoteUi() =
        InboxNoteUi(
            id = id,
            title = title,
            description = description,
            dateCreated = formatNoteCreationDate(dateCreated),
            isSurvey = type == SURVEY,
            isActioned = status == ACTIONED,
            actions = mapInboxActionsToUi(),
        )

    private fun InboxNote.mapInboxActionsToUi(): List<InboxNoteActionUi> {
        if (type == SURVEY && status == ACTIONED) {
            return emptyList()
        }

        val noteActionsUi = actions
            .map { it.toInboxActionUi(id) }
            .toMutableList()

        addDismissActionIfMissing(noteActionsUi)

        return noteActionsUi
    }

    private fun InboxNote.addDismissActionIfMissing(noteActionsUi: MutableList<InboxNoteActionUi>) {
        if (!actionsHaveDismiss(noteActionsUi)) {
            noteActionsUi.add(
                InboxNoteActionUi(
                    id = 0,
                    parentNoteId = id,
                    label = DEFAULT_DISMISS_LABEL,
                    textColor = R.color.color_surface_variant,
                    url = "",
                    onClick = { actionId, noteId -> dismissNote(actionId, noteId) }
                )
            )
        }
    }

    private fun actionsHaveDismiss(noteActionsUi: List<InboxNoteActionUi>) =
        noteActionsUi.any { it.label == DEFAULT_DISMISS_LABEL }

    private fun InboxNoteAction.toInboxActionUi(parentNoteId: Long) =
        InboxNoteActionUi(
            id = id,
            parentNoteId = parentNoteId,
            label = label,
            textColor = getActionTextColor(),
            url = url,
            onClick = ::handleInboxNoteAction
        )

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
            updateDismissingActionState(noteId, actionId, isDimissing = true)
            inboxRepository
                .dismissNote(noteId)
                .fold(
                    onFailure = { showSyncError() },
                    onSuccess = {}
                )
            updateDismissingActionState(noteId, actionId, isDimissing = false)
        }
    }

    private fun updateDismissingActionState(noteId: Long, actionId: Long, isDimissing: Boolean) {
        _inboxState.value = _inboxState.value?.copy(
            notes = _inboxState.value?.notes?.map { note ->
                if (note.id == noteId) {
                    val updatedActions =
                        note.actions.map { action ->
                            if (action.id == actionId) {
                                action.copy(isDismissing = isDimissing)
                            } else {
                                action
                            }
                        }
                    note.copy(actions = updatedActions)
                } else note
            } ?: emptyList()
        )
    }

    private fun showSyncError() {
        triggerEvent(Event.ShowSnackbar(R.string.inbox_screen_sync_error))
    }

    private fun InboxNoteAction.getActionTextColor() =
        if (isPrimary) R.color.color_secondary
        else R.color.color_surface_variant

    @SuppressWarnings("MagicNumber", "ReturnCount")
    private fun formatNoteCreationDate(createdDate: String): String {
        val creationDate = dateUtils.getDateFromIso8601String(createdDate)
        val now = Date()

        val minutes = DateTimeUtils.minutesBetween(now, creationDate)
        when {
            minutes < 1 -> return resourceProvider.getString(R.string.inbox_note_recency_now)
            minutes < 60 -> return resourceProvider.getString(R.string.inbox_note_recency_minutes, minutes)
        }
        val hours = DateTimeUtils.hoursBetween(now, creationDate)
        when {
            hours == 1 -> return resourceProvider.getString(R.string.inbox_note_recency_one_hour)
            hours < 24 -> return resourceProvider.getString(R.string.inbox_note_recency_hours, hours)
        }
        val days = DateTimeUtils.daysBetween(now, creationDate)
        when {
            days == 1 -> return resourceProvider.getString(R.string.inbox_note_recency_one_day)
            days < 30 -> return resourceProvider.getString(R.string.inbox_note_recency_days, days)
        }
        return resourceProvider.getString(
            R.string.inbox_note_recency_date_time,
            dateUtils.toDisplayMMMddYYYYDate(creationDate?.time ?: 0) ?: ""
        )
    }

    data class InboxState(
        val isLoading: Boolean = false,
        val notes: List<InboxNoteUi> = emptyList(),
        val onRefresh: () -> Unit = {},
        val isRefreshing: Boolean = false
    )

    data class InboxNoteUi(
        val id: Long,
        val title: String,
        val description: String,
        val dateCreated: String,
        val isSurvey: Boolean,
        val isActioned: Boolean,
        val actions: List<InboxNoteActionUi>
    )

    data class InboxNoteActionUi(
        val id: Long,
        val parentNoteId: Long,
        val label: String,
        @ColorRes val textColor: Int,
        val url: String,
        val isDismissing: Boolean = false,
        val onClick: (Long, Long) -> Unit
    )

    sealed class InboxNoteActionEvent : Event() {
        data class OpenUrlEvent(val url: String) : InboxNoteActionEvent()
    }
}
