package com.woocommerce.android.ui.dashboard.inbox

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.model.DashboardWidget.Type.INBOX
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetAction
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetMenu
import com.woocommerce.android.ui.dashboard.DashboardViewModel.RefreshEvent
import com.woocommerce.android.ui.dashboard.defaultHideMenuEntry
import com.woocommerce.android.ui.dashboard.inbox.DashboardInboxViewModel.Factory
import com.woocommerce.android.ui.inbox.InboxNoteActionEvent
import com.woocommerce.android.ui.inbox.InboxNoteUi
import com.woocommerce.android.ui.inbox.domain.InboxNote
import com.woocommerce.android.ui.inbox.domain.InboxRepository
import com.woocommerce.android.ui.inbox.toInboxNoteUi
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
@HiltViewModel(assistedFactory = Factory::class)
class DashboardInboxViewModel @AssistedInject constructor(
    private val resourceProvider: ResourceProvider,
    private val dateUtils: DateUtils,
    private val inboxRepository: InboxRepository,
    savedStateHandle: SavedStateHandle,
    @Assisted private val parentViewModel: DashboardViewModel,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
) : ScopedViewModel(savedStateHandle) {
    companion object {
        const val MAX_NUMBER_OF_NOTES_TO_DISPLAY_IN_CARD = 3
    }

    private val _refreshTrigger = MutableSharedFlow<RefreshEvent>(extraBufferCapacity = 1)
    private val refreshTrigger = merge(parentViewModel.refreshTrigger, _refreshTrigger)
        .onStart { emit(RefreshEvent()) }

    val menu = DashboardWidgetMenu(
        items = listOf(
            DashboardWidget.Type.INBOX.defaultHideMenuEntry {
                parentViewModel.onHideWidgetClicked(INBOX)
            }
        )
    )

    val button = DashboardWidgetAction(
        titleResource = R.string.dashboard_action_view_all_messages,
        action = ::onNavigateToInbox
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val viewState = refreshTrigger
        .transformLatest {
            val shouldFetchFirst = it.isForced || inboxRepository.observeInboxNotes().first().isEmpty()
            if (shouldFetchFirst) {
                emit(ViewState.Loading)
            }

            emitAll(
                observeMostRecentNotes(shouldFetchFirst)
                    .map { result ->
                        result.fold(
                            onSuccess = { notes ->
                                ViewState.Content(
                                    notes
                                        .take(MAX_NUMBER_OF_NOTES_TO_DISPLAY_IN_CARD)
                                        .map { note ->
                                            note.toInboxNoteUi(
                                                dateUtils,
                                                resourceProvider,
                                                ::onNoteAction
                                            ) { _, noteId -> onNoteDismissed(noteId) }
                                        }
                                )
                            },
                            onFailure = { ViewState.Error }
                        )
                    }
            )
        }
        .asLiveData()

    private fun observeMostRecentNotes(shouldFetchFirst: Boolean) = channelFlow<Result<List<InboxNote>>> {
        if (shouldFetchFirst) {
            inboxRepository.fetchInboxNotes()
                .onFailure {
                    send(Result.failure(it))
                    return@channelFlow
                }
        }

        coroutineScope {
            val notesJob = launch {
                inboxRepository.observeInboxNotes()
                    .collect { notes ->
                        send(Result.success(notes))
                    }
            }

            if (!shouldFetchFirst) {
                inboxRepository.fetchInboxNotes()
                    .onFailure {
                        notesJob.cancel()
                        send(Result.failure(it))
                    }
            }
        }
    }

    private fun onNoteDismissed(noteId: Long) {
        parentViewModel.trackCardInteracted(INBOX.trackingIdentifier)
        viewModelScope.launch {
            inboxRepository.dismissNote(noteId)
                .onFailure { showSyncError() }
        }
    }

    private fun onNoteAction(actionId: Long, noteId: Long) {
        parentViewModel.trackCardInteracted(INBOX.trackingIdentifier)
        val clickedNote = (viewState.value as? ViewState.Content)?.notes?.firstOrNull { noteId == it.id }
        clickedNote?.let {
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

    private fun showSyncError() {
        triggerEvent(Event.ShowSnackbar(R.string.inbox_screen_sync_error))
    }

    private fun onNavigateToInbox() {
        parentViewModel.trackCardInteracted(INBOX.trackingIdentifier)
        triggerEvent(NavigateToInbox)
    }

    fun onRefresh() {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.DYNAMIC_DASHBOARD_CARD_RETRY_TAPPED,
            mapOf(
                AnalyticsTracker.KEY_TYPE to DashboardWidget.Type.INBOX.trackingIdentifier
            )
        )
        _refreshTrigger.tryEmit(RefreshEvent(isForced = true))
    }

    sealed class ViewState {
        data object Loading : ViewState()
        data object Error : ViewState()
        data class Content(val notes: List<InboxNoteUi>) : ViewState()

        @StringRes val title: Int = INBOX.titleResource
    }

    @AssistedFactory
    interface Factory {
        fun create(parentViewModel: DashboardViewModel): DashboardInboxViewModel
    }

    data object NavigateToInbox : Event()
}
